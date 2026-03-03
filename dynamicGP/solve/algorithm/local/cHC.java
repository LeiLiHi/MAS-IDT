package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamicGP.model.ImageChance;
import org.lileischeduler.dynamicGP.model.ImageWindow;
import org.lileischeduler.dynamicGP.model.Target;
import org.lileischeduler.dynamicGP.solve.operator.Insert;

import java.util.ArrayList;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class cHC {
    public void run(List<Target> finished, List<Target> left, int iteration) {
        // 预分配性能优化
        List<Target> targets = new ArrayList<>(finished.size() + left.size());
        targets.addAll(finished);
        targets.addAll(left);
        // 快速空检查
        if (targets.isEmpty()) return;
        // 预缓存目标数量，避免重复调用size()
        final int targetNum = targets.size();
        // 使用增强型随机数生成

        while (iteration > 0) {
            // 使用局部随机数生成器
            int index = random.nextInt(targetNum);
            Target target = targets.get(index);
            // 缓存之前的机会
            ImageChance preChance = target.imageChance;
            target.imageChance = null;
            finished.remove(target);
            // 预先分配性能优化
            boolean foundChance = false;
            // 减少不必要的list创建
            for (ImageWindow imageWindow : target.optionalImageWindows) {
                // 直接遍历，避免额外的list创建和shuffle
                for (int i = 0; i < imageWindow.imageChanceList.size(); i++) {
                    // 随机取索引模拟shuffle效果
                    int randomIndex = random.nextInt(imageWindow.imageChanceList.size());
                    ImageChance imageChance = imageWindow.imageChanceList.get(randomIndex);
                    // 跳过相同机会
                    if (imageChance == preChance) continue;
                    // 冲突检查
                    boolean isConflict = new Insert().ConflictCheck(imageChance, finished);
                    if (isConflict) {
                        target.imageChance = imageChance;
                        foundChance = true;
                        break;
                    }
                }
                // 找到合适机会后跳出外层循环
                if (foundChance) break;
            }

            // 插入逻辑保持不变
            if (target.imageChance != null) {
                int pos = new Insert().findInsertPosition(finished, target);
                finished.add(pos, target);
            }

            iteration--;
        }

        // 使用更高效的遍历方式
        left.clear();
        for (Target t : targets) {
            if (t.imageChance == null) {
                left.add(t);
            }
        }
    }
}