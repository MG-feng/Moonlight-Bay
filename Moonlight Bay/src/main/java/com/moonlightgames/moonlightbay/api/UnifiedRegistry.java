package com.moonlightgames.moonlightbay.api;

import com.moonlightgames.moonlightbay.platform.neoforge.NeoForgeRegistries;
import com.moonlightgames.moonlightbay.platform.forge.ForgeRegistries;
import com.moonlightgames.moonlightbay.platform.fabric.FabricRegistries;

import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 统一注册器 - 整合所有注册功能
 * 
 * 使用示例：
 * // 注册物品
 * UnifiedRegistry.registerItem("my_mod", "diamond_sword", () -> new DiamondSword());
 * 
 * // 注册方块
 * UnifiedRegistry.registerBlock("my_mod", "my_block", () -> new MyBlock());
 * 
 * // 注册带物品的方块
 * UnifiedRegistry.registerBlockWithItem("my_mod", "my_block", 
 *     () -> new MyBlock(), () -> new BlockItem(MyBlock.get()));
 */
public final class UnifiedRegistry {
    
    private UnifiedRegistry() {}
    
    // ========== 待注册队列 ==========
    private static final List<Runnable> pendingRegistrations = new ArrayList<>();
    private static boolean isInitialized = false;
    
    // ========== 物品注册 ==========
    private static final Map<String, Supplier<Object>> itemSuppliers = new HashMap<>();
    
    /**
     * 注册物品
     * @param modId 模组ID
     * @param id 物品ID（不含modId前缀）
     * @param supplier 物品实例供应器
     */
    public static void registerItem(String modId, String id, Supplier<Object> supplier) {
        String fullId = modId + ":" + id;
        if (isInitialized) {
            getItemRegistry().register(fullId, supplier);
        } else {
            itemSuppliers.put(fullId, supplier);
            pendingRegistrations.add(() -> getItemRegistry().register(fullId, supplier));
        }
    }
    
    /**
     * 批量注册物品
     */
    public static void registerItems(String modId, Map<String, Supplier<Object>> items) {
        for (Map.Entry<String, Supplier<Object>> entry : items.entrySet()) {
            registerItem(modId, entry.getKey(), entry.getValue());
        }
    }
    
    // ========== 方块注册 ==========
    private static final Map<String, Supplier<Object>> blockSuppliers = new HashMap<>();
    
    /**
     * 注册方块
     */
    public static void registerBlock(String modId, String id, Supplier<Object> supplier) {
        String fullId = modId + ":" + id;
        if (isInitialized) {
            getBlockRegistry().register(fullId, supplier);
        } else {
            blockSuppliers.put(fullId, supplier);
            pendingRegistrations.add(() -> getBlockRegistry().register(fullId, supplier));
        }
    }
    
    /**
     * 注册带物品的方块（自动创建对应物品）
     */
    public static void registerBlockWithItem(String modId, String id, 
                                              Supplier<Object> blockSupplier,
                                              Supplier<Object> itemSupplier) {
        registerBlock(modId, id, blockSupplier);
        registerItem(modId, id, itemSupplier);
    }
    
    /**
     * 批量注册方块
     */
    public static void registerBlocks(String modId, Map<String, Supplier<Object>> blocks) {
        for (Map.Entry<String, Supplier<Object>> entry : blocks.entrySet()) {
            registerBlock(modId, entry.getKey(), entry.getValue());
        }
    }
    
    // ========== 实体注册 ==========
    private static final Map<String, Supplier<Object>> entitySuppliers = new HashMap<>();
    
    /**
     * 注册实体类型
     */
    public static void registerEntity(String modId, String id, Supplier<Object> supplier) {
        String fullId = modId + ":" + id;
        if (isInitialized) {
            getEntityRegistry().register(fullId, supplier);
        } else {
            entitySuppliers.put(fullId, supplier);
            pendingRegistrations.add(() -> getEntityRegistry().register(fullId, supplier));
        }
    }
    
    // ========== 配方序列化器注册 ==========
    private static final Map<String, Supplier<Object>> recipeSuppliers = new HashMap<>();
    
    /**
     * 注册配方序列化器
     */
    public static void registerRecipeSerializer(String modId, String id, Supplier<Object> supplier) {
        String fullId = modId + ":" + id;
        if (isInitialized) {
            getRecipeRegistry().register(fullId, supplier);
        } else {
            recipeSuppliers.put(fullId, supplier);
            pendingRegistrations.add(() -> getRecipeRegistry().register(fullId, supplier));
        }
    }
    
    // ========== 声音事件注册 ==========
    private static final Map<String, Supplier<Object>> soundSuppliers = new HashMap<>();
    
    /**
     * 注册声音事件
     */
    public static void registerSoundEvent(String modId, String id, Supplier<Object> supplier) {
        String fullId = modId + ":" + id;
        if (isInitialized) {
            getSoundRegistry().register(fullId, supplier);
        } else {
            soundSuppliers.put(fullId, supplier);
            pendingRegistrations.add(() -> getSoundRegistry().register(fullId, supplier));
        }
    }
    
