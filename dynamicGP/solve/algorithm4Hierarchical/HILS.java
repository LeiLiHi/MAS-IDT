package org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical;

import org.lileischeduler.dynamicGP.HeuristicGP;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.algorithm4Integrated.VNS;
import org.lileischeduler.dynamicGP.solve.operator.Allocation;
import org.lileischeduler.dynamicGP.solve.operator.DecodeDownload;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class HILS {
    /**
     * 多层级，任务先分配，再规划成像，再数传。有循环(注意有虚拟任务)
     */
    public void run(double[] para, BufferedWriter writer, LoadSatellite loadSatellite, LoadStation loadStation, List<Target> targets) throws IOException {
        double iteration = para[0];             // 迭代次数
        double step = para[1];                  // ALNS的迭代次数
        double runTime = para[2];               // 运行时间
        double noImprove = para[3];             // 未提升次数
        double[] scoBest = new double[]{0, 0};
        double improve = 0;
        // 0. 场景载入
        long start = System.currentTimeMillis();
        writer.write( "CompleteRate, Profit, Time\n");
        List<Target> unscheduled = new ArrayList<>(targets);
        for (int i = 0; i < iteration; i++) {
            // 1. 任务分配（未完成任务的重新分配）
            Allocation.allocate(loadSatellite.satelliteList, unscheduled, "random");
            // 2. 成像任务规划（单星规划）
            for (Satellite satellite : loadSatellite.satelliteList) {
                List<Target> st = satellite.scheduledTargets;
                for (Target target : st) {
                    target.reSet();
                }
                satellite.currentTargets.addAll(st);
                ILS.run(satellite, (int) step);
            }
            // 3. 数传解码
            loadSatellite.satelliteList.sort(Comparator.comparingInt(s -> ((Satellite) s).scheduledTargets.size()).reversed());
            double[] finishedNum = DecodeDownload.decode(loadSatellite.satelliteList, loadStation.stationList);

            // 4. 更新（保留最佳的卫星任务方案）
            unscheduled.clear();
            for (Satellite satellite : loadSatellite.satelliteList) {
                unscheduled.addAll(satellite.currentTargets);           // 当前卫星所有未完成的任务
                satellite.currentTargets.clear();                       // 清空，下一次分配
            }

            // 5. 利用剩余资源再次插入
            double[] temp = HeuristicGP.decodeTask(unscheduled, loadSatellite.satelliteList, loadStation.stationList);
            Iterator<Target> iterator = unscheduled.iterator();
            while (iterator.hasNext()) {
                Target target = iterator.next();
                if (target.imageChance != null && target.transitionWindow != null) {
                    iterator.remove();  // 安全地移除元素
                }
            }
            finishedNum[0] += temp[0];
            finishedNum[1] += temp[1];

            if (finishedNum[1] > scoBest[1]) {
                scoBest = finishedNum;
                improve = 0;
            }

            loadStation.reSet();                                        // 重置地面站的状态
            improve++;
            double currentTime = (System.currentTimeMillis() - start) / 1000.0;
            writer.write( scoBest[0] + "," + scoBest[1] + "," + currentTime + "\n");
            writer.flush();
            if (improve > noImprove || currentTime > runTime || scoBest[0] == targets.size()) {
                break;
            }
        }
    }
}
