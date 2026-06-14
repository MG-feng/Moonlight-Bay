package moonlightbay.core;

import arc.Core;
import arc.math.Mathf;
import arc.util.Log;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.world.Building;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.PowerNode;

public class AdaptiveScheduler {

    private boolean initialized = false;
    private boolean debugMode = false;

    // 频率管理
    private Map<Object, Integer> updateFrequencies = new ConcurrentHashMap<>();
    private Map<Object, Integer> currentFrames = new ConcurrentHashMap<>();
    private Map<Object, Long> lastUpdateTime = new ConcurrentHashMap<>();

    // 处理器休眠
    private Map<LogicBlock, Boolean> processorSleepStatus =
        new ConcurrentHashMap<>();
    private Map<LogicBlock, String> processorOutputHash =
        new ConcurrentHashMap<>();
    private Map<LogicBlock, Integer> processorIdleFrames =
        new ConcurrentHashMap<>();

    // 性能监控
    private float currentFPS = 60;
    private float targetFPS = 60;
    private int thresholdFPS = 45;
    private float[] fpsHistory = new float[60];
    private int fpsIndex = 0;

    // 动态调整
    private int adaptInterval = 120;
    private int frameCounter = 0;
    private boolean isPerformanceCritical = false;
    private float lastAdaptTime = 0;

    // 分区管理
    private Map<Integer, List<Object>> sectorObjects =
        new ConcurrentHashMap<>();
    private int sectorSize = 50;

    public void init() {
        if (initialized) return;
        initialized = true;

        debugMode = isDebugEnabled();
        thresholdFPS = getSchedulerThreshold();

        // 初始化频率桶
        for (int i = 1; i <= 60; i++) {
            updateFrequencies.put("bucket_" + i, i);
        }

        Log.info(
            "[AdaptiveScheduler] Initialized with threshold: " +
                thresholdFPS +
                " FPS"
        );
    }

    public void onClientLoad() {
        registerUpdateHooks();
        startMonitoringThread();
    }

    public void onWorldLoad() {
        // 重置所有状态
        updateFrequencies.clear();
        currentFrames.clear();
        processorSleepStatus.clear();
        processorOutputHash.clear();
        processorIdleFrames.clear();
        sectorObjects.clear();

        // 分析世界对象
        analyzeWorldObjects();

        Log.info(
            "[AdaptiveScheduler] World loaded, analyzing " +
                updateFrequencies.size() +
                " objects"
        );
    }

    private void analyzeWorldObjects() {
        if (!Vars.state.isGame()) return;

        // 收集所有建筑
        for (Building build : Vars.world.buildings()) {
            if (build == null) continue;

            int frequency = determineInitialFrequency(build);
            updateFrequencies.put(build, frequency);
            assignToSector(build);
        }

        // 收集所有单位
        for (Unit unit : Groups.unit) {
            if (unit == null) continue;

            int frequency = determineInitialFrequency(unit);
            updateFrequencies.put(unit, frequency);
            assignToSector(unit);
        }

        // 收集逻辑处理器
        for (Building build : Vars.world.buildings()) {
            if (build instanceof LogicBlock) {
                processorSleepStatus.put((LogicBlock) build, false);
            }
        }
    }

    private int determineInitialFrequency(Building build) {
        if (build instanceof LogicBlock) return 60;
        if (build.block.isTurret) return 60;
        if (build.block.hasItems || build.block.hasLiquids) return 30;
        if (build.block.isStatic()) return 10;
        if (build instanceof PowerNode) return 20;
        return 20;
    }

    private int determineInitialFrequency(Unit unit) {
        if (unit.isPlayer()) return 60;
        if (unit.isEnemy() && isInCombatZone(unit)) return 60;
        if (unit.type.armor > 5) return 30; // 重型单位
        return 15;
    }

