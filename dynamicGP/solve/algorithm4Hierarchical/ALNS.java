package org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical;

import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.operator.DeleteAndInsert;
import org.lileischeduler.dynamicGP.solve.operator.Evaluate;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class ALNS {
    private static double[] deleteFitness = new double[]{1, 1, 1, 1, 1, 1, 1}; // 适应度数组，包含7个删除动作的适应度
    private static double[] insertFitness = new double[]{1, 1, 1, 1}; // 适应度数组，包含4个插入动作的适应度

    /*********************** ALNS: 自适应大邻域搜索 ***********************/
    public static void AdaptiveLargeNeighborSearch(Satellite satellite, int deleteNum, int iteration) {
        List<Target> finishedTargets = satellite.scheduledTargets;
        List<Target> leftTargets = new ArrayList<>(satellite.currentTargets);

        for (int i = finishedTargets.size() - 1; i >= 0; i--) {
            Target target = finishedTargets.get(i);
            if (target.priority == 0) finishedTargets.remove(i);
        }
        for (Target target : finishedTargets) target.reSet();
        leftTargets.addAll(finishedTargets);
        finishedTargets.clear();


        if (leftTargets.isEmpty() && finishedTargets.isEmpty()) return;
        if (finishedTargets.isEmpty()) new Insert().insertVirtualTarget(finishedTargets);
        // 初始化：尝试插入left中所有的目标
        Collections.shuffle(leftTargets, random);
        new Insert().insert(finishedTargets, leftTargets);
        double bestScore = getScore(finishedTargets);
        List<Target> bestInsert = new ArrayList<>(finishedTargets);
        List<Target> bestLeft = new ArrayList<>(leftTargets);
        // 默认finishedTarget第一个和最后一个为虚拟任务 Z
        if (deleteNum > (finishedTargets.size() - 2) / 2) deleteNum = (finishedTargets.size() - 2) / 2;
        for (int i = 0; i < iteration; i++) {
            if (leftTargets.isEmpty()) break;
            // 删除指定的任务(选择一个动作进行删除) 7个删除  4个插入
            int delete = rouletteSelect(deleteFitness);
            int insert = rouletteSelect(insertFitness);
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
                deleteFitness[delete] += 0.1;
                insertFitness[insert] += 0.1;
            } else {
                deleteFitness[delete] = Math.max(1, deleteFitness[delete] - 0.05);
                insertFitness[insert] = Math.max(1, insertFitness[insert] - 0.05);
                reSetBestSolution(finishedTargets, leftTargets, bestInsert, bestLeft);
            }
        }
        reSetBestSolution(finishedTargets, leftTargets, bestInsert, bestLeft);
        satellite.currentTargets.clear();
        satellite.currentTargets.addAll(leftTargets);
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

