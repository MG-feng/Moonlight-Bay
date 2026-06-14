package moonlightbay.core;

import arc.Core;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.util.Log;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.world.Building;
import mindustry.world.blocks.units.UnitBlock;

public class TextureStreamer {

    private boolean initialized = false;
    private boolean debugMode = false;

    // 纹理管理
    private Map<String, Texture> fullResTextures = new ConcurrentHashMap<>();
    private Map<String, WeakReference<Texture>> lowResCache =
        new ConcurrentHashMap<>();
    private Map<String, Integer> textureRefCount = new ConcurrentHashMap<>();

    // 距离追踪
    private Map<Unit, Integer> unitDistanceCache = new ConcurrentHashMap<>();
    private Map<Building, Integer> buildingDistanceCache =
        new ConcurrentHashMap<>();

    // 性能监控
    private long vramUsage = 0;
    private long vramLimit = 512 * 1024 * 1024;
    private int streamingDistance = 50;
    private int highResDistance = 30;
    private int checkInterval = 60;
    private int frameCounter = 0;

    // 优先级队列
    private PriorityQueue<TextureRequest> requestQueue = new PriorityQueue<>();

    public void init() {
        if (initialized) return;
        initialized = true;

        debugMode = isDebugEnabled();
        streamingDistance = getStreamingDistance();
        highResDistance = streamingDistance / 2;

        detectVRAMLimit();
        registerTextureEvents();

        Log.info(
            "[TextureStreamer] Initialized with distance: " +
                streamingDistance +
                ", VRAM limit: " +
                (vramLimit / 1024 / 1024) +
                "MB"
        );
    }

    public void onClientLoad() {
        registerTextureHooks();
        startStreamingThread();
    }

    private void detectVRAMLimit() {
        try {
            // 获取可用显存（估算）
            int maxTextureSize = Core.graphics
                .getGL()
                .getInteger(org.lwjgl.opengl.GL11.GL_MAX_TEXTURE_SIZE);
            vramLimit = (long) maxTextureSize * maxTextureSize * 4; // RGBA
            vramLimit = Math.min(vramLimit, 1024L * 1024L * 1024L);
            vramLimit = Math.max(vramLimit, 256L * 1024L * 1024L);
        } catch (Exception e) {
            vramLimit = 512 * 1024 * 1024L;
        }
    }

    private void registerTextureEvents() {
        Events.on(WorldLoadEvent.class, event -> {
            fullResTextures.clear();
            lowResCache.clear();
            textureRefCount.clear();
        });
    }

    private void registerTextureHooks() {
        // 钩子：拦截纹理加载请求
        // 实际实现需要修改游戏渲染管线
    }

