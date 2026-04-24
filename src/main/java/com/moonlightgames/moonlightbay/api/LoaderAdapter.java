package com.moonlightgames.moonlightbay.api;

/**
 * Moonlight Bay适配器公共API
 * 
 * 其他模组调用的唯一入口。被动模式：仅在被调用时返回加载器信息。
 * 
 * 使用示例：
 * LoaderType loader = LoaderAdapter.getCurrentLoader();
 * if (loader == LoaderType.NEOFORGE) {
 *     // NeoForge特定代码
 * }
 */
public final class LoaderAdapter {
    
    private LoaderAdapter() {}  // 私有构造，防止实例化
    
    private static volatile LoaderType cachedLoader = null;  // 缓存检测结果
    
    /**
     * 获取当前运行的模组加载器类型
     * @return 检测到的加载器类型，失败返回UNKNOWN
     */
    public static LoaderType getCurrentLoader() {
        if (cachedLoader != null) {
            return cachedLoader;
        }
        
        synchronized (LoaderAdapter.class) {
            if (cachedLoader != null) {
                return cachedLoader;
            }
            cachedLoader = detectLoader();
            return cachedLoader;
        }
    }
    
    /**
     * 通过Class.forName检测加载器类型
     * 优先级: NeoForge > Forge > Fabric
     */
    private static LoaderType detectLoader() {
        // 检测NeoForge
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader");
            return LoaderType.NEOFORGE;
        } catch (ClassNotFoundException ignored) {}
        
        // 检测Forge
        try {
            Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            return LoaderType.FORGE;
        } catch (ClassNotFoundException ignored) {}
        
        // 检测Fabric
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return LoaderType.FABRIC;
        } catch (ClassNotFoundException ignored) {}
        
        return LoaderType.UNKNOWN;
    }
    
    /**
     * 清除缓存（主要用于测试）
     */
    public static void clearCache() {
        cachedLoader = null;
    }
    
    /**
     * 检查当前是否为指定加载器
     */
    public static boolean isLoader(LoaderType type) {
        return getCurrentLoader() == type;
    }
    
    /**
     * 检查是否为Forge系（Forge或NeoForge）
     */
    public static boolean isForgeLike() {
        return getCurrentLoader().isForgeLike();
    }
    
    /**
     * 检查是否为Fabric
     */
    public static boolean isFabricLike() {
        return getCurrentLoader().isFabricLike();
    }
}