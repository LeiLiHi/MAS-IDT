package org.lileischeduler.dynamicGP.model;

import lombok.Getter;
import lombok.Setter;
import org.lileischeduler.tianZhi.model.Point;

import java.util.*;

@Setter
@Getter
public class Target {
    public int id;                                 // 目标编号
    public double priority;                    // 观测得分
    public double timeEfficiency;                  // 时效性得分
    private long frequency = 1;                     // 观测频次（默认1次）
    private long gapTime = 0;                       // 间隔时间（默认0次）
    private int arrivedTime = 0;                    // fixme 目标的到达时间（默认初始阶段就到达了）
    private int deadline;                           // 最晚提交时间（有效时间2）
    private String targetName;						// 目标名称
    private String targetType;					    // 目标类型(点/区域/点群/移动)
    private double requestResolution;               // 要求的分辨率
    private Point point;		                    // 点目标坐标
    private Map<Satellite, List<ImageWindow>> satWindows = new HashMap<>();
    public List<ImageWindow> optionalImageWindows = new ArrayList<>();     // 可选择的成像机会（经过延长）
    public long durationTime = 10;                                          // 目标的成像持续时间

    private double singlePriority;

    // Decision one : 成像卫星
    public Satellite satellite;
    public ImageChance imageChance;                                         // 选择的成像机会
    public ImageWindow imageWindow;                                         // 选择的成像窗口
    public ImageChance latestChance;                                        // 最晚的开始成像机会（最晚开始的时间也从这里开始）

    // Decision two : 地面数传站（如果是由同一颗卫星，同一个数传窗口则不用转换时间）
    public Station station;                                                 // 选择的数传站
    public TransitionWindow transitionWindow;                               // 选择的数传窗口
    public long transitionStartTime;                                        // 数传开始时间
    public long transitionEndTime;


    /***** 删除\插入属性 *****/
    public double trueScore;                                                // 任务收益(解评估，临时计算，带时间的收益：删除)
    public double unitScore;                                                // 单位收益(初始化，收益/成像时长：删除和插入)
    public double maxScore;                                                 // 实时最大收益(基于timeLine进行更新，update函数：删除和插入)
    public double preTransition = 0;                                        // 最小转换时间(目标插入时：删除)
    public double afterTransition = 0;                                      // 最小转换时间(目标插入时：删除)
    public double chanceNum;                                                // 成像机会数量(初始化时赋予：删除和插入)

    public void allocate( Satellite satellite ) {
        this.satellite = satellite;
        this.optionalImageWindows = satWindows.get(satellite);
        if (! satellite.currentTargets.contains(this)){
            satellite.currentTargets.add(this);
        }
    }

    /**
     * 多频任务判断自己的成像是否有效
     */
    public boolean isEffective4Frequency(){
        if (this.imageChance == null) return false;
        if (frequency < 2) return true;
        ImageChance selfImageChance = this.imageChance;
        List<ImageChance> imageChanceList =
                connectTargets
                        .stream()
                        .map(Target::getImageChance)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(ImageChance::getStartTime))
                        .toList();
        long nowTime = -10000;
        for (ImageChance imageChance : imageChanceList) {
            if (imageChance.getStartTime() - gapTime > nowTime) {
                nowTime = imageChance.endTime;
                if (imageChance.equals(selfImageChance)){
                    return true;
                }
            }
        }
        return false;
    }

    public List<Target> connectTargets; // 多频目标的关联机会
    /**
     * 获得当前任务的执行卫星
     */
    public Satellite getSatellite() {
        if (this.imageChance == null) return null;
        else return satellite;
    }

    /**
     * 重置当前任务，机会、窗口、最晚开始时间
     */
    public void reSet() {
        // 虚拟任务不更新（fixme）
        if (priority == 0) return;
        this.imageWindow = null;
        this.latestChance = null;
        this.imageChance = null;
        this.station = null;
        this.transitionWindow = null;
        this.transitionStartTime = 0;
        this.transitionEndTime = 0;
    }

}
