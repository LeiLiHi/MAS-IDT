package org.lileischeduler.dynamicGP.solve.operator;

import org.lileischeduler.dynamicGP.model.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class DeleteAndInsert {

    /**
     * 随机删除list中的元素，不包括最小和最大的元素
     *
     * @param finishedTargets 已完成的任务列表(包含虚拟任务，请不要删除)
     * @param leftTargets     剩余任务列表
     * @param deleteNums      待删除任务列表
     */
    public static void randomDelete(List<Target> finishedTargets, List<Target> leftTargets, int deleteNums) {
        while (deleteNums > 0 && !finishedTargets.isEmpty() && finishedTargets.size() > 2) {
            int index = random.nextInt(finishedTargets.size() - 2) + 1;
            Target target = finishedTargets.get(index);
            target.reSet();
            finishedTargets.remove(index);
            leftTargets.add(target);
            deleteNums--;
        }
    }

    public static void delete(List<Target> finishedTargets, List<Target> leftTargets, int deleteNums, int action) {
        // 检查输入有效性
        if (finishedTargets == null || finishedTargets.isEmpty() || deleteNums <= 0) {
            return; // 无效输入，直接返回
        }
        // 确保 deleteNums 不超过 finishedTargets的大小
        deleteNums = Math.min(deleteNums, finishedTargets.size() - 2);
        // 复制 finishedTargets, 使用 Comparator 排序并找到删除的索引
        List<Target> targetsToRemove = new ArrayList<>(finishedTargets);
        // 获取按 trueScore 排序的索引
        switch (action) {
            case 0:                 // 随机删除
                randomDelete(finishedTargets, leftTargets, deleteNums);
                break;
            case 1:                 // 任务收益
                targetsToRemove.sort(Comparator.comparingDouble(target -> target.trueScore));
                break;
            case 2:                 // 单位收益
                targetsToRemove.sort(Comparator.comparingDouble(target -> target.unitScore));
                break;
            case 3:                 // 实时收益
                targetsToRemove.sort(Comparator.comparingDouble(target -> target.maxScore));
                break;
            case 4:                 // 最大机会数量
                targetsToRemove.sort(Comparator.comparingDouble(target -> target.chanceNum));
                Collections.reverse(targetsToRemove);
                break;
            case 5:                 // 最大转换时间
                targetsToRemove.sort(Comparator.comparingDouble(target -> target.preTransition + target.afterTransition));
                Collections.reverse(targetsToRemove);
                break;
            case 6:                 // 最佳姿态角度
                targetsToRemove.sort(Comparator.comparingDouble(target -> Math.abs(target.imageChance.yawAngle) + Math.abs(target.imageChance.pitchAngle)));
                Collections.reverse(targetsToRemove);
                break;
        }
        // 找到要删除的最低分数的目标
        for (int i = 0; i < deleteNums && action > 0; i++) {
            Target targetToRemove = targetsToRemove.get(i);
            if (targetToRemove.priority == 0) {         // 确保删除的不是虚拟任务
                deleteNums++;
                continue;
            }
            int originalIndex = finishedTargets.indexOf(targetToRemove);
            // 清除对象特定的属性
            targetToRemove.reSet();
            // 从 finishedTargets 中移除这个目标
            finishedTargets.remove(originalIndex);
            leftTargets.add(targetToRemove);
        }
    }

    public static void insert(List<Target> left, int action) {
        switch (action) {
            case 0:                 // 随机
                Collections.shuffle(left, random);
                break;
            case 1:
                left.sort(Comparator.comparingDouble(target -> target.unitScore));
                Collections.reverse(left);
                break;
            case 2:
                left.sort(Comparator.comparingDouble(target -> target.maxScore));
                Collections.reverse(left);
                break;
            case 3:
                left.sort(Comparator.comparingDouble(target -> target.chanceNum));
                break;
        }
    }
}
