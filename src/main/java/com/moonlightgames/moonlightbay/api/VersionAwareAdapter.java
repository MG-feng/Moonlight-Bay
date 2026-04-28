package com.moonlightgames.moonlightbay.api;

import com.moonlightgames.moonlightbay.platform.LoaderDetector;

/**
 * 版本感知的适配器
 * 用于处理同一加载器不同版本之间的 API 差异
 * 
 * 使用场景：
 * - Forge 1.12.2 和 Forge 1.20.1 注册物品的方式不同
 * - Fabric 0.14.x 和 0.15.x 的 API 变化
 * - NeoForge 早期版本和最新版本的差异
 */
public final class VersionAwareAdapter {
    
    private VersionAwareAdapter() {}
    
    // ========== Forge 版本差异处理 ==========
    
    /**
     * Forge 版本分类
     */
    public enum ForgeVersionGroup {
        LEGACY,     // 1.7.10 - 1.12.2（旧版注册系统）
        MODERN,     // 1.13 - 1.20.1（新版注册系统）
        NEOFORGE    // 1.20.2+（NeoForge）
    }
    
    /**
     * 获取当前 Forge/NeoForge 的版本分组
     * 不同分组需要使用不同的 API 调用方式
     */
    public static ForgeVersionGroup getForgeVersionGroup() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        if (loader == LoaderType.NEOFORGE) {
            return ForgeVersionGroup.NEOFORGE;
        }
        
        if (loader == LoaderType.FORGE) {
            String version = LoaderDetector.getLoaderVersion();
            int[] parsed = LoaderDetector.parseVersion(version);
            
            // 通过 Minecraft 版本判断（更可靠）
            String mcVersion = getMinecraftVersion();
            
            if (mcVersion.startsWith("1.7.") || 
                mcVersion.startsWith("1.8.") ||
                mcVersion.startsWith("1.9.") ||
                mcVersion.startsWith("1.10.") ||
                mcVersion.startsWith("1.11.") ||
                mcVersion.startsWith("1.12.")) {
                return ForgeVersionGroup.LEGACY;
            }
            
            return ForgeVersionGroup.MODERN;
        }
        
        return ForgeVersionGroup.MODERN;
    }
    
    /**
     * 获取 Minecraft 版本
     */
    public static String getMinecraftVersion() {
        try {
            Class<?> clazz = Class.forName("net.minecraft.SharedConstants");
            var versionField = clazz.getDeclaredField("VERSION_STRING");
            versionField.setAccessible(true);
            return (String) versionField.get(null);
        } catch (Exception e) {
            // 备用方案：从 manifest 读取
            return "unknown";
        }
    }
    
    // ========== Fabric 版本差异处理 ==========
    
    /**
     * Fabric 版本分类
     */
    public enum FabricVersionGroup {
        OLD,        // 0.14.x 及更早
        NEW         // 0.15.x 及更新
    }
    
    public static FabricVersionGroup getFabricVersionGroup() {
        String version = LoaderDetector.getLoaderVersion();
        int[] parsed = LoaderDetector.parseVersion(version);
        
        if (parsed.length > 0 && parsed[0] >= 0 && parsed[0] < 15) {
            return FabricVersionGroup.OLD;
        }
        return FabricVersionGroup.NEW;
    }
    
    // ========== 通用 API 选择器 ==========
    
    /**
     * 根据当前加载器和版本，选择正确的实现
     * 
     * 使用示例：
     * String registryMethod = VersionAwareAdapter.select(
     *     "forge_legacy_registry",      // Forge 1.12.2 及更早
     *     "forge_modern_registry",      // Forge 1.13 - 1.20.1
     *     "neoforge_registry",          // NeoForge 1.20.2+
     *     "fabric_old_registry",        // Fabric 0.14.x
     *     "fabric_new_registry"         // Fabric 0.15.x+
     * );
     */
    public static <T> T select(
        T forgeLegacyImpl,
        T forgeModernImpl,
        T neoforgeImpl,
        T fabricOldImpl,
        T fabricNewImpl
    ) {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return neoforgeImpl;
                
            case FORGE:
                ForgeVersionGroup forgeGroup = getForgeVersionGroup();
                if (forgeGroup == ForgeVersionGroup.LEGACY) {
                    return forgeLegacyImpl;
                } else {
                    return forgeModernImpl;
                }
                
            case FABRIC:
                FabricVersionGroup fabricGroup = getFabricVersionGroup();
                if (fabricGroup == FabricVersionGroup.OLD) {
                    return fabricOldImpl;
                } else {
                    return fabricNewImpl;
                }
                
            case QUILT:
                // Quilt 兼容 Fabric 新版 API
                return fabricNewImpl;
                
            default:
                // 默认使用最通用的实现
                return forgeModernImpl;
        }
    }
    
    /**
     * 简化版选择器（只区分 Forge 系和 Fabric 系）
     */
    public static <T> T selectSimple(T forgeLikeImpl, T fabricLikeImpl) {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        if (loader.isForgeLike()) {
            return forgeLikeImpl;
        } else if (loader.isFabricLike()) {
            return fabricLikeImpl;
        }
        return forgeLikeImpl;
    }
}