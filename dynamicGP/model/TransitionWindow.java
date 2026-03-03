package org.lileischeduler.dynamicGP.model;

import java.util.List;

public class TransitionWindow {
    public int windowId;                                                   // 窗口的id（与窗口list中的序列号一致）
    public int windowTd;                                                   // 窗口所属的时区（便于更新卫星的资源）
    public Station station;                                                // 窗口所属的卫星
    public List<TransitionChance> transitionChanceList;                    // 该窗口成像机会的列表
    public long startTime;                                               // 开始时间
    public long endTime;                                                 // 结束时间
    public long duration;                                                // 持续时间
}
