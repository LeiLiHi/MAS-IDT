package org.lileischeduler.dynamicGP;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.lileischeduler.dynamicGP.LLMChat.AiAssistant;
import org.lileischeduler.dynamicGP.generateData.LoadSatellite;
import org.lileischeduler.dynamicGP.generateData.LoadStation;
import org.lileischeduler.dynamicGP.model.*;
import org.lileischeduler.tool.Tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.lileischeduler.dynamicGP.HeuristicGP.decodeTask;
import static org.lileischeduler.tool.Tool.random;

// 遗传规划主类
public class GeneticProgramming {
    private static final String[] FUNCTIONS = {"+", "-", "*", "/", "max", "min"};
    private static final String[] VARIABLES = {"DownloadNum", "DownloadDuration", "HistoryScore", "DownloadRate", "Storage", "Energy", "Transition", "Inclination", "EnergyConsumption"};
    private static int POPULATION_SIZE = 20;
    private static int MAX_GENERATIONS = 50;
    private static double CROSSOVER_RATE = 0.7;
    private static double ELITE_RATE = 0.1;
    private static double MUTATION_RATE = 0.2;
    private static int DEPTH = 24;
    private static double MAX_ITERATIONTIME = 100;
    private static double[] EliteMutateProb = new double[VARIABLES.length];
    private static double[] LLMMutateProb = new double[VARIABLES.length];
    private String filePath = "dataLLMMutate.json";
    private String filePath0 = "dataLLMIndividual.json";
    private static extractExpressionToTree exTree = new extractExpressionToTree();
    public NodeInfo hisBest;
    public BufferedWriter writer = null;

    public GeneticProgramming(double[] para, BufferedWriter wr) {
        writer = wr;
        POPULATION_SIZE = (int) para[0];
        MAX_GENERATIONS = (int) para[1];
        CROSSOVER_RATE = para[2];
        ELITE_RATE = para[3];
        MUTATION_RATE = para[4];
        DEPTH = (int) para[5];
        MAX_ITERATIONTIME = para[6];
    }

    public GeneticProgramming() {
    }

    /****** 辅助变量 ******/
    private static final Map<String, Integer> VARIABLE_INDEX_MAP = new HashMap<>();
    ExecutorService executor = Executors.newCachedThreadPool();         // 线程池
    Future<?> lastFuture = null;                                        // 跟踪上一个任务的Future
    Future<?> indiFuture = null;                                        // 跟踪上一个个体
    public static OpenAiChatModel model;
    public static AiAssistant aiAssistant;
    public static AiAssistant aiAssistant0;

    static {
        double uniformProbability = 1.0 / VARIABLES.length;
        Arrays.fill(EliteMutateProb, uniformProbability);
        Arrays.fill(LLMMutateProb, uniformProbability);

        for (int i = 0; i < VARIABLES.length; i++) {
            VARIABLE_INDEX_MAP.put(VARIABLES[i], i);
        }

        model = OpenAiChatModel.builder()
                .baseUrl("https://api.xty.app/v1")
                .apiKey("sk-9zHSTBwikrmMmtS5055f6701Ea7f47BfB3935c175bF369Da")
                .modelName("gpt-3.5-turbo")
                .timeout(Duration.ofSeconds(30))
                .build();
        aiAssistant = AiServices.create(AiAssistant.class, model);
        aiAssistant0 = AiServices.create(AiAssistant.class, model);
    }

    public Node generateRandomTree(int depth) {
        // 递归深度和概率控制
        if (depth <= 0 /*|| Math.random() < 1.0 / (depth + 1)*/) {
            // 终端节点生成
            return createTerminalNode();
        }
        // 函数节点生成
        return createFunctionNode(depth);
    }

    private Node createTerminalNode() {
        double rand = Math.random();
        if (rand < 1) {
            // 40%概率生成变量
            return new TerminalNode(
                    VARIABLES[(int) (Math.random() * VARIABLES.length)],
                    true
            );
        } else {
            // 30%概率生成特殊常数
            return new TerminalNode("1", false) {{
                this.value = 1.0;
            }};
        }
    }

    private Node createFunctionNode(int depth) {
        FunctionNode functionNode = new FunctionNode(
                FUNCTIONS[(int) (Math.random() * FUNCTIONS.length)]              // 索引某个操作符  加减乘除
        );

        // 平衡子树深度
        int leftDepth = (int) (Math.random() * depth);
        int rightDepth = depth - leftDepth - 1;

        functionNode.addChild(generateRandomTree(leftDepth));
        functionNode.addChild(generateRandomTree(rightDepth));

        return functionNode;
    }

