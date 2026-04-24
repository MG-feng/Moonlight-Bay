package com.moonlightgames.moonlightbay.api;

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 统一配置文件接口
 * 自动适配各加载器的配置目录
 * 
 * 使用示例：
 * ConfigHelper.init("my_mod");
 * ConfigHelper.set("someOption", true);
 * boolean value = ConfigHelper.getBoolean("someOption", false);
 */
public final class ConfigHelper {
    
    private ConfigHelper() {}
    
    private static ConfigBackend backend = null;
    private static boolean initialized = false;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // 配置数据
    private static final Map<String, Object> configData = new HashMap<>();
    private static File configFile = null;
    
    /**
     * 初始化配置系统
     * @param modId 模组ID（用于生成配置文件名）
     */
    public static void init(String modId) {
        if (initialized) return;
        
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
            case FORGE:
                backend = new ForgeLikeConfig();
                break;
            case FABRIC:
            case QUILT:
                backend = new FabricLikeConfig();
                break;
            default:
                backend = new FallbackConfig();
        }
        
        configFile = backend.getConfigFile(modId);
        load();
        initialized = true;
    }
    
    /**
     * 设置配置值
     */
    public static void set(String key, Object value) {
        configData.put(key, value);
        save();
    }
    
    /**
     * 获取布尔值
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        Object value = configData.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }
    
    /**
     * 获取整数值
     */
    public static int getInt(String key, int defaultValue) {
        Object value = configData.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }
    
    /**
     * 获取字符串值
     */
    public static String getString(String key, String defaultValue) {
        Object value = configData.get(key);
        if (value instanceof String) return (String) value;
        return defaultValue;
    }
    
    /**
     * 获取双精度浮点数
     */
    public static double getDouble(String key, double defaultValue) {
        Object value = configData.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        return defaultValue;
    }
    
    /**
     * 加载配置文件
     */
    @SuppressWarnings("unchecked")
    private static void load() {
        if (configFile == null || !configFile.exists()) return;
        
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            Map<String, Object> loaded = gson.fromJson(content, Map.class);
            if (loaded != null) {
                configData.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] Failed to load config: " + e.getMessage());
        }
    }
    
    /**
     * 保存配置文件
     */
    private static void save() {
        if (configFile == null) return;
        
        try {
            String json = gson.toJson(configData);
            Files.write(configFile.toPath(), json.getBytes());
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] Failed to save config: " + e.getMessage());
        }
    }
    
    // ========== 配置后端接口 ==========
    
    private interface ConfigBackend {
        File getConfigFile(String modId);
    }
    
    private static class ForgeLikeConfig implements ConfigBackend {
        @Override
        public File getConfigFile(String modId) {
            // Forge/NeoForge 配置文件通常在 ./config/ 目录
            try {
                Class<?> clazz = Class.forName("net.neoforged.fml.loading.FMLPaths");
                var method = clazz.getMethod("CONFIGDIR");
                Path configDir = (Path) method.invoke(null);
                return configDir.resolve(modId + ".json").toFile();
            } catch (Exception e) {
                // 降级方案
                return new File("config/" + modId + ".json");
            }
        }
    }
    
    private static class FabricLikeConfig implements ConfigBackend {
        @Override
        public File getConfigFile(String modId) {
            try {
                Class<?> clazz = Class.forName("net.fabricmc.loader.api.FabricLoader");
                var method = clazz.getMethod("getInstance");
                Object instance = method.invoke(null);
                var getConfigDir = instance.getClass().getMethod("getConfigDir");
                Path configDir = (Path) getConfigDir.invoke(instance);
                return configDir.resolve(modId + ".json").toFile();
            } catch (Exception e) {
                return new File("config/" + modId + ".json");
            }
        }
    }
    
    private static class FallbackConfig implements ConfigBackend {
        @Override
        public File getConfigFile(String modId) {
            return new File("config/" + modId + ".json");
        }
    }
}