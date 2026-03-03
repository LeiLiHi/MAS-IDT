package org.lileischeduler.dynamicGP.solve.algorithm4Integrated;

import lombok.Getter;
import lombok.Setter;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import static org.lileischeduler.tool.Tool.random;

public class IGA {
    public static Insert insert = new Insert();
    /***************** IGA：改进的遗传算法 ****************/
    public static void run(double[] param, BufferedWriter writer, LoadSatellite loadSatellite, LoadStation loadStation, List<Target> targetList) throws IOException {
        int populationSize = (int) param[0];
        int eliteCount = Math.max(3, (int) param[0] / 10);
        double maxTime = param[4];
        double noImprove = param[5];
        double improve = 0;
        // 种群初始化
        List<Satellite> satelliteList = loadSatellite.satelliteList;
        List<Station> stationList = loadStation.stationList;
        List<Chromosome> population = initializePopulation(targetList, satelliteList, stationList, (int) param[0]);
        population.sort((a1, a2) -> Double.compare(a2.getFitness()[1], a1.getFitness()[1]));
        Chromosome bestChromosome = population.get(0);
        double[] bestFitness = bestChromosome.getFitness();
        long startTime = System.currentTimeMillis();

        // 种群进化
        List<Chromosome> newPopulation = new ArrayList<>();
        for (int generation = 0; generation < param[1]; generation++) {
            // 选择（精英保留策略）
            newPopulation.clear();
            newPopulation.addAll(population.subList(0, Math.min(5, populationSize / 3)));
            while (newPopulation.size() < populationSize) {
                // 轮盘赌选择父代（选择一个精英解）
                Chromosome parent1 = population.get(random.nextInt(eliteCount));
                Chromosome parent2 = population.get(random.nextInt(populationSize));
                // 交叉生成子代 + 评估
                Chromosome offspring = CrossoverOperators.crossover(
                        parent1,
                        parent2,
                        random.nextInt(4),
                        param[2]
                );
                evaluateChromosomeFitness(offspring, targetList, satelliteList, stationList);

                // 变异操作 + 评估
                MutationOperators.mutate(offspring, random.nextInt(4), param[3]);
                evaluateChromosomeFitness(offspring, targetList, satelliteList, stationList);

                int pos = findInsertPosition(newPopulation, offspring);
                newPopulation.add(pos, offspring);
            }
            population.clear();
            population.addAll(newPopulation);


            if (population.get(0).getFitness()[1] > bestFitness[1]) {
                bestFitness = population.get(0).getFitness();
                bestChromosome = population.get(0);
                improve = 0;
            }
            long endTime = System.currentTimeMillis();
            double Time = (endTime - startTime) / 1000.0;
            // 可选：打印每代最优解
            writer.write(bestFitness[0] + "," + bestFitness[1] + "," + Time + "\n");
            writer.flush();
            improve++;
            if (bestFitness[0] == targetList.size() || improve > noImprove || Time > maxTime) break;
        }
        // 解码历史最优个体
        evaluateChromosomeFitness(bestChromosome, targetList, satelliteList, stationList);
    }

