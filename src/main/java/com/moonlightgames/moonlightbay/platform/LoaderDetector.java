package com.moonlightgames.moonlightbay.platform;

import com.moonlightgames.moonlightbay.api.LoaderAdapter;
import com.moonlightgames.moonlightbay.api.LoaderType;

/**
 * 平台检测器 - 获取加载器版本、详细信息
 * 支持识别不同版本加载器的差异
 */
public final class LoaderDetector {
    
    private LoaderDetector() {}
    
    // ========== 基础版本获取 ==========
    
    public static String getLoaderVersion() {
        LoaderType type = LoaderAdapter.getCurrentLoader();
        switch (type) {
            case NEOFORGE: return getNeoForgeVersion();
            case FORGE:    return getForgeVersion();
            case FABRIC:   return getFabricVersion();
            case QUILT:    return getQuiltVersion();
            case PAPER:    return getPaperVersion();
            case SPIGOT:   return getSpigotVersion();
            case VELOCITY: return getVelocityVersion();
            case IRIS:     return getIrisVersion();
            case OCULUS:   return getOculusVersion();
            default:       return "unknown";
        }
    }
    
    // ========== 版本号解析（用于判断 API 差异） ==========
    
    /**
     * 解析版本号为整数数组，便于比较
     * 例如 "47.1.0" -> [47, 1, 0]
     */
    public static int[] parseVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].split("-")[0]);
            }
            return result;
        } catch (Exception e) {
            return new int[]{0};
        }
    }
    
    /**
     * 比较两个版本号
     * @return 正数如果 v1 > v2，负数如果 v1 < v2，0 如果相等
     */
    public static int compareVersions(int[] v1, int[] v2) {
        int minLen = Math.min(v1.length, v2.length);
        for (int i = 0; i < minLen; i++) {
            if (v1[i] != v2[i]) return v1[i] - v2[i];
        }
        return v1.length - v2.length;
    }
    
    // ========== 各加载器版本获取实现 ==========
    
    private static String getNeoForgeVersion() {
        try {
            Class<?> clazz = Class.forName("net.neoforged.fml.loading.FMLLoader");
            var versionInfoField = clazz.getDeclaredField("versionInfo");
            versionInfoField.setAccessible(true);
            Object versionInfo = versionInfoField.get(null);
            var versionField = versionInfo.getClass().getDeclaredField("neoForgeVersion");
            versionField.setAccessible(true);
            return (String) versionField.get(versionInfo);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getForgeVersion() {
        // 注意：Forge 在不同版本中获取版本号的方式不同
        // 1.7.10 - 1.12.2: FMLCommonHandler.instance().getBrand()
        // 1.13+ - 1.20.1: FMLLoader.version
        try {
            // 尝试新版 Forge (1.13+)
            Class<?> clazz = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            var versionField = clazz.getDeclaredField("version");
            versionField.setAccessible(true);
            return (String) versionField.get(null);
        } catch (Exception e1) {
            try {
                // 尝试旧版 Forge (1.7.10 - 1.12.2)
                Class<?> clazz = Class.forName("net.minecraftforge.fml.common.FMLCommonHandler");
                var instanceMethod = clazz.getMethod("instance");
                Object instance = instanceMethod.invoke(null);
                var getBrandMethod = clazz.getMethod("getBrand");
                String brand = (String) getBrandMethod.invoke(instance);
                // brand 格式: "forge,1.12.2-14.23.5.2859"
                String[] parts = brand.split(",");
                if (parts.length > 1) return parts[1];
                return brand;
            } catch (Exception e2) {
                return "unknown";
            }
        }
    }
    
    private static String getFabricVersion() {
        try {
            Class<?> clazz = Class.forName("net.fabricmc.loader.api.FabricLoader");
            var getInstanceMethod = clazz.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            var getVersionMethod = clazz.getMethod("getModLoaderVersion");
            Object version = getVersionMethod.invoke(instance);
            return version.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getQuiltVersion() {
        try {
            Class<?> clazz = Class.forName("org.quiltmc.loader.api.QuiltLoader");
            var getInstanceMethod = clazz.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            var getVersionMethod = clazz.getMethod("getModLoaderVersion");
            Object version = getVersionMethod.invoke(instance);
            return version.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getPaperVersion() {
        try {
            Class<?> clazz = Class.forName("io.papermc.paper.ServerBuildInfo");
            var buildInfoField = clazz.getDeclaredField("buildInfo");
            buildInfoField.setAccessible(true);
            Object buildInfo = buildInfoField.get(null);
            var getVersionMethod = buildInfo.getClass().getMethod("getVersion");
            return (String) getVersionMethod.invoke(buildInfo);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getSpigotVersion() {
        try {
            // Spigot 版本信息在 Bukkit 类中
            Class<?> clazz = Class.forName("org.bukkit.Bukkit");
            var getVersionMethod = clazz.getMethod("getVersion");
            String version = (String) getVersionMethod.invoke(null);
            // 格式: "git-Spigot-1.20.4-1234" -> 提取 "1.20.4"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+(\\.\\d+)?)");
            var matcher = pattern.matcher(version);
            if (matcher.find()) return matcher.group(1);
            return version;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getVelocityVersion() {
        try {
            Class<?> clazz = Class.forName("com.velocitypowered.api.proxy.ProxyServer");
            // Velocity 版本需要通过插件获取，这里简化处理
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getIrisVersion() {
        try {
            Class<?> clazz = Class.forName("net.irisshaders.iris.Iris");
            var getVersionMethod = clazz.getMethod("getVersion");
            return (String) getVersionMethod.invoke(null);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static String getOculusVersion() {
        try {
            Class<?> clazz = Class.forName("net.oculus.Oculus");
            var getVersionMethod = clazz.getMethod("getVersion");
            return (String) getVersionMethod.invoke(null);
        } catch (Exception e) {
            return "unknown";
        }
    }
}