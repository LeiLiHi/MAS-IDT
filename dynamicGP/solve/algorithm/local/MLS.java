package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.lileischeduler.tool.Tool.random;

public class MLS {
    /******** MLS(Multi-start Local Search):多起点局部搜索算法 ****/
    public static void run(List<Target> finished, List<Target> left, List<Target> targets, int maxStarts, int localSearchIterations) {
        // 初始化全局最佳解
        List<Target> bestGlobalSolution = null;
        double bestGlobalEvaluation = Double.NEGATIVE_INFINITY;
        List<Target> initialSolution = new ArrayList<>();
        List<Target> localOptimum = new ArrayList<>();
        for (int start = 0; start < maxStarts; start++) {
            // 随机生成一个初始解
            initialSolution.clear();
            initialSolution.addAll(targets);
            Collections.shuffle(initialSolution, random); // 随机打乱
            // 对当前解进行局部搜索
            localOptimum.clear();
            localSearch(initialSolution, localSearchIterations, localOptimum);
            // 评价当前局部最优解
            double localEvaluation = evaluateIndividuals(localOptimum);
            // 更新全局最优解
            if (localEvaluation > bestGlobalEvaluation) {
                bestGlobalEvaluation = localEvaluation;
                bestGlobalSolution = new ArrayList<>(localOptimum);
            }
        }
        assert bestGlobalSolution != null;
        // 输出最终全局最优解
        new Insert().decodeTarget(bestGlobalSolution, finished, left);
    }

    /**
     * target是实例对象，并行计算过程中会出现问题
     * @param targets               带规划目标
     * @param maxStarts             最大起点数量
     * @param localSearchIterations 局部搜索次数
     */
    public static void parallelRun(List<Target> targets, int maxStarts, int localSearchIterations) {
        // 初始化全局最佳解
        List<Target> bestGlobalSolution = null;
        double bestGlobalEvaluation = Double.NEGATIVE_INFINITY;
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<LocalSearchResult>> futures = new ArrayList<>();
        for (int start = 0; start < maxStarts; start++) {
            // 随机生成一个初始解
            List<Target> initialSolution = new ArrayList<>(targets);
            Collections.shuffle(initialSolution, random); // 随机打乱
            // 提交局部搜索任务到线程池
            Future<LocalSearchResult> future = executorService.submit(new Callable<LocalSearchResult>() {
                @Override
                public LocalSearchResult call() {
                    // 局部搜索逻辑
                    List<Target> optimalSolution = new ArrayList<>(initialSolution);
                    double bestLocalEvaluation = evaluateIndividuals(optimalSolution);

                    for (int i = 0; i < localSearchIterations; i++) {
                        // 生成邻域解
                        List<Target> neighborSolution = new ArrayList<>(optimalSolution);
                        NeighborOperator.singleSwap(neighborSolution); // 生成邻域解
                        double neighborEvaluation = evaluateIndividuals(neighborSolution);

                        // 更新局部最优解
                        if (neighborEvaluation > bestLocalEvaluation) {
                            bestLocalEvaluation = neighborEvaluation;
                            optimalSolution = neighborSolution; // 更新局部最佳解
                        }
                    }
                    return new LocalSearchResult(optimalSolution, bestLocalEvaluation);
                }
            });
            futures.add(future);
        }
        // 等待所有任务完成并获取结果
        for (Future<LocalSearchResult> future : futures) {
            try {
                LocalSearchResult result = future.get();
                double localEvaluation = result.evaluation;
                List<Target> localOptimum = result.solution;
                // 更新全局最优解
                if (localEvaluation > bestGlobalEvaluation) {
                    bestGlobalEvaluation = localEvaluation;
                    bestGlobalSolution = new ArrayList<>(localOptimum);
                    System.out.println("新全局最佳解找到: " + bestGlobalEvaluation);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown(); // 关闭线程池
        assert bestGlobalSolution != null;
        new Insert().checkFinished(bestGlobalSolution);
        // 输出最终全局最优解
//        System.out.println("最终优化后的解: " + bestGlobalEvaluation);
    }

    // 局部搜索结果类，用于存储解和评估结果
    static class LocalSearchResult {
        List<Target> solution;
        double evaluation;
        LocalSearchResult(List<Target> solution, double evaluation) {
            this.solution = solution;
            this.evaluation = evaluation;
        }
    }

    // 局部搜索方法
    private static void localSearch(List<Target> solution, int iterations, List<Target> optimalSolution) {
        optimalSolution.addAll(solution);
        double bestLocalEvaluation = evaluateIndividuals(optimalSolution);
        List<Target> neighborSolution = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            // 生成一个邻域解
            neighborSolution.clear();
            neighborSolution.addAll(optimalSolution);
            NeighborOperator.singleSwap(neighborSolution); // 生成邻域解
            double neighborEvaluation = evaluateIndividuals(neighborSolution);

            // 更新局部最优解
            if (neighborEvaluation > bestLocalEvaluation) {
                bestLocalEvaluation = neighborEvaluation;
                optimalSolution.clear();
                optimalSolution.addAll(neighborSolution); // 更新局部最佳解
            }
        }
    }
    /***************** 评估函数 ******************/
    private static double evaluateIndividuals(List<Target> targets) {
        // 适应度评估
        return new Insert().insertEarliest(targets);
    }
}
