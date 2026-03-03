package org.lileischeduler.dynamicGP;

import org.lileischeduler.AlgorithmConfig;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class APP {
    public static void main(String[] args) throws IOException {
        AlgorithmConfig config = null;
        try {
            String path = "config.yaml";
            if (args.length > 0) path = args[0];
            InputStream inputStream = new FileInputStream(path);
            Yaml yaml = new Yaml();
            config = yaml.loadAs(inputStream, AlgorithmConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert config != null;
        for (int i = 0; i < config.getSatelliteNum().size(); i++) {
            int satNum = config.getSatelliteNum().get(i);
            int staNum = config.getStationNum().get(i);
            int tarNum = config.getTargetNum().get(i);
            String distribution = config.getDistribution();

            LoadSatellite satSet = new LoadSatellite(config.getSatPath(), config.getCsvPath());
            satSet.loadSatellite(satNum);
            LoadStation staSet = new LoadStation(staNum, config.getCsvPath());
            List<Target> targets = new GenerateTargets(" ", 1, 1).generateStaticTargets(tarNum, distribution);

            calWindows(satSet, staSet, targets);


            String savePath = config.getSavePath() + satNum + "_" + staNum + "_" + tarNum + "_" + distribution;
            if (Files.notExists(Paths.get(savePath)))  Files.createDirectories(Paths.get(savePath));

            for (String alName : config.getAlgorithms().keySet()) {
                for (int j = 0; j < config.getRunTimes(); j++) {
                    satSet.reSet();
                    staSet.reSet();
                    for (Target target : targets) target.reSet();

                    String fileName = "/" + alName + ".csv";
                    System.out.println(alName + j + " start running！");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(savePath + fileName, true));
                    writer.write(alName + "," + j + ",generation" + "\n");
                    switch (alName) {
                        case "H-ALNS" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            HALNS halns = new HALNS();
                            halns.run(para, writer, satSet, staSet, targets);
                        }
                        case "H-ILS" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            HILS hils = new HILS();
                            hils.run(para, writer, satSet, staSet, targets);
                        }
                        case "H-LAHC" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            HLAHC hlahc = new HLAHC();
                            hlahc.run(para, writer, satSet, staSet, targets);
                        }
                        case "I-IGA" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            IGA.run(para, writer, satSet, staSet, targets);
                        }
                        case "I-VNS" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            VNS.run(para, writer, satSet, staSet, targets);
                        }
                        case "LLM-GP" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            LLMGP.run(para, writer, satSet, staSet, targets);
                        }
                        case "B-VNS" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            BVNS bvns = new BVNS();
                            bvns.run(para, writer, satSet, staSet, targets);
                        }
                        case "B-RLGA" -> {
                            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get(alName);
                            double[] para = algorithm.getAlgorithmPara();
                            BRLGA brlga = new BRLGA();
                            brlga.run(para, writer, satSet, staSet, targets);
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
