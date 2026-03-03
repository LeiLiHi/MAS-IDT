package org.lileischeduler.dynamicGP.model;

import lombok.Getter;
import lombok.Setter;
import org.lileischeduler.dynamicGP.solve.operator.Insert;
import org.lileischeduler.tianZhi.model.Point;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.lileischeduler.dynamic.generateData.dataTool.*;

@Setter
@Getter
public class Satellite {
    private double EARTH_RADIUS = 6371.0;
    public double transitionGround = 30;                                      // 卫星切换到不同地面站的时间
    public double maxStorage = 5000;                                          // 最大的固存
    public double maxEnergy = 3000;                                         // 单轨最大电量
    public int id;                                                          // 卫星的id
    public double imageTransitionRate = 3.5;                                // 成像数传时间比
    public String satelliteName;                                            // 卫星名称
    public int resolution;                                                  // 卫星的分辨率
    public List<double[]> orbitData = new ArrayList<>();                    // 卫星的轨道数据 （xyz 地理坐标）
    public List<Target> newArrival = new ArrayList<>();                     // 新到达的任目标（待规划1）
    public List<Target> currentTargets = new ArrayList<>();                 // 现有未规划目标（待规划2-重分配）
    public List<Target> scheduledTargets = new ArrayList<>();               // 已规划目标（首尾插入虚拟任务，尽量不可修改）
    public List<Target> finishedTargets = new ArrayList<>();                // 已完成的目标（时间点前的所有目标，不可修改）
    public List<Target> failedTargets = new ArrayList<>();                  // 已经失败无法被完成的任务
    public Map<Integer, List<TransitionWindow>> transitionWindows = new HashMap<>();        // 该卫星拥有的数传窗口


    /******** 辅助性能变量 ********/
    private final double[] satPos1 = new double[3];
    private final double[] satPos2 = new double[3];
    private final double[] targetPos = new double[3];
    private final Map<String, Double> properties = new HashMap<>();
    public double score = 0;


