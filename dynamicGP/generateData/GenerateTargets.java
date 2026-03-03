package org.lileischeduler.dynamicGP.generateData;

import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.tianZhi.model.Point;

import java.util.*;

import static org.lileischeduler.dynamic.generateData.dataTool.latLonToECEF;

public class GenerateTargets {
    public Random rand = new Random(50);
    public List<Integer> timeLine;                          // 动态任务到达的时间线
    public Map<Integer, List<Target>> targets;              // 动态到达目标表

    /**
     * 给出当前生成的目标信息
     */
    public void information() {
        System.out.println("动态到达的时间列表为：" + timeLine);
        System.out.println("动态到达的时间列表大小为：" + timeLine.size());
        int targetNum = 0;
        List<Integer> targetsNum = new ArrayList<>();
        List<Integer> targetSize = new ArrayList<>();
        List<Double> timeDoubles = new ArrayList<>();
        List<Double> benefits = new ArrayList<>();
        for (Integer i : timeLine) {
            double roundedValue = Math.round((i/3600.0) * 10) / 10.0;
            timeDoubles.add(roundedValue);
        }
        for (List<Target> targetList : targets.values()) {
            double profit = 0;
            for (Target target : targetList) {
                profit += target.priority + target.timeEfficiency;
            }
            benefits.add(profit);
            targetsNum.add(targetNum);
            targetSize.add(targetList.size());
            targetNum += targetList.size();
        }
        System.out.println("动态到达任务总数为：" + targetNum);
        System.out.println("任务数量列表：" + targetsNum);
        System.out.println("到达任务个数列表" + targetSize);
        System.out.println("收益列表为：" + benefits);
        System.out.println("小时制：" + timeDoubles);
    }

    /**
     * 目标构造函数
     *
     * @param label     区域分布 or 全球分布
     * @param baseNum   基础目标数量
     * @param randomNum 随机目标数量
     */
    public GenerateTargets(String label, int baseNum, int randomNum) {
        timeLine = timePoint();
        targets = new HashMap<Integer, List<Target>>();
        for (Integer integer : timeLine) {
            List<Target> tempTargets = generateTarget(rand.nextInt(randomNum) + baseNum, integer, label, 7200);
            tempTargets.sort(Comparator.comparing(Target::getPriority).reversed());                     // 按照收益排序
            targets.put(integer, tempTargets);
        }
    }


    /**
     * 生成动态到达的目标
     *
     * @param num        批次到达的任务数量
     * @param arriveTime 任务的到达时间
     * @param label      到达类型
     * @param duration   任务的持续时间
     * @return 返回当前的任务集合
     */
    public List<Target> generateTarget(int num, int arriveTime, String label, int duration) {
        List<Target> targets = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            double latitude;
            double longitude;
            Target target = new Target();
            if (label.equals("区域分布")) {
                latitude = rand.nextDouble() * 50 + 3;
                longitude = rand.nextDouble() * 60 + 73;
            } else {
                latitude = (rand.nextDouble() - 0.5) * 106;
                longitude = (rand.nextDouble() - 0.5) * 360;
            }
            target.setRequestResolution(rand.nextInt(3) + 1);           // 目标所需的分辨率
            target.setPriority(rand.nextInt(10) + 1);                    // 目标的优先级
            target.timeEfficiency = rand.nextInt(10) + 1;

            arriveTime = rand.nextInt(16 * 3600);
            target.setArrivedTime(arriveTime);                               // 目标到达时间

            duration = 6 * 3600;
            target.setDeadline(Math.min(arriveTime + duration, 86399));      // 目标的截止提交时间

            target.durationTime = rand.nextInt(5) + 15;                // 任务的持续时间
            target.unitScore = target.priority / target.durationTime;        // 任务的单位收益

            double[] xyz = latLonToECEF(latitude, longitude, 6371.0);
            target.setPoint(new Point(xyz[0], xyz[1], xyz[2]));                  // 点的维度和经度

            targets.add(target);
        }
        return targets;
    }

    public List<Target> generateStaticTargets(int num, String label) {
        return generateTarget(num, 0, label, 24 * 3600);
    }


    /**
     * 生成动态到达的时间点
     * @return 时间列表
     */
    public List<Integer> timePoint() {
        List<Integer> timePoints = new ArrayList<>();
        int currentTime = 0;
        do {
            timePoints.add(currentTime);
            currentTime = currentTime + 1800 + rand.nextInt(900);
        } while (currentTime <= 23 * 3600);
        return timePoints;
    }
}