    // 二分查找插入位置 （從大到小）
    public static int findInsertPosition(List<Chromosome> chromosomeList, Chromosome chromosome) {
        int low = 0;
        int high = chromosomeList.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (chromosomeList.get(mid).getFitness()[1] > chromosome.getFitness()[1]) {
                low = mid + 1;  // newTarget 应在 mid 左侧
            } else {
                high = mid;  // newTarget 应在 mid 右侧
            }
        }
        return low;  // 返回插入位置
    }

    public static List<Chromosome> initializePopulation(
            List<Target> targetList,
            List<Satellite> satelliteList,
            List<Station> stationList,
            int populationSize
    ) {
        // 种群集合
        List<Chromosome> population = new ArrayList<>();
        // 种群初始化参数
        int satelliteCount = satelliteList.size();
        int stationCount = stationList.size();
        // 随机生成初始种群
        for (int i = 0; i < populationSize; i++) {
            Chromosome chromosome = new Chromosome(satelliteCount * stationCount);
            // 为每个任务随机分配编码
            for (int j = 0; j < targetList.size(); j++) {
                // 随机生成编码值：范围为 [0, satelliteCount * stationCount - 1]
                int encodingValue = random.nextInt(satelliteCount * stationCount);
                chromosome.addGene(encodingValue);
            }
            population.add(chromosome);
        }
        evaluatePopulationFitness(population, targetList, satelliteList, stationList);
        return population;
    }

    // 适应度评估函数
    public static void evaluateChromosomeFitness(
            Chromosome chromosome,
            List<Target> targetList,
            List<Satellite> satelliteList,
            List<Station> stationList
    ) {
        // 卫星 地面站 目标数据置空
        for (Target target : targetList) target.reSet();
        for (Satellite satellite : satelliteList) satellite.scheduledTargets.clear();
        for (Station station : stationList) station.currentTargets.clear();

        int satelliteCount = satelliteList.size();
        int stationCount = stationList.size();

        List<Integer> genes = chromosome.getGenes();
        List<Integer> unFinished = chromosome.getUnFinished();
        unFinished.clear();

        double[] fitness = new double[]{0.0, 0.0};
        // 计算每个染色体的适应度
        for (int i = 0; i < targetList.size(); i++) {
            int gene = genes.get(i);
            Target target = targetList.get(i);

            // 卫星选择与规划：使用 mod 运算,插入失败则跳过
            int satelliteIndex = gene % satelliteCount;
            Satellite satellite = satelliteList.get(satelliteIndex);
            satellite.calculateWindows(target);
            boolean isInserted = insert.insertEarliest(target, satellite.scheduledTargets);

            if (!isInserted) {
                unFinished.add(i);
                continue;
            }

            // 地面站选择
            int stationIndex;
            if (satelliteCount == stationCount) {
                // 特殊情况：卫星数量等于地面站数量
                stationIndex = gene / stationCount;
            } else {
                // 一般情况
                stationIndex = gene % stationCount;
            }
            Station station = stationList.get(stationIndex);

            boolean isArranged = station.isFeasible0(target);
            if (!isArranged) {      // 如果插入失败，那么重置任务并跳过
                unFinished.add(i);
                target.reSet();
                satellite.scheduledTargets.remove(target);
            } else {               // 时间依赖收益
                fitness[0] += 1;
                fitness[1] += target.priority;
            }
        }
        chromosome.setFitness(fitness);
    }

    public static void evaluatePopulationFitness(
            List<Chromosome> population,
            List<Target> targetList,
            List<Satellite> satelliteList,
            List<Station> stationList
    ) {
        for (Chromosome chromosome : population) {
            evaluateChromosomeFitness(chromosome, targetList, satelliteList, stationList);
        }
    }
}

@Getter
@Setter
// 染色体类
class Chromosome {
    private List<Integer> genes;  // 基因序列
    private double[] fitness;  // 适应度
    private List<Integer> unFinished = new ArrayList<>();     // 未完成任务的索引
    private int satAndStaNum;

    public Chromosome(int satAndStaNum) {
        this.genes = new ArrayList<>();
        this.satAndStaNum = satAndStaNum;
    }

    public void addGene(int gene) {
        genes.add(gene);
    }
}

class CrossoverOperators {
    // 单点交叉
    public static Chromosome singlePointCrossover(
            Chromosome parent1,
            Chromosome parent2,
            double crossoverRate
    ) {
        // 随机判断是否进行交叉
        if (Math.random() > crossoverRate) {
            // 不进行交叉，返回第一个父代的副本
            return copyChromosome(parent1);
        }

        Chromosome offspring = new Chromosome(parent1.getSatAndStaNum());
        int chromosomeLength = parent1.getGenes().size();

        // 随机选择交叉点
        int crossoverPoint = random.nextInt(chromosomeLength);

        // 前半部分取parent1，后半部分取parent2
        for (int i = 0; i < chromosomeLength; i++) {
            if (i < crossoverPoint) {
                offspring.addGene(parent1.getGenes().get(i));
            } else {
                offspring.addGene(parent2.getGenes().get(i));
            }
        }

        return offspring;
    }

