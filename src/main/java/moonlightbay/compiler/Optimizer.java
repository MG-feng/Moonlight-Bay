package moonlightbay.compiler;

import java.util.*;
import moonlightbay.compiler.CLikeParser.ASTNode;

public class Optimizer {

    private int optimizationLevel;
    private boolean verbose;

    // 优化统计
    private int constantFoldingCount = 0;
    private int deadCodeRemovedCount = 0;
    private int instructionsMergedCount = 0;

    public Optimizer() {
        this.verbose = false;
    }

    public ASTNode optimizeAST(ASTNode ast, int optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
        this.constantFoldingCount = 0;
        this.deadCodeRemovedCount = 0;
        this.instructionsMergedCount = 0;

        if (ast == null || optimizationLevel < 1) return ast;

        ASTNode result = ast;

        // Level 1: 基本优化
        if (optimizationLevel >= 1) {
            result = constantFolding(result);
            result = deadCodeElimination(result);
            result = simplifyExpressions(result);
        }

        // Level 2: 中等优化
        if (optimizationLevel >= 2) {
            result = instructionMerging(result);
            result = propagateCopies(result);
            result = eliminateCommonSubexpressions(result);
        }

        // Level 3: 激进优化
        if (optimizationLevel >= 3) {
            result = loopOptimization(result);
            result = inlineConstants(result);
            result = unrollLoops(result);
        }

        if (verbose && optimizationLevel > 0) {
            System.out.println(
                "[Optimizer] Constant folding: " + constantFoldingCount
            );
            System.out.println(
                "[Optimizer] Dead code removed: " + deadCodeRemovedCount
            );
            System.out.println(
                "[Optimizer] Instructions merged: " + instructionsMergedCount
            );
        }

        return result;
    }

    public String optimizeMLog(String mlogCode) {
        if (mlogCode == null || optimizationLevel < 1) return mlogCode;

        String[] lines = mlogCode.split("\n");
        List<String> optimized = new ArrayList<>();
        List<String> result = new ArrayList<>();

        // 第一遍：基本清理
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // 跳过死跳转后的代码
            if (isDeadCode(line, optimized)) {
                deadCodeRemovedCount++;
                continue;
            }

            optimized.add(line);
        }

        // 第二遍：指令合并
        for (int i = 0; i < optimized.size(); i++) {
            String current = optimized.get(i);

            if (i + 1 < optimized.size()) {
                String next = optimized.get(i + 1);
                String merged = tryMergeInstructions(current, next);
                if (merged != null) {
                    result.add(merged);
                    instructionsMergedCount++;
                    i++;
                    continue;
                }
            }

            result.add(current);
        }

        // 第三遍：优化跳转
        result = optimizeJumps(result);

