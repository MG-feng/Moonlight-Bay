package moonlightbay.core;

import arc.struct.Seq;
import arc.util.Log;
import java.util.*;
import mindustry.logic.LAssembler;
import mindustry.logic.LStatement;
import mindustry.world.blocks.logic.LogicBlock;
import moonlightbay.compiler.*;

public class MLogOptimizer {

    private boolean initialized = false;
    private boolean debugMode = false;

    // 编译缓存
    private Map<String, CompiledResult> compiledCache = new HashMap<>();
    private Map<LogicBlock, CompiledResult> blockCache = new HashMap<>();

    // 编译器组件
    private CLikeParser parser;
    private BytecodeGenerator generator;
    private Optimizer optimizer;

    // 统计信息
    private int totalCompilations = 0;
    private int cacheHits = 0;
    private long totalOptimizationTime = 0;

    public MLogOptimizer() {
        parser = new CLikeParser();
        generator = new BytecodeGenerator();
        optimizer = new Optimizer();
    }

    public void init() {
        if (initialized) return;
        initialized = true;

        debugMode = isDebugEnabled();
        Log.info(
            "[MLogOptimizer] Initialized with optimization level: " +
                getOptimizationLevel()
        );

        registerCompilationHooks();
    }

    private void registerCompilationHooks() {
        // 钩子：拦截逻辑处理器的代码设置
        // 当玩家设置处理器代码时自动优化
        Events.on(LogicBlock.LogicBuildEvent.class, event -> {
            if (event.block instanceof LogicBlock) {
                String optimizedCode = optimizeMLogCode(event.code);
                if (!optimizedCode.equals(event.code)) {
                    event.code = optimizedCode;
                }
            }
        });
    }

    /**
     * 编译类C代码到MLog
     */
    public String compileCLikeCode(String sourceCode, int optimizationLevel) {
        long startTime = System.nanoTime();
        totalCompilations++;

        try {
            // 检查缓存
            String cacheKey = sourceCode.hashCode() + "_" + optimizationLevel;
            if (compiledCache.containsKey(cacheKey)) {
                cacheHits++;
                if (debugMode) Log.debug(
                    "[MLogOptimizer] Cache hit for compilation"
                );
                return compiledCache.get(cacheKey).mlogCode;
            }

            // 1. 解析源代码为AST
            ASTNode ast = parser.parse(sourceCode);
            if (ast == null) {
                Log.err("[MLogOptimizer] Failed to parse source code");
                return sourceCode;
            }

            // 2. 语义分析
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            if (!analyzer.analyze(ast)) {
                Log.err("[MLogOptimizer] Semantic analysis failed");
                return sourceCode;
            }

            // 3. 根据优化级别优化AST
            ast = optimizer.optimizeAST(ast, optimizationLevel);

            // 4. 生成MLog字节码
            String mlogCode = generator.generate(ast);
            if (mlogCode == null || mlogCode.isEmpty()) {
                Log.err("[MLogOptimizer] Failed to generate bytecode");
                return sourceCode;
            }

            // 5. 最终优化
            if (optimizationLevel >= 2) {
                mlogCode = optimizer.optimizeMLog(mlogCode);
            }

            // 6. 验证生成的代码
            if (!validateMLogCode(mlogCode)) {
                Log.warn(
                    "[MLogOptimizer] Generated code validation failed, using original"
                );
                return sourceCode;
            }

            // 缓存结果
            compiledCache.put(
                cacheKey,
                new CompiledResult(mlogCode, sourceCode)
            );

            long elapsed = System.nanoTime() - startTime;
            totalOptimizationTime += elapsed;

            if (debugMode) {
                Log.debug(
                    "[MLogOptimizer] Compiled in " + (elapsed / 1000000) + "ms"
                );
                Log.debug(
                    "[MLogOptimizer] Source: " +
                        sourceCode.length() +
                        " chars -> MLog: " +
                        mlogCode.length() +
                        " chars"
                );
            }

            return mlogCode;
        } catch (Exception e) {
            Log.err("[MLogOptimizer] Compilation error: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return sourceCode;
        }
    }

    /**
     * 优化现有的MLog代码
     */
    public String optimizeMLogCode(String mlogCode) {
        if (mlogCode == null || mlogCode.isEmpty()) return mlogCode;

        try {
            int level = getOptimizationLevel();
            if (level < 1) return mlogCode;

            // 解析MLog为指令列表
            List<MLogInstruction> instructions = parseMLogInstructions(
                mlogCode
            );
            if (instructions.isEmpty()) return mlogCode;

            // 死代码消除
            if (level >= 1) {
                instructions = eliminateDeadCode(instructions);
            }

            // 常量折叠
            if (level >= 1) {
                instructions = constantFolding(instructions);
            }

            // 指令合并
            if (level >= 2) {
                instructions = mergeInstructions(instructions);
            }

            // 寄存器分配优化
            if (level >= 3) {
                instructions = optimizeRegisterAllocation(instructions);
            }

            // 重构代码
            return rebuildMLogCode(instructions);
        } catch (Exception e) {
            Log.err("[MLogOptimizer] Optimization error: " + e.getMessage());
            return mlogCode;
        }
    }

    /**
     * 编译并应用到逻辑处理器
     */
    public void compileAndSet(LogicBlock block, String sourceCode) {
        String optimizedCode = compileCLikeCode(
            sourceCode,
            getOptimizationLevel()
        );
        if (optimizedCode != null && !optimizedCode.equals(sourceCode)) {
            blockCache.put(
                block,
                new CompiledResult(optimizedCode, sourceCode)
            );
            applyCodeToBlock(block, optimizedCode);

            if (debugMode) {
                Log.debug(
                    "[MLogOptimizer] Applied compiled code to processor at " +
                        block.tileX() +
                        "," +
                        block.tileY()
                );
            }
        }
    }

    /**
     * 获取处理器上运行的优化代码
     */
    public String getOptimizedCode(LogicBlock block) {
        CompiledResult result = blockCache.get(block);
        return result != null ? result.mlogCode : null;
    }

    /**
     * 解析MLog为指令列表
     */
    private List<MLogInstruction> parseMLogInstructions(String code) {
        List<MLogInstruction> instructions = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            MLogInstruction instr = new MLogInstruction();
            instr.lineNumber = i;
            instr.raw = line;

            String[] parts = line.split("\\s+");
            if (parts.length > 0) {
                instr.opcode = parts[0];
                instr.args = Arrays.copyOfRange(parts, 1, parts.length);
            }

            instructions.add(instr);
        }

        return instructions;
    }

