package org.lileischeduler.dynamicGP.solve.algorithm.rl;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.DeleteAndInsert;
import org.lileischeduler.dynamic.solve.operator.Evaluate;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamicGP.solve.algorithm.Qtable;

import java.util.ArrayList;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class RLALNS {
    // 状态：改进 未改1  未改2  未改3  未改4 未改5以上  动作删除7*插入4
    private static Qtable qtable = new Qtable(6, 4 * 7);
    private static List<List<Integer>> actions = initialActions();
        /*********************** RL-ALNS: 自适应大邻域搜索 ***********************/
    public static void RLAdaptiveLargeNeighborSearch(List<Target> finishedTargets, List<Target> leftTargets, int deleteNum) {
        if (leftTargets.isEmpty()) return;
        if (finishedTargets.isEmpty()) new Insert().insertVirtualTarget(finishedTargets);
        // 初始化：尝试插入left中所有的目标
        new Insert().insert(finishedTargets, leftTargets);
        double bestScore = getScore(finishedTargets);
        List<Target> bestInsert = new ArrayList<>(finishedTargets);
        List<Target> bestLeft = new ArrayList<>(leftTargets);
        System.out.println("初始解：" + bestScore + " 剩余任务：" + leftTargets.size());
        int notImproved = 0;int state = 0;
        // 默认finishedTarget第一个和最后一个为虚拟任务 Z
        if (deleteNum > (finishedTargets.size() - 2) / 2) deleteNum = (finishedTargets.size() - 2) / 2;
        for (int i = 0; i < 100; i++) {
            if (leftTargets.isEmpty()) break;
            // 删除指定的任务(选择一个动作进行删除) 7个删除  4个插入
            state = Math.min(4, notImproved);
            List<Integer> action = actions.get(state);
            int delete = action.get(0);
            int insert = action.get(1);
            DeleteAndInsert.delete(finishedTargets, leftTargets, deleteNum, delete);
            DeleteAndInsert.insert(leftTargets, insert);
            // 顶前插入和后向松弛
            new Insert().deleteInsertForwards(1, finishedTargets);
            new Insert().deleteSlackAfterwards(finishedTargets.size() - 2, finishedTargets);
            new Insert().insert(finishedTargets, leftTargets);
            double score = Evaluate.evaluate(finishedTargets, null, "优先级");
            if (score > bestScore) {
                bestScore = score;
                bestInsert.clear();
                bestInsert.addAll(finishedTargets);
                bestLeft.clear();
                bestLeft.addAll(leftTargets);
                qtable.update(state, action.indexOf(action), 0);
                System.out.println("改进解：" + bestScore + " 剩余任务：" + leftTargets.size());
            } else {
                qtable.update(state, action.indexOf(action), 2);
                reSetBestSolution(finishedTargets, leftTargets, bestInsert, bestLeft);
            }
        }
        reSetBestSolution(finishedTargets, leftTargets, bestInsert, bestLeft);
//        Insert.checkFinished(finishedTargets);
//        System.out.println("最优解序列解码适应度为：" + bestScore0);
    }


    /**
     * 初始化动作空间
     */
    public static List<List<Integer>> initialActions() {
        List<List<Integer>> actions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            List<Integer> a1 = new ArrayList<>();
            a1.add(i);
            for (int j = 0; j < 4; j++) {
                a1.add(j);
            }
            actions.add(a1);
        }
        return actions;
    }

    private static void reSetBestSolution(List<Target> finishedTargets, List<Target> leftTargets, List<Target> bestInsert, List<Target> bestLeft) {
        // 包含顶前插入过程
        new Insert().firstInsert(bestInsert);
        // 添加后向松弛
        new Insert().deleteSlackAfterwards(bestInsert.size() - 2, bestInsert);
        finishedTargets.clear();
        finishedTargets.addAll(bestInsert);
        leftTargets.clear();
        leftTargets.addAll(bestLeft);
    }


    public static double getScore(List<Target> finishedTargets) {
        double bestScore = 0.0;
        for (Target target : finishedTargets) {
            bestScore += target.getPriority();
        }
        return bestScore;
    }

    // 轮盘赌选择删除动作
    private static int rouletteSelect(double[] fitnessList) {
        double totalFitness = 0;
        for (double fitness : fitnessList) {
            totalFitness += fitness;
        }
        double randomValue = random.nextDouble() * totalFitness; // 从0到总适应度的随机值
        for (int i = 0; i < fitnessList.length; i++) {
            randomValue -= fitnessList[i]; // 减去当前动作的适应度
            if (randomValue <= 0) {
                return i; // 返回对应的动作
            }
        }
        return 0;
    }

}
