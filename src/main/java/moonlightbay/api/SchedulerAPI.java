package moonlightbay.api;

import arc.util.Log;
import mindustry.gen.Unit;
import mindustry.world.Building;
import moonlightbay.core.AdaptiveScheduler;

public class SchedulerAPI {

    private AdaptiveScheduler scheduler;

    SchedulerAPI(AdaptiveScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 设置对象的更新频率
     * @param obj 建筑或单位
     * @param frequencyHz 频率（Hz，1-60）
     */
    public void setUpdateFrequency(Object obj, int frequencyHz) {
        if (scheduler == null || obj == null) return;

        int clamped = Math.max(1, Math.min(60, frequencyHz));
        scheduler.setUpdateFrequency(obj, clamped);

        if (isDebugEnabled()) {
            Log.debug(
                "[SchedulerAPI] Set frequency for " +
                    obj.getClass().getSimpleName() +
                    " to " +
                    clamped +
                    "Hz"
            );
        }
    }

    /**
     * 获取对象的当前更新频率
     * @param obj 建筑或单位
     * @return 频率（Hz）
     */
    public int getUpdateFrequency(Object obj) {
        if (scheduler == null || obj == null) return 30;
        return scheduler.getUpdateFrequency(obj);
    }

    /**
     * 标记对象为关键对象（总是保持高频率60Hz）
     * @param obj 建筑或单位
     */
    public void markCritical(Object obj) {
        if (scheduler == null || obj == null) return;
        scheduler.markCritical(obj);

        if (isDebugEnabled()) {
            Log.debug(
                "[SchedulerAPI] Marked as critical: " +
                    obj.getClass().getSimpleName()
            );
        }
    }

    /**
     * 检查对象是否被标记为关键
     * @param obj 建筑或单位
     * @return 是否关键
     */
    public boolean isCritical(Object obj) {
        if (scheduler == null || obj == null) return false;
        return scheduler.getUpdateFrequency(obj) >= 60;
    }

    /**
     * 设置FPS阈值（低于此值开始优化）
     * @param fps FPS值（20-60）
     */
    public void setFPSThreshold(int fps) {
        if (scheduler == null) return;

        int clamped = Math.max(20, Math.min(60, fps));
        Core.settings.put("moonlightbay.scheduler_threshold", clamped);
        Log.info("[SchedulerAPI] FPS threshold set to " + clamped);
    }

    /**
     * 获取当前FPS阈值
     * @return FPS值
     */
    public int getFPSThreshold() {
        return Core.settings.getInt("moonlightbay.scheduler_threshold", 45);
    }

    /**
     * 获取当前游戏FPS
     * @return 当前FPS
     */
    public float getCurrentFPS() {
        return Core.graphics.getFramesPerSecond();
    }

    /**
     * 检查是否处于性能优化模式
     * @return 是否正在优化
     */
    public boolean isPerformanceOptimizing() {
        if (scheduler == null) return false;
        return getCurrentFPS() < getFPSThreshold();
    }

    /**
     * 临时提高指定区域对象的更新频率
     * @param x 中心X坐标
     * @param y 中心Y坐标
     * @param radius 半径
     * @param duration 持续时间（秒）
     */
    public void boostArea(float x, float y, float radius, float duration) {
        if (scheduler == null) return;

        Log.info(
            "[SchedulerAPI] Boosting area around (" +
                x +
                "," +
                y +
                ") for " +
                duration +
                "s"
        );

        // 实际实现需要找到区域内的对象并提高频率
        // 使用定时器在duration后恢复
    }

    /**
     * 获取调度器统计信息
     */
    public java.util.Map<String, Object> getSchedulerStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("currentFPS", getCurrentFPS());
        stats.put("thresholdFPS", getFPSThreshold());
        stats.put("isOptimizing", isPerformanceOptimizing());
        stats.put("debugMode", isDebugEnabled());
        return stats;
    }

    private boolean isDebugEnabled() {
        return Core.settings.getBool("moonlightbay.api_debug", false);
    }
}
