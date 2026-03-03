package org.lileischeduler.dynamicGP.model;


import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.lileischeduler.dynamicGP.generateData.dataTool.*;

@Getter
@Setter
/**
 * 地面站类，计算和卫星的数传窗口，检验卫星当前的窗口是否符合约束
 */
public class Station {
    public int id;                                          // 地面站Id
    private double EARTH_RADIUS = 6371.0;                   // 地球半径
    public int timeZone = 3600;                             // 默认1小时作为时区划分
    public String stationName;                              // 地面站名称
    public double[] locations = new double[3];              // 地面站的坐标（维度、经度、高度）
    public double[] positions = new double[3];              // 地面站转换后的xyz坐标（xyz）
    public double maxElevation = 60;                        // 最大的仰角(90)
    public double minElevation = 0;                         // 最小的仰角(30)
    public long transitionTime;                             // 该地面站的窗口转换时间（秒）
    public List<Target> currentTargets = new ArrayList<>(); // 本轮解码该地面站接收到的任务

    /****** 中间变量 *****/
    private final double[] sat = new double[3];             // 缓存卫星的位置
    private List<TransitionWindow> windows = new ArrayList<>();

    /**
     * 检查当前任务是否能够安排数传窗口
     * @param target 待筹划任务
     * @return true 可以插入，false 不可以插入
     */
    public boolean isFeasible0(Target target) {
        if (target.imageChance == null) {
            System.out.println("该任务未安排成像，无需数传！！");
            return false;
        }
        // 是否存在窗口可行
        boolean isOK = false;
        long transitionStartTime = -1;
        long transitionDuration;
        long transitionEndTime = -1;
        TransitionWindow transitionWindowD = null;

        long imageStartTime = target.imageChance.startTime;
        // 在当前卫星上找该地面站的数传窗口并判断与当前的任务列表是否发生冲突
        Satellite satellite = target.imageChance.imageWindow.satellite;
        List<TransitionWindow> transitionWindows;
        if (satellite.transitionWindows.containsKey(this.id)) {
            transitionWindows = satellite.transitionWindows.get(this.id);
        } else return false;

        // 按照时间顺序插入
        List<Target> checkTargets = new ArrayList<>(currentTargets);
        for (Target target1 : satellite.scheduledTargets) {
            // 1. 暂未数传或者在同一个站上已安排则不加入
            if (target1.station == null || target1.station.equals(this)) continue;
            int index = downloadFind(checkTargets, target1);
            checkTargets.add(index, target1);
        }

        long wEndTime, wStartTime;                          // 数传窗口的结束和开始时间
        long iDuration = target.durationTime;               // 成像的持续时间
        double imaDataRate = satellite.imageTransitionRate; // 放录比 数传：成像
        for (TransitionWindow transitionWindow : transitionWindows) {
            // 1. 数传窗口结束小于成像开始:不可以使用，跳过
            wEndTime = transitionWindow.endTime;
            wStartTime = transitionWindow.startTime;
            if (wEndTime < imageStartTime) continue;

            // 2. 从开始区间开始尝试安排（当前任务的数传开始为 数传窗口的开始或者成像任务的开始）
            transitionStartTime = Math.max(wStartTime, imageStartTime);
            transitionDuration = (int) Math.ceil(iDuration * imaDataRate);
            transitionEndTime = transitionStartTime + transitionDuration;
            transitionWindowD = transitionWindow;
            if (transitionEndTime > wEndTime) continue;             // 需要的数传结束时间超出窗口的时间

            //  判断电量和固存是否满足（开始时间--卫星）
            if (!storageAndEnergy(transitionStartTime, transitionWindow, satellite)) continue;

            // 3. checkTargets 当前未安排任何任务，一定不会发生冲突
            if (checkTargets.isEmpty()) {
                isOK = true;
                break;
            }

            // 4. 确定影响该窗口的目标范围区间（按时间顺序排列）
            int startIndex = -1, endIndex = -1;
            for (int i = 0; i < checkTargets.size(); i++) {
                Target target1 = checkTargets.get(i);
                // 4.1 不在数传窗口区间的任务直接跳过： 目标的结束时间+转换时间小于窗口开始时间 窗口的结束时间 + 转换时间 小于目标的开始时间
                if (target1.transitionEndTime + Math.max(satellite.transitionGround, transitionTime) < wStartTime) {
                    startIndex = i;
                    continue;
                }
                endIndex = i;
                // 4.1.3 不会对目标造成影响
                if (target1.transitionStartTime > wEndTime + Math.max(transitionTime, satellite.transitionGround)) {
                    break;
                }
            }

            // 5 如果startIndex为-1：从0开始判断冲突；如果endIndex为-1, 说明所有待检查窗口都早于wEndTime
            if (endIndex == -1) {
                isOK = true;
                break;
            }
            if (startIndex == -1) startIndex += 1;                            // 开始索引从加1开始
            if (endIndex == 0) endIndex = 1;            // 只有1个任务(第一个任务没被跳过)

            // 1 是否该地面站上的任务冲突
            for (int i = startIndex; i <= endIndex; i++) {
                // 5.1 如果是第1个任务那么需要尝试插入到它的前面,检查与这个任务的冲突情况
                if (i == checkTargets.size()) break;
                Target target2 = checkTargets.get(i);
                if (i == startIndex) {              // 这种条件下默认实时传输：成像开始等于数传传输开始
                    boolean feasible = isFeasible0(transitionEndTime, satellite, target2);
                    if (feasible) {                 // 插入到这个位置，即任务i的前面
                        isOK = true;
                        break;
                    }
                }

                // 5.2 否则插入到target2的后面，target3的前面（根据前一个任务以及窗口时间确定下一个任务的开始时间）: 1 同星不同站 2 同站不同星 3 同星同站同窗口（无转换时间） 4 不同星也不同站，不可能出现
                if (i + 1 < checkTargets.size()) {
                    Target target3 = checkTargets.get(i + 1);
                    if (target3.transitionStartTime - target2.transitionEndTime < target.durationTime) {
                        continue;
                    }
                }

                if (satellite.equals(target2.getSatellite()) && !target2.station.equals(this)) {
                    transitionStartTime = (long) Math.max(target2.transitionEndTime + satellite.transitionGround, transitionStartTime);
                } else if (!satellite.equals(target2.getSatellite()) && target2.station.equals(this)) {
                    transitionStartTime = Math.max(target2.transitionEndTime + transitionTime, transitionStartTime);
                } else {
                    transitionStartTime = Math.max(target2.transitionEndTime, transitionStartTime);
                }
                transitionEndTime = transitionStartTime + transitionDuration;
                if (transitionEndTime > wEndTime) continue;
                if (!storageAndEnergy(transitionStartTime, transitionWindow, satellite)) continue;

                // 5.3 后面没有任务或者不会影响时，一定能够传输成功
                if (i + 1 == checkTargets.size()) {
                    isOK = true;
                    break;
                } else {
                    // 检查约束是否通过  fixme 后面的多个任务也可能存在冲突
                    if (isFeasible0(transitionEndTime, satellite, checkTargets.get(i + 1)) &&
                            (i + 2 >= checkTargets.size() || isFeasible0(transitionEndTime, satellite, checkTargets.get(i + 2))) &&
                            (i + 3 >= checkTargets.size() || isFeasible0(transitionEndTime, satellite, checkTargets.get(i + 3))) &&
                            (i + 4 >= checkTargets.size() || isFeasible0(transitionEndTime, satellite, checkTargets.get(i + 4))) &&
                            (i + 5 >= checkTargets.size() || isFeasible0(transitionEndTime, satellite, checkTargets.get(i + 5))) &&
                            (i + 6 >= checkTargets.size() || isFeasible0(transitionEndTime, satellite, checkTargets.get(i + 6)))) {
                        isOK = true;
                        break;
                    }
                }
            }
            if (isOK) break;
        }
        if (isOK) {
            target.station = this;
            target.transitionWindow = transitionWindowD;
            target.transitionEndTime = transitionEndTime;
            target.transitionStartTime = transitionStartTime;
            int fIndex = downloadFind(currentTargets, target);
            currentTargets.add(fIndex, target);
        } else {
            return false;
        }
        return true;
    }

