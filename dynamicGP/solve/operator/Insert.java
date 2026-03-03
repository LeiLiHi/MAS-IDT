package org.lileischeduler.dynamicGP.solve.operator;

import org.lileischeduler.dynamicGP.model.ImageChance;
import org.lileischeduler.dynamicGP.model.ImageWindow;
import org.lileischeduler.dynamicGP.model.Target;

import java.util.*;


public class Insert {
    public String revenue = "时间";
    public List<Target> leftTarget = new ArrayList<>();
    public List<Target> finishedTarget = new ArrayList<>();


    /************** 创建虚拟任务：方便插入和松弛 **********/
    public void insertVirtualTarget(List<Target> targets) {
        Target target1 = new Target();
        Target target2 = new Target();
        target1.priority = 0;
        target2.priority = 0;

        ImageWindow imageWindow1 = new ImageWindow();
        ImageChance imageChance1 = new ImageChance();
        imageWindow1.windowId = 0;
        imageWindow1.getImageChanceList().add(imageChance1);
        imageWindow1.startTime = -9999;
        imageWindow1.endTime = -9999;
        imageChance1.pitchAngle = 0;
        imageChance1.yawAngle = 0;
        imageChance1.startTime = -9999;
        imageChance1.endTime = -9999;
        imageChance1.imageWindow = imageWindow1;

        ImageWindow imageWindow2 = new ImageWindow();
        ImageChance imageChance2 = new ImageChance();
        imageWindow2.windowId = 0;
        imageWindow2.getImageChanceList().add(imageChance2);
        imageWindow2.startTime = 999999999999L;
        imageWindow2.endTime = 999999999999L;
        imageChance2.pitchAngle = 0;
        imageChance2.yawAngle = 0;
        imageChance2.startTime = 999999999999L;
        imageChance2.endTime = 999999999999L;
        imageChance2.imageWindow = imageWindow2;

        target1.imageWindow = imageWindow1;
        target1.imageChance = imageChance1;
        target1.latestChance = imageChance1;
        target1.optionalImageWindows.add(imageWindow1);

        target2.imageWindow = imageWindow2;
        target2.imageChance = imageChance2;
        target2.latestChance = imageChance2;
        target2.optionalImageWindows.add(imageWindow2);

        targets.add(target1);
        targets.add(target2);
    }
    
    
    /**
     * 成像机会与成像机会之间的转换时间计算，默认imageChance0在前面
     *
     * @param imageChance0 成像机会1
     * @param imageChance  成像机会2
     * @return 返回需要的转换时间，如果修改这里的转换时间参数，记得对应修改insertEarliest中的检查范围
     */
    public static double calTransition(ImageChance imageChance0, ImageChance imageChance) {
        double angle11 = imageChance0.getPitchAngle();
        double angle22 = imageChance0.getRollAngle();
        double angle33 = imageChance0.getYawAngle();

        double angle44 = imageChance.getPitchAngle();
        double angle55 = imageChance.getRollAngle();
        double angle66 = imageChance.getYawAngle();
        double absAngle = Math.abs(angle11 - angle44) + Math.abs(angle22 - angle55) + Math.abs(angle33 - angle66);
        if (absAngle <= 10) {
            return 11.6;
        } else if (absAngle <= 30) {
            return 5 + absAngle / 1.5;
        } else if (absAngle <= 60) {
            return 10 + absAngle / 2.0;
        } else if (absAngle <= 90) {
            return 16 + absAngle / 2.5;
        } else {
            return 22 + absAngle / 3.0;
        }
    }

    /**
     * 得到的 targets如果存在成像机会的话是按照开始时间进行排列的
     * @param targets       待插入任务列表
     * @return              返回当前的收益
     */
    public double insertEarliest(List<Target> targets) {
        finishedTarget.clear();
        leftTarget.clear();
        for (Target target : targets) {
            target.reSet();                      // 重置一下
            if (target.optionalImageWindows.isEmpty()) {
                leftTarget.add(target);
                continue;
            }
            for (ImageWindow imageWindow : target.optionalImageWindows) {
                for (ImageChance imageChance : imageWindow.getImageChanceList()) {
                    boolean insertPosition = ConflictCheck(imageChance, finishedTarget);
                    if (insertPosition) {
                        target.imageChance = imageChance;
                        // 找到插入位置并插入
                        int insertIndex = findInsertPosition(finishedTarget, target);
                        finishedTarget.add(insertIndex, target);
                        break;
                    }
                }
                if (target.imageChance != null) break;
            }
            if (target.imageChance == null) leftTarget.add(target);
        }
        targets.clear();
        targets.addAll(finishedTarget);
        targets.addAll(leftTarget);

        return Evaluate.evaluate(finishedTarget, targets, revenue);
    }

