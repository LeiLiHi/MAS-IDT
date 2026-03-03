package org.lileischeduler.dynamicGP.solve.algorithm.evolutionary;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class MA {
    /***************** MA(Memetic Algorithm)：模因算法 ****************/
    public static void MemeticAlgorithm(List<Target> finished, List<Target> left, List<Target> targets, double[] parameters) {
        if (targets.isEmpty()) return;
        int populationSize = (int) parameters[0];           // 种群数量
        int terminal = (int) parameters[1];                 // 终止条件
        double mutationRate = parameters[2];                // 变异率
        double crossoverRate = parameters[3];               // 交叉率
        double localSearchRate = parameters[4];             // 局部搜索率
        double bestScore = Double.MIN_VALUE;                // 最佳适应度
        List<Integer> bestTargets = new ArrayList<>();
        // 0. 种群初始化函数
        List<List<Integer>> population = initializePopulation(populationSize, targets.size());
        // 1. 种群进化主循环
        int generation = 0; // 当前代数
        while (generation < terminal) { // 最大代数
            // 1.1 适应度评估
            List<Double> fitnessScores = new ArrayList<>();
            for (List<Integer> individual : population) {
                double score = evaluateIndividuals(targets, individual);
                fitnessScores.add(score);
            }
            double currentBest = Collections.max(fitnessScores);
            if (currentBest > bestScore) {
                bestScore = currentBest;
                int index = fitnessScores.indexOf(currentBest);
                bestTargets.clear();
                bestTargets.addAll(population.get(index));
            }
            System.out.println("第 " + generation + " 代的最优个体适应度值为： " + currentBest);

            // 1.2 进化操作：选择、交叉、变异
            List<List<Integer>> childPopulation = new ArrayList<>();
            List<Double> childFitnessScores = new ArrayList<>();
            // 生成子代
            boolean isLocalSearch = true;
            while (childPopulation.size() < populationSize) {
                // 锦标赛选择父代
                List<Integer> parent1 = tournamentSelect(population, fitnessScores);
                List<Integer> parent2 = tournamentSelect(population, fitnessScores);
                List<Integer> offspring;
                if (random.nextDouble() < crossoverRate) offspring = crossover(parent1, parent2);
                else offspring = new ArrayList<>(parent1);                              // 个体交叉
                if (random.nextDouble() < mutationRate) mutate(offspring);              // 个体变异
                if (random.nextDouble() < localSearchRate && isLocalSearch) {           // 局部搜索
                    isLocalSearch = false;
                    localSearch(targets, offspring, 100);
                }
                // 计算子代适应度
                double childFitness =  evaluateIndividuals(targets, offspring); // 假设的适应度评估函数
                childPopulation.add(offspring);
                childFitnessScores.add(childFitness);
            }
            // 一对一生存者竞争，保留适应度更优秀的个体
            for (int i = 0; i < populationSize; i++) {
                if (childFitnessScores.get(i) > fitnessScores.get(i)) {
                    population.set(i, childPopulation.get(i)); // 替换个体
                    fitnessScores.set(i, childFitnessScores.get(i)); // 更新适应度
                }
            }
            generation++; // 迭代增加
        }
        // 解码当前的最佳方案
        new Insert().decodeInteger(targets, bestTargets, finished, left);
    }



    /***************** 局部搜索 ******************/
    public static void localSearch(List<Target> targets, List<Integer> offspring, int iteration) {
        List<Target> currentSolution = new ArrayList<>();
        for (Integer index : offspring) {
            currentSolution.add(targets.get(index));
        }
        double bestEvaluation = new Insert().insertEarliest(currentSolution);         // 当前方案评估
        // 固定迭代次数
        for (int tt = 0; tt < iteration; tt++) {
            List<Target> tempSolution = new ArrayList<>(currentSolution);
            switch (random.nextInt(3)) {
                case 0:
                    NeighborOperator.singleSwap(tempSolution);
                    break;
                case 1:
                    NeighborOperator.twoPointsReverse(tempSolution);
                    break;
                case 2:
                    NeighborOperator.insertOperator(tempSolution);
                    break;
            }
            double neighborEvaluation = new Insert().insertEarliest(tempSolution); // 评价新值
            if (neighborEvaluation > bestEvaluation) {
                bestEvaluation = neighborEvaluation;
                currentSolution = new ArrayList<>(tempSolution); // 更新当前解
            }
        }
        offspring.clear();
        for (Target solution : currentSolution) {
            int index = targets.indexOf(solution);
            if (index != -1) {
                offspring.add(index);
            }
        }
    }
    /***************** 评估函数 ******************/
    private static double evaluateIndividuals(List<Target> targets, List<Integer> individual) {
        List<Target> individualTargets = new ArrayList<>();
        for (Integer index : individual) {
            individualTargets.add(targets.get(index));
        }
        // 适应度评估
        return new Insert().insertEarliest(individualTargets);
    }
    /***************** 锦标赛选择 ****************/
    private static List<Integer> tournamentSelect(List<List<Integer>> population, List<Double> fitnessScores) {
        int tournamentSize = 10; // 可以调整锦标赛的大小
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = random.nextInt(population.size());
            selected.add(randomIndex);
        }

        // 选择适应度最高的个体
        double bestFitness = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        for (Integer index : selected) {
            if (fitnessScores.get(index) > bestFitness) {
                bestFitness = fitnessScores.get(index);
                bestIndex = index;
            }
        }
        return population.get(bestIndex);
    }

    private static List<List<Integer>> initializePopulation(int populationSize, int targetSize) {
        List<List<Integer>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Integer> individual = new ArrayList<>();
            for (int j = 0; j < targetSize; j++) {
                individual.add(j); // 随机初始化个体
            }
            Collections.shuffle(individual,random);
            population.add(individual);
        }
        return population;
    }

    /******************* 交叉操作 ****************/
    private static List<Integer> crossover(List<Integer> parent1, List<Integer> parent2) {
        int size = parent1.size();
        // 随机选择两个交叉点
        int crossoverPoint1 = random.nextInt(size);
        int crossoverPoint2 = random.nextInt(size);
        // 确保 crossoverPoint1 < crossoverPoint2
        if (crossoverPoint1 > crossoverPoint2) {
            int temp = crossoverPoint1;
            crossoverPoint1 = crossoverPoint2;
            crossoverPoint2 = temp;
        }
        // 创建后代并填充部分遗传信息
        List<Integer> offspring = new ArrayList<>(Collections.nCopies(size, -1)); // 使用 -1 作为占位符
        // 从 parent1 在 crossoverPoint1 和 crossoverPoint2 之间复制基因到后代
        for (int i = crossoverPoint1; i <= crossoverPoint2; i++) {
            offspring.set(i, parent1.get(i)); // 将 parent1 的基因复制到后代
        }
        // 处理 parent2 中的基因
        for (int i = 0; i < size; i++) {
            int gene = parent2.get(i);
            // 如果 offspring 中仍然有空位，就放入 city
            if (!offspring.contains(gene)) {
                // 找到下一个空位
                for (int j = 0; j < size; j++) {
                    if (offspring.get(j) == -1) {
                        offspring.set(j, gene); // 填充空位
                        break; // 填充一个后跳出循环
                    }
                }
            }
        }
        return offspring;
    }

    private static void mutate(List<Integer> individual) {
        if (random.nextDouble() > 0.5) {
            mutateMultipleSwap(individual, 20);
        } else {
            mutateReverse(individual);
        }
    }

    // 交换两个位置的数字
    private static void mutateSwap(List<Integer> individual) {
        int index1 = random.nextInt(individual.size());
        int index2 = random.nextInt(individual.size());
        // 确保两个索引不相同
        while (index1 == index2) {
            index2 = random.nextInt(individual.size());
        }
        // 交换元素
        Collections.swap(individual, index1, index2);
    }

    // 多次交换两个位置的数字
    private static void mutateMultipleSwap(List<Integer> individual, int swapCount) {
        for (int i = 0; i < swapCount; i++) {
            mutateSwap(individual); // 调用交换方法
        }
    }

    // 反转两个位置之间的编码
    private static void mutateReverse(List<Integer> individual) {
        int startIndex = random.nextInt(individual.size());
        int endIndex = random.nextInt(individual.size());
        // 确保 startIndex < endIndex
        if (startIndex > endIndex) {
            int temp = startIndex;
            startIndex = endIndex;
            endIndex = temp;
        }
        // 反转[startIndex, endIndex]
        List<Integer> subList = new ArrayList<>(individual.subList(startIndex, endIndex + 1));
        Collections.reverse(subList);
        for (int i = 0; i < subList.size(); i++) {
            individual.set(startIndex + i, subList.get(i));
        }
    }
}