        return String.join("\n", result);
    }

    private ASTNode constantFolding(ASTNode node) {
        if (node == null) return node;

        // 递归处理子节点
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, constantFolding(node.children.get(i)));
        }

        // 处理赋值语句中的常量表达式
        if ("assignment".equals(node.type)) {
            String value = (String) node.properties.get("value");
            if (value != null && isConstantExpression(value)) {
                String folded = foldConstantExpression(value);
                if (!folded.equals(value)) {
                    node.properties.put("value", folded);
                    constantFoldingCount++;
                }
            }
        }

        // 处理条件语句中的常量条件
        if ("if".equals(node.type) || "while".equals(node.type)) {
            String condition = (String) node.properties.get("condition");
            if (condition != null && isConstantExpression(condition)) {
                String folded = foldConstantExpression(condition);
                if (!folded.equals(condition)) {
                    node.properties.put("condition", folded);
                    constantFoldingCount++;
                }
            }
        }

        return node;
    }

    private ASTNode deadCodeElimination(ASTNode node) {
        if (node == null) return node;

        // 移除永远为false的if分支
        if ("if".equals(node.type)) {
            String condition = (String) node.properties.get("condition");
            if (condition != null && isConstantExpression(condition)) {
                String folded = foldConstantExpression(condition);
                if (folded.equals("0")) {
                    // 条件永远为假，移除then分支
                    deadCodeRemovedCount++;
                    return node.children.size() > 1
                        ? node.children.get(1)
                        : null;
                } else if (!folded.equals("0") && !folded.equals("false")) {
                    // 条件永远为真，移除else分支
                    deadCodeRemovedCount++;
                    return node.children.isEmpty()
                        ? null
                        : node.children.get(0);
                }
            }
        }

        // 递归处理子节点
        List<ASTNode> newChildren = new ArrayList<>();
        for (ASTNode child : node.children) {
            ASTNode optimized = deadCodeElimination(child);
            if (optimized != null) {
                newChildren.add(optimized);
            }
        }
        node.children = newChildren;

        return node;
    }

    private ASTNode simplifyExpressions(ASTNode node) {
        if (node == null) return node;

        // 简化 x = x + 0 为 x = x
        if ("assignment".equals(node.type)) {
            String value = (String) node.properties.get("value");
            String target = (String) node.properties.get("target");

            if (value != null && target != null) {
                // x = x + 0 -> x = x
                if (
                    value.matches(target + "\\s*\\+\\s*0") ||
                    value.matches("0\\s*\\+\\s*" + target)
                ) {
                    node.properties.put("value", target);
                }
                // x = x * 1 -> x = x
                if (
                    value.matches(target + "\\s*\\*\\s*1") ||
                    value.matches("1\\s*\\*\\s*" + target)
                ) {
                    node.properties.put("value", target);
                }
            }
        }

        // 递归处理子节点
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, simplifyExpressions(node.children.get(i)));
        }

        return node;
    }

    private ASTNode instructionMerging(ASTNode node) {
        if (node == null) return node;

        // 合并连续的赋值语句
        if ("block".equals(node.type)) {
            List<ASTNode> merged = new ArrayList<>();

            for (int i = 0; i < node.children.size(); i++) {
                ASTNode current = node.children.get(i);

                if (
                    i + 1 < node.children.size() &&
                    canMerge(current, node.children.get(i + 1))
                ) {
                    ASTNode mergedNode = mergeAssignments(
                        current,
                        node.children.get(i + 1)
                    );
                    if (mergedNode != null) {
                        merged.add(mergedNode);
                        instructionsMergedCount++;
                        i++;
                        continue;
                    }
                }

                merged.add(current);
            }

            node.children = merged;
        }

        // 递归处理子节点
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, instructionMerging(node.children.get(i)));
        }

        return node;
    }

    private ASTNode propagateCopies(ASTNode node) {
        if (node == null) return node;

        // 传播副本：将 a = b; use a 替换为 use b
        Map<String, String> copyMap = new HashMap<>();

        if ("block".equals(node.type)) {
            for (ASTNode child : node.children) {
                if ("assignment".equals(child.type)) {
                    String target = (String) child.properties.get("target");
                    String value = (String) child.properties.get("value");

                    if (
                        value != null &&
                        !value.contains(" ") &&
                        !value.contains("+") &&
                        !value.contains("-") &&
                        !value.contains("*") &&
                        !value.contains("/")
                    ) {
                        copyMap.put(target, value);
                    }
                } else {
                    // 替换引用
                    replaceReferences(child, copyMap);
                }
            }
        }

        // 递归处理子节点
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, propagateCopies(node.children.get(i)));
        }

        return node;
    }

    private ASTNode eliminateCommonSubexpressions(ASTNode node) {
        if (node == null) return node;

        // 公共子表达式消除
        Map<String, String> exprCache = new HashMap<>();

        if ("block".equals(node.type)) {
            for (ASTNode child : node.children) {
                if ("assignment".equals(child.type)) {
                    String value = (String) child.properties.get("value");
                    if (
                        (value != null && value.contains("+")) ||
                        value.contains("*")
                    ) {
                        if (exprCache.containsKey(value)) {
                            // 重用之前的结果
                            child.properties.put("value", exprCache.get(value));
                        } else {
                            exprCache.put(
                                value,
                                (String) child.properties.get("target")
                            );
                        }
                    }
                }
            }
        }

        // 递归处理
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(
                i,
                eliminateCommonSubexpressions(node.children.get(i))
            );
        }

        return node;
    }

    private ASTNode loopOptimization(ASTNode node) {
        if (node == null) return node;

        // 循环优化：循环不变代码外提
        if ("while".equals(node.type) || "for".equals(node.type)) {
            // 找到循环体中不随循环变化的代码并提到循环外
            // 简化实现
        }

        // 递归处理
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, loopOptimization(node.children.get(i)));
        }

        return node;
    }

    private ASTNode inlineConstants(ASTNode node) {
        if (node == null) return node;

        // 常量内联
        Map<String, String> constants = new HashMap<>();

        if ("block".equals(node.type)) {
            // 收集常量定义
            for (ASTNode child : node.children) {
                if (
                    "declaration".equals(child.type) &&
                    child.properties.containsKey("initializer")
                ) {
                    String name = (String) child.properties.get("name");
                    String value = (String) child.properties.get("initializer");
                    if (isNumeric(value)) {
                        constants.put(name, value);
                    }
                }
            }

            // 内联常量
            for (ASTNode child : node.children) {
                replaceConstants(child, constants);
            }
        }

        // 递归处理
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, inlineConstants(node.children.get(i)));
        }

        return node;
    }

    private ASTNode unrollLoops(ASTNode node) {
        if (node == null || optimizationLevel < 3) return node;

        // 循环展开（仅对小循环）
        // 简化实现

        // 递归处理
        for (int i = 0; i < node.children.size(); i++) {
            node.children.set(i, unrollLoops(node.children.get(i)));
        }

        return node;
    }

    private List<String> optimizeJumps(List<String> instructions) {
        List<String> result = new ArrayList<>();
        Map<String, Integer> labelPositions = new HashMap<>();

        // 第一遍：记录标签位置
        for (int i = 0; i < instructions.size(); i++) {
            String line = instructions.get(i);
            if (line.endsWith(":")) {
                labelPositions.put(line.substring(0, line.length() - 1), i);
            }
        }

        // 第二遍：优化跳转
        for (int i = 0; i < instructions.size(); i++) {
            String line = instructions.get(i);

            // 优化 jump label; label: 为直接跳转
            if (line.startsWith("jump") && i + 1 < instructions.size()) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String targetLabel = parts[1];
                    Integer targetPos = labelPositions.get(targetLabel);
                    if (
                        targetPos != null &&
                        targetPos == i + 1 &&
                        (parts.length == 2 || parts[2].equals("always"))
                    ) {
                        // 跳转到下一行，可以移除
                        continue;
                    }
                }
            }

            result.add(line);
        }

        return result;
    }

    private boolean canMerge(ASTNode a, ASTNode b) {
        if (a == null || b == null) return false;
        if (
            !"assignment".equals(a.type) || !"assignment".equals(b.type)
        ) return false;

        String targetA = (String) a.properties.get("target");
        String valueB = (String) b.properties.get("value");

        return valueB != null && valueB.equals(targetA);
    }

    private ASTNode mergeAssignments(ASTNode a, ASTNode b) {
        String targetA = (String) a.properties.get("target");
        String valueA = (String) a.properties.get("value");
        String valueB = (String) b.properties.get("value");

        if (valueB != null && valueB.equals(targetA)) {
            ASTNode merged = new ASTNode("assignment");
            merged.properties.put(
                "target",
                (String) b.properties.get("target")
            );
            merged.properties.put("value", valueA);
            return merged;
        }

        return null;
    }

    private void replaceReferences(ASTNode node, Map<String, String> copyMap) {
        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.properties.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                for (Map.Entry<String, String> copy : copyMap.entrySet()) {
                    value = value.replace(copy.getKey(), copy.getValue());
                }
                entry.setValue(value);
            }
        }

        for (ASTNode child : node.children) {
            replaceReferences(child, copyMap);
        }
    }

    private void replaceConstants(ASTNode node, Map<String, String> constants) {
        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.properties.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                for (Map.Entry<
                    String,
                    String
                > constant : constants.entrySet()) {
                    value = value.replace(
                        constant.getKey(),
                        constant.getValue()
                    );
                }
                entry.setValue(value);
            }
        }

        for (ASTNode child : node.children) {
            replaceConstants(child, constants);
        }
    }

    private boolean isConstantExpression(String expr) {
        return (
            expr.matches("[0-9]+") ||
            expr.matches("[0-9]+\\s*[+\\-*/]\\s*[0-9]+") ||
            expr.equals("true") ||
            expr.equals("false")
        );
    }

    private String foldConstantExpression(String expr) {
        expr = expr.trim();

        try {
            if (expr.contains("+")) {
                String[] parts = expr.split("\\+");
                if (parts.length == 2) {
                    int left = Integer.parseInt(parts[0].trim());
                    int right = Integer.parseInt(parts[1].trim());
                    return String.valueOf(left + right);
                }
            } else if (expr.contains("-")) {
                String[] parts = expr.split("-");
                if (parts.length == 2) {
                    int left = Integer.parseInt(parts[0].trim());
                    int right = Integer.parseInt(parts[1].trim());
                    return String.valueOf(left - right);
                }
            } else if (expr.contains("*")) {
                String[] parts = expr.split("\\*");
                if (parts.length == 2) {
                    int left = Integer.parseInt(parts[0].trim());
                    int right = Integer.parseInt(parts[1].trim());
                    return String.valueOf(left * right);
                }
            } else if (expr.contains("/")) {
                String[] parts = expr.split("/");
                if (parts.length == 2) {
                    int left = Integer.parseInt(parts[0].trim());
                    int right = Integer.parseInt(parts[1].trim());
                    return String.valueOf(left / right);
                }
            }
        } catch (NumberFormatException e) {
            // 不是数字常量
        }

        if (expr.equals("true")) return "1";
        if (expr.equals("false")) return "0";

        return expr;
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isDeadCode(String line, List<String> previous) {
        if (previous.isEmpty()) return false;

        String last = previous.get(previous.size() - 1);
        // 如果前一条是无条件跳转，当前行就是死代码
        return last.startsWith("jump") && last.contains("always");
    }

    private String tryMergeInstructions(String line1, String line2) {
        String[] parts1 = line1.split("\\s+");
        String[] parts2 = line2.split("\\s+");

        // set a x; set b a -> set b x
        if (parts1[0].equals("set") && parts2[0].equals("set")) {
            if (parts2[2].equals(parts1[1])) {
                return "set " + parts2[1] + " " + parts1[2];
            }
        }

        // op add a b c; set d a -> op add d b c
        if (
            parts1[0].equals("op") &&
            parts2[0].equals("set") &&
            parts2[2].equals(parts1[1])
        ) {
            String op = parts1[1];
            String result = parts2[1];
            String a = parts1[2];
            String b = parts1[3];
            return "op " + op + " " + result + " " + a + " " + b;
        }

        return null;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public Map<String, Integer> getOptimizationStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("constantFolding", constantFoldingCount);
        stats.put("deadCodeRemoved", deadCodeRemovedCount);
        stats.put("instructionsMerged", instructionsMergedCount);
        return stats;
    }
}