    /**
     * 将当前任务插入到已规划列表，成功为true 失败为false
     * @param target            待插入任务
     * @param finishedTarget    已规划列表  按照时间顺序排列
     * @return                  插入成功返回true 插入失败返回false
     */
    public boolean insertEarliest(Target target, List<Target> finishedTarget) {
        target.reSet();
        if (target.optionalImageWindows.isEmpty()) return false;
        for (ImageWindow imageWindow : target.optionalImageWindows) {
            for (ImageChance imageChance : imageWindow.imageChanceList) {
                boolean insertPosition = ConflictCheck(imageChance, finishedTarget);
                if (insertPosition) {
                    target.imageChance = imageChance;
                    // 找到插入位置并插入
                    int insertIndex = findInsertPosition(finishedTarget, target);
                    finishedTarget.add(insertIndex, target);
                    break;
                }
            }
            if (target.imageChance != null) break;
        }
        return target.imageChance != null;
    }

    /**
     * false 为冲突  true 为不冲突
     * @param imageChance   成像机会
     * @param targets       目标列表（按照开始时间维护的一个list）
     * @return              返回 二元值
     */
    public boolean ConflictCheck(ImageChance imageChance, List<Target> targets) {
        if (targets.isEmpty()) return true;

        for (Target target : targets) {
            ImageChance imageChance1 = target.imageChance;
            if (imageChance1.endTime + 90 < imageChance.startTime) {
                continue;
            }
            // 已完成机会目前在待插入机会的后面很远的地方，直接跳出(1 已完成机会开始时间 > 待插入机会的开始时间; 2 已完成机会距离待插入机会有很远的距离)
            if (imageChance.endTime + 90 < imageChance1.startTime) {
                break;      // 没有冲突，后续也一定没有冲突
            }
            double transition = calTransition(imageChance1, imageChance);
            if (imageChance1.startTime < imageChance.startTime) {  // 1 在 0 的前面
                if (imageChance.startTime - imageChance1.endTime < transition + 1) return false;
            } else {
                if (imageChance1.startTime - imageChance.endTime < transition + 1) return false;
            }
        }
        return true;
    }

    /**
     * false 是冲突
     * @param imageChance       机会1
     * @param imageChance1      机会2
     * @return                  二元变量 是否冲突  false 冲突  true 不冲突
     */
    public boolean ConflictCheck(ImageChance imageChance, ImageChance imageChance1) {
        if (imageChance == null || imageChance1 == null) return true;
        double transition = calTransition(imageChance1, imageChance);
        if (imageChance1.startTime < imageChance.startTime) {  // 1 在 0 的前面
            return !(imageChance.startTime - imageChance1.endTime < transition + 1);
        } else {
            return !(imageChance1.startTime - imageChance.endTime < transition + 1);
        }
    }

