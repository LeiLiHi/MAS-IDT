package org.lileischeduler.dynamicGP;

import org.lileischeduler.AlgorithmConfig;
import org.lileischeduler.RandomConfig;
import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Node;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Target;
import org.nd4j.shade.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.lileischeduler.dynamicGP.APP.calWindows;
import static org.lileischeduler.dynamicGP.HeuristicGP.UpdateSatelliteProperties;
import static org.lileischeduler.dynamicGP.HeuristicGP.decodeTask;

/**
 * 在线任务调度对比，用于对比其他规则的时候算法效果
 */
public class RuleEvaluate {
    public static void main(String[] args) throws IOException {
        RandomConfig config = null;
        try {
            String path = "configPlus.yaml";
            if (args.length > 0) path = args[0];
            InputStream inputStream = new FileInputStream(path);
            Yaml yaml = new Yaml();
            config = yaml.loadAs(inputStream, RandomConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert config != null;
        for (int i = 0; i < config.getSatelliteNum().size(); i++) {
            int satNum = config.getSatelliteNum().get(i);
            int staNum = config.getStationNum().get(i);
            int tarNum = config.getTargetNum().get(i);
            int randNum = config.getRandomNum().get(i);
            String distribution = config.getDistribution().get(i);
            String savePath = config.getSavePath() + satNum + "_" + staNum + "_" + tarNum + "_" + distribution;
            if (Files.notExists(Paths.get(savePath))) Files.createDirectories(Paths.get(savePath));
            List<Integer> tar = new ArrayList<>();
            Random rand = new Random(randNum);
            for (int j = 0; j < 10; j++) {
                int num = (int) (tarNum + (rand.nextDouble() - 0.2) * tarNum * 0.1);
                if (tar.contains(num)) {
                    j--;
                    continue;
                }
                tar.add(num);
            }
            System.out.println(tar.toString());
            // 循环目标场景（生成随机的目标数量 计算窗口）
            for (int k = 0; k < 10; k++) {
                LoadSatellite satSet = new LoadSatellite(config.getSatPath(), config.getCsvPath());
                // 根据最佳序列计算卫星的评分(历史得分)
                satSet.loadSatellite(satNum);
                UpdateSatelliteProperties(satSet.satelliteList);
                LoadStation staSet = new LoadStation(staNum, config.getCsvPath());
                GenerateTargets ge = new GenerateTargets(" ", 1, 1);

                ge.rand.setSeed(randNum + k);
                int tarNumFinal = tar.get(k);
                System.out.println(tarNumFinal);
                // 每个卫星和地面站 目标数量随机10个（9个卫星和地面站规模 交替对应 密集和稀疏场景）
                List<Target> targets = ge.generateStaticTargets(tarNumFinal, distribution);
                targets.sort((o1, o2) -> o1.getArrivedTime() - o2.getArrivedTime());
                calWindows(satSet, staSet, targets);
                // for循环 读取规则文件
                long start = System.currentTimeMillis();
                String fileName = satNum + "-" + staNum + "-" + tarNumFinal + "-" + (config.getRunTimes() - 1) + "00-verify.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(savePath + "/" + fileName, false));
                try {
                        Collections.shuffle(satSet.satelliteList);
                        for (Target target : targets) target.reSet();
                        staSet.reSet();
                        satSet.reSet();
                        double[] fit = decodeTask(targets, satSet.satelliteList, staSet.stationList);
                        long end = System.currentTimeMillis();
                        double time = (end - start) / 1000.0;
                        writer.write(fit[0] + "," + fit[1] + "," + time + "\n");
                        writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }
}
