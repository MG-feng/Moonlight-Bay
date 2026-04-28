package com.moonlightgames.moonlightbay.api;

import com.moonlightgames.moonlightbay.platform.LoaderDetector;
import java.util.function.BooleanSupplier;

/**
 * 光影加载器适配器
 * 检测当前运行的光影加载器类型，并提供统一的接口
 * 
 * 使用示例：
 * if (ShaderAdapter.isShaderAvailable()) {
 *     ShaderAdapter.applyCustomShader("my_shader.json");
 * }
 */
public final class ShaderAdapter {
    
    private ShaderAdapter() {}
    
    private static ShaderType cachedShaderType = null;
    private static boolean isShaderLoaded = false;
    
    /**
     * 光影类型枚举
     */
    public enum ShaderType {
        IRIS,           // Iris (Fabric/NeoForge)
        OCULUS,         // Oculus (Forge)
        OPTIFINE,       // OptiFine (独立)
        NONE            // 无光影加载器
    }
    
    /**
     * 获取当前光影加载器类型
     */
    public static ShaderType getCurrentShader() {
        if (cachedShaderType != null) return cachedShaderType;
        
        // 按优先级检测（Iris/Oculus 优先于 OptiFine）
        if (isIrisPresent()) {
            cachedShaderType = ShaderType.IRIS;
        } else if (isOculusPresent()) {
            cachedShaderType = ShaderType.OCULUS;
        } else if (isOptiFinePresent()) {
            cachedShaderType = ShaderType.OPTIFINE;
        } else {
            cachedShaderType = ShaderType.NONE;
        }
        
        return cachedShaderType;
    }
    
    /**
     * 检查是否有任何光影加载器
     */
    public static boolean isShaderAvailable() {
        return getCurrentShader() != ShaderType.NONE;
    }
    
    /**
     * 检查是否为 Iris/Oculus（现代光影 API）
     */
    public static boolean isIrisFamily() {
        ShaderType type = getCurrentShader();
        return type == ShaderType.IRIS || type == ShaderType.OCULUS;
    }
    
    /**
     * 应用自定义着色器（如果光影加载器支持）
     */
    public static void applyCustomShader(String shaderPath) {
        ShaderType type = getCurrentShader();
        
        switch (type) {
            case IRIS:
                applyIrisShader(shaderPath);
                break;
            case OCULUS:
                applyOculusShader(shaderPath);
                break;
            case OPTIFINE:
                applyOptiFineShader(shaderPath);
                break;
            default:
                // 无光影加载器，忽略
                break;
        }
    }
    
    /**
     * 获取光影加载器版本
     */
    public static String getShaderVersion() {
        ShaderType type = getCurrentShader();
        switch (type) {
            case IRIS: return getIrisVersion();
            case OCULUS: return getOculusVersion();
            case OPTIFINE: return getOptiFineVersion();
            default: return "none";
        }
    }
    
    // ========== 检测方法 ==========
    
    private static boolean isIrisPresent() {
        try {
            Class.forName("net.irisshaders.iris.Iris");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean isOculusPresent() {
        try {
            Class.forName("net.oculus.Oculus");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean isOptiFinePresent() {
        try {
            Class.forName("optifine.OptiFineClassTransformer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    // ========== 版本获取 ==========
    
    private static String getIrisVersion() {
        try {
            Class<?> clazz = Class.forName("net.irisshaders.iris.Iris");
            var method = clazz.getMethod("getVersion");
            return (String) method.invoke(null);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getOculusVersion() {
        try {
            Class<?> clazz = Class.forName("net.oculus.Oculus");
            var method = clazz.getMethod("getVersion");
            return (String) method.invoke(null);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getOptiFineVersion() {
        try {
            Class<?> clazz = Class.forName("optifine.OptiFineClassTransformer");
            var field = clazz.getDeclaredField("VERSION");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    // ========== 着色器应用（具体实现需要模组提供） ==========
    
    private static void applyIrisShader(String path) {
        // Iris 特定的着色器加载逻辑
        // 需要通过 Iris API 调用
    }
    
    private static void applyOculusShader(String path) {
        // Oculus 与 Iris API 相同
        applyIrisShader(path);
    }
    
    private static void applyOptiFineShader(String path) {
        // OptiFine 的着色器加载方式不同
        // 需要操作 optifine 内部类
    }
}