    // 二分查找插入位置
    public static int findInsertPosition(List<Target> list, Target newTarget) {
        int low = 0;
        int high = list.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (list.get(mid).imageChance.startTime < newTarget.imageChance.startTime) {
                low = mid + 1;  // newTarget 应在 mid 右侧
            } else {
                high = mid;  // newTarget 应在 mid 左侧
            }
        }
        return low;  // 返回插入位置
    }

    /**
     * 插入后已完成紧前插入和后向松弛
     *
     * @param finished 已完成任务
     * @param left     剩余任务
     */
    public void insert(List<Target> finished, List<Target> left) {
        List<Target> trueLeft = new ArrayList<>(left);
        for (Target target : left) {
            for (int i = 0; i < finished.size() - 1; i++) {

                if (finished.contains(target)) {
                    int a = 0;
                }

                boolean isInsert = firstPositionInsert(target, finished.get(i), finished.get(i + 1));
                // 2.4 将任务插入到（i + 1）的位置，并更新最早\晚成像机会
                if (isInsert) {
                    finished.add(i + 1, target);
                    // 2.4.1 紧前插入：target的后续任务重新安排
                    deleteInsertForwards(i + 1, finished);
                    // 2.4.2 后向松弛：基于新插入的任务，更新它之前所有任务的最晚成像机会
                    deleteSlackAfterwards(i + 1, finished);
                    // 2.4.3 删除trueLeft中已完成的任务目标
                    trueLeft.remove(target);
                    break;
                }
            }
        }
        left.clear();
        left.addAll(trueLeft);
    }
    private boolean firstPositionInsert(Target target, Target preTarget, Target nextTarget) {
        target.reSet();
        // 2.1 判断target的窗口的最晚时间是否可能满足插入条件：① 结束时间不能比preTarget早 ② 开始时间不能比next的最晚时间晚
        for (ImageWindow imageWindow : target.optionalImageWindows) {
            if (imageWindow.endTime < preTarget.imageChance.endTime || imageWindow.startTime
                    >= nextTarget.latestChance.startTime) continue;
            // 2.2 尝试插入机会:与preTarget和nextTarget的最晚无冲突即可插入
            for (ImageChance imageChance : imageWindow.getImageChanceList()) {
                if (imageChance.startTime <= preTarget.imageChance.endTime) continue;
                if (imageChance.endTime >= nextTarget.latestChance.startTime) continue;
                // 2.2.1 判断与前任务有无冲突
                double transitionPre = calTransition(preTarget.imageChance, imageChance);
                if (imageChance.startTime - preTarget.imageChance.endTime < transitionPre + 1) continue;
                // 2.2.2 判断与后任务有无冲突
                double transitionNext = calTransition(imageChance, nextTarget.latestChance);
                if (nextTarget.latestChance.startTime - imageChance.endTime < transitionNext + 1) continue;
                target.imageChance = imageChance;
                target.imageWindow = imageWindow;
                // 更新前一个目标的后向转换时间 和 自己的前向转换时间
                preTarget.afterTransition = transitionPre;
                target.preTransition = transitionPre;
                break;
            }
            // 2.3 这是首个可插入机会，直接break，其他先不找
            if (target.imageChance != null) break;
        }
        return target.imageChance != null;
    }

    /**
     * 顶前插入函数，用于更新insertPosition后所有任务的最早成像机会
     * @param insertPosition 更新起点(不包括自己的这个位置 >= 0)
     * @param finishedTarget 已完成的任务列表(第一个和最后一个为虚拟任务)
     */
    public void deleteInsertForwards(int insertPosition, List<Target> finishedTarget) {
        Target nextTarget;
        Target preTarget;
        for (int j = insertPosition; j < finishedTarget.size() - 1; j++) {
            // 2.4.1.1 下一个任务的机会无变动，直接break，之后的任务也一定满足约束转换关系
            preTarget = finishedTarget.get(j);
            nextTarget = finishedTarget.get(j + 1);
            // 2.4.1.2 有变动，循环当前的窗口及之后的窗口找到新的最早开始机会（nextTarget）
            int windowID = nextTarget.imageChance.imageWindow.windowId;
            nextTarget.imageChance = null;
            for (int k = windowID; k < nextTarget.optionalImageWindows.size(); k++) {
                ImageWindow nextWindow = nextTarget.optionalImageWindows.get(k);
                if (nextWindow.endTime <= preTarget.imageChance.endTime) continue;
                for (ImageChance imageChance : nextWindow.imageChanceList) {
                    double transitionPre = calTransition(preTarget.imageChance, imageChance);
                    if (imageChance.startTime - preTarget.imageChance.endTime < transitionPre + 1)
                        continue;
                    nextTarget.imageChance = imageChance;
                    nextTarget.imageWindow = imageChance.imageWindow;

                    // 更新前目标的后向和后目标的前向时间
                    preTarget.afterTransition = transitionPre;
                    nextTarget.preTransition = transitionPre;
                    break;
                }
                // 完成更新后直接跳出
                if (nextTarget.imageChance != null) break;
            }
            /********** clear here if everything goes well! ***********/
            if (nextTarget.imageChance == null) {
                System.out.println("插入出现问题！！！");
                System.exit(1);
            }
            /********** clear here if everything goes well! ***********/
        }
    }

    /**
     * 删除后向松弛函数，用于更新insertPosition及其以前所有已完成目标的最晚完成机会（不跳过，从头到尾全部更新）
     * 如果想要直接从头到尾全部更新，记得插入虚拟任务，insertPosition是列表的 size - 2
     * @param insertPosition 更新的第一个位置(< finishedTarget.size() - 2)
     * @param finishedTarget 已完成任务列表(第一个和最后一个是虚拟任务)
     */
    public void deleteSlackAfterwards(int insertPosition, List<Target> finishedTarget) {
        Target preTarget;
        Target nextTarget;
        for (int j = insertPosition + 1; j >= 1; j--) {
            preTarget = finishedTarget.get(j - 1);
            nextTarget = finishedTarget.get(j);
            // 2.4.2.2
            List<ImageWindow> preWindows = preTarget.optionalImageWindows;
            // 确定初始搜索索引
            int tempIndex = (j == insertPosition + 1 || preTarget.latestChance == null) ?
                    preWindows.size() - 1 : preTarget.latestChance.imageWindow.windowId;
            preTarget.latestChance = null;
            for (int k = tempIndex; k >= 0; k--) {
                ImageWindow window = preWindows.get(k);
                if (window.startTime >= nextTarget.latestChance.startTime) continue;

                List<ImageChance> preChances = window.getImageChanceList();
                for (int l = preChances.size() - 1; l >= 0; l--) {
                    ImageChance chance = preChances.get(l);
                    double transition3 = calTransition(chance, nextTarget.latestChance);
                    if (nextTarget.latestChance.startTime - chance.endTime >= transition3 + 1) {
                        preTarget.latestChance = chance;
                        break;
                    }
                }
                if (preTarget.latestChance != null) break;
            }
            /********** clear here if everything goes well! ***********/
            if (preTarget.latestChance == null) {
                System.out.println("松弛出现问题！！！");
                System.exit(1);
            }
            /********** clear here if everything goes well! ***********/
        }
    }

    /*********** 前位插入: 给定目标列表,定死的时间顺序后续任务不能比前面任务更早完成 ************/
    public double firstInsert(List<Target> targets) {
        Target preTarget = null;
        finishedTarget = new ArrayList<>();
        leftTarget = new ArrayList<>();
        // 0. 按照当前的目标序列安排成像资源
        for (Target currentTarget : targets) {
            currentTarget.reSet();
        }
        for (Target currentTarget : targets) {
            if (currentTarget.optionalImageWindows.isEmpty()) continue;
            if (preTarget == null) {                  // 全局首个任务
                currentTarget.imageWindow = currentTarget.optionalImageWindows.get(0);
                currentTarget.imageChance = currentTarget.imageWindow.getImageChanceList().get(0);
                preTarget = currentTarget;
                finishedTarget.add(currentTarget);
                continue;
            }
            // 1. 根据 preTarget 安排后续目标的机会
            ImageChance preImageChance = preTarget.imageChance;
            for (ImageWindow imageWindow : currentTarget.optionalImageWindows) {
                // 1.1 如果待插入窗口的结束时间 小于 启用机会的结束时间，那么必定无法插入，直接跳过
                if (imageWindow.endTime <= preImageChance.endTime) continue;
                for (ImageChance imageChance : imageWindow.getImageChanceList()) {
                    // 1.2 窗口机会的开始时间 小于 启用机会的结束时间，该机会必定无法插入，直接跳过 fixme 加入最小转换时间 节省窗口计算
                    if (imageChance.startTime <= preImageChance.endTime) continue;
                    double transition = calTransition(preImageChance, imageChance);
                    // 1.3 机会的开始时间 减去 前一机会的结束时间 大于 转换时间：可行赋予后直接break
                    if (imageChance.startTime - preImageChance.endTime >= transition + 1) {
                        currentTarget.imageWindow = imageWindow;
                        currentTarget.imageChance = imageChance;
                        break;
                    }
                }
                // 1.4 在当前窗口成功插入，直接break，否则循环下一个窗口
                if (currentTarget.imageChance != null) {
                    break;
                }
            }
            // 2. 当前任务插入失败，放入无法规划任务列表，否则加入已规划任务列表，并成为上一个已规划任务
            if (currentTarget.imageWindow == null) {
                leftTarget.add(currentTarget);
            } else {
                preTarget = currentTarget;
                finishedTarget.add(currentTarget);
            }
        }
        double getScore = finishedTarget.stream()
                .mapToDouble(Target::getPriority)
                .sum();
        double leftScore = leftTarget.stream()
                .mapToDouble(Target::getPriority)
                .sum();
//        System.out.println("规划得分： " + getScore + " 未规划得分： " + leftScore);
        return getScore;
    }

    /**
     * 评估函数
     * 基于整数值解码当前的任务列表index 解码卫星规划方案
     *
     * @param finished 已完成的目标列表
     * @param left     未规划的任务列表
     * @param targets  现有的任务列表
     */
    public void decodeTarget(List<Target> targets, List<Target> finished, List<Target> left) {
        finished.clear();
        left.clear();
        for (Target target : targets) {
            target.reSet();
        }
        // 适应度评估
        new Insert().insertEarliest(targets);
        for (Target target : targets) {
            if (target.imageChance != null) {
                finished.add(target);
            } else {
                left.add(target);
            }
        }
    }
}
