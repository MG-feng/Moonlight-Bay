package com.moonlightgames.moonlightbay.api;

/**
 * 支持的模组加载器类型枚举
 * 扩展支持：NeoForge、Forge、Fabric、Quilt、LiteLoader、OptiFine、Paper、Spigot、Velocity
 */
public enum LoaderType {
    // === Minecraft 模组加载器 ===
    NEOFORGE,   // NeoForge - Minecraft 1.20.2+，Forge 的继任者
    FORGE,      // Forge - 经典模组加载器，支持 1.2.5 - 1.20.1
    FABRIC,     // Fabric - 轻量级模组加载器，支持 1.14+
    QUILT,      // Quilt - Fabric 的分支，兼容 Fabric 模组，支持 1.19+
    LITELOADER, // LiteLoader - 轻量级客户端模组加载器，支持 1.7.10 - 1.12.2
    
    // === 光影加载器 ===
    IRIS,       // Iris - Fabric/NeoForge 光影加载器
    OCULUS,     // Oculus - Forge 版 Iris
    OPTIFINE,   // OptiFine - 独立优化模组，包含光影功能
    
    // === 服务端/插件加载器 ===
    PAPER,      // Paper - 高性能 Bukkit 服务端
    SPIGOT,     // Spigot - 主流 Bukkit 服务端
    VELOCITY,   // Velocity - 现代代理端
    BUNGEECORD, // BungeeCord - 传统代理端
    SPONGE,     // Sponge - 混合端（模组+插件）
    
    UNKNOWN;    // 未知加载器
    
    /**
     * 判断是否为 Forge 系加载器（Forge 或 NeoForge）
     */
    public boolean isForgeLike() {
        return this == FORGE || this == NEOFORGE;
    }
    
    /**
     * 判断是否为 Fabric 系加载器（Fabric 或 Quilt）
     * Quilt 完全兼容 Fabric 模组
     */
    public boolean isFabricLike() {
        return this == FABRIC || this == QUILT;
    }
    
    /**
     * 判断是否为 Bukkit 系服务端（Paper、Spigot）
     */
    public boolean isBukkitLike() {
        return this == PAPER || this == SPIGOT;
    }
    
    /**
     * 判断是否为代理端（Velocity、BungeeCord）
     */
    public boolean isProxy() {
        return this == VELOCITY || this == BUNGEECORD;
    }
    
    /**
     * 判断是否为光影加载器
     */
    public boolean isShaderLoader() {
        return this == IRIS || this == OCULUS || this == OPTIFINE;
    }
}