    private boolean isInCombatZone(Unit unit) {
        // 检查周围是否有敌人或子弹
        for (Unit other : Groups.unit) {
            if (
                other != unit &&
                other.team != unit.team &&
                other.within(unit, 100f)
            ) {
                return true;
            }
        }
        return false;
    }

    private void assignToSector(Object obj) {
        if (obj instanceof Building) {
            Building build = (Building) obj;
            int sectorX = build.tileX() / sectorSize;
            int sectorY = build.tileY() / sectorSize;
            int key = sectorX * 10000 + sectorY;
            sectorObjects.computeIfAbsent(key, k -> new ArrayList<>()).add(obj);
        } else if (obj instanceof Unit) {
            Unit unit = (Unit) obj;
            int sectorX = (int) unit.x / sectorSize;
            int sectorY = (int) unit.y / sectorSize;
            int key = sectorX * 10000 + sectorY;
            sectorObjects.computeIfAbsent(key, k -> new ArrayList<>()).add(obj);
        }
    }

    private void registerUpdateHooks() {
        // 钩子：拦截更新循环
        Events.on(WorldLoadEvent.class, event -> {
            if (isPerformanceCritical) {
                applyAggressiveOptimizations();
            }
        });
    }

    private void startMonitoringThread() {
        Thread monitorThread = new Thread(() -> {
            while (initialized && Vars.state.isGame()) {
                try {
                    updatePerformanceMetrics();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void update() {
        if (!initialized || !Vars.state.isGame()) return;

        frameCounter++;

        // 更新FPS监控
        if (frameCounter % 30 == 0) {
            updateFPS();
        }

        // 定期调整调度策略
        if (frameCounter >= adaptInterval) {
            frameCounter = 0;
            adaptScheduling();
        }

        // 管理处理器休眠（每帧检查）
        manageProcessorSleep();

        // 执行智能更新
        performSmartUpdates();
    }

    private void updateFPS() {
        currentFPS = Core.graphics.getFramesPerSecond();

        // 保存历史
        fpsHistory[fpsIndex % fpsHistory.length] = currentFPS;
        fpsIndex++;

        // 计算平均FPS
        float avgFPS = 0;
        for (float fps : fpsHistory) {
            avgFPS += fps;
        }
        avgFPS /= fpsHistory.length;

        // 判断性能状态
        isPerformanceCritical = avgFPS < thresholdFPS;

        if (isPerformanceCritical) {
            adaptInterval = 30; // 更快的响应
        } else {
            adaptInterval = 120;
        }

        if (debugMode && frameCounter % 120 == 0) {
            Log.debug(
                "[AdaptiveScheduler] FPS: " +
                    currentFPS +
                    " (avg: " +
                    avgFPS +
                    ")"
            );
        }
    }

    private void updatePerformanceMetrics() {
        // 更新性能指标
    }

    private void adaptScheduling() {
        if (isPerformanceCritical) {
            // 性能不足，激进优化
            applyAggressiveOptimizations();
        } else if (currentFPS > targetFPS + 10) {
            // 性能充裕，恢复质量
            applyConservativeOptimizations();
        } else {
            // 平衡模式
            applyBalancedOptimizations();
        }
    }

    private void applyAggressiveOptimizations() {
        int reducedCount = 0;
        int sleptCount = 0;

        // 大幅降低非关键对象频率
        for (Map.Entry<Object, Integer> entry : updateFrequencies.entrySet()) {
            Object obj = entry.getKey();
            int currentFreq = entry.getValue();

            if (!isObjectCritical(obj) && currentFreq > 10) {
                int newFreq = Math.max(5, currentFreq / 3);
                updateFrequencies.put(obj, newFreq);
                reducedCount++;
            }
        }

        // 强制更多处理器进入休眠
        for (Map.Entry<
            LogicBlock,
            Boolean
        > entry : processorSleepStatus.entrySet()) {
            if (!entry.getValue() && shouldForceSleep(entry.getKey())) {
                sleepProcessor(entry.getKey());
                sleptCount++;
            }
        }

        // 降低视距
        if (Vars.renderer != null && reducedCount > 10) {
            // Vars.renderer.maxZoomOut = 2.5f;
        }

        if (debugMode && (reducedCount > 0 || sleptCount > 0)) {
            Log.debug(
                "[AdaptiveScheduler] Aggressive: reduced " +
                    reducedCount +
                    " objects, slept " +
                    sleptCount +
                    " processors"
            );
        }
    }

    private void applyConservativeOptimizations() {
        int restoredCount = 0;

        // 恢复对象频率
        for (Map.Entry<Object, Integer> entry : updateFrequencies.entrySet()) {
            Object obj = entry.getKey();
            int currentFreq = entry.getValue();
            int targetFreq = getTargetFrequency(obj);

            if (currentFreq < targetFreq) {
                int newFreq = Math.min(targetFreq, currentFreq + 5);
                updateFrequencies.put(obj, newFreq);
                restoredCount++;
            }
        }

        // 唤醒休眠的处理器
        for (Map.Entry<
            LogicBlock,
            Boolean
        > entry : processorSleepStatus.entrySet()) {
            if (entry.getValue()) {
                wakeProcessor(entry.getKey());
                restoredCount++;
            }
        }

        if (debugMode && restoredCount > 0) {
            Log.debug(
                "[AdaptiveScheduler] Conservative: restored " +
                    restoredCount +
                    " objects"
            );
        }
    }

    private void applyBalancedOptimizations() {
        int adjustedCount = 0;

        // 平衡调整
        for (Map.Entry<Object, Integer> entry : updateFrequencies.entrySet()) {
            Object obj = entry.getKey();
            int currentFreq = entry.getValue();
            int targetFreq = getTargetFrequency(obj);

            if (Math.abs(currentFreq - targetFreq) > 10) {
                int newFreq = (currentFreq + targetFreq) / 2;
                updateFrequencies.put(obj, newFreq);
                adjustedCount++;
            }
        }

        if (debugMode && adjustedCount > 0 && frameCounter % 300 == 0) {
            Log.debug(
                "[AdaptiveScheduler] Balanced: adjusted " +
                    adjustedCount +
                    " objects"
            );
        }
    }

    private int getTargetFrequency(Object obj) {
        if (obj instanceof Building) {
            return determineInitialFrequency((Building) obj);
        } else if (obj instanceof Unit) {
            return determineInitialFrequency((Unit) obj);
        }
        return 30;
    }

    private boolean isObjectCritical(Object obj) {
        if (obj instanceof Building) {
            Building build = (Building) obj;
            if (build.block.isTurret) return true;
            if (build instanceof LogicBlock) {
                LogicBlock processor = (LogicBlock) build;
                if (Boolean.TRUE.equals(processorSleepStatus.get(processor))) {
                    return false;
                }
                return true;
            }
            if (build.items != null && build.items.total() > 0) return true;
            if (
                build.power != null &&
                build.power.graph.getBatteryStored() > 1000
            ) return true;
        } else if (obj instanceof Unit) {
            Unit unit = (Unit) obj;
            if (unit.isPlayer()) return true;
            if (isInCombatZone(unit)) return true;
        }
        return false;
    }

    private boolean shouldForceSleep(LogicBlock processor) {
        // 检查处理器是否长期空闲
        Integer idleFrames = processorIdleFrames.get(processor);
        if (idleFrames == null) return false;
        return idleFrames > 180; // 3秒空闲
    }

    private void manageProcessorSleep() {
        for (Building build : Vars.world.buildings()) {
            if (!(build instanceof LogicBlock)) continue;

            LogicBlock processor = (LogicBlock) build;
            String currentHash = getProcessorOutputHash(processor);
            String lastHash = processorOutputHash.get(processor);

            if (currentHash.equals(lastHash)) {
                // 输出未变
                Integer idleFrames = processorIdleFrames.getOrDefault(
                    processor,
                    0
                );
                processorIdleFrames.put(processor, idleFrames + 1);

                if (
                    idleFrames > 60 &&
                    !processorSleepStatus.getOrDefault(processor, false)
                ) {
                    // 空闲超过1秒，进入休眠
                    sleepProcessor(processor);
                }
            } else {
                // 输出改变
                processorOutputHash.put(processor, currentHash);
                processorIdleFrames.put(processor, 0);

                if (processorSleepStatus.getOrDefault(processor, false)) {
                    wakeProcessor(processor);
                }
            }
        }
    }

    private String getProcessorOutputHash(LogicBlock processor) {
        // 计算处理器输出内容的哈希
        try {
            java.lang.reflect.Field codeField =
                LogicBlock.class.getDeclaredField("code");
            codeField.setAccessible(true);
            String code = (String) codeField.get(processor);
            if (code != null) {
                return Integer.toHexString(code.hashCode());
            }
        } catch (Exception e) {
            // 使用对象身份哈希
        }
        return Integer.toHexString(System.identityHashCode(processor));
    }

    private void sleepProcessor(LogicBlock processor) {
        if (debugMode) {
            Log.debug(
                "[AdaptiveScheduler] Sleeping processor at " +
                    processor.tileX() +
                    "," +
                    processor.tileY()
            );
        }
        processorSleepStatus.put(processor, true);
        updateFrequencies.put(processor, 1);

        // 通过反射降低处理器活动
        try {
            java.lang.reflect.Field enabledField =
                LogicBlock.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.setBoolean(processor, false);
        } catch (Exception e) {
            // 忽略
        }
    }

    private void wakeProcessor(LogicBlock processor) {
        if (debugMode) {
            Log.debug(
                "[AdaptiveScheduler] Waking processor at " +
                    processor.tileX() +
                    "," +
                    processor.tileY()
            );
        }
        processorSleepStatus.put(processor, false);
        updateFrequencies.put(processor, 60);

        // 重新启用处理器
        try {
            java.lang.reflect.Field enabledField =
                LogicBlock.class.getDeclaredField("enabled");
            enabledField.setAccessible(true);
            enabledField.setBoolean(processor, true);
        } catch (Exception e) {
            // 忽略
        }
    }

    private void performSmartUpdates() {
        // 智能更新：只更新本帧应该更新的对象
        for (Map.Entry<Object, Integer> entry : updateFrequencies.entrySet()) {
            Object obj = entry.getKey();
            int frequency = entry.getValue();

            int currentFrame = currentFrames.getOrDefault(obj, 0);
            if (currentFrame >= 60 / frequency) {
                currentFrames.put(obj, 0);
                // 执行更新
                performUpdate(obj);
            } else {
                currentFrames.put(obj, currentFrame + 1);
            }
        }
    }

    private void performUpdate(Object obj) {
        // 实际更新逻辑
    }

    /**
     * 手动设置对象更新频率
     */
    public void setUpdateFrequency(Object obj, int frequencyHz) {
        if (frequencyHz >= 1 && frequencyHz <= 60) {
            updateFrequencies.put(obj, frequencyHz);
        }
    }

    /**
     * 获取对象当前频率
     */
    public int getUpdateFrequency(Object obj) {
        return updateFrequencies.getOrDefault(obj, 30);
    }

    /**
     * 标记为关键对象
     */
    public void markCritical(Object obj) {
        if (obj instanceof Building || obj instanceof Unit) {
            updateFrequencies.put(obj, 60);
        }
    }

    private int getSchedulerThreshold() {
        return Core.settings.getInt("moonlightbay.scheduler_threshold", 45);
    }

    private boolean isDebugEnabled() {
        return Core.settings.getBool("moonlightbay.api_debug", false);
    }
}
