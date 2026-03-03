package org.lileischeduler.dynamicGP;

import org.lileischeduler.dynamicGP.model.FunctionNode;
import org.lileischeduler.dynamicGP.model.Node;
import org.lileischeduler.dynamicGP.model.TerminalNode;

import java.util.*;

public class extractExpressionToTree {

    private Set<String> operators = Set.of("+", "-", "*", "/", "min", "max");

    public Node parse(String expression) {
        // Removing whitespace and initializing stack & queue
        expression = expression.replaceAll("\\s+", "");
        Deque<String> tokens = tokenize(expression);
        return parseExpression(tokens);
    }

    private Deque<String> tokenize(String expression) {
        Deque<String> tokens = new LinkedList<>();
        StringBuilder token = new StringBuilder();
        for (char ch : expression.toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == '.') {
                token.append(ch);
            } else {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                tokens.add(String.valueOf(ch));
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    private Node parseExpression(Deque<String> tokens) {
        if (tokens.isEmpty()) return null;

        Node node = parseTerm(tokens);

        while (!tokens.isEmpty() && (tokens.peek().equals("+") || tokens.peek().equals("-"))) {
            String op = tokens.poll();
            FunctionNode functionNode = new FunctionNode(op);
            functionNode.addChild(node);
            functionNode.addChild(parseTerm(tokens));
            node = functionNode;
        }

        return node;
    }

    private Node parseTerm(Deque<String> tokens) {
        Node node = parseFactor(tokens);

        while (!tokens.isEmpty() && (tokens.peek().equals("*") || tokens.peek().equals("/"))) {
            String op = tokens.poll();
            FunctionNode functionNode = new FunctionNode(op);
            functionNode.addChild(node);
            functionNode.addChild(parseFactor(tokens));
            node = functionNode;
        }

        return node;
    }

    private Node parseFactor(Deque<String> tokens) {
        String token = tokens.poll();

        if (operators.contains(token)) {
            FunctionNode functionNode = new FunctionNode(token);
            tokens.poll(); // assuming opening parenthesis
            while (!tokens.peek().equals(")")) {
                functionNode.addChild(parseExpression(tokens));
                if (tokens.peek().equals(",")) tokens.poll();
            }
            tokens.poll(); // assuming closing parenthesis
            return functionNode;
        } else if (token.equals("(")) {
            Node node = parseExpression(tokens);
            tokens.poll(); // assuming closing parenthesis
            return node;
        } else {
            if (Character.isLetter(token.charAt(0))) {
                return new TerminalNode(token, true);
            } else {
                return new TerminalNode(token, false);
            }
        }
    }

    public static void main(String[] args) {
        String expression = "min(((max(((Transition - (Transition - DownloadNum)) + EnergyConsumption), DownloadDuration) - ((((DownloadDuration - HistoryScore) * (Transition + DownloadRate)) - (Inclination - HistoryScore)) + min((Storage + DownloadRate), Inclination))) * min(min(HistoryScore, Inclination), Storage)), (min(max(Inclination, (DownloadRate + max((Inclination + Storage), Transition))), (HistoryScore * HistoryScore)) + DownloadDuration))";
        extractExpressionToTree e = new extractExpressionToTree();

        Node expressionTree = e.parse(expression);
        System.out.println(expression);
        System.out.println(expressionTree.toExpression());
        System.out.println("Expression parsed successfully.");
        // You can add additional functionality to traverse or evaluate the tree here.
    }
}

