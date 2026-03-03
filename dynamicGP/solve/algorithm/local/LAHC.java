package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class LAHC {
    /********************* LAHC(Late Acceptance Hill Climbing): 逾期接受爬山算法 *******************/
    public static void run(List<Target> finished, List<Target> left, List<Target> targets, int iteration, int historyLength) {
        // 初始化一个随机顺序的路径作为当前解决方案
        List<Target> currentSolution = new ArrayList<>(targets);
        Collections.shuffle(currentSolution, random); // 随机打乱

        // 初始适应度评估
        double currentEvaluation = evaluateIndividuals(currentSolution);
        double bestEvaluation = currentEvaluation;
        List<Target> bestSolution = new ArrayList<>(currentSolution);

        // 初始化历史适应度列表
        List<Double> fitnessHistory = new ArrayList<>(historyLength);
        for (int i = 0; i < historyLength; i++) {
            fitnessHistory.add(currentEvaluation);
        }
        List<Target> neighborSolution = new ArrayList<>();
        // LAHC主循环
        for (int tt = 0; tt < iteration; tt++) {
            // 创建邻域解
            neighborSolution.clear();
            neighborSolution.addAll(currentSolution);
            // 多种邻域搜索策略
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
            // 评估邻域解
            double neighborEvaluation = evaluateIndividuals(neighborSolution);
            // 延迟接受判断
            int historyIndex = tt % historyLength;
            double acceptanceThreshold = fitnessHistory.get(historyIndex);
            // 接受准则
            if (neighborEvaluation > currentEvaluation || neighborEvaluation > acceptanceThreshold) {
                // 接受新解
                currentSolution.clear();
                currentSolution.addAll(neighborSolution);
                currentEvaluation = neighborEvaluation;

                // 更新最优解
                if (currentEvaluation > bestEvaluation) {
                    bestEvaluation = currentEvaluation;
                    bestSolution.clear();
                    bestSolution.addAll(currentSolution);
                }
            }

            // 更新历史适应度列表
            fitnessHistory.set(historyIndex, currentEvaluation);
        }
        new Insert().decodeTarget(bestSolution, finished, left);
    }
    /***************** 评估函数 ******************/
    private static double evaluateIndividuals(List<Target> targets) {
        // 适应度评估
        return new Insert().insertEarliest(targets);
    }
}
