package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class SA {
    /**
     * 算法
     * @param finished      上一代已完成的任务
     * @param left          上一代剩余的任务
     * @param targets       待规划任务(包含上一代剩余和本次新到)
     * @param iteration     (迭代次数)
     */
    public static void run(List<Target> finished, List<Target> left, List<Target> targets, int iteration) {
        // 初始化参数
        double initialTemperature = 1000.0;   // 初始温度
        double coolingRate = 0.95;            // 冷却率
        double finalTemperature = 1.0;        // 终止温度
        double innerLoop = 5;                 // 内循环

        // 初始化一个随机顺序的路径作为当前解决方案
        List<Target> currentSolution = new ArrayList<>(targets);

        Collections.shuffle(currentSolution, random);

        double currentEvaluation = evaluateIndividuals(currentSolution);
        double bestEvaluation = currentEvaluation;
        List<Target> bestSolution = new ArrayList<>(currentSolution);

        // 模拟退火主循环
        double temperature = initialTemperature;
        List<Target> neighborSolution = new ArrayList<>();
        for (int tt = 0; tt < iteration || temperature > finalTemperature; tt++) {
            // 邻域搜索小循环
            for (int ss = 0; ss < innerLoop; ss++) {
                neighborSolution.clear();
                neighborSolution.addAll(currentSolution);
                switch (random.nextInt(3)) {
                    case 0:
                        NeighborOperator.singleSwap(neighborSolution);
                        break;
                    case 1:
                        NeighborOperator.twoPointsReverse(neighborSolution);
                        break;
                    case 2:
                        NeighborOperator.insertOperator(neighborSolution);
                        break;
                }
                double neighborEvaluation = evaluateIndividuals(neighborSolution);                       // 计算邻域解的评估值
                double deltaEvaluation = neighborEvaluation - currentEvaluation;                         // 计算评估值的变化
                if (deltaEvaluation > 0 || Math.exp(deltaEvaluation / temperature) > random.nextDouble()) {    // 决策是否接受新解
                    currentSolution.clear();
                    currentSolution.addAll(neighborSolution);
                    currentEvaluation = neighborEvaluation;
                    if (currentEvaluation > bestEvaluation) {                                            // 更新全局最优解
                        bestEvaluation = currentEvaluation;
                        bestSolution.clear();
                        bestSolution.addAll(currentSolution);
                    }
                }
            }
            temperature *= coolingRate;                                                                  // 降温
        }
        new Insert().decodeTarget(bestSolution, finished, left);
    }
    /***************** 评估函数 ******************/
    private static double evaluateIndividuals(List<Target> targets) {
        // 适应度评估
        return new Insert().insertEarliest(targets);
    }
}
