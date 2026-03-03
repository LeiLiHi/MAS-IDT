package org.lileischeduler.dynamicGP;

import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical.ALNS;
import org.lileischeduler.dynamicGP.solve.operator.Allocation;
import org.lileischeduler.dynamicGP.solve.operator.DecodeDownload;

import java.io.IOException;
import java.util.*;

public class Hierarchical {
    /**
     * 多层级，任务先分配，再规划成像，再数传。有循环(注意有虚拟任务)
     */
    public static void main(String[] args) throws IOException {
        double[] scoBest = new double[]{0,0};
        Map<Satellite, List<Target>> hisBest = new HashMap<>();
        List<Target> bestUnfinished = new ArrayList<>();
        // 0. 场景载入
        long start = System.currentTimeMillis();

        // 初始化satellite  station  targets
        LoadSatellite loadSatellite = new LoadSatellite(15);
        LoadStation loadStation = new LoadStation(4, "c");
        List<Target> generateTargets = new GenerateTargets(" ", 1, 1).generateStaticTargets(1000, "全球分布");

        // 计算所有地面站对卫星的数传窗口
        for (Satellite satellite : loadSatellite.satelliteList) {
            for (Station station : loadStation.stationList) {
                station.transitionWindows(satellite);
            }
        }

        List<Target> unscheduled = new ArrayList<>(generateTargets);
        for (int i = 0; i < 1000; i++) {
            // 1. 任务分配（未完成任务的重新分配）
            Allocation.allocate(loadSatellite.satelliteList, unscheduled, "random");
            // 2. 成像任务规划（单星规划）
            for (Satellite satellite : loadSatellite.satelliteList) {
                ALNS.AdaptiveLargeNeighborSearch(satellite, (int)(satellite.scheduledTargets.size() * 0.1), 50);
            }
            // 3. 数传解码
            loadSatellite.satelliteList.sort(Comparator.comparingInt(s -> ((Satellite) s).scheduledTargets.size()).reversed());
            double[] finishedNum = DecodeDownload.decode(loadSatellite.satelliteList, loadStation.stationList);
            System.out.println(Arrays.toString(finishedNum));
            // 4. 更新（保留最佳的卫星任务方案）
            unscheduled.clear();
            for (Satellite satellite : loadSatellite.satelliteList) {
                unscheduled.addAll(satellite.currentTargets);           // 当前卫星所有未完成的任务
                satellite.currentTargets.clear();                       // 清空，下一次分配
            }
            loadStation.reSet();                                        // 重置地面站的状态

            /*if (finishedNum[1] > scoBest[1]) {
                scoBest = finishedNum;
                for (Satellite sat : loadSatellite.satelliteList) {
                    if (!hisBest.containsKey(sat)) {
                        hisBest.put(sat, new ArrayList<>());
                    }
                    hisBest.get(sat).clear();
                    hisBest.get(sat).addAll(sat.scheduledTargets);
                }
                bestUnfinished.clear();
                bestUnfinished.addAll(unscheduled);
            } else {
                // 未完成任务  已完成（窗口重置）
                unscheduled.clear();
                unscheduled.addAll(bestUnfinished);
                for (Target target : unscheduled) {
                    target.reSet();
                }
                for (Satellite sat : hisBest.keySet()) {
                    List<Target> targets = hisBest.get(sat);
                    if (targets.isEmpty()) continue;
                    for (Target target : targets) {
                        if (target.priority <= 0) continue;
                        target.reSet();
                        sat.calculateWindows(target);
                    }
                    // 虚拟任务(更新)
                    sat.scheduledTargets.clear();
                    sat.currentTargets.clear();
                    sat.currentTargets.addAll(targets);
                    sat.currentTargets.remove(0);
                    sat.currentTargets.remove(sat.currentTargets.size() - 1);
                }
            }*/
        }
    }
}
