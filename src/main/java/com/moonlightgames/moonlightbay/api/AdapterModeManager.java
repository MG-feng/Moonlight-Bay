package main.java.com.moonlightgames.moonlightbay.api;

import com.moonlightgames.moonlightbay.gui.CompatibilityQuestionScreen;
import com.moonlightgames.moonlightbay.server.ServerQuestionHandler;
import net.minecraft.client.Minecraft;

import java.util.*;

/**
 * 适配器模式管理器
 * 控制 Moonlight Bay 的适配行为
 * 
 * 使用示例：
 * AdapterModeManager.init();
 * if (AdapterModeManager.shouldAdapt("jei", "FABRIC", "NEOFORGE")) {
 *     // 执行适配代码
 * }
 */
public final class AdapterModeManager {
    
    private AdapterModeManager() {}
    
    /**
     * 适配模式枚举
     */
    public enum AdapterMode {
        /** 自动全部适配（默认）- 即使模组没调用 Moonlight Bay，也自动适配 */
        AUTO_ALL,
        /** 仅白名单 - 只适配白名单里的模组 */
        WHITELIST_ONLY,
        /** 仅黑名单 - 适配除了黑名单外的所有模组 */
        BLACKLIST_ONLY,
        /** 询问 - 进入游戏时弹窗/控制台询问 */
        ASK,
        /** 仅调用 Moonlight 的模组 - 只适配主动调用 API 的模组 */
        CALL_ONLY,
        /** 禁止 - 完全不适配任何模组 */
        DISABLED
    }
    
    private static AdapterMode currentMode = AdapterMode.AUTO_ALL;
    
    // 白名单/黑名单
    private static final Set<String> whitelist = new HashSet<>();
    private static final Set<String> blacklist = new HashSet<>();
    
    // 已记住的选择（用于询问模式）
    private static final Map<String, Boolean> rememberedChoices = new HashMap<>();
    
    // 主动调用 Moonlight Bay 的模组
    private static final Set<String> calledMods = new HashSet<>();
    
    // 是否在服务端
    private static boolean isDedicatedServer = false;
    
    // 是否已初始化
    private static boolean initialized = false;
    
    /**
     * 初始化（从配置读取）
     */
    public static void init() {
        if (initialized) return;
        
        // 读取模式
        String modeName = ConfigHelper.getString("适配器模式.当前模式", "AUTO_ALL");
        try {
            currentMode = AdapterMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            currentMode = AdapterMode.AUTO_ALL;
        }
        
        // 读取白名单
        List<String> whitelistConfig = ConfigHelper.getStringList("适配器模式.白名单模组");
        whitelist.clear();
        whitelist.addAll(whitelistConfig);
        
        // 读取黑名单
        List<String> blacklistConfig = ConfigHelper.getStringList("适配器模式.黑名单模组");
        blacklist.clear();
        blacklist.addAll(blacklistConfig);
        
        // 读取记住的选择
        loadRememberedChoices();
        
        // 检测是否为服务端
        isDedicatedServer = !isClient();
        
        initialized = true;
        
        System.out.println("[Moonlight Bay] 适配模式管理器已初始化");
        System.out.println("[Moonlight Bay] 当前模式: " + currentMode);
        System.out.println("[Moonlight Bay] 白名单: " + whitelist);
        System.out.println("[Moonlight Bay] 黑名单: " + blacklist);
        System.out.println("[Moonlight Bay] 服务端模式: " + isDedicatedServer);
    }
    
    /**
     * 加载记住的选择
     */
    @SuppressWarnings("unchecked")
    private static void loadRememberedChoices() {
        Object obj = ConfigHelper.getRawConfig().get("适配器模式.记住的选择");
        if (obj instanceof Map) {
            Map<String, Boolean> loaded = (Map<String, Boolean>) obj;
            rememberedChoices.clear();
            rememberedChoices.putAll(loaded);
        }
    }
    
    /**
     * 保存记住的选择
     */
    private static void saveRememberedChoices() {
        ConfigHelper.set("适配器模式.记住的选择", new HashMap<>(rememberedChoices));
    }
    
    /**
     * 判断是否应该适配指定模组
     * 
     * @param modId 模组 ID
     * @param sourceLoader 模组原本的加载器
     * @param targetLoader 当前加载器
     * @return true 表示应该适配
     */
    public static boolean shouldAdapt(String modId, String sourceLoader, String targetLoader) {
        if (!initialized) {
            init();
        }
        
        // 如果加载器相同，不需要适配
        if (sourceLoader.equals(targetLoader)) {
            return false;
        }
        
        switch (currentMode) {
            case DISABLED:
                return false;
                
            case CALL_ONLY:
                // 只有主动调用 Moonlight Bay 的模组才适配
                return calledMods.contains(modId);
                
            case WHITELIST_ONLY:
                return whitelist.contains(modId);
                
            case BLACKLIST_ONLY:
                return !blacklist.contains(modId);
                
            case ASK:
                return askUser(modId, sourceLoader, targetLoader);
                
            case AUTO_ALL:
            default:
                return true;
        }
    }
    
