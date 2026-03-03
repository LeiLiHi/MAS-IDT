package org.lileischeduler.dynamicGP;


import org.lileischeduler.dynamicGP.score.Score;
import org.lileischeduler.dynamicGP.solve.algorithm4Integrated.IGA;
import org.lileischeduler.dynamicGP.solve.algorithm4Integrated.VNS;
import org.lileischeduler.dynamicGP.solve.operator.Insert;
import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.tool.Tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntegratedMain {
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();

        // 初始化satellite  station  targets
        LoadSatellite loadSatellite = new LoadSatellite(15);
        LoadStation loadStation = new LoadStation(2, "c");
        List<Target> generateTargets = new GenerateTargets(" ", 1, 1).generateStaticTargets(1000, "全球分布");

        // 计算所有地面站对卫星的数传窗口
        for (Satellite satellite : loadSatellite.satelliteList) {
            for (Station station : loadStation.stationList) {
                station.transitionWindows(satellite);
            }
        }

        // 到达任务排序(按照优先级次序)
        generateTargets.sort((a1, a2) -> Double.compare(a2.priority, a1.priority));
        double[] param = {50, 100, 0.7, 0.8};

//        // 1. 求解方法one：GA
//        IGA.run(generateTargets, loadSatellite.satelliteList, loadStation.stationList, param);
        // 强化一下（未完成任务再插入一遍）
        for (Target target : generateTargets) {
            if (target.imageChance!=null && target.station != null) continue;
            // 1.1 成像资源（选择卫星）
            boolean isInserted;
            for (Satellite satellite : loadSatellite.satelliteList) {
                satellite.calculateWindows(target);                  // 计算任务的成像资源
                // 1.1.1 插入到最早的可行时间窗口（调出卫星的scheduledList，只能按照顺序插入，不能改变之前的任务时间）
                isInserted = new Insert().insertEarliest(target, satellite.scheduledTargets);
                if (isInserted) {
                    boolean isArranged = false;
                    for (Station station : loadStation.stationList) {
                        isArranged = station.isFeasible0(target);
                        if (isArranged) {           // 1.2.1 数传规划成功，停止找数传站
                            break;
                        }
                    }
                    if (!isArranged) {              // 1.2.2 数传规划失败，放弃成像，下一颗卫星或者停止
                        target.imageChance.imageWindow.satellite.scheduledTargets.remove(target);
                        target.reSet();
                    } else break;                   // 否则 停止迭代卫星
                }
            }
        }
        double[] scores = Score.scores(loadStation.stationList);
        System.out.println("IGA:  完成数" + scores[0] + " 收益值" + scores[1]);

//        loadStation.checkFeasible(false);
//        loadSatellite.checkFeasible(false);
        for (Target target : generateTargets) target.reSet();
        loadStation.reSet();
        loadSatellite.reSet();

        // 2. 求解方法two：VNS 局部搜索策略（初始化-->随机删除任务-->成像数传更新-->插入剩余任务）
//        VNS.run(generateTargets, loadSatellite.satelliteList, loadStation.stationList, param);
//        loadStation.checkFeasible(true);
//        loadSatellite.checkFeasible(false);

        long end = System.currentTimeMillis();
        double consume = (end - start) / 1000.0;
        System.out.println("总耗时为：" + consume);
    }
}