    /**
     * 递归变异节点，仅替换操作符或变量名称
     */
    private void mutateNode(Node node) {
        // 概率判定是否进行变异
        boolean is = random.nextDouble() >= MUTATION_RATE;                            // 当前节点是否进行变异
        if (is && node instanceof FunctionNode) {
            ((FunctionNode) node).operation = FUNCTIONS[random.nextInt(FUNCTIONS.length)];
        } else if (is && node instanceof TerminalNode) {
            int selectedIndex;                          // 大模型还是自身变异
            if (random.nextDouble() < 0.5)
                selectedIndex = selectVariableByProbability(random, EliteMutateProb);
            else
                selectedIndex = selectVariableByProbability(random, LLMMutateProb);
            ((TerminalNode) node).name = VARIABLES[selectedIndex];
        }
        // 递归处理子节点
        if (node instanceof FunctionNode) {
            for (Node child : ((FunctionNode) node).children) {
                mutateNode(child);
            }
        }
    }

    int selectVariableByProbability(Random random, double[] probabilities) {
        double total = Arrays.stream(probabilities).sum();
        double rand = random.nextDouble() * total; // 乘以总概率以支持未归一化的情况
        double cumulative = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return i;
            }
        }
        // 防止浮点误差，返回最后一个变量
        return probabilities.length - 1;
    }

    // 交叉操作
    public Node[] crossover(Node parent1, Node parent2) {
        // 深拷贝避免修改原始结构
        Node copy1 = parent1.deepCopy();
        Node copy2 = parent2.deepCopy();

        // 收集两棵树所有可交叉节点（包含父节点+子节点位置信息）
        List<CrossoverPoint> points1 = collectCrossoverPoints(copy1);
        List<CrossoverPoint> points2 = collectCrossoverPoints(copy2);

        // 随机选择交换点（单点交叉）
        CrossoverPoint p1 = points1.get(random.nextInt(points1.size()));
        CrossoverPoint p2 = points2.get(random.nextInt(points2.size()));

        // 执行子树交换
        return new Node[]{swapSubtrees(copy1, p1, p2.node), swapSubtrees(copy2, p2, p1.node)};
    }


    // 辅助类记录节点位置信息
    private static class CrossoverPoint {
        Node node;
        FunctionNode parent;
        int childIndex;

        CrossoverPoint(Node node, FunctionNode parent, int childIndex) {
            this.node = node;
            this.parent = parent;
            this.childIndex = childIndex;
        }
    }

    // 收集所有可交叉节点及其父级信息
    private List<CrossoverPoint> collectCrossoverPoints(Node root) {
        List<CrossoverPoint> points = new ArrayList<>();
        // 添加根节点作为可交换点（父级为null）
        points.add(new CrossoverPoint(root, null, -1));

        // 递归遍历函数节点子树
        if (root instanceof FunctionNode) {
            traverseFunctionNode((FunctionNode) root, points);
        }
        return points;
    }

    // 执行实际子树交换
    private Node swapSubtrees(Node root, CrossoverPoint point, Node newSubtree) {
        if (point.parent == null) {
            // 当交换根节点时直接返回新子树
            return newSubtree;
        } else {
            // 在原树中替换指定位置的子树
            point.parent.children.set(point.childIndex, newSubtree);
            return root;
        }
    }

    // 适应度评估
    public double[] fitness(Node tree, LoadSatellite satellites, List<Target> targets, LoadStation station) {
        for (Satellite satellite : satellites.satelliteList) {
            satellite.score = tree.evaluate(satellite.getProperties());
        }
        satellites.satelliteList.sort((s1, s2) ->
                Double.compare(s2.score, s1.score));
        for (Target target : targets) target.reSet();
        station.reSet();
        satellites.reSet();
        return decodeTask(targets, satellites.satelliteList, station.stationList);
    }


    // 主遗传算法
    public void evolve(LoadSatellite satellites, List<Target> targets, LoadStation station) throws IOException {
        // 种群初始化
        List<NodeInfo> pops = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Node pop = generateRandomTree(DEPTH);
            double[] fit = fitness(pop, satellites, targets, station);
            NodeInfo nodeInfo = new NodeInfo(pop, fit, null);
            pops.add(nodeInfo);
        }
        pops.sort((a, b) ->
                Double.compare(b.fitness[1], a.fitness[1]));
        // 精英种群（10%）
        List<NodeInfo> Elites = new ArrayList<>(pops.subList(0, (int) (POPULATION_SIZE * ELITE_RATE)));
        NodeInfo tem = pops.get(0);
        hisBest = new NodeInfo(tem.node.deepCopy(), tem.fitness, null);

        // 进化
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_GENERATIONS; i++) {
            synchronized (pops) {
                if (random.nextDouble() < CROSSOVER_RATE) {                     // 交叉
                    // 精英 当前种群各自选一个
                    NodeInfo p1 = pops.get(random.nextInt(pops.size()));
                    NodeInfo p2 = Elites.get(random.nextInt(Elites.size()));
                    Node[] off = crossover(p1.node, p2.node);
                    NodeInfo o1 = new NodeInfo(off[0], null, null);
                    NodeInfo o2 = new NodeInfo(off[1], null, null);
                    pops.add(o1);
                    pops.add(o2);
                }

                for (NodeInfo nodeInfo : pops) {
                    mutateNode(nodeInfo.node);
                    // 重新评估，更新表达式、适应度值
                    double[] fit = fitness(nodeInfo.node, satellites, targets, station);
                    nodeInfo.Expression = nodeInfo.node.toExpression();
                    nodeInfo.fitness = fit;
                }
                // 更新种群(精英个体、普通个体弹出最后几个比较差的个体)
                pops.sort((a, b) -> Double.compare(b.fitness[1], a.fitness[1]));
            }
            synchronized (pops) {
                synchronized (Elites) {
                    Elites.clear();
                    Elites.addAll(pops.subList(0, (int) (POPULATION_SIZE * ELITE_RATE)));
                }
                while (pops.size() > POPULATION_SIZE) pops.remove(pops.size() - 1);
                NodeInfo temp = pops.get(0);
                if (temp.fitness[1] >= hisBest.fitness[1]) {
                    hisBest = new NodeInfo(temp.node.deepCopy(), temp.fitness, null);
                }
            }

            // 根据精英更新变异概率
            updateMutationProbability(i, Elites);

            // 仅当上一个任务不存在或已完成 以及 其他条件时时，才提交新概率的变异
            if ((lastFuture == null || lastFuture.isDone()) && (i + 1) % 3 == 0) {
                lastFuture = executor.submit(() -> {
                    // 根据精英个体的expression score 和description生成新的（最多5个）
                    String input = "";
                    synchronized (Elites) {
                        for (NodeInfo nodeInfo : Elites) {
                            input = "Expression: " + nodeInfo.node.toExpression() + "\n"
                                    + "Fitness: " + Arrays.toString(nodeInfo.fitness) + "\n";
                        }
                    }
                    String answer = aiAssistant.summarizeAsArray(input);
                    double[] mutate = Tool.extractDoubleArray(answer);
                    if (mutate != null) LLMMutateProb = mutate;
                    System.out.println(answer);
                    // 记录输入和输出
                    Tool.appendToFile(filePath, input);
                    Tool.appendToFile(filePath, answer);
                });
            }

            // 生成新的expression （抽取后递归计算）
            if ((indiFuture == null || indiFuture.isDone()) && (i) % 3 == 0) {
                indiFuture = executor.submit(() -> {
                    // 根据精英个体的expression score 和description生成新的（最多5个）
                    String input = "";
                    synchronized (Elites) {
                        for (NodeInfo nodeInfo : Elites) {
                            input = "Expression: " + nodeInfo.node.toExpression() + "\n"
                                    + "Fitness: " + Arrays.toString(nodeInfo.fitness) + "\n";
                        }
                    }

                    String answer = aiAssistant0.generateIndividual(input);
                    Node node = exTree.parse(answer.split("\n")[0].split(":")[1]);
                    NodeInfo nodeInfo = new NodeInfo(node, null, null);

                    synchronized (pops) {
                        pops.add(nodeInfo);
                    }

                    // 记录输入和输出
                    System.out.println(answer);
                    Tool.appendToFile(filePath0, input);
                    Tool.appendToFile(filePath0, answer);
                });
            }
            // 更新变异概率（基于精英解的）
            double time = (System.currentTimeMillis() - startTime) / 1000.0;
            if (time > MAX_ITERATIONTIME || hisBest.fitness[0] == targets.size()) break;
//            System.out.println("第 " + i + " 最优：" + hisBest.fitness[0] + " " + hisBest.fitness[1] + " " + hisBest.node.toExpression());
        }
        executor.shutdownNow();                // 关闭线程
    }

    // 递归遍历节点树，统计变量出现次数
    private void countVariableOccurrences(Node node, double[] counts) {
        if (node instanceof TerminalNode) {
            TerminalNode tn = (TerminalNode) node;
            if (tn.isVariable) {
                Integer index = VARIABLE_INDEX_MAP.get(tn.name);
                if (index != null) {
                    counts[index]++;
                }
            }
        } else {        // 否则是功能节点
            FunctionNode fn = (FunctionNode) node;
            for (int i = 0; i < fn.children.size(); i++) {
                Node child = fn.children.get(i);
                countVariableOccurrences(child, counts);
            }
        }
    }

    // 递归遍历函数节点
    private void traverseFunctionNode(FunctionNode fn, List<CrossoverPoint> points) {
        for (int i = 0; i < fn.children.size(); i++) {
            Node child = fn.children.get(i);
            points.add(new CrossoverPoint(child, fn, i)); // 记录子节点位置
            if (child instanceof FunctionNode) {
                traverseFunctionNode((FunctionNode) child, points);
            }
        }
    }

    public void updateMutationProbability(int generation, List<NodeInfo> Elites) {
        if ((generation + 1) % 10 == 0) {  // 更新变异概率（精英）
            double pseudoCount = 5 - (4.0 * generation / MAX_GENERATIONS);
            double[] counts = new double[VARIABLES.length];
            Arrays.fill(counts, pseudoCount);
            for (NodeInfo elite : Elites) {
                countVariableOccurrences(elite.node, counts);
            }
            double sum = Arrays.stream(counts).sum();
            for (int j = 0; j < VARIABLES.length; j++) {
                EliteMutateProb[j] /= sum;
            }
        }
    }
}
