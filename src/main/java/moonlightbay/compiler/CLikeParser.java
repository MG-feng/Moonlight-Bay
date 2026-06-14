package moonlightbay.compiler;

import java.util.*;
import java.util.regex.*;

public class CLikeParser {

    private List<String> tokens;
    private int currentPos;
    private List<ParseError> errors;

    public CLikeParser() {
        errors = new ArrayList<>();
    }

    public ASTNode parse(String sourceCode) {
        errors.clear();
        tokens = tokenize(sourceCode);
        currentPos = 0;

        ASTNode root = new ASTNode();
        root.type = "program";
        root.startLine = 1;
        root.endLine = tokens.size();

        try {
            while (currentPos < tokens.size()) {
                ASTNode statement = parseStatement();
                if (statement != null) {
                    root.children.add(statement);
                }
            }
        } catch (ParseException e) {
            errors.add(new ParseError(e.getMessage(), currentPos));
            return null;
        }

        if (!errors.isEmpty()) {
            return null;
        }

        return root;
    }

    private List<String> tokenize(String sourceCode) {
        List<String> result = new ArrayList<>();
        String[] lines = sourceCode.split("\n");

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();
            if (line.isEmpty() || line.startsWith("//")) continue;

            // 简单的分词
            String[] parts = line.split("\\s+");
            for (String part : parts) {
                // 处理括号
                if (
                    part.contains("(") ||
                    part.contains(")") ||
                    part.contains("{") ||
                    part.contains("}")
                ) {
                    for (char c : part.toCharArray()) {
                        if (c == '(' || c == ')' || c == '{' || c == '}') {
                            result.add(String.valueOf(c));
                        } else if (Character.isLetterOrDigit(c) || c == '_') {
                            // 累积标识符
                            StringBuilder ident = new StringBuilder();
                            while (
                                currentPos < part.length() &&
                                (Character.isLetterOrDigit(
                                        part.charAt(currentPos)
                                    ) ||
                                    part.charAt(currentPos) == '_')
                            ) {
                                ident.append(part.charAt(currentPos));
                                currentPos++;
                            }
                            result.add(ident.toString());
                        }
                    }
                } else {
                    result.add(part);
                }
            }
        }