    /**
     * 询问用户（客户端弹窗，服务端控制台）
     */
    private static boolean askUser(String modId, String sourceLoader, String targetLoader) {
        // 检查是否已记住选择
        if (rememberedChoices.containsKey(modId)) {
            return rememberedChoices.get(modId);
        }
        
        // 检查是否应该记住选择
        boolean rememberChoice = ConfigHelper.getBoolean("适配器模式.询问模式.记住选择", true);
        int timeout = ConfigHelper.getInt("适配器模式.询问模式.超时秒数", 30);
        
        boolean result;
        
        if (isDedicatedServer) {
            // 服务端：控制台询问
            result = ServerQuestionHandler.askUser(modId, sourceLoader, targetLoader, timeout);
        } else {
            // 客户端：弹窗询问
            result = askClient(modId, sourceLoader, targetLoader, timeout);
        }
        
        // 记住选择
        if (rememberChoice) {
            rememberedChoices.put(modId, result);
            saveRememberedChoices();
        }
        
        return result;
    }
    
    /**
     * 客户端弹窗询问
     */
    private static boolean askClient(String modId, String sourceLoader, String targetLoader, int timeout) {
        // 在 Minecraft 主线程中显示弹窗
        // 由于这是同步调用，需要特殊处理
        try {
            // 获取 Minecraft 客户端实例
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                // 创建并显示弹窗
                CompatibilityQuestionScreen screen = new CompatibilityQuestionScreen(
                    modId, sourceLoader, targetLoader,
                    () -> { /* 适配回调 */ },
                    () -> { /* 禁用回调 */ },
                    () -> { /* 记住回调 */ }
                );
                minecraft.execute(() -> minecraft.setScreen(screen));
                
                // 等待用户响应（简化实现，实际需要异步处理）
                // 这里暂时返回 true
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 显示弹窗失败: " + e.getMessage());
        }
        
        // 默认适配
        return true;
    }
    
    /**
     * 检测是否为客户端环境
     */
    private static boolean isClient() {
        try {
            Class.forName("net.minecraft.client.Minecraft");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 记录模组调用了 Moonlight Bay
     * 
     * @param modId 模组 ID
     */
    public static void registerCall(String modId) {
        calledMods.add(modId);
        System.out.println("[Moonlight Bay] 模组已注册调用: " + modId);
    }
    
    /**
     * 检查模组是否调用了 Moonlight Bay
     */
    public static boolean isCalled(String modId) {
        return calledMods.contains(modId);
    }
    
    /**
     * 获取当前模式
     */
    public static AdapterMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * 设置当前模式
     */
    public static void setCurrentMode(AdapterMode mode) {
        currentMode = mode;
        ConfigHelper.set("适配器模式.当前模式", mode.name());
        System.out.println("[Moonlight Bay] 切换到模式: " + mode);
    }
    
    /**
     * 添加白名单
     */
    public static void addToWhitelist(String modId) {
        whitelist.add(modId);
        ConfigHelper.set("适配器模式.白名单模组", new ArrayList<>(whitelist));
    }
    
    /**
     * 移除白名单
     */
    public static void removeFromWhitelist(String modId) {
        whitelist.remove(modId);
        ConfigHelper.set("适配器模式.白名单模组", new ArrayList<>(whitelist));
    }
    
    /**
     * 添加黑名单
     */
    public static void addToBlacklist(String modId) {
        blacklist.add(modId);
        ConfigHelper.set("适配器模式.黑名单模组", new ArrayList<>(blacklist));
    }
    
    /**
     * 移除黑名单
     */
    public static void removeFromBlacklist(String modId) {
        blacklist.remove(modId);
        ConfigHelper.set("适配器模式.黑名单模组", new ArrayList<>(blacklist));
    }
    
    /**
     * 获取白名单
     */
    public static Set<String> getWhitelist() {
        return new HashSet<>(whitelist);
    }
    
    /**
     * 获取黑名单
     */
    public static Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }
    
    /**
     * 清除记住的选择
     */
    public static void clearRememberedChoices() {
        rememberedChoices.clear();
        saveRememberedChoices();
    }
}