package moonlightbay.api;

import arc.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import moonlightbay.core.*;

public class MoonlightAPI {

    private static MoonlightAPI instance;

    // 核心组件
    private MLogOptimizer mlogOptimizer;
    private TextureStreamer textureStreamer;
    private AdaptiveScheduler scheduler;
    private ModLoaderBooster modLoaderBooster;

    // API 实例
    private MLogCompilerAPI mlogCompilerAPI;
    private TextureAPI textureAPI;
    private SchedulerAPI schedulerAPI;

    // 扩展系统
    private Map<String, Object> registeredExtensions =
        new ConcurrentHashMap<>();
    private Map<String, APIEventListener> eventListeners =
        new ConcurrentHashMap<>();

    // 配置
    private boolean debugMode = false;
    private boolean apiEnabled = true;

    private MoonlightAPI() {}

    public static MoonlightAPI getInstance() {
        if (instance == null) {
            instance = new MoonlightAPI();
        }
        return instance;
    }

    public static void registerMLogAPI(MLogOptimizer optimizer) {
        getInstance().mlogOptimizer = optimizer;
        getInstance().mlogCompilerAPI = new MLogCompilerAPI(optimizer);
        logDebug("MLog API registered");
        getInstance().fireEvent("mlog_api_registered", optimizer);
    }

    public static void registerTextureAPI(TextureStreamer streamer) {
        getInstance().textureStreamer = streamer;
        getInstance().textureAPI = new TextureAPI(streamer);
        logDebug("Texture API registered");
        getInstance().fireEvent("texture_api_registered", streamer);
    }

    public static void registerSchedulerAPI(AdaptiveScheduler scheduler) {
        getInstance().scheduler = scheduler;
        getInstance().schedulerAPI = new SchedulerAPI(scheduler);
        logDebug("Scheduler API registered");
        getInstance().fireEvent("scheduler_api_registered", scheduler);
    }

    public static void registerModLoaderAPI(ModLoaderBooster booster) {
        getInstance().modLoaderBooster = booster;
        logDebug("Mod Loader API registered");
        getInstance().fireEvent("modloader_api_registered", booster);
    }

    private static void logDebug(String message) {
        if (getInstance().debugMode) {
            Log.info("[Moonlight API] " + message);
        }
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        Log.info("[Moonlight API] Debug mode: " + (enabled ? "ON" : "OFF"));
    }

    public void setAPIEnabled(boolean enabled) {
        this.apiEnabled = enabled;
        Log.info("[Moonlight API] API enabled: " + enabled);
    }

    public boolean isAPIEnabled() {
        return apiEnabled;
    }

    // API 获取方法
    public static MLogCompilerAPI getMLogCompiler() {
        if (!getInstance().apiEnabled) return null;
        return getInstance().mlogCompilerAPI;
    }

    public static TextureAPI getTextureAPI() {
        if (!getInstance().apiEnabled) return null;
        return getInstance().textureAPI;
    }

    public static SchedulerAPI getSchedulerAPI() {
        if (!getInstance().apiEnabled) return null;
        return getInstance().schedulerAPI;
    }

    // 扩展注册系统
    public void registerExtension(String name, Object extension) {
        if (registeredExtensions.containsKey(name)) {
            Log.warn("[Moonlight API] Extension already registered: " + name);
        }
        registeredExtensions.put(name, extension);
        logDebug("Extension registered: " + name);
        fireEvent("extension_registered", name);
    }

    public void unregisterExtension(String name) {
        if (registeredExtensions.remove(name) != null) {
            logDebug("Extension unregistered: " + name);
            fireEvent("extension_unregistered", name);
        }
    }

    public Object getExtension(String name) {
        return registeredExtensions.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtension(String name, Class<T> type) {
        Object ext = registeredExtensions.get(name);
        if (type.isInstance(ext)) {
            return (T) ext;
        }
        return null;
    }

    public Set<String> getRegisteredExtensions() {
        return Collections.unmodifiableSet(registeredExtensions.keySet());
    }

    public boolean hasExtension(String name) {
        return registeredExtensions.containsKey(name);
    }

    // 事件系统
    public interface APIEventListener {
        void onEvent(String event, Object data);
    }

    public void addEventListener(String event, APIEventListener listener) {
        eventListeners.put(event, listener);
        logDebug("Event listener added: " + event);
    }

    public void removeEventListener(String event) {
        eventListeners.remove(event);
    }

    private void fireEvent(String event, Object data) {
        APIEventListener listener = eventListeners.get(event);
        if (listener != null) {
            try {
                listener.onEvent(event, data);
            } catch (Exception e) {
                Log.err(
                    "[Moonlight API] Event handler error: " + e.getMessage()
                );
            }
        }

        // 通配符事件
        APIEventListener wildcardListener = eventListeners.get("*");
        if (wildcardListener != null) {
            try {
                wildcardListener.onEvent(event, data);
            } catch (Exception e) {
                Log.err(
                    "[Moonlight API] Wildcard event handler error: " +
                        e.getMessage()
                );
            }
        }
    }

    // 工具方法
    public static boolean isModLoaded(String modName) {
        return mindustry.Vars.mods.getMod(modName) != null;
    }

    public static String getModVersion() {
        return "0.0.1-alpha";
    }

    public static String getAPIVersion() {
        return "1.0.0";
    }

    public static Map<String, Object> getAPIMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("apiVersion", getAPIVersion());
        metrics.put("modVersion", getModVersion());
        metrics.put("debugMode", getInstance().debugMode);
        metrics.put("apiEnabled", getInstance().apiEnabled);
        metrics.put(
            "extensionsCount",
            getInstance().registeredExtensions.size()
        );

        if (getInstance().mlogOptimizer != null) {
            metrics.put("mlogStats", getInstance().mlogOptimizer.getStats());
        }

        return metrics;
    }
}