    // ========== 创意模式标签页注册 ==========
    private static final Map<String, Supplier<Object>> tabSuppliers = new HashMap<>();
    
    /**
     * 注册创意模式标签页
     */
    public static void registerCreativeTab(String modId, String id, Supplier<Object> supplier) {
        String fullId = modId + ":" + id;
        if (isInitialized) {
            getCreativeTabRegistry().register(fullId, supplier);
        } else {
            tabSuppliers.put(fullId, supplier);
            pendingRegistrations.add(() -> getCreativeTabRegistry().register(fullId, supplier));
        }
    }
    
    // ========== 初始化 ==========
    
    /**
     * 初始化注册器（在模组设置事件中调用）
     */
    public static void init() {
        if (isInitialized) return;
        
        // 执行所有待注册任务
        for (Runnable task : pendingRegistrations) {
            task.run();
        }
        
        isInitialized = true;
        System.out.println("[Moonlight Bay] UnifiedRegistry 初始化完成，已处理 " + 
                           pendingRegistrations.size() + " 个注册项");
    }
    
    // ========== 内部注册器接口 ==========
    
    public interface ItemRegistry {
        void register(String id, Supplier<Object> supplier);
    }
    
    public interface BlockRegistry {
        void register(String id, Supplier<Object> supplier);
    }
    
    public interface EntityRegistry {
        void register(String id, Supplier<Object> supplier);
    }
    
    public interface RecipeRegistry {
        void register(String id, Supplier<Object> supplier);
    }
    
    public interface SoundRegistry {
        void register(String id, Supplier<Object> supplier);
    }
    
    public interface CreativeTabRegistry {
        void register(String id, Supplier<Object> supplier);
    }
    
    // ========== 获取各加载器的注册器实例 ==========
    
    private static ItemRegistry getItemRegistry() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return NeoForgeRegistries.ITEM;
            case FORGE:
                return ForgeRegistries.ITEM;
            case FABRIC:
            case QUILT:
                return FabricRegistries.ITEM;
            default:
                return FallbackRegistries.ITEM;
        }
    }
    
    private static BlockRegistry getBlockRegistry() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return NeoForgeRegistries.BLOCK;
            case FORGE:
                return ForgeRegistries.BLOCK;
            case FABRIC:
            case QUILT:
                return FabricRegistries.BLOCK;
            default:
                return FallbackRegistries.BLOCK;
        }
    }
    
    private static EntityRegistry getEntityRegistry() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return NeoForgeRegistries.ENTITY;
            case FORGE:
                return ForgeRegistries.ENTITY;
            case FABRIC:
            case QUILT:
                return FabricRegistries.ENTITY;
            default:
                return FallbackRegistries.ENTITY;
        }
    }
    
    private static RecipeRegistry getRecipeRegistry() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return NeoForgeRegistries.RECIPE;
            case FORGE:
                return ForgeRegistries.RECIPE;
            case FABRIC:
            case QUILT:
                return FabricRegistries.RECIPE;
            default:
                return FallbackRegistries.RECIPE;
        }
    }
    
    private static SoundRegistry getSoundRegistry() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return NeoForgeRegistries.SOUND;
            case FORGE:
                return ForgeRegistries.SOUND;
            case FABRIC:
            case QUILT:
                return FabricRegistries.SOUND;
            default:
                return FallbackRegistries.SOUND;
        }
    }
    
    private static CreativeTabRegistry getCreativeTabRegistry() {
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        
        switch (loader) {
            case NEOFORGE:
                return NeoForgeRegistries.CREATIVE_TAB;
            case FORGE:
                return ForgeRegistries.CREATIVE_TAB;
            case FABRIC:
            case QUILT:
                return FabricRegistries.CREATIVE_TAB;
            default:
                return FallbackRegistries.CREATIVE_TAB;
        }
    }
    
    // ========== 降级注册器（内部类） ==========
    
    private static class FallbackRegistries {
        static final ItemRegistry ITEM = (id, supplier) -> {
            System.err.println("[Moonlight Bay] 无法注册物品，未检测到加载器: " + id);
        };
        static final BlockRegistry BLOCK = (id, supplier) -> {
            System.err.println("[Moonlight Bay] 无法注册方块，未检测到加载器: " + id);
        };
        static final EntityRegistry ENTITY = (id, supplier) -> {
            System.err.println("[Moonlight Bay] 无法注册实体，未检测到加载器: " + id);
        };
        static final RecipeRegistry RECIPE = (id, supplier) -> {
            System.err.println("[Moonlight Bay] 无法注册配方，未检测到加载器: " + id);
        };
        static final SoundRegistry SOUND = (id, supplier) -> {
            System.err.println("[Moonlight Bay] 无法注册声音，未检测到加载器: " + id);
        };
        static final CreativeTabRegistry CREATIVE_TAB = (id, supplier) -> {
            System.err.println("[Moonlight Bay] 无法注册标签页，未检测到加载器: " + id);
        };
    }
}