    /**
     * 判断当前卫星的固存和电量是否满足约束
     * @param transitionStartTime   待判断目标的数传开始时间
     * @param transitionWindow      待判断的数传窗口
     * @param satellite             待判断的卫星
     * @return                      true为可行，false为不可行
     */
    public boolean storageAndEnergy(long transitionStartTime, TransitionWindow transitionWindow, Satellite satellite) {
        List<Target> targets = satellite.scheduledTargets;
        double occupied = 0, consumed = 0;
        for (Target target : targets) {
            if (target.imageChance == null || target.transitionWindow == null) continue;
            // 固存判断：小于数传开始时间的
            if (target.imageChance.startTime < transitionStartTime) {
                occupied += target.durationTime * 0.5;
            }
            if (target.transitionEndTime < transitionStartTime) {
                occupied -= target.durationTime * 0.5;
            }
            // 电量：只找同一圈的
            if (target.imageChance.imageWindow.windowTd == transitionWindow.windowTd) {
                consumed += target.durationTime * 0.8;
            }
            if (target.transitionWindow.windowTd == transitionWindow.windowTd) {
                consumed += (target.transitionEndTime - transitionStartTime) * 0.7;
            }
        }
        if (occupied > satellite.maxStorage) return false;
        return !(consumed > satellite.maxEnergy);
    }