    // 两点交叉
    public static Chromosome twoPointCrossover(
            Chromosome parent1,
            Chromosome parent2,
            double crossoverRate
    ) {
        // 随机判断是否进行交叉
        if (Math.random() > crossoverRate) {
            return copyChromosome(parent1);
        }

        Chromosome offspring = new Chromosome(parent1.getSatAndStaNum());
        int chromosomeLength = parent1.getGenes().size();

        // 随机选择两个交叉点
        int crossoverPoint1 = random.nextInt(chromosomeLength);
        int crossoverPoint2 = random.nextInt(chromosomeLength);

        // 确保 crossoverPoint1 小于 crossoverPoint2
        if (crossoverPoint1 > crossoverPoint2) {
            int temp = crossoverPoint1;
            crossoverPoint1 = crossoverPoint2;
            crossoverPoint2 = temp;
        }

        for (int i = 0; i < chromosomeLength; i++) {
            if (i < crossoverPoint1 || i >= crossoverPoint2) {
                offspring.addGene(parent1.getGenes().get(i));
            } else {
                offspring.addGene(parent2.getGenes().get(i));
            }
        }

        return offspring;
    }

    // 均匀交叉
    public static Chromosome uniformCrossover(
            Chromosome parent1,
            Chromosome parent2,
            double crossoverRate
    ) {
        // 随机判断是否进行交叉
        if (Math.random() > crossoverRate) {
            return copyChromosome(parent1);
        }

        Chromosome offspring = new Chromosome(parent1.getSatAndStaNum());
        int chromosomeLength = parent1.getGenes().size();

        for (int i = 0; i < chromosomeLength; i++) {
            // 随机选择基因来源
            if (Math.random() < 0.5) {
                offspring.addGene(parent1.getGenes().get(i));
            } else {
                offspring.addGene(parent2.getGenes().get(i));
            }
        }

        return offspring;
    }

    // 自适应交叉（基于适应度）
    public static Chromosome adaptiveCrossover(
            Chromosome parent1,
            Chromosome parent2,
            double crossoverRate
    ) {
        // 根据父代适应度确定交叉概率
        double adaptiveCrossoverRate = calculateAdaptiveCrossoverRate(
                parent1.getFitness()[1],
                parent2.getFitness()[1]
        );

        if (Math.random() > adaptiveCrossoverRate) {
            return copyChromosome(parent1);
        }

        Chromosome offspring = new Chromosome(parent1.getSatAndStaNum());
        int chromosomeLength = parent1.getGenes().size();

        // 优先选择适应度高的父代基因
        for (int i = 0; i < chromosomeLength; i++) {
            if (parent1.getFitness()[1] > parent2.getFitness()[1]) {
                offspring.addGene(parent1.getGenes().get(i));
            } else {
                offspring.addGene(parent2.getGenes().get(i));
            }
        }

        return offspring;
    }

    // 自适应交叉率计算
    private static double calculateAdaptiveCrossoverRate(
            double fitness1,
            double fitness2
    ) {
        // 根据适应度差异计算交叉概率
        double fitnessDifference = Math.abs(fitness1 - fitness2);
        return Math.min(1.0, fitnessDifference / (fitness1 + fitness2));
    }

    // 深拷贝染色体
    private static Chromosome copyChromosome(Chromosome original) {
        Chromosome copy = new Chromosome(original.getSatAndStaNum());
        for (Integer gene : original.getGenes()) {
            copy.addGene(gene);
        }
        copy.setFitness(original.getFitness());
        return copy;
    }

