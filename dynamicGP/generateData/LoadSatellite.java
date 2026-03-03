package org.lileischeduler.dynamicGP.generateData;

import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Station;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.lileischeduler.tool.Tool.random;

public class LoadSatellite {
    // 数据存放的路径
    public String filePath = ".\\basicalData\\orbitData\\";
    public String filePath2 = ".\\basicalData\\";
    public List<Satellite> satelliteList = new ArrayList<Satellite>();

    public LoadSatellite(String path1, String path2) {
        filePath = path1;                   // OPT路径
        filePath2 = path2;                  // CSV路径
    }

    public LoadSatellite(int num, String path){
        if (num > 50) {
            IntStream.range(0, num).parallel().forEach(i -> {
                String satelliteName = "OPT" + i;                               // 卫星名称
                Satellite satellite = new Satellite(path, satelliteName);   // 初始化卫星
                satellite.setId(i);                                             // 卫星的 id
                satellite.resolution = 1;                                       // 设置分辨率
                // 将卫星添加到共享的卫星列表中
                synchronized (satelliteList) {
                    satelliteList.add(satellite);                               // 卫星列表
                }
            });
        } else {
            for (int i = 0; i < num; i++) {
                String satelliteName = "OPT" + i;                               // 卫星名称
                Satellite satellite = new Satellite(path, satelliteName);   // 初始化卫星
                satellite.setId(i);                                             // 卫星的 id
                satellite.resolution = 1;                                       // 设置分辨率
                satelliteList.add(satellite);                                   // 卫星列表
            }
        }
        // 按照satellite Id 进行排序
        satelliteList.sort(Comparator.comparing(Satellite::getId));
    }

    public LoadSatellite(int num) throws FileNotFoundException {
        this.loadSatellite(num);
    }

    /**
     * 导入指定的卫星数量
     * @param num     卫星数量
     */
    public void loadSatellite(int num) throws FileNotFoundException {
        if (num > 50) {
            IntStream.range(0, num).parallel().forEach(i -> {
                String satelliteName = "OPT" + i;                               // 卫星名称
                Satellite satellite = new Satellite(filePath, satelliteName);   // 初始化卫星
                satellite.setId(i);                                             // 卫星的 id
                satellite.resolution = 1;                                       // 设置分辨率
                // 将卫星添加到共享的卫星列表中
                synchronized (satelliteList) {
                    satelliteList.add(satellite);                               // 卫星列表
                }
            });
        } else {
            for (int i = 0; i < num; i++) {
                String satelliteName = "OPT" + i;                               // 卫星名称
                Satellite satellite = new Satellite(filePath, satelliteName);   // 初始化卫星
                satellite.setId(i);                                             // 卫星的 id
                satellite.resolution = 1;                                       // 设置分辨率
                satelliteList.add(satellite);                                   // 卫星列表
            }
        }
        // 按照satellite Id 进行排序
        satelliteList.sort(Comparator.comparing(Satellite::getId));
        try (BufferedReader br = new BufferedReader(new FileReader(filePath2 + "//satellite.csv"))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.trim().split(",");
                // TEL
                int index = Integer.parseInt(values[0]);
                int resolution = Integer.parseInt(values[3]);
                String TEL2 = values[8];
                String[] temp = TEL2.split("\\s+");
                double inclination = Double.parseDouble(temp[3]);
                double recordPlay = Double.parseDouble(values[10]);
                double storage = Double.parseDouble(values[11]);
                double energy = Double.parseDouble(values[12]);
                double transmission = Double.parseDouble(values[13]);
                if (index == satelliteList.size()) break;
                Satellite satellite = satelliteList.get(index);
                satellite.resolution = resolution;
                satellite.maxStorage = storage;
                satellite.maxEnergy = energy;
                satellite.transitionGround = transmission;
                satellite.imageTransitionRate = recordPlay;
                satellite.getProperties().put("DownloadRate", recordPlay);
                satellite.getProperties().put("Storage", storage);
                satellite.getProperties().put("Energy", energy);
                satellite.getProperties().put("EnergyConsumption", random.nextDouble());
                satellite.getProperties().put("Transition", transmission);
                satellite.getProperties().put("Inclination", inclination);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    /**
     * 检查地面站上面的数传任务是否满足约束
     * @param outputInfo   true为输出信息，false 为不输出
     */
    public void checkFeasible(boolean outputInfo) {
        List<Target> currentTargets = new ArrayList<>();
        double transitionTime;
        for (Satellite satellite : satelliteList) {
            currentTargets.clear();
            transitionTime = satellite.transitionGround;
            currentTargets.addAll(satellite.scheduledTargets);
            System.out.println(satellite.satelliteName + "开始约束检查！   成像任务数为：" + currentTargets.size());
            // 1. 成像任务之间的转换时间
            for (int i = 0; i < currentTargets.size() - 1; i++) {
                Target t1 = currentTargets.get(i);
                Target t2 = currentTargets.get(i + 1);
                double transition = Insert.calTransition(t1.imageChance, t2.imageChance);
                if (t1.imageChance.endTime + transition > t2.imageChance.startTime) {
                    System.out.println("任务转换时间约束不满足！");
                    System.exit(0);
                }
            }
            // 2. 数传任务之间的转换时间(按照开始时间从小到大排序)
            currentTargets.sort((a1,a2)->Long.compare(a1.transitionStartTime,a2.transitionStartTime));
            for (int i = 0; i < currentTargets.size() - 1; i++) {
                Target t1 = currentTargets.get(i);
                Target t2 = currentTargets.get(i + 1);
                if (t1.transitionEndTime > t2.transitionStartTime) {
                    System.out.println("任务数传时间重叠！");
                    System.exit(0);
                }
                if (t1.station.equals(t2.station)) {
                    if (t1.transitionEndTime > t2.transitionEndTime) {
                        System.out.println("任务(同星同站)数传转换时间约束不满足！");
                        System.exit(0);
                    }
                } else {
                    if (t1.transitionEndTime + transitionTime > t2.transitionEndTime) {
                        System.out.println("任务(同星不同站)数传转换时间约束不满足！");
                        System.exit(0);
                    }
                }
            }




        }
        System.out.println("卫星检查通过");
    }

    /* 重置所有station的任务列表 */
    public void reSet() {
        for (Satellite satellite : satelliteList) {
            satellite.scheduledTargets.clear();
        }
    }
}
