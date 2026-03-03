package org.lileischeduler.dynamicGP;

import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.operator.Allocation;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class BiLevel {
    /**
     * 先成像，再数传，不做分配
     */
    public static void main(String[] args) throws IOException {
        // 初始化satellite  station  targets
        LoadSatellite loadSatellite = new LoadSatellite(15);
        LoadStation loadStation = new LoadStation(4, "basicalData\\");
        List<Target> generateTargets = new GenerateTargets(" ", 1, 1).generateStaticTargets(500, "全球分布");

        // 计算所有地面站对卫星的数传窗口
        for (Satellite satellite : loadSatellite.satelliteList) {
            for (Station station : loadStation.stationList) {
                station.transitionWindows(satellite);
            }
        }

        List<Target> unscheduled = new ArrayList<>(generateTargets);
        for (int i = 0; i < 2000; i++) {
            // 1. 任务分配（未完成任务的重新分配）
            Allocation.allocate(loadSatellite.satelliteList, unscheduled, "random");
            // 2. 成像数传一体化调度（解码策略）
            unscheduled.clear();
            for (Satellite satellite : loadSatellite.satelliteList) {
                boolean isInserted;
                List<Target> reTargets = new ArrayList<>(satellite.currentTargets);
                reTargets.addAll(satellite.scheduledTargets);
                satellite.scheduledTargets.clear();
                for (Target target: reTargets) {
                    isInserted = new Insert().insertEarliest(target, satellite.scheduledTargets);
                    boolean isArranged = false;
                    if (isInserted) {
                        for (Station station : loadStation.stationList) {
                            isArranged = station.isFeasible0(target);
                            if (isArranged) {           // 1.2.1 数传规划成功，停止找数传站
                                break;
                            }
                        }
                        if (!isArranged) {              // 1.2.2 数传规划失败，放弃成像，下一颗卫星或者停止
                            satellite.scheduledTargets.remove(target);
                            target.reSet();
                        } else continue;                   // 否则 停止迭代卫星
                    }
                    // 如果任务未完成
                    unscheduled.add(target);
                }
            }

            // 3. 更新
            int completeNum = 0;
            double profit = 0.0;
            for (Satellite satellite : loadSatellite.satelliteList) {
                satellite.currentTargets.clear();                       // 清空，下一次分配
            }
            for (Station station : loadStation.stationList) {
                completeNum += station.currentTargets.size();
                for (Target target : station.currentTargets) {
                    profit += target.priority;
                }
            }
            loadStation.reSet();                                        // 重置地面站的状态
            Collections.shuffle(unscheduled, random);
            System.out.println(completeNum + " " + unscheduled.size());
        }
    }
}
