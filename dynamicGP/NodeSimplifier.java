package org.lileischeduler.dynamicGP;

import org.lileischeduler.dynamicGP.model.FunctionNode;
import org.lileischeduler.dynamicGP.model.Node;
import org.lileischeduler.dynamicGP.model.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NodeSimplifier {
    // 简化节点的静态方法
    public static Node simplify(Node node) {
        if (node instanceof TerminalNode) {
            return node;
        }

        if (node instanceof FunctionNode) {
            FunctionNode functionNode = (FunctionNode) node;

            // 递归简化子节点
            List<Node> simplifiedChildren = new ArrayList<>();
            for (Node child : functionNode.children) {
                simplifiedChildren.add(simplify(child));
            }
            functionNode.children = simplifiedChildren;

            // 常数子树优化
            return simplifyConstantSubtree(functionNode);
        }

        return node;
    }

    // 简化常数子树
    private static Node simplifyConstantSubtree(FunctionNode node) {
        // 如果所有子节点都是常数节点，直接计算
        if (areAllChildrenConstants(node)) {
            Map<String, Double> emptyVariables = new HashMap<>();
            double constantValue = node.evaluate(emptyVariables);

            return new TerminalNode(String.valueOf(constantValue), false) {{
                this.value = constantValue;
            }};
        }

        // 特殊优化规则
        return optimizeSpecificPatterns(node);
    }

    // 检查是否所有子节点都是常数
    private static boolean areAllChildrenConstants(FunctionNode node) {
        return node.children.stream()
                .allMatch(child -> child instanceof TerminalNode &&
                        !((TerminalNode) child).isVariable);
    }

    // 特定模式优化
    private static Node optimizeSpecificPatterns(FunctionNode node) {
        // 优化加法恒等式
        if (node.operation.equals("+")) {
            // x + 0 = x
            if (isConstantZero(node.children.get(1))) {
                return node.children.get(0);
            }
            if (isConstantZero(node.children.get(0))) {
                return node.children.get(1);
            }
        }

        // 优化乘法恒等式
        if (node.operation.equals("*")) {
            // x * 1 = x
            if (isConstantOne(node.children.get(1))) {
                return node.children.get(0);
            }
            if (isConstantOne(node.children.get(0))) {
                return node.children.get(1);
            }

            // x * 0 = 0
            if (isConstantZero(node.children.get(0)) ||
                    isConstantZero(node.children.get(1))) {
                return new TerminalNode("0", false) {{
                    this.value = 0.0;
                }};
            }
        }

        return node;
    }

    // 判断是否为0
    private static boolean isConstantZero(Node node) {
        return node instanceof TerminalNode &&
                !((TerminalNode) node).isVariable &&
                ((TerminalNode) node).value == 0.0;
    }

    // 判断是否为1
    private static boolean isConstantOne(Node node) {
        return node instanceof TerminalNode &&
                !((TerminalNode) node).isVariable &&
                ((TerminalNode) node).value == 1.0;
    }

    // 深度简化
    public static Node deepSimplify(Node node) {
        Node simplified = simplify(node);

        // 如果是函数节点,继续深度简化
        if (simplified instanceof FunctionNode) {
            FunctionNode funcNode = (FunctionNode) simplified;

            // 子节点深度简化
            List<Node> newChildren = new ArrayList<>();
            for (Node child : funcNode.children) {
                newChildren.add(deepSimplify(child));
            }

            funcNode.children = newChildren;
        }
        return simplified;
    }
}
