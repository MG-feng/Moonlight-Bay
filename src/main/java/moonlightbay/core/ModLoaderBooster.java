package moonlightbay.core;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.mod.Mods.LoadedMod;
import org.json.JSONObject;

public class ModLoaderBooster {

    private boolean initialized = false;
    private boolean debugMode = false;

    // 并行执行器
    private ExecutorService parallelExecutor;
    private ForkJoinPool forkJoinPool;

    // 模组信息缓存
    private Map<String, ModInfo> modInfoCache = new ConcurrentHashMap<>();
    private Map<String, List<String>> dependencyGraph =
        new ConcurrentHashMap<>();

    // 加载优化
    private List<String> optimizedLoadOrder = new ArrayList<>();
    private Map<String, Long> loadTimes = new ConcurrentHashMap<>();

    // 科技树合并
    private Map<String, Object> mergedTechTree = new ConcurrentHashMap<>();
    private List<String> techTreeConflicts = new ArrayList<>();

    // 统计
    private long totalParseTime = 0;
    private int totalModsProcessed = 0;

    public void init() {
        if (initialized) return;
        initialized = true;

        debugMode = isDebugEnabled();

        int processors = Runtime.getRuntime().availableProcessors();
        parallelExecutor = Executors.newFixedThreadPool(
            Math.max(2, processors - 1)
        );
        forkJoinPool = new ForkJoinPool(processors);

        Log.info(
            "[ModLoaderBooster] Initialized with " + processors + " CPU cores"
        );
    }

    public void accelerateLoading() {
        long startTime = System.currentTimeMillis();
        Log.info("[ModLoaderBooster] Starting mod loading acceleration...");

        // 1. 并行解析所有模组配置
        parallelParseModConfigs();

        // 2. 分析依赖关系并优化加载顺序
        analyzeDependencies();
        scheduleOptimizedLoadingOrder();

        // 3. 合并科技树冲突
        mergeTechTreeConflicts();

        // 4. 并行加载非关键资源
        parallelLoadNonCriticalAssets();

        // 5. 预热常用类
        warmupCommonClasses();

        long elapsed = System.currentTimeMillis() - startTime;
        Log.info(
            "[ModLoaderBooster] Acceleration completed in " +
                elapsed +
                "ms (processed " +
                totalModsProcessed +
                " mods)"
        );

        if (debugMode) {
            printLoadStats();
        }
    }

    private void parallelParseModConfigs() {
        Seq<LoadedMod> mods = Vars.mods.list();
        totalModsProcessed = mods.size;

        Log.info(
            "[ModLoaderBooster] Parsing " +
                totalModsProcessed +
                " mods in parallel..."
        );

        List<Future<ModInfo>> futures = new ArrayList<>();

        for (LoadedMod mod : mods) {
            Future<ModInfo> future = parallelExecutor.submit(() ->
                parseModConfig(mod)
            );
            futures.add(future);
        }

        // 收集结果
        for (Future<ModInfo> future : futures) {
            try {
                ModInfo info = future.get(10, TimeUnit.SECONDS);
                if (info != null && info.name != null) {
                    modInfoCache.put(info.name, info);
                }
            } catch (TimeoutException e) {
                Log.err("[ModLoaderBooster] Timeout parsing mod config");
            } catch (Exception e) {
                Log.err(
                    "[ModLoaderBooster] Failed to parse mod: " + e.getMessage()
                );
            }
        }

        Log.info(
            "[ModLoaderBooster] Parsed " + modInfoCache.size() + " mod configs"
        );
    }

    private ModInfo parseModConfig(LoadedMod mod) {
        long startTime = System.nanoTime();

        try {
            ModInfo info = new ModInfo();
            info.name = mod.name;
            info.displayName = mod.meta.displayName();
            info.version = mod.meta.version;
            info.author = mod.meta.author;
            info.description = mod.meta.description;
            info.dependencies =
                mod.meta.dependencies != null
                    ? new ArrayList<>(mod.meta.dependencies)
                    : new ArrayList<>();
            info.hidden = mod.meta.hidden;

            // 解析 mod.hjson 获取额外信息
            Fi configFile = mod.root.child("mod.hjson");
            if (!configFile.exists()) {
                configFile = mod.root.child("mod.json");
            }

            if (configFile.exists()) {
                String content = configFile.readString();
                info.rawConfig = content;

                // 解析JSON内容
                try {
                    JSONObject json = new JSONObject(content);
                    if (json.has("minGameVersion")) info.minGameVersion =
                        json.getString("minGameVersion");
                    if (json.has("repo")) info.repo = json.getString("repo");
                } catch (Exception e) {
                    // HJSON格式需要特殊处理
                }
            }

            // 扫描模组内容
            info.contentTypes = scanModContent(mod);

            long parseTime = System.nanoTime() - startTime;
            totalParseTime += parseTime;

            if (debugMode) {
                Log.debug(
                    "[ModLoaderBooster] Parsed " +
                        info.name +
                        " in " +
                        (parseTime / 1000000) +
                        "ms"
                );
            }

            return info;
        } catch (Exception e) {
            Log.err(
                "[ModLoaderBooster] Error parsing mod " +
                    mod.name +
                    ": " +
                    e.getMessage()
            );
            return null;
        }
    }

