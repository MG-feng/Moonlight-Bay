package main.java.com.moonlightgames.moonlightbay.server;

import com.moonlightgames.moonlightbay.api.ConfigHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 服务端询问处理器
 * 在控制台输出请求，等待用户输入
 * 
 * 使用示例：
 * boolean result = ServerQuestionHandler.askUser("暮色森林", "FABRIC", "NEOFORGE");
 */
public final class ServerQuestionHandler {
    
    private ServerQuestionHandler() {}
    
    // 默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT = 30;
    
    // 是否自动适配（超时后的默认行为）
    private static final boolean AUTO_ADAPT_ON_TIMEOUT = true;
    
    // 记住的选择缓存
    private static final java.util.Map<String, Boolean> rememberedChoices = new java.util.HashMap<>();
    
    /**
     * 询问用户是否适配模组（使用默认超时）
     * 
     * @param modName 模组名称
     * @param sourceLoader 源加载器
     * @param targetLoader 目标加载器
     * @return true 表示适配，false 表示不适配
     */
    public static boolean askUser(String modName, String sourceLoader, String targetLoader) {
        return askUser(modName, sourceLoader, targetLoader, DEFAULT_TIMEOUT);
    }
    
    /**
     * 询问用户是否适配模组（带超时）
     * 
     * @param modName 模组名称
     * @param sourceLoader 源加载器
     * @param targetLoader 目标加载器
     * @param timeoutSeconds 超时时间（秒）
     * @return true 表示适配，false 表示不适配
     */
    public static boolean askUser(String modName, String sourceLoader, String targetLoader, int timeoutSeconds) {
        
        // 检查是否已记住选择
        Boolean remembered = getRememberedChoice(modName);
        if (remembered != null) {
            return remembered;
        }
        
        // 输出询问信息到控制台
        System.out.println("");
        System.out.println("========================================");
        System.out.println("[Moonlight Bay] 兼容性询问");
        System.out.println("========================================");
        System.out.println("模组: " + modName);
        System.out.println("开发环境: " + sourceLoader);
        System.out.println("当前环境: " + targetLoader);
        System.out.println("");
        System.out.println("此模组可能不完全兼容当前加载器。");
        System.out.println("是否尝试适配运行？");
        System.out.println("");
        System.out.println("输入 'y' 适配，'n' 禁用，'r' 记住选择(始终适配)");
        System.out.println("========================================");
        System.out.print("等待输入 (" + timeoutSeconds + " 秒超时): ");
        
        // 异步等待用户输入
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();
                if (input == null) return AUTO_ADAPT_ON_TIMEOUT;
                
                input = input.trim().toLowerCase();
                
                if (input.equals("y") || input.equals("yes")) {
                    return true;
                }
                if (input.equals("n") || input.equals("no")) {
                    return false;
                }
                if (input.equals("r") || input.equals("remember")) {
                    // 记住选择，写入配置
                    rememberChoice(modName, true);
                    return true;
                }
                return AUTO_ADAPT_ON_TIMEOUT;
                
            } catch (Exception e) {
                System.err.println("[Moonlight Bay] 读取输入失败: " + e.getMessage());
                return AUTO_ADAPT_ON_TIMEOUT;
            }
        });
        
        try {
            boolean result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            System.out.println("");
            System.out.println("[Moonlight Bay] 选择结果: " + (result ? "适配" : "禁用"));
            return result;
        } catch (java.util.concurrent.TimeoutException e) {
            System.out.println("");
            System.out.println("[Moonlight Bay] 超时未响应 (" + timeoutSeconds + " 秒)，自动适配");
            return AUTO_ADAPT_ON_TIMEOUT;
        } catch (Exception e) {
            System.out.println("");
            System.err.println("[Moonlight Bay] 询问异常: " + e.getMessage());
            return AUTO_ADAPT_ON_TIMEOUT;
        }
    }
    
    /**
     * 记住用户的选择
     */
    private static void rememberChoice(String modName, boolean adapt) {
        // 缓存
        rememberedChoices.put(modName, adapt);
        
        // 写入配置（使用 Moonlight Bay 的配置）
        try {
            // 获取已记住的选择列表
            java.util.List<String> adaptList = ConfigHelper.getStringList("询问模式.已适配模组");
            java.util.List<String> denyList = ConfigHelper.getStringList("询问模式.已禁用模组");
            
            if (adapt) {
                if (!adaptList.contains(modName)) {
                    adaptList.add(modName);
                }
                denyList.remove(modName);
            } else {
                if (!denyList.contains(modName)) {
                    denyList.add(modName);
                }
                adaptList.remove(modName);
            }
            
            ConfigHelper.set("询问模式.已适配模组", adaptList);
            ConfigHelper.set("询问模式.已禁用模组", denyList);
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 保存记住选择失败: " + e.getMessage());
        }
        
        System.out.println("[Moonlight Bay] 已记住选择: " + modName + " -> " + (adapt ? "适配" : "禁用"));
    }
    
    /**
     * 检查是否已记住选择
     */
    @SuppressWarnings("unchecked")
    public static Boolean getRememberedChoice(String modName) {
        // 先检查缓存
        if (rememberedChoices.containsKey(modName)) {
            return rememberedChoices.get(modName);
        }
        
        // 从配置读取
        try {
            java.util.List<String> adaptList = ConfigHelper.getStringList("询问模式.已适配模组");
            if (adaptList.contains(modName)) {
                rememberedChoices.put(modName, true);
                return true;
            }
            
            java.util.List<String> denyList = ConfigHelper.getStringList("询问模式.已禁用模组");
            if (denyList.contains(modName)) {
                rememberedChoices.put(modName, false);
                return false;
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return null;  // 未记住
    }
    
    /**
     * 清除记住的选择
     */
    public static void clearRememberedChoices() {
        rememberedChoices.clear();
        ConfigHelper.set("询问模式.已适配模组", new java.util.ArrayList<>());
        ConfigHelper.set("询问模式.已禁用模组", new java.util.ArrayList<>());
        System.out.println("[Moonlight Bay] 已清除所有记住的选择");
    }
}