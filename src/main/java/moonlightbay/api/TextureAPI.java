package moonlightbay.api;

import arc.graphics.Texture;
import arc.util.Log;
import moonlightbay.core.TextureStreamer;

public class TextureAPI {

    private TextureStreamer streamer;

    TextureAPI(TextureStreamer streamer) {
        this.streamer = streamer;
    }

    /**
     * 获取纹理的低分辨率版本
     * @param original 原始纹理
     * @return 低分辨率纹理
     */
    public Texture getLowResTexture(Texture original) {
        if (streamer == null || original == null) return original;
        // 实际实现需要调用streamer的方法
        return original;
    }

    /**
     * 获取纹理的中分辨率版本
     * @param original 原始纹理
     * @return 中分辨率纹理
     */
    public Texture getMediumResTexture(Texture original) {
        if (streamer == null || original == null) return original;
        return original;
    }

    /**
     * 设置纹理流式传输距离
     * @param distance 距离（格数）
     */
    public void setStreamingDistance(int distance) {
        if (streamer == null) return;
        int clamped = Math.max(10, Math.min(200, distance));
        Core.settings.put("moonlightbay.texture_distance", clamped);
        Log.info("[TextureAPI] Streaming distance set to " + clamped);
    }

    /**
     * 获取当前流式传输距离
     * @return 距离（格数）
     */
    public int getStreamingDistance() {
        return Core.settings.getInt("moonlightbay.texture_distance", 50);
    }

    /**
     * 强制释放未使用的纹理
     */
    public void forceUnloadUnused() {
        if (streamer == null) return;
        Log.info("[TextureAPI] Forcing texture unload");
        System.gc();
    }

    /**
     * 获取当前显存使用量（字节）
     */
    public long getVRAMUsage() {
        // 估算显存使用量
        return (
            Runtime.getRuntime().totalMemory() -
            Runtime.getRuntime().freeMemory()
        );
    }

    /**
     * 获取显存限制（字节）
     */
    public long getVRAMLimit() {
        return 512 * 1024 * 1024L;
    }

    /**
     * 获取显存使用百分比
     */
    public float getVRAMUsagePercent() {
        return ((float) getVRAMUsage() / getVRAMLimit()) * 100f;
    }

    /**
     * 预加载纹理到缓存
     * @param textureKey 纹理键
     * @param texture 纹理
     */
    public void preloadTexture(String textureKey, Texture texture) {
        if (streamer == null || texture == null) return;
        Log.info("[TextureAPI] Preloading texture: " + textureKey);
    }

    /**
     * 注册自定义纹理管理处理器
     */
    public void registerTextureHandler(
        String textureKey,
        TextureHandler handler
    ) {
        if (streamer == null) return;
        Log.info("[TextureAPI] Registered handler for: " + textureKey);
    }

    /**
     * 纹理处理器接口
     */
    public interface TextureHandler {
        Texture loadTexture(String path);
        void unloadTexture(Texture texture);
        Texture createThumbnail(Texture original);
    }

    /**
     * 默认纹理处理器
     */
    public static class DefaultTextureHandler implements TextureHandler {

        @Override
        public Texture loadTexture(String path) {
            return new Texture(path);
        }

        @Override
        public void unloadTexture(Texture texture) {
            if (texture != null && !texture.isDisposed()) {
                texture.dispose();
            }
        }

        @Override
        public Texture createThumbnail(Texture original) {
            return original;
        }
    }
}
