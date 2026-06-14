package moonlightbay;

import arc.Core;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Slider;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.SettingsDialog;

public class Settings {

    private static final String PREFIX = "moonlightbay.";

    // 性能优化开关
    private static final String KEY_MLOG_OPTIMIZER = PREFIX + "mlog_optimizer";
    private static final String KEY_TEXTURE_STREAMER =
        PREFIX + "texture_streamer";
    private static final String KEY_MOD_LOADER_BOOSTER =
        PREFIX + "mod_loader_booster";
    private static final String KEY_ADAPTIVE_SCHEDULER =
        PREFIX + "adaptive_scheduler";

    // 高级设置
    private static final String KEY_MLOG_OPTIMIZATION_LEVEL =
        PREFIX + "mlog_optimization_level";
    private static final String KEY_TEXTURE_STREAMING_DISTANCE =
        PREFIX + "texture_streaming_distance";
    private static final String KEY_ADAPTIVE_SCHEDULER_THRESHOLD =
        PREFIX + "adaptive_scheduler_threshold";
    private static final String KEY_SLEEP_INACTIVE_PROCESSORS =
        PREFIX + "sleep_inactive_processors";
    private static final String KEY_SLEEP_CHECK_INTERVAL =
        PREFIX + "sleep_check_interval";

    // 开发工具
    private static final String KEY_DEV_MODE = PREFIX + "dev_mode";
    private static final String KEY_API_DEBUG = PREFIX + "api_debug";
    private static final String KEY_PERFORMANCE_METRICS =
        PREFIX + "performance_metrics";

    public Settings() {
        initDefaultSettings();
    }

    private void initDefaultSettings() {
        // 性能优化开关
        if (!Core.settings.has(KEY_MLOG_OPTIMIZER)) Core.settings.put(
            KEY_MLOG_OPTIMIZER,
            true
        );
        if (!Core.settings.has(KEY_TEXTURE_STREAMER)) Core.settings.put(
            KEY_TEXTURE_STREAMER,
            true
        );
        if (!Core.settings.has(KEY_MOD_LOADER_BOOSTER)) Core.settings.put(
            KEY_MOD_LOADER_BOOSTER,
            true
        );
        if (!Core.settings.has(KEY_ADAPTIVE_SCHEDULER)) Core.settings.put(
            KEY_ADAPTIVE_SCHEDULER,
            true
        );

        // 高级设置
        if (!Core.settings.has(KEY_MLOG_OPTIMIZATION_LEVEL)) Core.settings.put(
            KEY_MLOG_OPTIMIZATION_LEVEL,
            2
        );
        if (
            !Core.settings.has(KEY_TEXTURE_STREAMING_DISTANCE)
        ) Core.settings.put(KEY_TEXTURE_STREAMING_DISTANCE, 50);
        if (
            !Core.settings.has(KEY_ADAPTIVE_SCHEDULER_THRESHOLD)
        ) Core.settings.put(KEY_ADAPTIVE_SCHEDULER_THRESHOLD, 45);
        if (
            !Core.settings.has(KEY_SLEEP_INACTIVE_PROCESSORS)
        ) Core.settings.put(KEY_SLEEP_INACTIVE_PROCESSORS, true);
        if (!Core.settings.has(KEY_SLEEP_CHECK_INTERVAL)) Core.settings.put(
            KEY_SLEEP_CHECK_INTERVAL,
            30
        );

        // 开发工具
        if (!Core.settings.has(KEY_DEV_MODE)) Core.settings.put(
            KEY_DEV_MODE,
            false
        );
        if (!Core.settings.has(KEY_API_DEBUG)) Core.settings.put(
            KEY_API_DEBUG,
            false
        );
        if (!Core.settings.has(KEY_PERFORMANCE_METRICS)) Core.settings.put(
            KEY_PERFORMANCE_METRICS,
            false
        );
    }

