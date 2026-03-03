package org.lileischeduler.dynamicGP.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionNode extends Node {
    public String operation;
    public List<Node> children;

    public FunctionNode(String operation) {
        this.operation = operation;
        this.children = new ArrayList<>();
    }

    public void addChild(Node child) {
        children.add(child);
    }

    @Override
    public double evaluate(Map<String, Double> variables) {
        return switch (operation) {
            case "+" -> children.get(0).evaluate(variables) + children.get(1).evaluate(variables);
            case "-" -> children.get(0).evaluate(variables) - children.get(1).evaluate(variables);
            case "*" -> children.get(0).evaluate(variables) * children.get(1).evaluate(variables);
            case "/" -> {
                double denominator = children.get(1).evaluate(variables);
                yield denominator != 0 ? children.get(0).evaluate(variables) / denominator : 0;
            }
            case "max" -> Math.max(children.get(0).evaluate(variables), children.get(1).evaluate(variables));
            case "min" -> Math.min(children.get(0).evaluate(variables), children.get(1).evaluate(variables));
            default -> throw new IllegalArgumentException("未知操作");
        };
    }

    @Override
    public Node deepCopy() {
        FunctionNode copy = new FunctionNode(operation);
        for (Node child : children) {
            copy.addChild(child.deepCopy());
        }
        return copy;
    }

    @Override
    public String toExpression() {
        if (children.size() != 2) {
            return "无效表达式";
        }

        // 根据不同操作生成表达式
        return switch (operation) {
            case "+" -> "(" + children.get(0).toExpression() + " + " + children.get(1).toExpression() + ")";
            case "-" -> "(" + children.get(0).toExpression() + " - " + children.get(1).toExpression() + ")";
            case "*" -> "(" + children.get(0).toExpression() + " * " + children.get(1).toExpression() + ")";
            case "/" -> "(" + children.get(0).toExpression() + " / " + children.get(1).toExpression() + ")";
            case "max" -> "max(" + children.get(0).toExpression() + ", " + children.get(1).toExpression() + ")";
            case "min" -> "min(" + children.get(0).toExpression() + ", " + children.get(1).toExpression() + ")";
            default -> "未知操作";
        };
    }
}
