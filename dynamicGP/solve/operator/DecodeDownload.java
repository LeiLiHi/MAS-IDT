package org.lileischeduler.dynamicGP.solve.operator;

import org.lileischeduler.dynamicGP.HeuristicGP;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;

import java.util.ArrayList;
import java.util.List;

public class DecodeDownload {
    public static double[] decode(List<Satellite> satellites, List<Station> stations) {
        boolean existVirtual = false;
        boolean isInsert = false;
        double num = 0;
        double profit = 0.0;
        for (Satellite satellite : satellites) {
            for (Target target : satellite.scheduledTargets) {
                if (target.priority == 0) {
                    existVirtual = true;
                    continue;                 // 虚拟任务
                }
                for (Station station : stations) {
                    isInsert = station.isFeasible0(target);
                    if (isInsert) {              // 成功安排
                        break;
                    }
                }
            }
            for (int i = satellite.scheduledTargets.size() - 1; i > 0 ; i--) {
                Target target = satellite.scheduledTargets.get(i);
                if  (target.priority == 0) continue;
                if (target.imageChance == null || target.transitionWindow == null) {
                    target.imageChance = null;
                    satellite.currentTargets.add(target);
                    satellite.scheduledTargets.remove(i);
                }
            }
            if (existVirtual) {
                num += satellite.scheduledTargets.size() - 2;
            } else {
                num += satellite.scheduledTargets.size();
            }
            for (Target target : satellite.scheduledTargets) {
                profit += target.priority;
            }
        }

        return new double[]{num, profit};
    }

    public static double[] decode(List<Satellite> satellites, List<Station> stations, boolean t) {
        boolean isInsert = false;
        double num = 0;
        double profit = 0.0;
        for (Satellite satellite : satellites) {
            for (Target target : satellite.scheduledTargets) {
                if (target.priority == 0) {
                    continue;                 // 虚拟任务
                }
                for (Station station : stations) {
                    isInsert = station.isFeasible0(target);
                    if (isInsert) {              // 成功安排
                        break;
                    }
                }
            }
            for (int i = satellite.scheduledTargets.size() - 1; i > 0 ; i--) {
                Target target = satellite.scheduledTargets.get(i);
                if  (target.priority == 0) continue;
                if (target.imageChance == null || target.transitionWindow == null) {
                    target.imageChance = null;
                    satellite.currentTargets.add(target);
                    satellite.scheduledTargets.remove(i);
                }
            }
            num += satellite.scheduledTargets.size() - 2;

            for (Target target : satellite.scheduledTargets) {
                profit += target.priority;
            }
        }

        return new double[]{num, profit};
    }
}
