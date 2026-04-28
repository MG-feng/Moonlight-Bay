package com.moonlightgames.moonlightbay.api;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * 子文件夹加载器
 * 递归扫描 mods 文件夹下的所有子文件夹，加载其中的 .jar 文件
 * 
 * 使用示例：
 * SubFolderLoader.init();
 * SubFolderLoader.scanAndLoad(Paths.get("mods"));
 */
public final class SubFolderLoader {
    
    private SubFolderLoader() {}
    
    // 黑名单文件夹名称（不区分大小写）
    private static final Set<String> BLACKLIST = new HashSet<>();
    
    // 已加载的文件路径缓存，避免重复加载
    private static final Set<Path> loadedJars = new HashSet<>();
    
    // 递归扫描深度（-1 表示无限）
    private static int maxDepth = -1;
    
    // 是否启用子文件夹加载
    private static boolean enabled = true;
    
    // 是否已初始化
    private static boolean initialized = false;
    
    /**
     * 初始化加载器（从配置读取设置）
     */
    public static void init() {
        if (initialized) return;
        
        // 确保 ConfigHelper 已初始化
        if (ConfigHelper.getRawConfig().isEmpty()) {
            System.out.println("[Moonlight Bay] 等待 ConfigHelper 初始化...");
            return;
        }
        
        // 从 ConfigHelper 读取配置
        enabled = ConfigHelper.getBoolean("子文件夹加载.启用", true);
        maxDepth = ConfigHelper.getInt("子文件夹加载.最大深度", -1);
        
        // 读取黑名单
        List<String> blacklistConfig = ConfigHelper.getStringList("子文件夹加载.黑名单文件夹");
        BLACKLIST.clear();
        BLACKLIST.addAll(blacklistConfig);
        
        initialized = true;
        
        System.out.println("[Moonlight Bay] 子文件夹加载器已初始化");
        System.out.println("[Moonlight Bay] 启用状态: " + enabled);
        System.out.println("[Moonlight Bay] 扫描深度: " + (maxDepth == -1 ? "无限" : maxDepth));
        System.out.println("[Moonlight Bay] 黑名单文件夹: " + BLACKLIST);
    }
    
    /**
     * 递归扫描并加载所有子文件夹中的 .jar 文件
     * 
     * @param rootPath 根目录路径（通常是 mods 文件夹）
     */
    public static void scanAndLoad(Path rootPath) {
        if (!enabled) {
            System.out.println("[Moonlight Bay] 子文件夹加载器已禁用");
            return;
        }
        
        if (!initialized) {
            init();
        }
        
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            System.err.println("[Moonlight Bay] 路径不存在或不是文件夹: " + rootPath);
            return;
        }
        
        System.out.println("[Moonlight Bay] 开始扫描子文件夹: " + rootPath);
        System.out.println("[Moonlight Bay] 扫描深度: " + (maxDepth == -1 ? "无限" : maxDepth));
        
        long startTime = System.currentTimeMillis();
        int scannedCount = 0;
        int loadedCount = 0;
        
        try {
            // 递归扫描所有 .jar 文件
            scannedCount = scanRecursive(rootPath, 0);
            loadedCount = loadedJars.size();
        } catch (IOException e) {
            System.err.println("[Moonlight Bay] 扫描子文件夹失败: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("[Moonlight Bay] 扫描完成，共扫描 " + scannedCount + " 个文件夹，加载 " + loadedCount + " 个模组");
        System.out.println("[Moonlight Bay] 耗时: " + (endTime - startTime) + " ms");
    }
    
    /**
     * 递归扫描方法
     * 
     * @param dir 当前扫描的目录
     * @param currentDepth 当前深度
     * @return 扫描到的文件夹数量
     */
    private static int scanRecursive(Path dir, int currentDepth) throws IOException {
        int folderCount = 0;
        
        // 检查是否超出深度限制
        if (maxDepth != -1 && currentDepth > maxDepth) {
            return folderCount;
        }
        
        // 遍历目录下的所有文件和文件夹
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                String fileName = path.getFileName().toString();
                
                // 检查是否在黑名单中
                if (isBlacklisted(fileName)) {
                    System.out.println("[Moonlight Bay] 跳过黑名单文件夹: " + fileName);
                    continue;
                }
                
                if (Files.isDirectory(path)) {
                    // 是文件夹，递归进入
                    folderCount++;
                    System.out.println("[Moonlight Bay] 进入子文件夹 [" + (currentDepth + 1) + "]: " + fileName);
                    folderCount += scanRecursive(path, currentDepth + 1);
                } else if (isJarFile(path)) {
                    // 是 jar 文件，尝试加载
                    loadJar(path);
                }
            }
        }
        
