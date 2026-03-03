package org.lileischeduler.dynamicGP.solve.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class NeighborOperator {
    /******* 0. 单次序列交换 *******/
    public static <E> void singleSwap(List<E> individual) {
        int[] indices = getRandomStartEndIndices(individual);
        // 交换元素
        Collections.swap(individual, indices[0], indices[1]);
    }

    /******** 1. 多次交换 *********/
    public static <E> void multipleSwap(List<E> individual, int swapNum) {
        for (int i = 0; i < swapNum; i++) {
            singleSwap(individual);
        }
    }

    /******** 2. 两点反转 *********/
    public static <E> void twoPointsReverse(List<E> individual) {
        int[] indices = getRandomStartEndIndices(individual);
        // 反转[i, j]
        List<E> subList = new ArrayList<>(individual.subList(indices[0], indices[1] + 1));
        Collections.reverse(subList);
        for (int i = 0; i < subList.size(); i++) {
            individual.set(indices[0] + i, subList.get(i));
        }
    }

    /******** 3. 部分重构：部分标签转移到最后 *******/
    public static <E> void partialRemove(List<E> individual, int num) {
        List<E> subList = new ArrayList<>();
        while (num > 0 && !individual.isEmpty()) {
            int index = random.nextInt(individual.size());
            subList.add(individual.get(index));
            individual.remove(index);
            num--;
        }
        individual.addAll(subList);
    }

    /******* 4. 插入操作：标签插入到随机位置 *********/
    public static <E> void insertOperator(List<E> individual) {
        if (individual.size() <= 2) return;
        int fromIndex = random.nextInt(individual.size());
        int toIndex = random.nextInt(individual.size());
        // 确保fromIndex和toIndex不同
        while (fromIndex == toIndex) {
            toIndex = random.nextInt(individual.size());
        }
        E element = individual.remove(fromIndex);
        individual.add(toIndex < fromIndex ? toIndex : toIndex - 1, element);
    }

    /******* 5. 部分乱序 *********/
    public static <E> void partialShuffle(List<E> individual) {
        int[] indices = getRandomStartEndIndices(individual);
        // 对部分区间进行乱序
        List<E> subList = new ArrayList<>(individual.subList(indices[0], indices[1] + 1));
        Collections.shuffle(subList, random);
        for (int i = 0; i < subList.size(); i++) {
            individual.set(indices[0] + i, subList.get(i));
        }
    }

    /******* 6. 块交换 *********/
    public static <E> void blockSwap(List<E> individual) {
        if (individual.size() <= 4) return;
        // 确保有足够的空间进行块交换
        int blockSize = random.nextInt(individual.size() / 4) + 1;
        int startIndex1 = random.nextInt(individual.size() - blockSize);
        int startIndex2 = random.nextInt(individual.size() - blockSize);
        // 确保两个块不重叠
        while (Math.abs(startIndex1 - startIndex2) < blockSize) {
            startIndex2 = random.nextInt(individual.size() - blockSize);
        }
        // 交换块
        for (int i = 0; i < blockSize; i++) {
            Collections.swap(individual, startIndex1 + i, startIndex2 + i);
        }
    }

    /******* 7. 部分轮换 *********/
    public static <E> void partialRotation(List<E> individual) {
        if (individual.size() <= 3) return;
        // 选择轮换区间
        int[] indices = getRandomStartEndIndices(individual);
        int start = indices[0], end = indices[1];
        // 轮换方向和距离
        int rotationDirection = random.nextBoolean() ? 1 : -1;
        int rotationDistance = 1 + random.nextInt((end - start));
        List<E> subList = new ArrayList<>(individual.subList(start, end + 1));
        Collections.rotate(subList, rotationDirection * rotationDistance);
        // 将轮换后的子列表放回原列表
        for (int i = 0; i < subList.size(); i++) {
            individual.set(start + i, subList.get(i));
        }
    }

    /******* 8. 分组重排 *********/
    public static <E> void groupRearrange(List<E> individual) {
        if (individual.size() <= 3) return;
        int groupCount = 2 + random.nextInt((individual.size() / 2));                   // 随机选择分组数
        int groupSize = individual.size() / groupCount;
        List<List<E>> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            int start = i * groupSize;
            int end = (i == groupCount - 1) ? individual.size() : start + groupSize;
            groups.add(new ArrayList<>(individual.subList(start, end)));
        }
        Collections.shuffle(groups, random);                                                    // 打乱组的顺序[...]
        individual.clear();                                                             // 重新组装列表
        for (List<E> group : groups) {
            individual.addAll(group);
        }
    }

    /******* 辅助函数: 获取随机的开始和结束索引 *********/
    public static int[] getRandomStartEndIndices(List<?> individual) {
        if (individual.isEmpty()) {
            return null;
        }
        if (individual.size() == 1) return new int[]{0, 0};
        int startIndex = random.nextInt(individual.size());
        int endIndex;
        do {
            endIndex = random.nextInt(individual.size());
        } while (endIndex == startIndex);
        if (startIndex > endIndex) { // 确保startIndex < endIndex
            int temp = startIndex;
            startIndex = endIndex;
            endIndex = temp;
        }
        return new int[]{startIndex, endIndex};
    }



}
