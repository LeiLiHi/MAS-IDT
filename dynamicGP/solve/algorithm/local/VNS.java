package org.lileischeduler.dynamicGP.solve.algorithm.local;

import org.lileischeduler.dynamic.model.Target;
import org.lileischeduler.dynamic.solve.operator.Insert;
import org.lileischeduler.dynamic.solve.operator.NeighborOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class VNS {
    /***************** Variable Neighborhood Search ****************/
    public static void run(List<Target> finished, List<Target> left, List<Target> targets, int maxIterations) {
        /* Step 1: 创建一个随机顺序的路径作为初始解 */
        List<Target> bestSolution = new ArrayList<>(targets);
        Collections.shuffle(bestSolution, random); // 随机打乱
        double bestEvaluation = evaluateIndividuals(bestSolution); // 当前方案评估
        System.out.println("初始解: " + bestEvaluation);

        /* Step 2: 进行变邻域搜索 */
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<Target> tempSolution = new ArrayList<>(bestSolution);
            boolean improvement = true;
            int neighborhoodSize = 1;  // 初始化邻域大小
            /* Step 3: 在不同的邻域结构中搜索 */
            while (improvement) {
                switch (neighborhoodSize) {
                    case 2:
                        NeighborOperator.multipleSwap(tempSolution, 5);     // 多次交换1
                        break;
                    case 3:
                        NeighborOperator.multipleSwap(tempSolution, 9);     // 多次交换2
                        break;
                    case 4:
                        NeighborOperator.twoPointsReverse(tempSolution);             // 两点反转
                        break;
                    case 5:
                        NeighborOperator.partialRemove(tempSolution, 5);        // 部分重构
                        break;
                    case 6:
                        NeighborOperator.insertOperator(tempSolution);               // 插入操作
                        break;
                    case 7:
                        NeighborOperator.partialShuffle(tempSolution);               // 部分乱序操作
                        break;
                    case 8:
                        NeighborOperator.blockSwap(tempSolution);                    // 块交换
                        break;
                    case 9:
                        NeighborOperator.partialRotation(tempSolution);              // 轮转邻域
                        break;
                    case 10:
                        NeighborOperator.groupRearrange(tempSolution);               // 分组重排
                        break;
                    default:
                        NeighborOperator.singleSwap(tempSolution);                   // 单次交换
                        break;
                }
                double neighborEvaluation = evaluateIndividuals(tempSolution); // 评价邻域解
                /* Step 4: 更新解和评价 */
                if (neighborEvaluation > bestEvaluation) {
                    bestEvaluation = neighborEvaluation;
                    bestSolution = new ArrayList<>(tempSolution); // 更新当前解
                    System.out.println("第 " + iteration + " 代 提升解：" + bestEvaluation + "   操作：" + neighborhoodSize);
                    neighborhoodSize = 1;
                } else {
                    if (neighborhoodSize == 10) {
                        neighborhoodSize = 0;
                        improvement = false; // 如果没有提升，则退出领邻域循环
                    }
                    neighborhoodSize += 1;
                }
            }
        }

        /* Step 5: 输出最终解 */
        new Insert().decodeTarget(bestSolution, finished, left);
        System.out.println("优化后的解: " + bestEvaluation);
    }

    /***************** 评估函数 ******************/
    private static double evaluateIndividuals(List<Target> targets) {
        // 适应度评估
        return new Insert().insertEarliest(targets);
    }
}