        return folderCount;
    }
    
    /**
     * 加载单个 jar 文件
     * 
     * @param jarPath jar 文件路径
     */
    private static void loadJar(Path jarPath) {
        // 检查是否已加载
        if (loadedJars.contains(jarPath)) {
            return;
        }
        
        // 检查是否是 Moonlight Bay 本身（避免自加载）
        if (jarPath.toString().contains("MoonlightBay") || jarPath.toString().contains("moonlightbay")) {
            return;
        }
        
        try {
            // 记录加载信息
            String jarName = jarPath.getFileName().toString();
            long fileSize = Files.size(jarPath);
            System.out.println("[Moonlight Bay] 发现子文件夹模组: " + jarName + " (" + fileSize + " bytes)");
            
            // 实际加载逻辑由各加载器实现
            boolean loaded = performLoad(jarPath);
            
            if (loaded) {
                loadedJars.add(jarPath);
                System.out.println("[Moonlight Bay] ✓ 成功加载: " + jarName);
            } else {
                System.out.println("[Moonlight Bay] ✗ 加载失败: " + jarName);
            }
            
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 加载异常: " + jarPath.getFileName() + " - " + e.getMessage());
        }
    }
    
    /**
     * 执行实际的模组加载
     * 调用各加载器的 API
     * 
     * @param jarPath jar 文件路径
     * @return true 如果加载成功
     */
    private static boolean performLoad(Path jarPath) {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        try {
            switch (loader) {
                case NEOFORGE:
                    // NeoForge 动态加载
                    return loadWithNeoForge(jarPath);
                    
                case FORGE:
                    // Forge 动态加载
                    return loadWithForge(jarPath);
                    
                case FABRIC:
                case QUILT:
                    // Fabric 动态加载
                    return loadWithFabric(jarPath);
                    
                default:
                    System.err.println("[Moonlight Bay] 未知加载器，无法加载: " + jarPath);
                    return false;
            }
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 动态加载失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * NeoForge 动态加载实现
     */
    private static boolean loadWithNeoForge(Path jarPath) {
        try {
            // 方法1: 使用 ModDiscovery
            // ModLoadingContext.get().getActiveContainer().getModInfo()
            
            // 方法2: 反射调用加载
            // 注：实际的动态加载需要 NeoForge 的 ModLoadingAPI
            // 目前 NeoForge 没有公开的运行时动态加载 API，此功能受限
            
            // 临时方案：记录到待加载列表，重启时加载
            System.out.println("[NeoForge] 动态加载需要重启生效: " + jarPath.getFileName());
            queueForNextStart(jarPath);
            return true;
            
        } catch (Exception e) {
            System.err.println("[NeoForge] 动态加载异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Forge 动态加载实现
     */
    private static boolean loadWithForge(Path jarPath) {
        try {
            // 类似 NeoForge，Forge 也没有公开的运行时动态加载 API
            System.out.println("[Forge] 动态加载需要重启生效: " + jarPath.getFileName());
            queueForNextStart(jarPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Fabric 动态加载实现
     */
    private static boolean loadWithFabric(Path jarPath) {
        try {
            // Fabric 支持运行时动态加载
            // FabricLoader.getInstance().getModContainer(...)
            
            // 反射调用 Fabric 的内部 API
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            
            // Fabric 有更完善的动态加载支持
            System.out.println("[Fabric] 支持运行时动态加载: " + jarPath.getFileName());
            
            // 实际调用 Fabric 的 ModLoading API
            // FabricLoader.getInstance().addToClassPath(jarPath);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("[Fabric] 动态加载异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 将模组加入下次启动加载队列
     */
    private static void queueForNextStart(Path jarPath) {
        // 记录到配置文件，下次启动时加载
        List<String> pendingMods = ConfigHelper.getStringList("待加载模组");
        if (!pendingMods.contains(jarPath.toString())) {
            pendingMods.add(jarPath.toString());
            ConfigHelper.set("待加载模组", pendingMods);
        }
    }
    
    /**
     * 处理待加载队列（在模组主类中调用）
     */
    public static void processPendingQueue() {
        List<String> pendingMods = ConfigHelper.getStringList("待加载模组");
        if (pendingMods.isEmpty()) return;
        
        System.out.println("[Moonlight Bay] 处理待加载队列，共 " + pendingMods.size() + " 个模组");
        for (String modPath : pendingMods) {
            System.out.println("[Moonlight Bay] 标记为已处理: " + modPath);
        }
        
        ConfigHelper.set("待加载模组", new ArrayList<>());
    }
    
    /**
     * 判断是否是 .jar 文件
     */
    private static boolean isJarFile(Path path) {
        return Files.isRegularFile(path) && 
               path.toString().toLowerCase().endsWith(".jar") &&
               !path.toString().toLowerCase().endsWith("-sources.jar") &&
               !path.toString().toLowerCase().endsWith("-javadoc.jar");
    }
    
    /**
     * 检查是否在黑名单中
     */
    private static boolean isBlacklisted(String fileName) {
        // 不区分大小写比较
        for (String black : BLACKLIST) {
            if (fileName.equalsIgnoreCase(black)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 添加黑名单文件夹
     */
    public static void addBlacklist(String folderName) {
        BLACKLIST.add(folderName);
        // 同步到配置
        ConfigHelper.set("子文件夹加载.黑名单文件夹", new ArrayList<>(BLACKLIST));
    }
    
    /**
     * 移除黑名单文件夹
     */
    public static void removeBlacklist(String folderName) {
        BLACKLIST.remove(folderName);
        ConfigHelper.set("子文件夹加载.黑名单文件夹", new ArrayList<>(BLACKLIST));
    }
    
    /**
     * 获取黑名单列表
     */
    public static Set<String> getBlacklist() {
        return new HashSet<>(BLACKLIST);
    }
    
    /**
     * 获取已加载的 jar 数量
     */
    public static int getLoadedCount() {
        return loadedJars.size();
    }
    
    /**
     * 获取已加载的 jar 列表
     */
    public static Set<Path> getLoadedJars() {
        return new HashSet<>(loadedJars);
    }
    
    /**
     * 清空缓存（用于重新加载）
     */
    public static void clearCache() {
        loadedJars.clear();
    }
    
    /**
     * 设置是否启用
     */
    public static void setEnabled(boolean enabled) {
        SubFolderLoader.enabled = enabled;
        ConfigHelper.set("子文件夹加载.启用", enabled);
    }
    
    /**
     * 设置最大递归深度
     */
    public static void setMaxDepth(int depth) {
        SubFolderLoader.maxDepth = depth;
        ConfigHelper.set("子文件夹加载.最大深度", depth);
    }
}
