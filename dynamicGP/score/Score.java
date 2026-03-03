package org.lileischeduler.dynamicGP.score;

import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;

import java.util.List;

public class Score {
    /*****  根据地面站信息  *****/
    public static double[] scores(List<Station> stationList) {
        double completeNum = 0;
        double profit = 0;
        for (Station station : stationList) {
            for (Target target : station.currentTargets) {
                if (target.priority != 0) {
                    completeNum++;
                    profit += target.priority;
                }
            }
        }
        return new double[]{completeNum, profit};
    }
}
