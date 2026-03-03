package org.lileischeduler.dynamicGP;
import org.lileischeduler.RandomConfig;
import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical.*;
import org.lileischeduler.dynamicGP.solve.algorithm4Integrated.IGA;
import org.nd4j.shade.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class APPTrain {
    public static void main(String[] args) throws IOException {
        RandomConfig config = null;
        try {
            String path = "configPlusTrain.yaml";
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
            LoadSatellite satSet = new LoadSatellite(config.getSatPath(), config.getCsvPath());
            satSet.loadSatellite(satNum);
            LoadStation staSet = new LoadStation(staNum, config.getCsvPath());
            GenerateTargets ge = new GenerateTargets(" ", 1, 1);
            ge.rand.setSeed(randNum + 101);
            // 每个卫星和地面站 目标数量随机10个（9个卫星和地面站规模 交替对应 密集和稀疏场景）
            List<Target> targets = ge.generateStaticTargets(tarNum, distribution);
            calWindows(satSet, staSet, targets);
            for (String alName : config.getAlgorithms().keySet()) {
                for (int j = 0; j < config.getRunTimes(); j++) {
                    satSet.reSet();
                    staSet.reSet();
                    for (Target target : targets) target.reSet();
                    String fileName = "/" + alName + "-" + "train.csv";
                    String rulePath = "/" + satNum + "-" + staNum + "-" + tarNum + "-rules.txt";
                    System.out.println(alName + j + " start running！");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(savePath + fileName, true));
                    BufferedWriter ruleWriter = new BufferedWriter(new FileWriter(savePath + rulePath, true));
                    writer.write(alName + "," + j + ",generation" + "\n");
                    switch (alName) {
                        case "LLM-GP" -> {
                            RandomConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            LLMGP gp = new LLMGP();
                            gp.run(para, writer, satSet, staSet, targets);
                            System.out.println(gp.best.node.toExpression());
                            ruleWriter.write(gp.best.node.toExpression() + "\n");
                            ruleWriter.flush();
                        }
                    }
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
