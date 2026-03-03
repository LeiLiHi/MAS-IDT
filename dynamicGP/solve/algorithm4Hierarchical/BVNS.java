package org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical;

import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.operator.Allocation;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class BVNS {
    public void run(double[] para, BufferedWriter writer, LoadSatellite loadSatellite, LoadStation loadStation, List<Target> targets) throws IOException {
        double iteration = para[0];
        double maxTime = para[1];
        double noImprove = para[2];
        double improve = 0;
        double[] hisBest = new double[]{0.0, 0.0};
        List<Target> unscheduled = new ArrayList<>(targets);
        long start = System.currentTimeMillis();
        for (int i = 0; i < iteration; i++) {
            improve++;
            // 1. 任务分配（未完成任务的重新分配）
            Allocation.allocate(loadSatellite.satelliteList, unscheduled, "random");
            // 2. 成像数传一体化调度
            unscheduled.clear();
            for (Satellite satellite : loadSatellite.satelliteList) {
                boolean isInserted;
                List<Target> reTargets = new ArrayList<>(satellite.currentTargets);
                reTargets.addAll(satellite.scheduledTargets);
                satellite.scheduledTargets.clear();
                Collections.shuffle(reTargets);
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
            if (profit > hisBest[1]) {
                hisBest[1] = profit;
                hisBest[0] = completeNum;
            }
            loadStation.reSet();                                        // 重置地面站的状态
            Collections.shuffle(unscheduled, random);
            double time = (System.currentTimeMillis() - start) / 1000.0;
            writer.write(hisBest[0] + "," + hisBest[1] + "," + time + "\n");
            writer.flush();
            if (time > maxTime || improve > noImprove || hisBest[0] == targets.size()) break;
        }
    }
}