        return result;
    }

    private ASTNode parseStatement() throws ParseException {
        if (currentPos >= tokens.size()) return null;

        String token = tokens.get(currentPos);

        switch (token) {
            case "if":
                return parseIfStatement();
            case "while":
                return parseWhileStatement();
            case "for":
                return parseForStatement();
            case "print":
                return parsePrintStatement();
            case "sensor":
                return parseSensorStatement();
            case "control":
                return parseControlStatement();
            case "{":
                return parseBlockStatement();
            case "int":
            case "float":
            case "var":
                return parseDeclarationStatement();
            default:
                if (isIdentifier(token)) {
                    // 检查是否是赋值语句
                    if (
                        currentPos + 1 < tokens.size() &&
                        tokens.get(currentPos + 1).equals("=")
                    ) {
                        return parseAssignmentStatement();
                    }
                    return parseOperationStatement();
                }
                throw new ParseException("Unexpected token: " + token);
        }
    }

    private ASTNode parseIfStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "if";
        node.startLine = getCurrentLine();

        expect("if");
        expect("(");

        String condition = parseCondition();
        node.properties.put("condition", condition);

        expect(")");

        // then分支
        ASTNode thenBranch = parseStatement();
        if (thenBranch != null) {
            node.children.add(thenBranch);
        }

        // else分支
        if (
            currentPos < tokens.size() && tokens.get(currentPos).equals("else")
        ) {
            currentPos++;
            ASTNode elseBranch = parseStatement();
            if (elseBranch != null) {
                node.children.add(elseBranch);
            }
        }

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseWhileStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "while";
        node.startLine = getCurrentLine();

        expect("while");
        expect("(");

        String condition = parseCondition();
        node.properties.put("condition", condition);

        expect(")");

        ASTNode body = parseStatement();
        if (body != null) {
            node.children.add(body);
        }

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseForStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "for";
        node.startLine = getCurrentLine();

        expect("for");
        expect("(");

        // 初始化
        ASTNode init = parseStatement();
        if (init != null) {
            node.children.add(init);
        }

        expect(";");

        // 条件
        String condition = parseCondition();
        node.properties.put("condition", condition);

        expect(";");

        // 增量
        ASTNode increment = parseStatement();
        if (increment != null) {
            node.children.add(increment);
        }

        expect(")");

        ASTNode body = parseStatement();
        if (body != null) {
            node.children.add(body);
        }

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseAssignmentStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "assignment";
        node.startLine = getCurrentLine();

        String target = tokens.get(currentPos);
        node.properties.put("target", target);
        currentPos++;

        expect("=");

        String value = parseExpression();
        node.properties.put("value", value);

        expect(";");

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseDeclarationStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "declaration";
        node.startLine = getCurrentLine();

        String type = tokens.get(currentPos);
        node.properties.put("type", type);
        currentPos++;

        String name = tokens.get(currentPos);
        node.properties.put("name", name);
        currentPos++;

        if (currentPos < tokens.size() && tokens.get(currentPos).equals("=")) {
            currentPos++;
            String value = parseExpression();
            node.properties.put("initializer", value);
        }

        expect(";");

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parsePrintStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "print";
        node.startLine = getCurrentLine();

        expect("print");
        expect("(");

        String content = parseExpression();
        node.properties.put("content", content);

        expect(")");
        expect(";");

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseSensorStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "sensor";
        node.startLine = getCurrentLine();

        expect("sensor");

        String result = tokens.get(currentPos);
        node.properties.put("result", result);
        currentPos++;

        String entity = tokens.get(currentPos);
        node.properties.put("entity", entity);
        currentPos++;

        String property = tokens.get(currentPos);
        node.properties.put("property", property);
        currentPos++;

        expect(";");

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseControlStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "control";
        node.startLine = getCurrentLine();

        expect("control");

        String command = tokens.get(currentPos);
        node.properties.put("command", command);
        currentPos++;

        // 解析参数
        List<String> args = new ArrayList<>();
        while (
            currentPos < tokens.size() && !tokens.get(currentPos).equals(";")
        ) {
            args.add(tokens.get(currentPos));
            currentPos++;
        }
        node.properties.put("args", args);

        expect(";");

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseOperationStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "operation";
        node.startLine = getCurrentLine();

        String result = tokens.get(currentPos);
        node.properties.put("result", result);
        currentPos++;

        String operator = tokens.get(currentPos);
        node.properties.put("operator", operator);
        currentPos++;

        String a = tokens.get(currentPos);
        node.properties.put("a", a);
        currentPos++;

        String b = tokens.get(currentPos);
        node.properties.put("b", b);
        currentPos++;

        expect(";");

        node.endLine = getCurrentLine();
        return node;
    }

    private ASTNode parseBlockStatement() throws ParseException {
        ASTNode node = new ASTNode();
        node.type = "block";
        node.startLine = getCurrentLine();

        expect("{");

        while (
            currentPos < tokens.size() && !tokens.get(currentPos).equals("}")
        ) {
            ASTNode statement = parseStatement();
            if (statement != null) {
                node.children.add(statement);
            }
        }

        expect("}");

        node.endLine = getCurrentLine();
        return node;
    }

    private String parseCondition() throws ParseException {
        StringBuilder condition = new StringBuilder();

        while (
            currentPos < tokens.size() && !tokens.get(currentPos).equals(")")
        ) {
            condition.append(tokens.get(currentPos));
            currentPos++;
        }

        return condition.toString().trim();
    }

    private String parseExpression() throws ParseException {
        StringBuilder expression = new StringBuilder();

        while (
            currentPos < tokens.size() &&
            !tokens.get(currentPos).equals(";") &&
            !tokens.get(currentPos).equals(")") &&
            !tokens.get(currentPos).equals(",")
        ) {
            expression.append(tokens.get(currentPos));
            currentPos++;
        }

        return expression.toString().trim();
    }

    private void expect(String expected) throws ParseException {
        if (currentPos >= tokens.size()) {
            throw new ParseException(
                "Expected '" + expected + "' but reached end of input"
            );
        }

        String actual = tokens.get(currentPos);
        if (!actual.equals(expected)) {
            throw new ParseException(
                "Expected '" + expected + "' but got '" + actual + "'"
            );
        }

        currentPos++;
    }

    private boolean isIdentifier(String token) {
        return token.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    private int getCurrentLine() {
        return currentPos / 10 + 1; // 粗略估计
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // AST节点类
    public static class ASTNode {

        public String type;
        public Map<String, Object> properties = new HashMap<>();
        public List<ASTNode> children = new ArrayList<>();
        public int startLine;
        public int endLine;

        public ASTNode() {}

        public ASTNode(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return (
                "ASTNode{" +
                "type='" +
                type +
                '\'' +
                ", properties=" +
                properties +
                ", children=" +
                children.size() +
                '}'
            );
        }
    }

    public static class ParseException extends Exception {

        public ParseException(String message) {
            super(message);
        }
    }

    public static class ParseError {

        public String message;
        public int line;

        public ParseError(String message, int line) {
            this.message = message;
            this.line = line;
        }
    }
}
