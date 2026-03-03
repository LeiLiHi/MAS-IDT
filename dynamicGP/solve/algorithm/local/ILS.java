package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class ILS {
    /******** ILS(Iterated Local Search):迭代局部搜索算法 ****/
    public static void run(List<Target> finished, List<Target>left, List<Target> targets, int maxIterations) {
        // 初始化当前解和最佳解
        List<Target> bestSolution = new ArrayList<>(targets);
        Collections.shuffle(bestSolution, random); // 随机打乱以生成初始解
        double bestEvaluation = evaluateIndividuals(bestSolution);
        List<Target> currentSolution = new ArrayList<>(bestSolution);
        int iteration = 0;
        List<Target> localOptimum = new ArrayList<>();
        List<Target> perturbedSolution = new ArrayList<>();
        List<Target> newLocalOptimum = new ArrayList<>();
        while (iteration < maxIterations) {
            // 从当前解进行局部搜索以找到局部最优解s1
            localOptimum.clear();
            localSearch(currentSolution, localOptimum);
            // 对局部最优解s1进行扰动，获得新的解s2
            perturbedSolution.clear();
            perturb(localOptimum, perturbedSolution);
            // 从新解s2进行局部搜索，找到局部最优解s3
            newLocalOptimum.clear();
            localSearch(perturbedSolution, newLocalOptimum);
            double newEvaluation = evaluateIndividuals(newLocalOptimum);
            // 基于判断策略选择是否接受s3
            if (newEvaluation > bestEvaluation) {
                bestEvaluation = newEvaluation;
                bestSolution.clear();
                bestSolution.addAll(newLocalOptimum);
            }
            // 将新的局部最优解作为当前解
            currentSolution.clear();
            currentSolution.addAll(bestSolution);
            iteration++;
        }
        // 解码当前的最佳方案
        new Insert().decodeTarget(bestSolution, finished, left);
    }

    // 局部搜索方法示例
    private static void localSearch(List<Target> solution, List<Target> optimalSolution) {
        optimalSolution.addAll(solution);
        double bestLocalEvaluation = evaluateIndividuals(optimalSolution);

        // 在当前解上进行一些邻域搜索
        List<Target> neighborSolution = new ArrayList<>();
        for (int i = 0; i < 20; i++) { // 控制邻域搜索的次数
            neighborSolution.clear();
            neighborSolution.addAll(optimalSolution);
            NeighborOperator.singleSwap(neighborSolution); // 生成邻域解
            double neighborEvaluation = evaluateIndividuals(neighborSolution);

            // 更新局部最优解
            if (neighborEvaluation > bestLocalEvaluation) {
                bestLocalEvaluation = neighborEvaluation;
                optimalSolution.clear();
                optimalSolution.addAll(neighborSolution);
            }
        }
    }

    // 扰动机制：对当前解施加一些随机改变
    private static List<Target> perturb(List<Target> solution, List<Target> perturbedSolution) {
        perturbedSolution.addAll(solution);
        if (solution.size() <= 3) {
            return perturbedSolution;
        }

        // 随机选择两个不同的索引进行交换
        int index1 = random.nextInt(perturbedSolution.size());
        int index2 = random.nextInt(perturbedSolution.size());
        while (index1 == index2) {
            index2 = random.nextInt(perturbedSolution.size());
        }
        // 交换两个元素以生成新的解
        Collections.swap(perturbedSolution, index1, index2);
        return perturbedSolution;
    }

    /***************** 评估函数 ******************/
    private static double evaluateIndividuals(List<Target> targets) {
        // 适应度评估
        return new Insert().insertEarliest(targets);
    }
}