    // 主交叉方法
    public static Chromosome crossover(
            Chromosome parent1,
            Chromosome parent2,
            int type,
            double crossoverRate
    ) {
        switch (type) {
            case 0:
                return singlePointCrossover(parent1, parent2, crossoverRate);
            case 1:
                return twoPointCrossover(parent1, parent2, crossoverRate);
            case 2:
                return uniformCrossover(parent1, parent2, crossoverRate);
            case 3:
                return adaptiveCrossover(parent1, parent2, crossoverRate);
            default:
                return copyChromosome(parent1);
        }
    }

    // 交叉类型枚举
    public enum CrossoverType {
        SINGLE_POINT,   // 单点交叉
        TWO_POINT,      // 两点交叉
        UNIFORM,        // 均匀交叉
        ADAPTIVE        // 自适应交叉
    }
}

class MutationOperators {

    // 均匀变异
    public static void uniformMutation(
            Chromosome chromosome,
            double mutationRate
    ) {
        List<Integer> genes = chromosome.getGenes();

        for (int i = 0; i < genes.size(); i++) {
            // 按照变异概率决定是否变异
            if (Math.random() < mutationRate) {
                // 在 [0, satAndStaNum) 范围内生成新的基因值
                int newGene = random.nextInt(chromosome.getSatAndStaNum());
                genes.set(i, newGene);
            }
        }
    }

    // 贪婪变异
    public static void greedyMutation(
            Chromosome chromosome,
            double mutationRate
    ) {
        List<Integer> unFinished = chromosome.getUnFinished();
        List<Integer> genes = chromosome.getGenes();
        int maxGenes = chromosome.getSatAndStaNum();
        for (Integer uIndex : unFinished) {
            if (Math.random() < mutationRate) {
                // 在 [0, satAndStaNum) 范围内生成新的基因值
                int newGene = random.nextInt(maxGenes);
                genes.set(uIndex, newGene);
            }
        }
    }

    // 边界变异
    public static void boundaryMutation(
            Chromosome chromosome,
            double mutationRate
    ) {
        List<Integer> genes = chromosome.getGenes();

        for (int i = 0; i < genes.size(); i++) {
            if (Math.random() < mutationRate) {
                // 随机选择边界值
                int boundaryGene = Math.random() < 0.5 ? 0 : chromosome.getSatAndStaNum() - 1;
                genes.set(i, boundaryGene);
            }
        }
    }

    // 自适应变异
    public static void adaptiveMutation(
            Chromosome chromosome,
            double baseMutationRate,
            double fitness
    ) {
        // 根据适应度调整变异率
        double adaptiveMutationRate = calculateAdaptiveMutationRate(
                baseMutationRate,
                fitness
        );

        List<Integer> genes = chromosome.getGenes();

        for (int i = 0; i < genes.size(); i++) {
            if (Math.random() < adaptiveMutationRate) {
                int newGene = random.nextInt(chromosome.getSatAndStaNum());
                genes.set(i, newGene);
            }
        }
    }

    // 计算自适应变异率
    private static double calculateAdaptiveMutationRate(
            double baseMutationRate,
            double fitness
    ) {
        // 根据适应度动态调整变异率
        // 适应度越低，变异率越高
        return baseMutationRate * (1 + (1 - fitness));
    }

    // 主变异方法
    public static void mutate(
            Chromosome chromosome,
            int mutationType,
            double mutationRate,
            double... additionalParams
    ) {
        switch (mutationType) {
            case 0:
                uniformMutation(chromosome, mutationRate);
                break;
            case 1:
                boundaryMutation(chromosome, mutationRate);
                break;
            case 2:
                greedyMutation(chromosome, mutationRate);
                break;
            case 3:
                // 需要基础适应度
                double fitness = additionalParams.length > 0 ? additionalParams[0] : 0.5;
                adaptiveMutation(chromosome, mutationRate, fitness);
                break;
        }
    }

    // 变异类型枚举
    public enum MutationType {
        UNIFORM,     // 均匀变异 0
        BOUNDARY,    // 边界变异 1
        ADAPTIVE,    // 自适应变异2
        GREEDY       // 贪婪变异 3
    }
}