    /**
     * 当前窗口是否可行的判断函数：true 可行 false 不可行
     * 作用： 判断 endTime 是否与 target 冲突， target 的数传时间默认在endTime的后面
     *
     * @param endTime          任务的数传结束时间
     * @param satellite        待插入任务的执行卫星
     * @param target           待判断的目标
     * @return true 为可行， false为不可行
     */
    public boolean isFeasible0(long endTime, Satellite satellite, Target target) {
        // 0. 当前的结束时间在目标数传开始时间的后面：冲突
        if (endTime > target.transitionStartTime) return false;
        if (satellite.equals(target.getSatellite()) && !target.station.equals(this)) {          // 同星不同站，卫星需要转换时间
            return endTime + satellite.transitionGround <= target.transitionStartTime;
        } else if (!satellite.equals(target.getSatellite()) && target.station.equals(this)) {   // 同站不同星，地面站需要转换时间
            return endTime + transitionTime <= target.transitionStartTime;
        } else {                                                                                // 否则不重叠即可
            return endTime <= target.transitionStartTime;
        }
    }

    /**
     * 地面站的初始化函数
     *
     * @param dataPath    数据路径(txt/csv) 描述地面站的属性参数
     * @param stationName 地面站名称
     */
    public Station(String dataPath, String stationName) {
        this.stationName = stationName;
        String filePath = dataPath + stationName + ".txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // 按空格分割每一行
                String[] values = line.trim().split("\\s+");
                timeZone = Integer.parseInt(values[0]);
                locations[0] = Double.parseDouble(values[1]);
                locations[1] = Double.parseDouble(values[2]);
                locations[2] = Double.parseDouble(values[3]);
                maxElevation = Double.parseDouble(values[4]);
                minElevation = Double.parseDouble(values[5]);
                transitionTime = (long) Double.parseDouble(values[6]);
                double[] xyz = latLonToECEF(locations[0], locations[1], locations[2] + EARTH_RADIUS);
                positions[0] = xyz[0];
                positions[1] = xyz[1];
                positions[2] = xyz[2];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给一行字符列表(参照 basicalData 下的station.csv的表头数据)
     * @param values    某地面站的信息
     */
    public Station(String[] values){
        id = Integer.parseInt(values[0]);
        stationName = values[1];
        timeZone = Integer.parseInt(values[2]);
        locations[0] = Double.parseDouble(values[3]);
        locations[1] = Double.parseDouble(values[4]);
        locations[2] = Double.parseDouble(values[5]);
        maxElevation = Double.parseDouble(values[6]);
        minElevation = Double.parseDouble(values[7]);
        transitionTime = (long) Double.parseDouble(values[8]);
        double[] xyz = latLonToECEF(locations[0], locations[1], locations[2] + EARTH_RADIUS);
        positions[0] = xyz[0];
        positions[1] = xyz[1];
        positions[2] = xyz[2];
    }

    /**
     * 计算与卫星之间的传输窗口
     *
     * @param satellite 卫星
     *                  返回的窗口储存到卫星上面
     */
    public void transitionWindows(Satellite satellite) {
        // 循环卫星的轨道数据（经过转换后的xyz数据）
        double[] satPos;
        TransitionWindow window = null;
        int prei = -2;
        windows.clear();
        for (int i = 0; i < satellite.orbitData.size(); i++) {
            // 计算可见性(可见的话的继续计算，不可见先跳过)
            satPos = satellite.orbitData.get(i);                            // 卫星当前的位置
            boolean isVisible = isTargetVisible(satPos, positions);         // 计算天线是否可见
            if (isVisible) {
                // 计算地面站天线的仰角,以地心和该点连线为中心线 计算夹角，方位角（固定转换时间，可以不用方位角）
                System.arraycopy(satPos, 0, sat, 0, 3);
                double angle = calculateAngle(positions, sat);
                if (angle - maxElevation > 20) i = i + 50;
                if (angle - maxElevation > 10) i = i + 20;
                if (angle - maxElevation > 5) i = i + 10;
                if (angle > maxElevation || angle < minElevation) continue;
                // 生成数传窗口 或者 更新最晚的结束时间
                if (prei + 1 != i) {
                    window = new TransitionWindow();
                    windows.add(window);
                    window.startTime = i;
                }
                window.endTime = i;
                prei = i;                                                   // 用来判断是否生成新的窗口
            }
        }
        // 删除 duration 小于 30 的窗口
        Iterator<TransitionWindow> iterator = windows.iterator();
        TransitionWindow preWindow = null;
        while (iterator.hasNext()) {
            window = iterator.next();
            window.duration = window.endTime - window.startTime + 1;                    // 窗口的持续时间
            if ((preWindow != null && window.startTime - preWindow.endTime < -1) || window.duration < 30) {
                iterator.remove();
            } else {
                window.station = this;                                              // 设置窗口所属卫星
                preWindow = window;
            }
        }
        // 更新数传窗口的Id并划分数据的区间
        Map<Integer, List<TransitionWindow>> satTrWindows = satellite.transitionWindows;
        for (int i = 0; i < windows.size(); i++) {
            TransitionWindow w = windows.get(i);
            w.windowId = i;
            // 放入到对应的时区
            w.windowTd = (int) w.startTime / timeZone;
            satTrWindows.computeIfAbsent(this.id, k -> new ArrayList<>());
            satTrWindows.get(this.id).add(w);
        }
    }

    // 根据数传开始时间找插入位置
    public int downloadFind(List<Target> list, Target newTarget) {
        int low = 0;
        int high = list.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (list.get(mid).transitionStartTime < newTarget.transitionStartTime) {
                low = mid + 1;  // newTarget 应在 mid 右侧
            } else {
                high = mid;  // newTarget 应在 mid 左侧
            }
        }
        return low;  // 返回插入位置
    }