    public void registerSettings(SettingsDialog dialog) {
        dialog.addCategory("Moonlight Bay", Icon.settings, table -> {
            // 性能优化标题
            table
                .add("[accent]Performance Optimizers[]")
                .colspan(2)
                .padTop(10)
                .padBottom(10)
                .row();

            // MLog Optimizer
            CheckBox mlogCheck = new CheckBox("MLog Optimizer");
            mlogCheck.setChecked(isMLogOptimizerEnabled());
            mlogCheck.changed(() -> {
                Core.settings.put(KEY_MLOG_OPTIMIZER, mlogCheck.isChecked());
                Log.info(
                    "[Moonlight Bay] MLog Optimizer: " +
                        (mlogCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(mlogCheck).left().padBottom(5);
            table
                .add("Compile C-like code to optimized MLog, remove dead code")
                .left()
                .padBottom(5)
                .padLeft(20)
                .row();

            // Texture Streamer
            CheckBox textureCheck = new CheckBox("Texture Streamer");
            textureCheck.setChecked(isTextureStreamerEnabled());
            textureCheck.changed(() -> {
                Core.settings.put(
                    KEY_TEXTURE_STREAMER,
                    textureCheck.isChecked()
                );
                Log.info(
                    "[Moonlight Bay] Texture Streamer: " +
                        (textureCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(textureCheck).left().padBottom(5);
            table
                .add("Smart VRAM management, auto degrade distant textures")
                .left()
                .padBottom(5)
                .padLeft(20)
                .row();

            // Mod Loader Booster
            CheckBox loaderCheck = new CheckBox("Mod Loader Booster");
            loaderCheck.setChecked(isModLoaderBoosterEnabled());
            loaderCheck.changed(() -> {
                Core.settings.put(
                    KEY_MOD_LOADER_BOOSTER,
                    loaderCheck.isChecked()
                );
                Log.info(
                    "[Moonlight Bay] Mod Loader Booster: " +
                        (loaderCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(loaderCheck).left().padBottom(5);
            table
                .add("Parallel mod loading, reduce startup time")
                .left()
                .padBottom(5)
                .padLeft(20)
                .row();

            // Adaptive Scheduler
            CheckBox schedulerCheck = new CheckBox("Adaptive Scheduler");
            schedulerCheck.setChecked(isAdaptiveSchedulerEnabled());
            schedulerCheck.changed(() -> {
                Core.settings.put(
                    KEY_ADAPTIVE_SCHEDULER,
                    schedulerCheck.isChecked()
                );
                Log.info(
                    "[Moonlight Bay] Adaptive Scheduler: " +
                        (schedulerCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(schedulerCheck).left().padBottom(5);
            table
                .add("Dynamic update frequency, fix late-game lag")
                .left()
                .padBottom(5)
                .padLeft(20)
                .row();

            // 高级设置标题
            table
                .add("[accent]Advanced Settings[]")
                .colspan(2)
                .padTop(20)
                .padBottom(10)
                .row();

            // MLog优化级别
            table.add("MLog Optimization Level:").left().padBottom(5);
            Slider mlogSlider = new Slider(1, 3, 1);
            mlogSlider.setValue(getMLogOptimizationLevel());
            mlogSlider.changed(() -> {
                int value = (int) mlogSlider.getValue();
                Core.settings.put(KEY_MLOG_OPTIMIZATION_LEVEL, value);
                Log.info("[Moonlight Bay] MLog level set to: " + value);
            });
            table.add(mlogSlider).left().padBottom(5).padLeft(20).row();
            table
                .add("1=Basic, 2=Standard, 3=Maximum optimization")
                .colspan(2)
                .left()
                .padBottom(10)
                .row();

            // 纹理流式传输距离
            table.add("Texture Streaming Distance:").left().padBottom(5);
            Slider distSlider = new Slider(10, 200, 5);
            distSlider.setValue(getTextureStreamingDistance());
            distSlider.changed(() -> {
                int value = (int) distSlider.getValue();
                Core.settings.put(KEY_TEXTURE_STREAMING_DISTANCE, value);
                Log.info("[Moonlight Bay] Streaming distance set to: " + value);
            });
            table.add(distSlider).left().padBottom(5).padLeft(20).row();
            table
                .add("Tiles beyond this distance use low-res textures")
                .colspan(2)
                .left()
                .padBottom(10)
                .row();

            // 自适应调度阈值
            table.add("Adaptive Scheduler FPS Threshold:").left().padBottom(5);
            Slider thresholdSlider = new Slider(20, 60, 1);
            thresholdSlider.setValue(getAdaptiveSchedulerThreshold());
            thresholdSlider.changed(() -> {
                int value = (int) thresholdSlider.getValue();
                Core.settings.put(KEY_ADAPTIVE_SCHEDULER_THRESHOLD, value);
                Log.info(
                    "[Moonlight Bay] Scheduler threshold set to: " + value
                );
            });
            table.add(thresholdSlider).left().padBottom(5).padLeft(20).row();
            table
                .add("Start optimizing when FPS drops below this value")
                .colspan(2)
                .left()
                .padBottom(10)
                .row();

            // 休眠不活跃处理器
            CheckBox sleepCheck = new CheckBox("Sleep Inactive Processors");
            sleepCheck.setChecked(getSleepInactiveProcessors());
            sleepCheck.changed(() -> {
                Core.settings.put(
                    KEY_SLEEP_INACTIVE_PROCESSORS,
                    sleepCheck.isChecked()
                );
                Log.info(
                    "[Moonlight Bay] Sleep inactive processors: " +
                        (sleepCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(sleepCheck).left().padBottom(5).colspan(2).row();
            table
                .add("Put logic processors to sleep when output unchanged")
                .colspan(2)
                .left()
                .padBottom(20)
                .row();

            // 开发工具标题
            table
                .add("[accent]Developer Tools[]")
                .colspan(2)
                .padTop(20)
                .padBottom(10)
                .row();

            // 开发者模式
            CheckBox devCheck = new CheckBox("Developer Mode");
            devCheck.setChecked(isDevModeEnabled());
            devCheck.changed(() -> {
                Core.settings.put(KEY_DEV_MODE, devCheck.isChecked());
                Log.info(
                    "[Moonlight Bay] Developer mode: " +
                        (devCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(devCheck).left().padBottom(5).colspan(2).row();

            // API调试
            CheckBox debugCheck = new CheckBox("API Debug Output");
            debugCheck.setChecked(isAPIDebugEnabled());
            debugCheck.changed(() -> {
                Core.settings.put(KEY_API_DEBUG, debugCheck.isChecked());
                Log.info(
                    "[Moonlight Bay] API debug: " +
                        (debugCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(debugCheck).left().padBottom(5).colspan(2).row();

            // 性能指标显示
            CheckBox metricsCheck = new CheckBox("Show Performance Metrics");
            metricsCheck.setChecked(isPerformanceMetricsEnabled());
            metricsCheck.changed(() -> {
                Core.settings.put(
                    KEY_PERFORMANCE_METRICS,
                    metricsCheck.isChecked()
                );
                Log.info(
                    "[Moonlight Bay] Performance metrics: " +
                        (metricsCheck.isChecked() ? "ON" : "OFF")
                );
            });
            table.add(metricsCheck).left().padBottom(5).colspan(2).row();
        });
    }

    // Getters
    public boolean isMLogOptimizerEnabled() {
        return Core.settings.getBool(KEY_MLOG_OPTIMIZER, true);
    }

    public boolean isTextureStreamerEnabled() {
        return Core.settings.getBool(KEY_TEXTURE_STREAMER, true);
    }

    public boolean isModLoaderBoosterEnabled() {
        return Core.settings.getBool(KEY_MOD_LOADER_BOOSTER, true);
    }

    public boolean isAdaptiveSchedulerEnabled() {
        return Core.settings.getBool(KEY_ADAPTIVE_SCHEDULER, true);
    }

    public int getMLogOptimizationLevel() {
        return Core.settings.getInt(KEY_MLOG_OPTIMIZATION_LEVEL, 2);
    }

    public int getTextureStreamingDistance() {
        return Core.settings.getInt(KEY_TEXTURE_STREAMING_DISTANCE, 50);
    }

    public int getAdaptiveSchedulerThreshold() {
        return Core.settings.getInt(KEY_ADAPTIVE_SCHEDULER_THRESHOLD, 45);
    }

    public boolean getSleepInactiveProcessors() {
        return Core.settings.getBool(KEY_SLEEP_INACTIVE_PROCESSORS, true);
    }

    public int getSleepCheckInterval() {
        return Core.settings.getInt(KEY_SLEEP_CHECK_INTERVAL, 30);
    }

    public boolean isDevModeEnabled() {
        return Core.settings.getBool(KEY_DEV_MODE, false);
    }

    public boolean isAPIDebugEnabled() {
        return Core.settings.getBool(KEY_API_DEBUG, false);
    }

    public boolean isPerformanceMetricsEnabled() {
        return Core.settings.getBool(KEY_PERFORMANCE_METRICS, false);
    }
}
