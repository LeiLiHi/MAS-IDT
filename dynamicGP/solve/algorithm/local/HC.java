package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class HC {
    /***************** HillClimbing：遗传算法 ****************/
    public static void run(List<Target> finished, List<Target>left, List<Target> targets, int iteration) {
        // 初始化一个随机顺序的路径作为当前解决方案
        if (targets.isEmpty()) return;
        List<Target> bestSolution = new ArrayList<>(targets);        // 复制原始列表
        Collections.shuffle(bestSolution, random); // 随机打乱
        double bestEvaluation = new Insert().insertEarliest(bestSolution);   // 当前方案评估
        List<Target> tempSolution = new ArrayList<>();
        // 爬山算法的固定迭代次数
        for (int tt = 0; tt < iteration; tt++) {
            // 创建邻域解
            tempSolution.clear();
            tempSolution.addAll(bestSolution);
            NeighborOperator.singleSwap(tempSolution);
            double neighborEvaluation = new Insert().insertEarliest(tempSolution); // 评价新值
            // 更新最佳解
            if (neighborEvaluation > bestEvaluation) {
                bestEvaluation = neighborEvaluation;
                bestSolution.clear();
                bestSolution.addAll(tempSolution);    // 更新当前解
            }
        }
        // 解码当前的最佳方案
        new Insert().decodeTarget(bestSolution, finished, left);
    }
}
