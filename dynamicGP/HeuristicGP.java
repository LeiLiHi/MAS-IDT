package org.lileischeduler.dynamicGP;

import org.lileischeduler.dynamicGP.generateData.GenerateTargets;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.model.TransitionWindow;
import org.lileischeduler.dynamicGP.solve.operator.Insert;
import org.lileischeduler.tool.Tool;

import java.io.IOException;
import java.util.*;

public class HeuristicGP {
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();

        // 初始化satellite  station  targets
        LoadSatellite loadSatellite = new LoadSatellite(15);
        LoadStation loadStation = new LoadStation(4, "basicalData\\");
        List<Target> generateTargets = new GenerateTargets(" ", 1, 1).generateStaticTargets(500, "全球分布");

        // 计算所有地面站对卫星的数传窗口
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
        loadSatellite.satelliteList.parallelStream().forEach(satellite ->
                // 内层：对于每个卫星，顺序处理所有目标（单线程）
                generateTargets.forEach(target ->
                        satellite.calculateWindows(target)
                )
        );

        // 随机卫星和地面站序列插入（复制一份 卫星和任务和地面站 在另外一个线程寻优）
        double bestFit = 0;
        List<Satellite> orderedSat = new ArrayList<>();
        List<Station> orderedStation = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
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
        // 所有属性归一化，统计数传覆盖范围
        // 卫星属性（数传窗口总时长，轨道参数：高度、倾角，传输速度，固存，能源，机动能力（成像和数传），历史评分）
        GeneticProgramming gp = new GeneticProgramming();
        gp.evolve(loadSatellite, generateTargets, loadStation);

        // 再优化任务的序列
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(generateTargets);
            for (Target target : generateTargets) target.reSet();
            loadStation.reSet();
            loadSatellite.reSet();
            double[] complete = decodeTask(generateTargets, orderedSat, orderedStation);
            System.out.println("Fixed Sat Generation：" + i + "  完成：" + complete[0] + " 收益：" + complete[1]);
        }

        // 再优化任务和地面站的序列
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(generateTargets);
            Collections.shuffle(orderedStation);
            for (Target target : generateTargets) target.reSet();
            loadStation.reSet();
            loadSatellite.reSet();
            double [] complete = decodeTask(generateTargets, orderedSat, orderedStation);
            System.out.println("Fixed Sat Generation：" + i + "  完成：" + complete[0] + " 收益：" + complete[1]);
        }

        long end = System.currentTimeMillis();
        double consume = (end - start) / 1000.0;
        System.out.println("总耗时为：" + consume);
    }

    public static void UpdateSatelliteProperties(List<Satellite> satelliteList) {
        // 计算每个卫星数传窗口的总时长
        for (Satellite satellite : satelliteList) {
            double aa = 0;
            double times = 0;
            for (List<TransitionWindow> trans : satellite.transitionWindows.values()) {
                times += trans.size();
                for (TransitionWindow tran : trans) {
                    aa += tran.duration;
                }
            }
            satellite.getProperties().put("DownloadDuration", aa);
            satellite.getProperties().put("DownloadNum", times);
        }

        Map<String, List<Double>> attributeValues = new HashMap<>();
        // 将所有卫星属性按照最大最小进行归一化
        for (Satellite satellite : satelliteList) {
            Map<String, Double> props = satellite.getProperties();
            for (Map.Entry<String, Double> entry : props.entrySet()) {
                String attr = entry.getKey();
                Double val = entry.getValue();
                attributeValues.computeIfAbsent(attr, k -> new ArrayList<>()).add(val);
            }
        }

        // 2. 计算各属性的最大最小值
        Map<String, Double> maxMap = new HashMap<>();
        Map<String, Double> minMap = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : attributeValues.entrySet()) {
            String attr = entry.getKey();
            List<Double> values = entry.getValue();
            if (!values.isEmpty()) {
                maxMap.put(attr, Collections.max(values));
                minMap.put(attr, Collections.min(values));
            }
        }

        // 3. 对每个卫星进行归一化
        for (Satellite satellite : satelliteList) {
            Map<String, Double> props = satellite.getProperties();
            Map<String, Double> normalized = new HashMap<>();
            for (Map.Entry<String, Double> entry : props.entrySet()) {
                String attr = entry.getKey();
                Double val = entry.getValue();
                Double max = maxMap.get(attr);
                Double min = minMap.get(attr);
                if (max == null || min == null) {
                    // 若属性未被其他卫星定义，保留原值或抛异常
                    normalized.put(attr, val);
                    continue;
                }
                double range = max - min;
                double normVal = (range != 0) ? ((val - min) / range) : 0.0; // 处理零值范围
                normalized.put(attr, normVal);
            }
            // 更新卫星属性（假设支持直接修改Map）
            props.clear();
            props.putAll(normalized);
        }

    }


    public static double[] decodeTask(List<Target> generateTargets, List<Satellite> orderedSat, List<Station> orderedStation) {
        double profit = 0, complete = 0;
        for (Target target : generateTargets) {
            // 1.1 成像资源（选择卫星）
            boolean isInserted;
            for (Satellite satellite : orderedSat) {
                satellite.calculateWindows(target);                  // 计算任务的成像资源
                // 1.1.1 插入到最早的可行时间窗口（调出卫星的scheduledList，只能按照顺序插入，不能改变之前的任务时间）
                isInserted = new Insert().insertEarliest(target, satellite.scheduledTargets);
                if (isInserted) {
                    boolean isArranged = false;
                    for (Station station : orderedStation) {
                        isArranged = station.isFeasible0(target);
                        if (isArranged) {           // 1.2.1 数传规划成功，停止找数传站
                            profit += target.priority;
                            complete += 1;
                            break;
                        }
                    }
                    if (!isArranged) {              // 1.2.2 数传规划失败，放弃成像，下一颗卫星或者停止
                        target.imageChance.imageWindow.satellite.scheduledTargets.remove(target);
                        target.reSet();
                    } else break;                   // 否则 停止迭代卫星
                }
            }
        }
        return new double[]{complete, profit};
    }

    public static double[] randomInsert(LoadSatellite loadSatellite, LoadStation loadStation, List<Target> generateTargets) {
//        long start = System.currentTimeMillis();

        for (Target target : generateTargets) target.reSet();
        loadStation.reSet();
        loadSatellite.reSet();
        double[] completeNum;
        List<Satellite> satelliteList = loadSatellite.satelliteList;
        List<Station> stationList = loadStation.stationList;
        Collections.shuffle(stationList, Tool.random);
        Collections.shuffle(satelliteList, Tool.random);
        completeNum = decodeTask(generateTargets, satelliteList, stationList);

//        long end = System.currentTimeMillis();
//        double consume = (end - start) / 1000.0;
//        System.out.println("成像数传完成：" + completeNum[0] + " 收益：" + completeNum[1] + " 耗时：" + consume + "s");
        return completeNum;
    }
}