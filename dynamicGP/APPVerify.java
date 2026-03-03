package org.lileischeduler.dynamicGP;

import org.lileischeduler.RandomConfig;
import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Node;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.nd4j.shade.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lileischeduler.dynamicGP.HeuristicGP.UpdateSatelliteProperties;
import static org.lileischeduler.dynamicGP.HeuristicGP.decodeTask;

public class APPVerify {
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
                for (Satellite satellite : satSet.satelliteList) {
                    satellite.getProperties().put("HistoryScore", 0.5);
                    // 终端消融实验
                    if (config.getRunTimes() > 1) satellite.getProperties().put("DownloadNum", 0.0);
                    if (config.getRunTimes() > 3) satellite.getProperties().put("EnergyConsumption", 0.0);
                    if (config.getRunTimes() > 4) satellite.getProperties().put("Storage", 0.0);
                    if (config.getRunTimes() > 5) satellite.getProperties().put("Energy", 0.0);
                    if (config.getRunTimes() > 6) satellite.getProperties().put("Inclination", 0.0);

                    /*if (config.getRunTimes() == 2) satellite.getProperties().put("DownloadNum", 1.0);
                    if (config.getRunTimes() == 3) satellite.getProperties().put("HistoryScore", 1.0);
                    if (config.getRunTimes() == 4) satellite.getProperties().put("EnergyConsumption", 1.0);
                    if (config.getRunTimes() == 5) satellite.getProperties().put("Storage", 1.0);
                    if (config.getRunTimes() == 6) satellite.getProperties().put("Energy", 1.0);
                    if (config.getRunTimes() == 7) satellite.getProperties().put("Inclination", 1.0);*/
                }


                LoadStation staSet = new LoadStation(staNum, config.getCsvPath());
                GenerateTargets ge = new GenerateTargets(" ", 1, 1);
                ge.rand.setSeed(randNum + k);
                int tarNumFinal = tar.get(k);
                System.out.println(tarNumFinal);
                // 每个卫星和地面站 目标数量随机10个（9个卫星和地面站规模 交替对应 密集和稀疏场景）
                List<Target> targets = ge.generateStaticTargets(tarNumFinal, distribution);
                calWindows(satSet, staSet, targets);
                // for循环 读取txt文件
                long start = System.currentTimeMillis();
                String fileName = satNum + "-" + staNum + "-" + tarNumFinal + "-" + (config.getRunTimes() - 1) + "00-verify.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(savePath + "/" + fileName, false));
                try {
                    List<String> lines = Files.readAllLines(Paths.get(savePath + "/" + satNum + "-" + staNum + "-" + tarNum + "-rules.txt"));
                    for (String line : lines) {
                        Node node = new extractExpressionToTree().parse(line.split("\n")[0].split(":")[0]);
                        for (Satellite satellite : satSet.satelliteList) {
                            satellite.score = node.evaluate(satellite.getProperties());
                        }
                        satSet.satelliteList.sort((s1, s2) ->
                                Double.compare(s2.score, s1.score));
                        for (Target target : targets) target.reSet();
                        staSet.reSet();
                        satSet.reSet();
                        double[] fit = decodeTask(targets, satSet.satelliteList, staSet.stationList);
                        long end = System.currentTimeMillis();
                        double time = (end - start) / 1000.0;
                        writer.write(fit[0] + "," + fit[1] + "," + time + "\n");
                        writer.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    static void calWindows(LoadSatellite loadSatellite, LoadStation loadStation, List<Target> generateTargets) {
        for (Satellite satellite : loadSatellite.satelliteList) {
            for (Station station : loadStation.stationList) {
                station.transitionWindows(satellite);
            }
        }
        for (Target target : generateTargets) {
            for (Satellite satellite : loadSatellite.satelliteList) {
                satellite.calculateWindows(target);
            }
        }
    }

}
