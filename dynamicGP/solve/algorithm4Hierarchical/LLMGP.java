package org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical;

import org.lileischeduler.dynamicGP.GeneticProgramming;
import org.lileischeduler.dynamicGP.NodeInfo;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.dynamicGP.HeuristicGP.*;

public class LLMGP {
    public static NodeInfo best = null;

    public static void run(double[] para, BufferedWriter writer, LoadSatellite loadSatellite, LoadStation loadStation, List<Target> targets) throws IOException {
        // 随机卫星和地面站序列插入（复制一份 卫星和任务和地面站 在另外一个线程寻优）
        double bestFit = 0;
        List<Satellite> orderedSat = new ArrayList<>();
        List<Station> orderedStation = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            double[] temp = randomInsert(loadSatellite, loadStation, targets);
            if (temp[1] > bestFit) {
                bestFit = temp[1];
                orderedSat.clear();
                orderedStation.clear();
                orderedSat.addAll(loadSatellite.satelliteList);
                orderedStation.addAll(loadStation.stationList);
            }
        }

        // 根据最佳序列计算卫星的评分(历史得分)
        UpdateSatelliteProperties(loadSatellite.satelliteList);
        for (int i = 0; i < orderedSat.size(); i++) {
            Satellite satellite = orderedSat.get(i);
            satellite.getProperties().put("HistoryScore", 1 * Math.pow(0.9, i));
        }

        GeneticProgramming gp = new GeneticProgramming(para, writer);
        gp.evolve(loadSatellite, targets, loadStation);

        best = gp.hisBest;

        // 优化任务序列插入序列
        double[] fit = gp.fitness(best.node, loadSatellite, targets, loadStation);
        writer.write(fit[0] + "," + fit[1] + "\n");
        writer.flush();
        orderedSat.clear();
        orderedStation.clear();
        orderedSat.addAll(loadSatellite.satelliteList);
        orderedStation.addAll(loadStation.stationList);

        // 再优化任务的序列
        /*long start = System.currentTimeMillis();
        for (int i = 0; i < para[7]; i++) {
            Collections.shuffle(targets);
            for (Target target : targets) target.reSet();
            loadStation.reSet();
            loadSatellite.reSet();
            double[] complete = decodeTask(targets, orderedSat, orderedStation);
            double Time = (System.currentTimeMillis() - start) / 1000.0;
            writer.write(complete[0] + "," + complete[1] + "," + Time + "\n");
            writer.flush();
            if (complete[1] == targets.size()) break;
        }*/
    }
}
