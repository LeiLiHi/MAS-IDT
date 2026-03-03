package org.lileischeduler.dynamicGP.generateData;

import com.opencsv.CSVReader;
import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class LoadStation {
    // 数据存放的路径
    public String filePathCsv = ".\\basicalData";
    public final List<Station> stationList = new ArrayList<Station>();


    /**
     * 导入指定的地面站数量
     * @param num 地面站数量
     */
    public LoadStation(int num, String filePathCsv) throws IOException {
        Path filePath = Paths.get(filePathCsv, "station.csv");
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(filePath));
             CSVReader csvReader = new CSVReader(reader)) {
            // 跳过标题
            csvReader.skip(1);
            String[] line;
            int count = 0;
            while ((line = csvReader.readNext()) != null && count < num) {
                stationList.add(new Station(line));
                count++;
            }
        } catch (IOException e) {
            throw new IOException("CSV解析错误: " + e.getMessage(), e);
        }
    }


    /**
     * 检查地面站上面的数传任务是否满足约束
     * @param outputInfo   true为输出信息，false 为不输出
     */
    public void checkFeasible(boolean outputInfo) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        List<Target> currentTargets;
        long transitionTime;
        for (Station station : stationList) {
            transitionTime = station.transitionTime;
            currentTargets = station.currentTargets;

            System.out.println(station.stationName + "开始约束检查！   记录任务数为：" + station.currentTargets.size());
            if (station.currentTargets.isEmpty()) return;              // 没有安排，则直接返回空

            for (int i = 0; i < currentTargets.size(); i++) {
                Target target = currentTargets.get(i);

                if (target.getImageChance() == null) {
                    System.out.println("安排任务无成像机会！");
                    System.exit(0);
                }
                if (target.transitionWindow == null) {
                    System.out.println("安排任务无数传窗口！");
                    System.exit(0);
                }
                if (target.imageChance.startTime > target.transitionStartTime) {
                    System.out.println("安排任务成像时间晚于数传时间！");
                    System.exit(0);
                }
                // 检查任务间的关系： 1 判断是否需要转换时间  2 判断转时间约束是否满足
                if (i == currentTargets.size() - 1) continue;
                Target target2 = currentTargets.get(i + 1);
                Satellite satellite = target.getImageChance().imageWindow.satellite;
                Satellite satellite2 = target2.getImageChance().imageWindow.satellite;
                if (satellite.equals(satellite2)) {             // 相同的卫星只需要结束小于开始
                    if (target.transitionEndTime > target2.transitionStartTime) {
                        System.out.println(target.transitionEndTime);
                        System.out.println(target2.transitionStartTime);
                        System.out.println("任务前后时间不满足约束1！");
                        System.exit(0);
                    }
                } else {
                    if (target.transitionEndTime + transitionTime > target2.transitionStartTime) {
                        System.out.println("前：" + target.transitionEndTime);
                        System.out.println("后：" + target2.transitionStartTime);
                        System.out.println("转：" + transitionTime);
                        System.out.println("任务前后时间不满足约束2！");
                        System.exit(0);
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
        }
        System.out.println("传输任务约束通过！");
    }

    /* 重置所有station的任务列表 */
    public void reSet() {
        for (Station station : stationList) {
            station.currentTargets.clear();
        }
    }
}
