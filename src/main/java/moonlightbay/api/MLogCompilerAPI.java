package moonlightbay.api;

import arc.util.Log;
import mindustry.world.blocks.logic.LogicBlock;
import moonlightbay.core.MLogOptimizer;

public class MLogCompilerAPI {

    private MLogOptimizer optimizer;
    private boolean verboseOutput = false;

    MLogCompilerAPI(MLogOptimizer optimizer) {
        this.optimizer = optimizer;
        this.verboseOutput = isVerboseEnabled();
    }

    /**
     * 编译类C代码到MLog
     * @param sourceCode 源代码（类C语法）
     * @return 编译后的MLog代码
     */
    public String compile(String sourceCode) {
        if (optimizer == null) {
            Log.err("[MLogAPI] Optimizer not available");
            return sourceCode;
        }

        String result = optimizer.compileCLikeCode(sourceCode, 2);

        if (verboseOutput && result != null && !result.equals(sourceCode)) {
            Log.info("[MLogAPI] Compilation successful");
            Log.info(
                "[MLogAPI] Original size: " + sourceCode.length() + " chars"
            );
            Log.info("[MLogAPI] Compiled size: " + result.length() + " chars");
            Log.info(
                "[MLogAPI] Reduction: " +
                    (100 - ((result.length() * 100) / sourceCode.length())) +
                    "%"
            );
        }

        return result != null ? result : sourceCode;
    }

    /**
     * 编译类C代码到MLog（指定优化级别）
     * @param sourceCode 源代码
     * @param optimizationLevel 优化级别 1-3
     * @return 编译后的MLog代码
     */
    public String compile(String sourceCode, int optimizationLevel) {
        if (optimizer == null) return sourceCode;

        int level = Math.max(1, Math.min(3, optimizationLevel));
        return optimizer.compileCLikeCode(sourceCode, level);
    }

    /**
     * 编译并应用到逻辑处理器
     * @param processor 逻辑处理器方块
     * @param sourceCode 源代码
     */
    public void compileAndApply(LogicBlock processor, String sourceCode) {
        if (optimizer == null || processor == null) return;

        optimizer.compileAndSet(processor, sourceCode);

        if (verboseOutput) {
            Log.info(
                "[MLogAPI] Applied compiled code to processor at " +
                    processor.tileX() +
                    "," +
                    processor.tileY()
            );
        }
    }

    /**
     * 优化现有的MLog代码
     * @param mlogCode 原始MLog代码
     * @return 优化后的代码
     */
    public String optimize(String mlogCode) {
        if (optimizer == null || mlogCode == null) return mlogCode;
        return optimizer.optimizeMLogCode(mlogCode);
    }

    /**
     * 优化现有的MLog代码（指定级别）
     * @param mlogCode 原始MLog代码
     * @param optimizationLevel 优化级别
     * @return 优化后的代码
     */
    public String optimize(String mlogCode, int optimizationLevel) {
        if (optimizer == null || mlogCode == null) return mlogCode;

        // 临时设置优化级别
        int level = Math.max(1, Math.min(3, optimizationLevel));
        return optimizer.compileCLikeCode(mlogCode, level);
    }

    /**
     * 获取处理器上的已编译代码
     * @param processor 逻辑处理器
     * @return 已编译的MLog代码，如果没有则返回null
     */
    public String getCompiledCode(LogicBlock processor) {
        if (optimizer == null || processor == null) return null;
        return optimizer.getOptimizedCode(processor);
    }

    /**
     * 清除编译缓存
     */
    public void clearCache() {
        if (optimizer == null) return;
        optimizer.clearCache();
        Log.info("[MLogAPI] Compilation cache cleared");
    }

    /**
     * 验证MLog代码是否正确
     * @param code MLog代码
     * @return 是否有效
     */
    public boolean validate(String code) {
        if (code == null || code.isEmpty()) return false;

        try {
            mindustry.logic.LAssembler assembler =
                new mindustry.logic.LAssembler();
            assembler.assemble(code);
            return true;
        } catch (Exception e) {
            if (verboseOutput) {
                Log.err("[MLogAPI] Validation failed: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 获取编译统计信息
     */
    public java.util.Map<String, Object> getStats() {
        if (optimizer == null) return new java.util.HashMap<>();
        return optimizer.getStats();
    }

    /**
     * 示例代码：展示如何用类C语法编写逻辑
     */
    public static String getExampleCode() {
        return (
            "// 示例代码：自动采矿机\n" +
            "int copperCount = sensor(@this, @totalItems);\n" +
            "if (copperCount < 100) {\n" +
            "    control(@this, @enabled, true);\n" +
            "} else {\n" +
            "    control(@this, @enabled, false);\n" +
            "}\n" +
            "print(\"Copper: \", copperCount);\n" +
            "printflush(message1);"
        );
    }

    /**
     * 启用详细输出
     */
    public void setVerboseOutput(boolean enabled) {
        this.verboseOutput = enabled;
    }

    private boolean isVerboseEnabled() {
        return (
            mindustry.Vars.mods != null &&
            Core.settings.getBool("moonlightbay.api_debug", false)
        );
    }
}