    private void startStreamingThread() {
        // 启动后台流式传输线程
        Thread streamingThread = new Thread(() -> {
            while (initialized && Vars.state.isGame()) {
                try {
                    processTextureQueue();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        streamingThread.setDaemon(true);
        streamingThread.start();
    }

    public void update() {
        if (!initialized) return;

        frameCounter++;
        if (frameCounter >= checkInterval) {
            frameCounter = 0;
            performStreamingCheck();
        }

        updateVRAMUsage();
        if (vramUsage > vramLimit * 0.85) {
            evictTextures();
        }
    }

    private void performStreamingCheck() {
        if (!Vars.state.isGame() || Vars.player == null) return;

        Unit player = Vars.player.unit();
        if (player == null) return;

        // 检查所有单位的距离
        for (Unit unit : Groups.unit) {
            if (unit == null || unit == player) continue;

            float distance = unit.dst2(player);
            int distanceInt = (int) distance;

            Integer lastDist = unitDistanceCache.get(unit);
            if (lastDist == null || Math.abs(lastDist - distanceInt) > 20) {
                unitDistanceCache.put(unit, distanceInt);
                updateUnitTextureQuality(unit, distanceInt);
            }
        }

        // 检查建筑
        for (Building build : Vars.world.buildings()) {
            if (build == null) continue;

            float distance = build.dst2(player);
            int distanceInt = (int) distance;

            Integer lastDist = buildingDistanceCache.get(build);
            if (lastDist == null || Math.abs(lastDist - distanceInt) > 30) {
                buildingDistanceCache.put(build, distanceInt);
                updateBuildingTextureQuality(build, distanceInt);
            }
        }
    }

    private void updateUnitTextureQuality(Unit unit, int distance) {
        TextureQuality quality = getQualityForDistance(distance);

        if (quality == TextureQuality.LOW) {
            setUnitLowRes(unit);
        } else if (quality == TextureQuality.MEDIUM) {
            setUnitMediumRes(unit);
        } else {
            setUnitHighRes(unit);
        }
    }

    private void updateBuildingTextureQuality(Building build, int distance) {
        TextureQuality quality = getQualityForDistance(distance);

        if (quality == TextureQuality.LOW) {
            degradeBuildingTexture(build);
        } else if (quality == TextureQuality.MEDIUM) {
            setBuildingMediumRes(build);
        } else {
            restoreBuildingTexture(build);
        }
    }

    private TextureQuality getQualityForDistance(int distance) {
        if (distance > streamingDistance * streamingDistance) {
            return TextureQuality.LOW;
        } else if (distance > highResDistance * highResDistance) {
            return TextureQuality.MEDIUM;
        } else {
            return TextureQuality.HIGH;
        }
    }

    private void setUnitLowRes(Unit unit) {
        String key = unit.type.name + "_low";
        Texture lowRes = getOrCreateLowResTexture(key, unit.type.uiIcon);
        if (lowRes != null && unit.type.uiIcon != lowRes) {
            requestTextureChange(unit, lowRes);
        }
    }

    private void setUnitMediumRes(Unit unit) {
        String key = unit.type.name + "_med";
        Texture medRes = lowResCache.containsKey(key)
            ? lowResCache.get(key).get()
            : null;
        if (medRes == null) {
            medRes = createMediumResTexture(unit.type.uiIcon);
            lowResCache.put(key, new WeakReference<>(medRes));
        }
        if (medRes != null && unit.type.uiIcon != medRes) {
            requestTextureChange(unit, medRes);
        }
    }

    private void setUnitHighRes(Unit unit) {
        Texture highRes = fullResTextures.get(unit.type.name);
        if (highRes == null) {
            highRes = unit.type.uiIcon;
            fullResTextures.put(unit.type.name, highRes);
        }
        if (unit.type.uiIcon != highRes) {
            requestTextureChange(unit, highRes);
        }
    }

    private void degradeBuildingTexture(Building build) {
        // 建筑纹理降级实现
        if (build.block.hasIcon()) {
            requestTextureChange(
                build,
                getLowResTexture(build.block.name + "_low", build.block.uiIcon)
            );
        }
    }

    private void setBuildingMediumRes(Building build) {
        // 建筑中分辨率纹理
    }

    private void restoreBuildingTexture(Building build) {
        // 恢复建筑原纹理
    }

    private Texture getOrCreateLowResTexture(String key, Texture original) {
        WeakReference<Texture> ref = lowResCache.get(key);
        if (ref != null && ref.get() != null && !ref.get().isDisposed()) {
            return ref.get();
        }

        Texture lowRes = createLowResTexture(original);
        lowResCache.put(key, new WeakReference<>(lowRes));
        return lowRes;
    }

    private Texture createLowResTexture(Texture original) {
        try {
            int newWidth = Math.max(16, original.width / 4);
            int newHeight = Math.max(16, original.height / 4);

            Pixmap originalPix = original.getTextureData().getPixmap();
            Pixmap lowResPix = new Pixmap(newWidth, newHeight);
            lowResPix.draw(
                originalPix,
                0,
                0,
                original.width,
                original.height,
                0,
                0,
                newWidth,
                newHeight
            );

            Texture texture = new Texture(lowResPix);
            texture.setFilter(TextureFilter.linear);

            originalPix.dispose();
            lowResPix.dispose();

            updateVRAMUsage(texture.width * texture.height * 4);
            return texture;
        } catch (Exception e) {
            Log.err(
                "[TextureStreamer] Failed to create low-res texture: " +
                    e.getMessage()
            );
            return original;
        }
    }

    private Texture createMediumResTexture(Texture original) {
        try {
            int newWidth = Math.max(32, original.width / 2);
            int newHeight = Math.max(32, original.height / 2);

            Pixmap originalPix = original.getTextureData().getPixmap();
            Pixmap medResPix = new Pixmap(newWidth, newHeight);
            medResPix.draw(
                originalPix,
                0,
                0,
                original.width,
                original.height,
                0,
                0,
                newWidth,
                newHeight
            );

            Texture texture = new Texture(medResPix);
            texture.setFilter(TextureFilter.linear);

            originalPix.dispose();
            medResPix.dispose();

            return texture;
        } catch (Exception e) {
            return original;
        }
    }

    private void requestTextureChange(Object target, Texture texture) {
        TextureRequest request = new TextureRequest(
            target,
            texture,
            System.currentTimeMillis()
        );
        requestQueue.offer(request);
    }

    private void processTextureQueue() {
        int processed = 0;
        while (!requestQueue.isEmpty() && processed < 10) {
            TextureRequest request = requestQueue.poll();
            if (
                request != null &&
                request.texture != null &&
                !request.texture.isDisposed()
            ) {
                applyTextureChange(request.target, request.texture);
            }
            processed++;
        }
    }

    private void applyTextureChange(Object target, Texture texture) {
        try {
            if (target instanceof Unit) {
                ((Unit) target).type.uiIcon = texture;
            } else if (target instanceof Building) {
                // ((Building)target).block.uiIcon = texture; (需要修改)
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    private void updateVRAMUsage() {
        // 更新显存使用统计
    }

    private void updateVRAMUsage(int addedBytes) {
        vramUsage += addedBytes;
    }

    private void evictTextures() {
        Log.info(
            "[TextureStreamer] Evicting textures, VRAM usage: " +
                (vramUsage / 1024 / 1024) +
                "MB/" +
                (vramLimit / 1024 / 1024) +
                "MB"
        );

        // 按优先级释放纹理
        List<Map.Entry<String, WeakReference<Texture>>> entries =
            new ArrayList<>(lowResCache.entrySet());
        entries.sort((a, b) -> {
            int aRef = textureRefCount.getOrDefault(a.getKey(), 0);
            int bRef = textureRefCount.getOrDefault(b.getKey(), 0);
            return Integer.compare(aRef, bRef);
        });

        for (int i = 0; i < entries.size() / 2; i++) {
            Map.Entry<String, WeakReference<Texture>> entry = entries.get(i);
            Texture tex = entry.getValue().get();
            if (tex != null && !tex.isDisposed()) {
                tex.dispose();
                vramUsage -= tex.width * tex.height * 4;
            }
            lowResCache.remove(entry.getKey());
        }

        System.gc();
    }

    private int getStreamingDistance() {
        return Core.settings.getInt("moonlightbay.texture_distance", 50);
    }

    private boolean isDebugEnabled() {
        return Core.settings.getBool("moonlightbay.api_debug", false);
    }

    private enum TextureQuality {
        LOW,
        MEDIUM,
        HIGH,
    }

    private static class TextureRequest implements Comparable<TextureRequest> {

        Object target;
        Texture texture;
        long timestamp;

        TextureRequest(Object target, Texture texture, long timestamp) {
            this.target = target;
            this.texture = texture;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(TextureRequest other) {
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
}
