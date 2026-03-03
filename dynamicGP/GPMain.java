package org.lileischeduler.dynamicGP;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.lileischeduler.dynamicGP.LLMChat.AiAssistant;
import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.*;
import org.lileischeduler.tool.Tool;
import org.nd4j.shade.jackson.dataformat.yaml.YAMLFactory;


import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.lileischeduler.dynamicGP.HeuristicGP.*;
import static org.lileischeduler.tool.Tool.random;

public class GPMain {
    public static void main(String[] args) throws IOException {
        // 1 算法名称[x]  2 场景参数（卫星、数传、目标）[[1,2,3]] 3 算法参数[[0.1,3,2]]  4 路径
        LoadSatellite loadSatellite = new LoadSatellite(15);
        LoadStation loadStation = new LoadStation(4, "c");
        List<Target> generateTargets = new GenerateTargets(" ", 1, 1).generateStaticTargets(1000, "全球分布");

        // 计算所有地面站对卫星的数传窗口
        APP.calWindows(loadSatellite, loadStation, generateTargets);

        // 随机卫星和地面站序列插入（复制一份 卫星和任务和地面站 在另外一个线程寻优）
        double bestFit = 0;
        List<Satellite> orderedSat = new ArrayList<>();
        List<Station> orderedStation = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            double[] temp = randomInsert(loadSatellite, loadStation, generateTargets);
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

//        GeneticProgramming gp = new GeneticProgramming();
//        gp.evolve(loadSatellite, generateTargets, loadStation);

//        NodeInfo best = gp.hisBest;
//
//        // 优化任务序列插入序列
//        double[] fit = gp.fitness(best.node, loadSatellite, generateTargets, loadStation);

        orderedSat.clear();
        orderedStation.clear();
        orderedSat.addAll(loadSatellite.satelliteList);
        orderedStation.addAll(loadStation.stationList);

        // 再优化任务的序列
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(generateTargets);
            for (Target target : generateTargets) target.reSet();
            loadStation.reSet();
            loadSatellite.reSet();
            double[] complete = decodeTask(generateTargets, orderedSat, orderedStation);
            System.out.println("Fixed Sat Generation：" + i + "  完成：" + complete[0] + " 收益：" + complete[1]);
        }
    }
}