    public boolean checkFeasible(boolean outputInfo) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Station station = this;
        transitionTime = station.transitionTime;
        currentTargets = station.currentTargets;

        System.out.println(station.stationName + "开始约束检查！   记录任务数为：" + station.currentTargets.size());
        if (station.currentTargets.isEmpty()) return true;              // 没有安排，则直接返回空

        for (int i = 0; i < currentTargets.size(); i++) {
            Target target = currentTargets.get(i);

            if (target.getImageChance() == null) {
                System.out.println("安排任务无成像机会！");
                return false;
            }
            if (target.transitionWindow == null) {
                System.out.println("安排任务无数传窗口！");
                return false;
            }
            if (target.imageChance.startTime > target.transitionStartTime) {
                System.out.println("安排任务成像时间晚于数传时间！");
                return false;
            }
            // 检查任务间的关系
            if (i == currentTargets.size() - 1) continue;
            Target target2 = currentTargets.get(i + 1);
            Satellite satellite = target.getImageChance().imageWindow.satellite;
            Satellite satellite2 = target2.getImageChance().imageWindow.satellite;
            if (satellite.equals(satellite2)) {             // 相同的卫星只需要结束小于开始
                if (target.transitionEndTime > target2.transitionStartTime) {
                    System.out.println(target.transitionEndTime);
                    System.out.println(target2.transitionStartTime);
                    System.out.println("任务前后时间不满足约束1！");
                    return false;
                }
            } else {
                if (target.transitionEndTime + transitionTime > target2.transitionStartTime) {
                    System.out.println("前：" + target.transitionEndTime);
                    System.out.println("后：" + target2.transitionStartTime);
                    System.out.println("转：" + transitionTime);
                    System.out.println("任务前后时间不满足约束2！");
                    return false;
                }
            }
            if (outputInfo) {
                String imageBegin = format.format(target.imageChance.startTime * 1000);
                String imageEnd = format.format(target.imageChance.endTime * 1000);
                String dataTranBegin = format.format(target.transitionStartTime * 1000);
                String dataTranEnd = format.format(target.transitionEndTime * 1000);
                System.out.println(satellite.satelliteName
                        + " 成像：" + imageBegin
                        + " ~ " + imageEnd
                        + "    数传：" + dataTranBegin
                        + " ~ " + dataTranEnd);
            }
        }
        if (currentTargets.size() > 1 && outputInfo) {
            Target target = currentTargets.get(currentTargets.size() - 1);
            String imageBegin = format.format(target.imageChance.startTime * 1000);
            String imageEnd = format.format(target.imageChance.endTime * 1000);
            String dataTranBegin = format.format(target.transitionStartTime * 1000);
            String dataTranEnd = format.format(target.transitionEndTime * 1000);
            System.out.println(target.getImageChance().getImageWindow().satellite.satelliteName
                    + " 成像：" + imageBegin
                    + " ~ " + imageEnd
                    + "    数传：" + dataTranBegin
                    + " ~ " + dataTranEnd);
        }
        if (outputInfo){
            System.out.println(station.stationName + "  传输任务约束通过！");
        }
        return true;
    }
}