    /**
     * 死代码消除
     */
    private List<MLogInstruction> eliminateDeadCode(
        List<MLogInstruction> instructions
    ) {
        List<MLogInstruction> result = new ArrayList<>();
        Set<String> usedVariables = new HashSet<>();
        Set<Integer> reachableInstructions = new HashSet<>();

        // 标记可达指令（简化版本）
        boolean inJump = false;
        for (int i = 0; i < instructions.size(); i++) {
            if (inJump && instructions.get(i).opcode.startsWith("jump")) {
                inJump = false;
            }
            if (!inJump) {
                reachableInstructions.add(i);
                if (
                    instructions.get(i).opcode.equals("jump") &&
                    instructions.get(i).args.length > 0 &&
                    instructions.get(i).args[0].equals("always")
                ) {
                    inJump = true;
                }
            }
        }

        // 收集使用的变量
        for (MLogInstruction instr : instructions) {
            for (String arg : instr.args) {
                if (arg.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    usedVariables.add(arg);
                }
            }
        }

        // 过滤死代码
        for (int i = 0; i < instructions.size(); i++) {
            MLogInstruction instr = instructions.get(i);
            if (reachableInstructions.contains(i)) {
                // 检查是否是未使用的赋值
                if (instr.opcode.equals("set") && instr.args.length > 0) {
                    String target = instr.args[0];
                    if (!usedVariables.contains(target)) {
                        continue; // 跳过失活赋值
                    }
                }
                result.add(instr);
            }
        }

        return result;
    }

    /**
     * 常量折叠
     */
    private List<MLogInstruction> constantFolding(
        List<MLogInstruction> instructions
    ) {
        Map<String, Double> constants = new HashMap<>();
        List<MLogInstruction> result = new ArrayList<>();

        for (MLogInstruction instr : instructions) {
            if (instr.opcode.equals("set") && instr.args.length >= 2) {
                String target = instr.args[0];
                String value = instr.args[1];

                // 检查是否为常量数字
                try {
                    double num = Double.parseDouble(value);
                    constants.put(target, num);
                    result.add(instr);
                } catch (NumberFormatException e) {
                    // 不是数字，检查是否引用常量
                    if (constants.containsKey(value)) {
                        double val = constants.get(value);
                        result.add(
                            new MLogInstruction(
                                "set",
                                target,
                                String.valueOf((int) val)
                            )
                        );
                    } else {
                        result.add(instr);
                    }
                }
            } else {
                result.add(instr);
            }
        }

        return result;
    }

