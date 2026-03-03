package org.lileischeduler.dynamicGP;

import opennlp.tools.namefind.RegexNameFinder;
import org.lileischeduler.RandomConfig;
import org.lileischeduler.RandomConfig.Algorithm;
import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.algorithm4Hierarchical.*;
import org.lileischeduler.dynamicGP.solve.algorithm4Integrated.IGA;
import org.lileischeduler.dynamicGP.solve.algorithm4Integrated.VNS;
import org.nd4j.shade.yaml.snakeyaml.Yaml;
import scala.Int;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class APPPlus {
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
                satSet.loadSatellite(satNum);
                LoadStation staSet = new LoadStation(staNum, config.getCsvPath());
                GenerateTargets ge = new GenerateTargets(" ", 1, 1);
                ge.rand.setSeed(randNum + k);
                int tarNumFinal = tar.get(k);
                System.out.println(tarNumFinal);
                // 每个卫星和地面站 目标数量随机10个（9个卫星和地面站规模 交替对应 密集和稀疏场景）
                List<Target> targets = ge.generateStaticTargets(tarNumFinal, distribution);
                calWindows(satSet, staSet, targets);
                for (String alName : config.getAlgorithms().keySet()) {
                    for (int j = 0; j < config.getRunTimes(); j++) {
                        satSet.reSet();
                        staSet.reSet();
                        for (Target target : targets) target.reSet();
                        String fileName = "/" + alName + "-" + tarNumFinal + ".csv";
                        System.out.println(alName + j + " start running！");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(savePath + fileName, true));
                        writer.write(alName + "," + j + ",generation" + "\n");
                        switch (alName) {
                            case "H-ALNS" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                HALNS halns = new HALNS();
                                halns.run(para, writer, satSet, staSet, targets);
                            }
                            case "H-ILS" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                HILS hils = new HILS();
                                hils.run(para, writer, satSet, staSet, targets);
                            }
                            case "H-LAHC" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                HLAHC hlahc = new HLAHC();
                                hlahc.run(para, writer, satSet, staSet, targets);
                            }
                            case "I-IGA" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                IGA.run(para, writer, satSet, staSet, targets);
                            }
                            case "I-VNS" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                VNS.run(para, writer, satSet, staSet, targets);
                            }
                            case "LLM-GP" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                LLMGP.run(para, writer, satSet, staSet, targets);
                            }
                            case "B-VNS" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                BVNS bvns = new BVNS();
                                bvns.run(para, writer, satSet, staSet, targets);
                            }
                            case "B-RLGA" -> {
                                Algorithm algorithm = config.getAlgorithms().get(alName);
                                double[] para = algorithm.getAlgorithmPara();
                                BRLGA brlga = new BRLGA();
                                brlga.run(para, writer, satSet, staSet, targets);
                            }
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
