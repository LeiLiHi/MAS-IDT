package org.lileischeduler.dynamicGP.solve.algorithm;

import static org.lileischeduler.tool.Tool.random;

public class Qtable {
    private final int actionNum;                         // 动作数量
    private final double[][] matrix;                     // 状态矩阵

    /**
     * 初始化 Q-table 状态和动作数量
     * @param state     状态数量
     * @param action    动作数量
     */
    public Qtable(int state, int action){
        this.actionNum = action;
        this.matrix = new double[state][action];
        for(int i = 0; i < state; i++){            // 状态矩阵的初始化，全部设置为1
            for (int j = 0; j < action; j++) {
                matrix[i][j] = 1;
            }
        }
    }

    /**
     * Q-table 状态更新
     * @param stateIndex        状态index
     * @param actionIndex       动作index
     * @param effect            效果index：0为有提升  1为有变动无提升  2为无效
     */
    public void update(int stateIndex, int actionIndex, int effect){
        switch (effect){
            case 0 :
                matrix[stateIndex][actionIndex] += 1;
                break;
            case 2:
                break;
        }
    }

    /**
     * 从当前表格中选取一个动作
     * @param state 状态数量
     * @return      动作index
     */
    public int getAction(int state){
        // 贪心系数
        double EPSILON = 0.1;
        if (random.nextDouble() < EPSILON) {
            // 探索: 随机选择一个动作
            return random.nextInt(this.actionNum);
        } else {
            // 利用: 选择Q值最大的动作
            return getOptimalAction(state);
        }
    }

    /**
     * 选择当前状态下，奖励最大的动作
     * @param state     状态
     * @return          当前状态的最优动作
     */
    private int getOptimalAction(int state) {
        double maxQ = Double.NEGATIVE_INFINITY;
        int optimalAction = -1;
        for (int action = 0; action < actionNum; action++) {
            double q = this.matrix[state][action];
            if (q > maxQ) {
                maxQ = q;
                optimalAction = action;
            }
        }
        return optimalAction;
    }

}