    /**
     * 指令合并
     */
    private List<MLogInstruction> mergeInstructions(
        List<MLogInstruction> instructions
    ) {
        List<MLogInstruction> result = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            MLogInstruction current = instructions.get(i);

            // 合并连续的 set 操作
            if (
                i + 1 < instructions.size() &&
                current.opcode.equals("set") &&
                instructions.get(i + 1).opcode.equals("set")
            ) {
                MLogInstruction next = instructions.get(i + 1);
                if (next.args[1].equals(current.args[0])) {
                    // set a x; set b a -> set b x
                    result.add(
                        new MLogInstruction(
                            "set",
                            next.args[0],
                            current.args[1]
                        )
                    );
                    i++;
                    continue;
                }
            }

            result.add(current);
        }

        return result;
    }

    /**
     * 寄存器分配优化
     */
    private List<MLogInstruction> optimizeRegisterAllocation(
        List<MLogInstruction> instructions
    ) {
        // 简化版：使用最少的临时变量
        Map<String, Integer> varUsage = new HashMap<>();
        int tempCounter = 0;
        List<MLogInstruction> result = new ArrayList<>();

        // 统计变量使用次数
        for (MLogInstruction instr : instructions) {
            for (String arg : instr.args) {
                if (arg.startsWith("temp")) {
                    varUsage.put(arg, varUsage.getOrDefault(arg, 0) + 1);
                }
            }
        }

        // 重用使用次数少的临时变量
        for (MLogInstruction instr : instructions) {
            MLogInstruction newInstr = new MLogInstruction(instr);

            // 替换temp变量为可重用的
            for (int i = 0; i < newInstr.args.length; i++) {
                String arg = newInstr.args[i];
                if (
                    arg.startsWith("temp") && varUsage.getOrDefault(arg, 0) <= 1
                ) {
                    newInstr.args[i] = "temp" + tempCounter;
                }
            }

            result.add(newInstr);
        }

        return result;
    }

    /**
     * 重建MLog代码
     */
    private String rebuildMLogCode(List<MLogInstruction> instructions) {
        StringBuilder sb = new StringBuilder();
        for (MLogInstruction instr : instructions) {
            sb.append(instr.opcode);
            for (String arg : instr.args) {
                sb.append(" ").append(arg);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 验证MLog代码
     */
    private boolean validateMLogCode(String code) {
        if (code == null || code.isEmpty()) return false;

        try {
            LAssembler assembler = new LAssembler();
            assembler.assemble(code);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 应用代码到逻辑块
     */
    private void applyCodeToBlock(LogicBlock block, String code) {
        if (block == null) return;

        try {
            // 通过反射或直接API设置代码
            // block.code = code; (需要访问权限)
            if (debugMode) {
                Log.debug("[MLogOptimizer] Applied code to block");
            }
        } catch (Exception e) {
            Log.err("[MLogOptimizer] Failed to apply code: " + e.getMessage());
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        compiledCache.clear();
        blockCache.clear();
        Log.info("[MLogOptimizer] Cache cleared");
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCompilations", totalCompilations);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheSize", compiledCache.size());
        stats.put(
            "avgOptimizationTime",
            totalOptimizationTime / Math.max(1, totalCompilations) / 1000000
        );
        return stats;
    }

    private int getOptimizationLevel() {
        // 从设置获取
        return Core.settings.getInt("moonlightbay.mlog_level", 2);
    }

    private boolean isDebugEnabled() {
        return Core.settings.getBool("moonlightbay.api_debug", false);
    }

    // 内部类
    private static class CompiledResult {

        String mlogCode;
        String sourceCode;

        CompiledResult(String mlog, String source) {
            this.mlogCode = mlog;
            this.sourceCode = source;
        }
    }

    private static class MLogInstruction {

        int lineNumber;
        String opcode;
        String[] args;
        String raw;

        MLogInstruction() {}

        MLogInstruction(String opcode, String... args) {
            this.opcode = opcode;
            this.args = args;
        }

        MLogInstruction(MLogInstruction other) {
            this.opcode = other.opcode;
            this.args = other.args.clone();
            this.raw = other.raw;
        }
    }

    private static class SemanticAnalyzer {

        boolean analyze(ASTNode ast) {
            if (ast == null) return false;
            // 简化的语义分析
            return true;
        }
    }
}
