package org.lileischeduler.dynamicGP.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ImageWindow {
    public int windowId;                                                   // 窗口的id（与窗口list中的序列号一致）
    public int windowTd;                                                   // 窗口所属的时区（便于更新卫星的资源）
    public double conflictDegree = 0;                                      // 窗口的冲突度
    public Satellite satellite;                                            // 窗口所属的卫星
    public List<ImageChance> imageChanceList=new ArrayList<>();            // 该窗口成像机会的列表
    public long startTime;                                               // 开始时间
    public long endTime;                                                 // 结束时间
    public long duration;                                                // 持续时间
}
