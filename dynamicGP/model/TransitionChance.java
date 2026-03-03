package org.lileischeduler.dynamicGP.model;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TransitionChance {
    public TransitionWindow transitionWindow;                        // 该机会所属的成像窗口
    public long startTime;                                           // 机会的开始时间：时间戳，由字符串格式转换而来
    public double elevation;                                         // 仰角（度）


}
