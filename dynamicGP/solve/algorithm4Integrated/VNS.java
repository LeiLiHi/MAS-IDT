package org.lileischeduler.dynamicGP.solve.algorithm4Integrated;

import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class VNS {
    private static final Insert insert = new Insert();
    private static final List<Target> scheduled = new ArrayList<Target>();
    private static final List<Target> unfinished = new ArrayList<>();

    /***************** Variable Neighborhood Search ****************/
    public static void run(double[] param, BufferedWriter writer, LoadSatellite loadSatellite, LoadStation loadStation, List<Target> targets) throws IOException {
        /* Step 1: 创建一个随机顺序的路径作为初始解 */
        double iteration = param[0];
        double deleteRate = param[1];
        double maxTime = param[2];
        double noImprove = param[3];
        double improve = 0;

        long startTime = System.currentTimeMillis();


        List<Satellite> satelliteList = loadSatellite.satelliteList;
        List<Station> stationList = loadStation.stationList;
        initialization(targets, satelliteList, stationList);
        double bestFitness = scheduled.size();
        List<Target> bestSolution = new ArrayList<>(scheduled);
        List<Target> bestUnfinished = new ArrayList<>(unfinished);


        for (int generation = 0; generation < iteration; generation++) {
            /* Step 2: 进行邻域操作与评估 */
            int deleteNum = (int) (scheduled.size() * deleteRate);
            while (deleteNum > 0) {
                int removeIndex = random.nextInt(scheduled.size());
                Target target = scheduled.get(removeIndex);
                target.reSet();
                scheduled.remove(removeIndex);
                unfinished.add(target);
                deleteNum--;
            }

            // 重新评估剩余任务（目标、卫星及地面站全部重置）
            reSet(scheduled, satelliteList, stationList);
            Iterator<Target> iterator = scheduled.iterator();
            while (iterator.hasNext()) {
                Target target = iterator.next();
                boolean isInsert = insert(target, satelliteList, stationList);
                if (!isInsert) {
                    iterator.remove();
                    unfinished.add(target);
                }
            }

            // 第二轮插入
            Iterator<Target> unfinishedIterator = unfinished.iterator();
            while (unfinishedIterator.hasNext()) {
                Target target = unfinishedIterator.next();
                boolean isInsert = insert(target, satelliteList, stationList);
                if (isInsert) {
                    unfinishedIterator.remove();
                    scheduled.add(target);
                }
            }

            if (scheduled.size() > bestFitness) {
                bestFitness = scheduled.size();
                bestSolution.clear();
                bestSolution.addAll(scheduled);
                bestUnfinished.clear();
                bestUnfinished.addAll(unfinished);
            } else {
                scheduled.clear();
                unfinished.clear();
                scheduled.addAll(bestSolution);
                unfinished.addAll(bestUnfinished);
                reSet(scheduled, satelliteList, stationList);
                iterator = scheduled.iterator();
                while (iterator.hasNext()) {
                    Target target = iterator.next();
                    boolean isInsert = insert(target, satelliteList, stationList);
                    if (!isInsert) {
                        iterator.remove();
                        unfinished.add(target);
                    }
                }
            }
            double profit = 0.0;
            for (Target target : scheduled) {
                profit += target.priority;
            }

            long endTime = System.currentTimeMillis();
            double Time = (endTime - startTime) / 1000.0;

            writer.write(scheduled.size() + "," + profit + "," + Time + "\n");
            writer.flush();
            improve++;
            if (scheduled.size() == targets.size() || improve > noImprove || Time > maxTime) break;

        }
    }

    /**
     * 将当前任务插入
     * @param targets       待插入目标
     * @param satelliteList 卫星列表
     * @param stationList   地面站列表
     */
    public static void initialization(List<Target> targets, List<Satellite> satelliteList, List<Station> stationList) {
        int completeNum = 0;
        for (Target target : targets) {
            boolean isInsert = insert(target, satelliteList, stationList);
            if (isInsert) {
                completeNum++;
                scheduled.add(target);
            } else {
                unfinished.add(target);
            }
        }
//        System.out.println("初始化：" + completeNum);
    }

    public static boolean insert(Target target, List<Satellite> satelliteList, List<Station> stationList) {
        // 1.1 成像资源（选择卫星）
        boolean isInserted;
        for (Satellite satellite : satelliteList) {
            satellite.calculateWindows(target);                                     // 计算任务的成像资源
            // 1.1.1 插入到最早的可行时间窗口（调出卫星的scheduledList，只能按照顺序插入，不能改变之前的任务时间）
            isInserted = insert.insertEarliest(target, satellite.scheduledTargets);
            if (isInserted) {
                boolean isArranged;
                for (Station station : stationList) {
                    isArranged = station.isFeasible0(target);
                    if (isArranged) return true;      // 1.2.1 数传规划成功，停止找数传站


                }
                // 始终没有返回true，那么重置目标的窗口
                target.imageChance.imageWindow.satellite.scheduledTargets.remove(target);
                target.reSet();
            }
        }
        return false;
    }


        /** 辅助函数1
         * 每次评估以前都要清零
         * @param targetList        目标列表
         * @param satelliteList     卫星列表
         * @param stationList       地面站列表
         */
        public static void reSet(List<Target> targetList, List<Satellite> satelliteList, List<Station> stationList){
            for (Target target : targetList) target.reSet();
            for (Satellite satellite : satelliteList) satellite.scheduledTargets.clear();
            for (Station station : stationList) station.currentTargets.clear();
        }

    }
