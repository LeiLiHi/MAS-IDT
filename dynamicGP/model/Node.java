package org.lileischeduler.dynamicGP.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 抽象节点类
public abstract class Node {
    public abstract double evaluate(Map<String, Double> variables);

    public abstract Node deepCopy();

    public abstract String toExpression();
}
