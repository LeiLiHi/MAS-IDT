package org.lileischeduler.dynamicGP.solve.operator;

import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.model.ImageChance;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.util.List;

public class Evaluate {
    /**
     * 评估函数
     * @param targets       已完成的目标列表
     * @param allTargets    所有的目标列表
     * @param mode          评估模式
     * @return              最终收益
     */
    public static double evaluate(List<Target> targets, List<Target> allTargets, String mode) {
        double result = 0;
        switch (mode) {
            case "优先级":
                return targets.stream()
                        .mapToDouble(Target::getPriority)
                        .sum();
            case "完成率":
                return 1.0 * targets.size() / allTargets.size();
            case "时间": // 30min 之后的  按照时间收益按时间比例折损,之前的按时间比例增加
                for (Target target : targets) {
                    ImageChance imageChance = target.getImageChance();
                    long tarA = target.getArrivedTime();
                    long tarD = target.getDeadline();
                    long fenmu = tarD - tarA;
//                    if (fenmu < 3600) fenmu = tarD - tarA;
                    if (fenmu == 0) fenmu = 1;
                    target.trueScore = target.timeEfficiency * (tarD - imageChance.startTime) / fenmu + target.priority;
                    result += target.trueScore;
                }
                break;
        }
        return result;
    }

    public static double[] evaluateSatellite(LoadSatellite loadSatellite) {
        double result = 0;
        int finishedNum = 0;
        int unfinisedNum = 0;
        int scheduledNum = 0;
        for (Satellite satellite : loadSatellite.satelliteList) {
            result += evaluate(satellite.finishedTargets, null, "时间");
            System.out.println(satellite.satelliteName + "  完成任务数量为：" + satellite.finishedTargets.size() + "  失败任务：" + satellite.failedTargets.size());
            finishedNum += satellite.finishedTargets.size();
            scheduledNum += satellite.scheduledTargets.size() + satellite.newArrival.size();
            unfinisedNum += satellite.failedTargets.size();
        }
        System.out.println("完成的任务总数为：" + finishedNum + " 失败的任务总数为：" + unfinisedNum + "  " + scheduledNum);
        return new double[]{result, finishedNum, unfinisedNum};
    }
}
