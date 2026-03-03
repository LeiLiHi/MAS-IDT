package org.lileischeduler.dynamicGP.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ImageChance {
    public ImageWindow imageWindow;                        // 该机会所属的成像窗口
    public long startTime;                                 // 机会的开始时间：时间戳，由字符串格式转换而来
    public long endTime;                                   // 机会的结束时间：开始加任务的持续时间
    public double pitchAngle;                              // 俯仰角
    public double rollAngle = 0;                           // 滚动角
    public double yawAngle;                                // 偏航角
}
