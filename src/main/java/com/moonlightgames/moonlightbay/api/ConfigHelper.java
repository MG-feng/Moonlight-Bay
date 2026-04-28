package com.moonlightgames.moonlightbay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

/**
 * 统一配置文件接口
 * 自动适配各加载器的配置目录
 * 
 * 使用示例：
 * ConfigHelper.init("moonlightbay");
 * ConfigHelper.set("someOption", true);
 * boolean value = ConfigHelper.getBoolean("someOption", false);
 */
public final class ConfigHelper {
    
    private ConfigHelper() {}
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Path configPath = null;
    private static Map<String, Object> configData = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * 初始化配置系统
     * @param modId 模组ID（用于生成配置文件名）
     */
    public static void init(String modId) {
        if (initialized) return;
        
        configPath = getConfigDir().resolve(modId + ".json");
        load();
        initialized = true;
        
        System.out.println("[Moonlight Bay] 配置文件路径: " + configPath);
    }
    
    /**
     * 获取各加载器的配置目录
     */
    private static Path getConfigDir() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
            case FORGE:
                // Forge/NeoForge 配置目录
                try {
                    Class<?> clazz = Class.forName("net.neoforged.fml.loading.FMLPaths");
                    var method = clazz.getMethod("CONFIGDIR");
                    return (Path) method.invoke(null);
                } catch (Exception e) {
                    return Paths.get("config");
                }
                
            case FABRIC:
            case QUILT:
                // Fabric 配置目录
                try {
                    Class<?> clazz = Class.forName("net.fabricmc.loader.api.FabricLoader");
                    var method = clazz.getMethod("getInstance");
                    Object instance = method.invoke(null);
                    var getConfigDir = instance.getClass().getMethod("getConfigDir");
                    return (Path) getConfigDir.invoke(instance);
                } catch (Exception e) {
                    return Paths.get("config");
                }
                
            default:
                return Paths.get("config");
        }
    }
    
    /**
     * 加载配置文件
     */
    @SuppressWarnings("unchecked")
    private static void load() {
        if (configPath == null || !Files.exists(configPath)) {
            // 配置文件不存在，创建默认配置
            createDefaultConfig();
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                configData = loaded;
            }
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 加载配置文件失败: " + e.getMessage());
            createDefaultConfig();
        }
    }
    
    /**
     * 保存配置文件
     */
    private static void save() {
        if (configPath == null) return;
        
        try {
            // 确保目录存在
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(configData, writer);
            }
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 保存配置文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private static void createDefaultConfig() {
        configData = getDefaultConfig();
        save();
        System.out.println("[Moonlight Bay] 已创建默认配置文件: " + configPath);
    }
    
    /**
     * 获取默认配置内容
     */
    private static Map<String, Object> getDefaultConfig() {
        Map<String, Object> defaults = new HashMap<>();
        
        // 子文件夹加载配置
        Map<String, Object> subfolderConfig = new HashMap<>();
        subfolderConfig.put("启用", true);
        subfolderConfig.put("最大深度", -1);
        subfolderConfig.put("黑名单文件夹", new ArrayList<>());
        defaults.put("子文件夹加载", subfolderConfig);
        
        // 适配器模式配置
        Map<String, Object> adapterConfig = new HashMap<>();
        adapterConfig.put("当前模式", "AUTO_ALL");
        adapterConfig.put("白名单模组", new ArrayList<>());
        adapterConfig.put("黑名单模组", new ArrayList<>());
        
        Map<String, Object> askConfig = new HashMap<>();
        askConfig.put("记住选择", true);
        askConfig.put("超时秒数", 30);
        adapterConfig.put("询问模式", askConfig);
        defaults.put("适配器模式", adapterConfig);
        
        return defaults;
    }
    
    // ========== 配置读取方法 ==========
    
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
        Object value = getNestedValue(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }
    
    /**
     * 获取整数值
     */
    public static int getInt(String key, int defaultValue) {
        Object value = getNestedValue(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取字符串值
     */
    public static String getString(String key, String defaultValue) {
        Object value = getNestedValue(key);
        if (value instanceof String) return (String) value;
        return defaultValue;
    }
    
    /**
     * 获取字符串列表
     */
    @SuppressWarnings("unchecked")
    public static List<String> getStringList(String key) {
        Object value = getNestedValue(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object obj : (List<?>) value) {
                if (obj instanceof String) {
                    result.add((String) obj);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    /**
     * 获取嵌套配置值（支持点号分隔，如 "适配器模式.当前模式"）
     */
    private static Object getNestedValue(String key) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = configData;
        
        for (int i = 0; i < parts.length - 1; i++) {
            Object obj = current.get(parts[i]);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                return null;
            }
        }
        
        return current.get(parts[parts.length - 1]);
    }
    
    /**
     * 获取原始配置数据（用于直接操作）
     */
    public static Map<String, Object> getRawConfig() {
        return configData;
    }
    
    /**
     * 重新加载配置文件
     */
    public static void reload() {
        load();
    }
}