    private Set<String> scanModContent(LoadedMod mod) {
        Set<String> content = new HashSet<>();

        // 扫描内容类型
        if (mod.root.child("content").exists()) {
            Fi contentDir = mod.root.child("content");
            for (Fi file : contentDir.list()) {
                if (
                    file.extension().equals("json") ||
                    file.extension().equals("hjson")
                ) {
                    String name = file.nameWithoutExtension();
                    if (name.contains("-")) {
                        content.add(name.split("-")[0]);
                    } else {
                        content.add("block");
                    }
                }
            }
        }

        if (mod.root.child("scripts").exists()) {
            content.add("scripts");
        }

        if (mod.root.child("assets").exists()) {
            content.add("assets");
        }

        return content;
    }

    private void analyzeDependencies() {
        Log.info("[ModLoaderBooster] Analyzing dependencies...");

        // 构建依赖图
        for (ModInfo info : modInfoCache.values()) {
            dependencyGraph.put(info.name, info.dependencies);
        }

        // 检测循环依赖
        detectCircularDependencies();

        // 检测缺失依赖
        detectMissingDependencies();
    }

    private void detectCircularDependencies() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String modName : dependencyGraph.keySet()) {
            if (detectCycle(modName, visited, recursionStack)) {
                Log.warn(
                    "[ModLoaderBooster] Circular dependency detected involving: " +
                        modName
                );
            }
        }
    }

    private boolean detectCycle(
        String node,
        Set<String> visited,
        Set<String> stack
    ) {
        if (stack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        stack.add(node);

        List<String> deps = dependencyGraph.get(node);
        if (deps != null) {
            for (String dep : deps) {
                if (detectCycle(dep, visited, stack)) {
                    return true;
                }
            }
        }

        stack.remove(node);
        return false;
    }

    private void detectMissingDependencies() {
        for (ModInfo info : modInfoCache.values()) {
            for (String dep : info.dependencies) {
                if (!modInfoCache.containsKey(dep) && !isBuiltinMod(dep)) {
                    Log.warn(
                        "[ModLoaderBooster] Mod '" +
                            info.name +
                            "' requires missing mod: " +
                            dep
                    );
                }
            }
        }
    }

    private boolean isBuiltinMod(String modName) {
        // 检查是否为内置模组
        return (
            modName.equals("mindustry-core") ||
            modName.equals("arc") ||
            modName.startsWith("builtin-")
        );
    }

    private void scheduleOptimizedLoadingOrder() {
        Log.info("[ModLoaderBooster] Calculating optimal loading order...");

        // 拓扑排序
        List<String> order = topologicalSort();

        // 优化排序：按类型分组
        List<String> coreMods = new ArrayList<>();
        List<String> contentMods = new ArrayList<>();
        List<String> utilityMods = new ArrayList<>();

        for (String modName : order) {
            ModInfo info = modInfoCache.get(modName);
            if (info == null) continue;

            if (
                info.contentTypes.contains("scripts") &&
                info.contentTypes.size() <= 2
            ) {
                utilityMods.add(modName);
            } else if (info.contentTypes.contains("assets")) {
                contentMods.add(modName);
            } else {
                coreMods.add(modName);
            }
        }

        // 构建优化顺序: 核心 -> 内容 -> 工具
        optimizedLoadOrder.clear();
        optimizedLoadOrder.addAll(coreMods);
        optimizedLoadOrder.addAll(contentMods);
        optimizedLoadOrder.addAll(utilityMods);

        if (debugMode) {
            Log.debug(
                "[ModLoaderBooster] Optimized load order: " + optimizedLoadOrder
            );
        }
    }

    private List<String> topologicalSort() {
        List<String> result = new ArrayList<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        // 计算入度
        for (String node : dependencyGraph.keySet()) {
            inDegree.putIfAbsent(node, 0);
        }

        for (Map.Entry<
            String,
            List<String>
        > entry : dependencyGraph.entrySet()) {
            for (String dep : entry.getValue()) {
                if (modInfoCache.containsKey(dep)) {
                    inDegree.put(dep, inDegree.getOrDefault(dep, 0) + 1);
                }
            }
        }

        // 入度为0的入队
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Kahn算法
        while (!queue.isEmpty()) {
            String node = queue.poll();
            result.add(node);

            for (Map.Entry<
                String,
                List<String>
            > entry : dependencyGraph.entrySet()) {
                if (entry.getValue().contains(node)) {
                    int newDegree = inDegree.get(entry.getKey()) - 1;
                    inDegree.put(entry.getKey(), newDegree);
                    if (newDegree == 0) {
                        queue.offer(entry.getKey());
                    }
                }
            }
        }

        return result;
    }

    private void mergeTechTreeConflicts() {
        Log.info("[ModLoaderBooster] Merging tech tree modifications...");

        // 收集所有科技树修改
        Map<String, List<ModInfo>> techModifications = new HashMap<>();

        for (ModInfo info : modInfoCache.values()) {
            if (info.rawConfig != null && info.rawConfig.contains("techTree")) {
                // 解析科技树修改
                extractTechModifications(info, techModifications);
            }
        }

        // 解决冲突：保留最后一个加载的模组的修改
        for (Map.Entry<
            String,
            List<ModInfo>
        > entry : techModifications.entrySet()) {
            if (entry.getValue().size() > 1) {
                String tech = entry.getKey();
                List<String> conflictingMods = new ArrayList<>();
                for (ModInfo info : entry.getValue()) {
                    conflictingMods.add(info.name);
                }
                techTreeConflicts.add(tech + " conflicts: " + conflictingMods);
                Log.warn(
                    "[ModLoaderBooster] Tech tree conflict on '" +
                        tech +
                        "' between " +
                        conflictingMods
                );
            }
        }
    }

    private void extractTechModifications(
        ModInfo info,
        Map<String, List<ModInfo>> techMods
    ) {
        // 简化的实现
        if (info.rawConfig.contains("parent")) {
            // 提取节点信息
        }
    }

    private void parallelLoadNonCriticalAssets() {
        Log.info("[ModLoaderBooster] Parallel loading non-critical assets...");

        List<Future<Void>> futures = new ArrayList<>();

        for (ModInfo info : modInfoCache.values()) {
            if (info.contentTypes.contains("assets")) {
                futures.add(
                    forkJoinPool.submit(() -> {
                        preloadModAssets(info);
                        return null;
                    })
                );
            }
        }

        for (Future<Void> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 忽略加载错误
            }
        }
    }

    private void preloadModAssets(ModInfo info) {
        // 预加载模组资源
        if (debugMode) {
            Log.debug("[ModLoaderBooster] Preloading assets for: " + info.name);
        }
    }

    private void warmupCommonClasses() {
        // 预热常用类以减少首次加载延迟
        try {
            Class.forName("mindustry.Vars");
            Class.forName("arc.Core");
            Class.forName("mindustry.game.EventType");
        } catch (ClassNotFoundException e) {
            // 忽略
        }
    }

    private void printLoadStats() {
        Log.info("[ModLoaderBooster] ===== Load Statistics =====");
        Log.info("[ModLoaderBooster] Total mods: " + totalModsProcessed);
        Log.info("[ModLoaderBooster] Parsed mods: " + modInfoCache.size());
        Log.info(
            "[ModLoaderBooster] Average parse time: " +
                (totalParseTime / Math.max(1, totalModsProcessed) / 1000000) +
                "ms"
        );
        Log.info(
            "[ModLoaderBooster] Optimized load order: " +
                optimizedLoadOrder.size() +
                " mods"
        );
        Log.info(
            "[ModLoaderBooster] Tech tree conflicts: " +
                techTreeConflicts.size()
        );
    }

    /**
     * 应用优化后的加载顺序到游戏
     */
    public void applyLoadingOrder() {
        // 通过反射修改模组加载顺序
        try {
            java.lang.reflect.Field field = Vars.mods
                .getClass()
                .getDeclaredField("mods");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Seq<LoadedMod> mods = (Seq<LoadedMod>) field.get(Vars.mods);

            // 重新排序
            mods.sort((a, b) -> {
                int aIndex = optimizedLoadOrder.indexOf(a.name);
                int bIndex = optimizedLoadOrder.indexOf(b.name);
                if (aIndex == -1) aIndex = Integer.MAX_VALUE;
                if (bIndex == -1) bIndex = Integer.MAX_VALUE;
                return Integer.compare(aIndex, bIndex);
            });

            Log.info("[ModLoaderBooster] Applied optimized loading order");
        } catch (Exception e) {
            Log.err(
                "[ModLoaderBooster] Failed to apply loading order: " +
                    e.getMessage()
            );
        }
    }

    public void shutdown() {
        if (parallelExecutor != null) {
            parallelExecutor.shutdown();
            try {
                if (!parallelExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    parallelExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                parallelExecutor.shutdownNow();
            }
        }

        if (forkJoinPool != null) {
            forkJoinPool.shutdown();
        }
    }

    private boolean isDebugEnabled() {
        return Core.settings.getBool("moonlightbay.api_debug", false);
    }

    // 模组信息类
    public static class ModInfo {

        String name;
        String displayName;
        String version;
        String author;
        String description;
        String minGameVersion;
        String repo;
        List<String> dependencies = new ArrayList<>();
        Set<String> contentTypes = new HashSet<>();
        String rawConfig;
        boolean hidden;
    }
}
