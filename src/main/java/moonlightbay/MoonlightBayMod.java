package moonlightbay;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.SettingsDialog;
import moonlightbay.api.MoonlightAPI;
import moonlightbay.core.*;

public class MoonlightBayMod extends Mod {

    private MLogOptimizer mlogOptimizer;
    private TextureStreamer textureStreamer;
    private ModLoaderBooster modLoaderBooster;
    private AdaptiveScheduler adaptiveScheduler;
    private Settings settingsManager;

    public MoonlightBayMod() {
        Log.info("[Moonlight Bay] ========================================");
        Log.info("[Moonlight Bay] Loading Moonlight Bay v0.0.1 Alpha");
        Log.info("[Moonlight Bay] Author: MGFeng | Studio: Moonlight Games");
        Log.info("[Moonlight Bay] ========================================");

        // 初始化设置管理器
        settingsManager = new Settings();

        // 初始化核心组件
        mlogOptimizer = new MLogOptimizer();
        textureStreamer = new TextureStreamer();
        modLoaderBooster = new ModLoaderBooster();
        adaptiveScheduler = new AdaptiveScheduler();

        // 注册API
        MoonlightAPI.registerMLogAPI(mlogOptimizer);
        MoonlightAPI.registerTextureAPI(textureStreamer);
        MoonlightAPI.registerSchedulerAPI(adaptiveScheduler);

        // 根据设置启动功能
        if (settingsManager.isMLogOptimizerEnabled()) {
            mlogOptimizer.init();
            Log.info("[Moonlight Bay] ✓ MLog Optimizer initialized");
        }

        if (settingsManager.isTextureStreamerEnabled()) {
            textureStreamer.init();
            Log.info("[Moonlight Bay] ✓ Texture Streamer initialized");
        }

        if (settingsManager.isModLoaderBoosterEnabled()) {
            modLoaderBooster.init();
            Log.info("[Moonlight Bay] ✓ Mod Loader Booster initialized");
        }

        if (settingsManager.isAdaptiveSchedulerEnabled()) {
            adaptiveScheduler.init();
            Log.info("[Moonlight Bay] ✓ Adaptive Scheduler initialized");
        }

        // 监听游戏事件
        Events.on(ClientLoadEvent.class, event -> onClientLoad());
        Events.on(WorldLoadEvent.class, event -> onWorldLoad());

        Log.info("[Moonlight Bay] Mod loaded successfully!");
    }

    private void onClientLoad() {
        Log.info("[Moonlight Bay] Client load event triggered");
        if (settingsManager.isTextureStreamerEnabled()) {
            textureStreamer.onClientLoad();
        }
        if (settingsManager.isAdaptiveSchedulerEnabled()) {
            adaptiveScheduler.onClientLoad();
        }
    }

    private void onWorldLoad() {
        Log.info("[Moonlight Bay] World load event triggered");
        if (settingsManager.isAdaptiveSchedulerEnabled()) {
            adaptiveScheduler.onWorldLoad();
        }
    }

    @Override
    public void init() {
        Log.info("[Moonlight Bay] Running post-init tasks");
        if (settingsManager.isModLoaderBoosterEnabled()) {
            modLoaderBooster.accelerateLoading();
        }
    }

    @Override
    public void loadContent() {
        Log.info("[Moonlight Bay] Loading content assets");
    }

    @Override
    public void registerClientSettings(SettingsDialog dialog) {
        settingsManager.registerSettings(dialog);
    }
}