    /**
     * 初始化函数，输入卫星名称，记录卫星的轨道数
     * @param satelliteName 卫星名称
     */
    public Satellite(String dataPath, String satelliteName) {
        this.satelliteName = satelliteName;
        String filePath = dataPath + satelliteName + ".txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            double AveHeight = 0.0;
            int count = 0;
            while ((line = br.readLine()) != null) {
                count++;
                // 按空格分割每一行
                String[] values = line.trim().split("\\s+");
                double[] row = new double[values.length];
                // 转换为 double 并保留 6 位小数,添加卫星的轨道高度
                AveHeight += Double.parseDouble(values[0]);
                for (int i = 0; i < values.length; i++) {
                    row[i] = Math.round(Double.parseDouble(values[i]) * 1000000.0) / 1000000.0;
                }
                double[] xyz = latLonToECEF(row[1], row[2], row[0] + EARTH_RADIUS);
                row[0] = xyz[0]; row[1] = xyz[1]; row[2] = xyz[2];
                orbitData.add(row); // 将转换后的行添加到列表中
            }
            this.properties.put("OrbitHeight", AveHeight/count);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTargets(int timeLine) {
        // 更新过时间点的目标以及资源（找到最晚的那个  当作参考标准）
        for (int i = scheduledTargets.size() - 1; i > -1; i--) {
            Target target = scheduledTargets.get(i);
            // 保留虚拟任务（防止ALNS）
            if (target.priority == 0.0) continue;
            // 这个任务已经在时间维度上面被完成，被移出后可以不用再管
            if (target.imageChance != null && target.imageChance.startTime <= timeLine) {
                // 二分查找得到目标的插入位置
                int index = new Insert().findInsertPosition(finishedTargets, scheduledTargets.get(i));
                scheduledTargets.remove(i);
                finishedTargets.add(index, target);
            }
        }
        ImageChance latestChance;
        if (finishedTargets.isEmpty()) {
            latestChance = null;
        } else {
            latestChance = finishedTargets.get(finishedTargets.size() - 1).imageChance;
        }

        for (Target target : scheduledTargets) {
            if (target.priority == 0.0) continue;
            updateTimeRevenue(target, timeLine);
            updateWindowsChances(target, latestChance, timeLine);
            updateChanceNum(target);
        }

        // 更新未完成任务资源,即现有剩余任务表和新到的任务
        for (int i = currentTargets.size() - 1; i > -1; i--) {
            Target target = currentTargets.get(i);
            updateTimeRevenue(target, timeLine);
            updateWindowsChances(target, latestChance, timeLine);
            updateChanceNum(target);
            /* 张子阳注释!
            if (target.optionalImageWindows.isEmpty()) {
                failedTargets.add(target);
                currentTargets.remove(i);
            }*/
        }

    }


    public void updateTargets0(int timeLine) {
        // 更新过时间点的目标以及资源（找到最晚的那个  当作参考标准）
        for (int i = scheduledTargets.size() - 1; i > -1; i--) {
            Target target = scheduledTargets.get(i);
            // 保留虚拟任务（防止ALNS）
            if (target.priority == 0.0) continue;
            // 这个任务已经在时间维度上面被完成，被移出后可以不用再管
            if (target.imageChance != null && target.imageChance.startTime <= timeLine) {
                // 二分查找得到目标的插入位置
                int index = new Insert().findInsertPosition(finishedTargets, scheduledTargets.get(i));
                scheduledTargets.remove(i);
                finishedTargets.add(index, target);
            }
        }
        ImageChance latestChance;
        if (finishedTargets.isEmpty()) {
            latestChance = null;
        } else {
            latestChance = finishedTargets.get(finishedTargets.size() - 1).imageChance;
        }

        for (Target target : scheduledTargets) {
            if (target.priority == 0.0) continue;
            updateTimeRevenue(target, timeLine);
            updateWindowsChances(target, latestChance, timeLine);
            updateChanceNum(target);
        }

        // 更新未完成任务资源,即现有剩余任务表和新到的任务
        for (int i = currentTargets.size() - 1; i > -1; i--) {
            Target target = currentTargets.get(i);
            updateTimeRevenue(target, timeLine);
            updateWindowsChances(target, latestChance, timeLine);
            updateChanceNum(target);
            if (target.optionalImageWindows.isEmpty()) {
                failedTargets.add(target);
                currentTargets.remove(i);
            }
        }

        for (int i = newArrival.size() - 1; i > -1; i--) {
            Target target = newArrival.get(i);
            updateTimeRevenue(target, timeLine);
            updateWindowsChances(target, latestChance, timeLine);
            updateChanceNum(target);
            if (target.optionalImageWindows.isEmpty()) {
                failedTargets.add(target);
                newArrival.remove(i);
            }
        }

    }
    public void updateTimeRevenue(Target target, int timeLine) {
        long tarA = target.getArrivedTime();
        long tarD = target.getDeadline();
        long fenmu = tarD - tarA - 1800;
        if (fenmu < 3600) fenmu = tarD - tarA;
        target.maxScore = target.priority * (tarD - timeLine) / fenmu;
    }
    public void updateChanceNum(Target target) {
        int num = 0;
        for (ImageWindow imageWindow : target.optionalImageWindows) {
            num += imageWindow.imageChanceList.size();
        }
        target.chanceNum = num;
    }

    /**
     * 更新目标的机会列表
     * @param target   目标
     * @param latestChance 已完成任务的最晚机会
     * @param timeLine 时间点
     */
    public void updateWindowsChances(Target target, ImageChance latestChance, int timeLine) {
        for (int j = target.optionalImageWindows.size() - 1; j > -1; j--) {
            ImageWindow imageWindow = target.optionalImageWindows.get(j);
            // 1. 跳过开始时间比当前时间点晚的窗口
            if (imageWindow.startTime - 120 > timeLine) {
                continue;
            }
            // 2. 如果窗口的结束时间早于当前时间点，则直接移除
            if (imageWindow.endTime <= timeLine) {
                target.optionalImageWindows.remove(j);
                continue;
            }
            // 3. 筛选窗口内的成像机会
            List<ImageChance> chanceList = imageWindow.imageChanceList;
            for (int i = chanceList.size() - 1; i >= 0; i--) {
                ImageChance chance = chanceList.get(i);
                // 移除机会：机会开始时间早于当前时间点或与最新机会冲突
                boolean isInvalid = chance.startTime <= timeLine || !new Insert().ConflictCheck(latestChance, chance);
                if (isInvalid) {
                    chanceList.remove(i); // 从机会列表中移除
                }
            }
            // 4. 如果机会列表不为空，更新窗口的开始时间
            if (!chanceList.isEmpty()) {
                imageWindow.startTime = chanceList.get(0).startTime;
            }
            // 5. 如果机会列表为空，移除该窗口
            if (chanceList.isEmpty()) {
                target.optionalImageWindows.remove(j);
            }
        }
        // 6. 对窗口重新编号
        int id = 0;
        for (ImageWindow window : target.optionalImageWindows) {
            window.windowId = id++;
        }
    }

    /**
     * 计算当前目标的时间窗口(根据目标的到达和结束时间)
     *
     * @param target 已分配目标
     */
    public void calculateWindows(Target target) {
        calculateWindows(target, target.getDeadline() - target.getArrivedTime(), false);
    }
    public void calculateWindows(Target target, int timeRange, boolean reCalculate) {
        long duration = target.durationTime;
        // 0. 判断当前任务是否曾被计算过时间窗口，是则直接将机会调整给当前窗口
        target.optionalImageWindows.clear();
        if (target.getSatWindows().get(this) != null && !reCalculate) {
            target.getOptionalImageWindows().addAll(target.getSatWindows().get(this));
            return;
        }
        // 1. 获取目标的点位信息：地理坐标（维度，经度，海拔）
        Point position = target.getPoint();                     // 目标的坐标位置
        double[] tempAngles;
        targetPos[0] = position.getX();
        targetPos[1] = position.getY();
        targetPos[2] = position.getZ();
        int prei = -2;                                          // 判断是否有新的成像窗口出现
        List<ImageWindow> windows = target.optionalImageWindows;
        // 2. 计算可见时间窗口 循环卫星的轨道数据（根据任务的到达时间进行计算）
        ImageWindow window = null;
        for (int i = target.getArrivedTime(); i < target.getArrivedTime() + timeRange; i++) {
            // 如果是光学卫星，判断太阳高度角
            double sunlight = orbitData.get(i)[3];
            if (this.satelliteName.contains("OPT") && sunlight < 8) {
                if (sunlight < 0) i = i + 40;
                if (8 - sunlight > 4 ) i = i + 40;
                continue;
            }

            // 或者使用 System.arraycopy（创建新对象速度会慢很多！）
             System.arraycopy(orbitData.get(i), 0, satPos1, 0, 3);
             System.arraycopy(orbitData.get(i + 1), 0, satPos2, 0, 3);

            boolean isVisible = isTargetVisible(satPos1, targetPos);
            if (isVisible) {
                tempAngles = calculateAngles(satPos1, satPos2, targetPos);
                if (Math.abs(tempAngles[0]) > 45 || Math.abs(tempAngles[1]) > 30 || Math.abs(tempAngles[2]) > 45) {
                    if (Math.abs(tempAngles[0]) - 45 > 5 || Math.abs(tempAngles[1]) - 30 > 5 || Math.abs(tempAngles[2]) - 45 > 5) i += 20;
                    continue;
                }
                // 找到上一个成像机会的角度 每秒角度变化超过直接跳过
                ImageChance imageChance = new ImageChance();                    // 当前秒可见，生成成像机会
                imageChance.startTime = i;                                      // 设置机会的开始时间
                imageChance.endTime = i + duration - 1;                         // 机会的结束时间
                imageChance.pitchAngle = tempAngles[0];                         // 设定俯仰和偏航角
                imageChance.yawAngle = tempAngles[1];
                if (prei + 1 != i) {                                            // 第一个窗口或者新窗口出现
                    window = new ImageWindow();
                    window.startTime = i;                                       // 设置窗口的开始时间
                    windows.add(window);                                        // 目标的有效窗口列表
                }

                if (!window.imageChanceList.isEmpty()) {                        // 秒后变化过大窗口不要 直接跳过
                    int size = window.imageChanceList.size() - 1;
                    ImageChance ce = window.imageChanceList.get(size);
                    double absYaw = Math.abs(ce.yawAngle - imageChance.yawAngle);
                    double absPitch = Math.abs(ce.pitchAngle - imageChance.pitchAngle);
                    double absAngle = absYaw + absPitch;
                    if (absAngle > 0.35 || absPitch > 0.2 || absYaw > 0.2) continue;
                }
                window.imageChanceList.add(imageChance);
                imageChance.imageWindow = window;
                prei = i;
            } else i += 60;
        }
        // 删除 duration 小于 30 的窗口
        Iterator<ImageWindow> iterator = windows.iterator();
        ImageWindow preWindow = null;
        while (iterator.hasNext()) {
            window = iterator.next();
            int lastIndex = window.imageChanceList.size() - 1;
            window.endTime = window.imageChanceList.get(lastIndex).startTime;    // 窗口结束时间
            window.duration = window.endTime - window.startTime + 1;             // 窗口的持续时间
            window.windowTd = (int) (window.startTime / 5400);
            if ((preWindow != null && window.startTime - preWindow.endTime < 2000) || window.getDuration() < Math.max(50, 4 * target.durationTime)) {
                iterator.remove();
            } else {
                window.satellite = this;                                              // 设置窗口所属卫星
                preWindow = window;
            }
        }
        // 重新设置 ID
        for (int i = 0; i < windows.size(); i++) {
            window = windows.get(i);
            window.windowId = i; // 重新设置 ID 为新索引
        }
        target.getSatWindows().put(this, new ArrayList<>(windows));
    }
}
