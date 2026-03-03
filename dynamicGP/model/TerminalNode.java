package org.lileischeduler.dynamicGP.model;

import java.util.Map;

// 终端节点(变量/常量)
public class TerminalNode extends Node {
    public String name;
    public double value;
    public boolean isVariable;

    public TerminalNode(String name, boolean isVariable) {
        this.name = name;
        this.isVariable = isVariable;
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        return isVariable ? variables.get(name) : value;
    }

    @Override
    public Node deepCopy() {
        return new TerminalNode(name, isVariable);
    }

    @Override
    public String toExpression() {
        return isVariable ? name : String.valueOf(value);
    }
}
