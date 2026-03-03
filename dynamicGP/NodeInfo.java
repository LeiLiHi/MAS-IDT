package org.lileischeduler.dynamicGP;

import org.lileischeduler.dynamicGP.model.Node;

public class NodeInfo {
    public Node node;                          // 节点
    String Expression;                  // 表达式
    double[] fitness;                   // 适应度值  0是完成率 1是收益率
    String Description;

    public NodeInfo(Node no, double[] fit, String description) {
        node = no;
        fitness = fit;
        Description = description;
        Expression = no.toExpression();
    }
}
