package com.moonlightgames.moonlightbay.platform.neoforge;

import com.moonlightgames.moonlightbay.api.UnifiedRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;

public final class NeoForgeRegistries {
    
    private NeoForgeRegistries() {}
    
    // 存储每个模组的 DeferredRegister
    private static final Map<String, DeferredRegister<Item>> ITEM_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<Block>> BLOCK_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<EntityType<?>>> ENTITY_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<RecipeSerializer<?>>> RECIPE_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<SoundEvent>> SOUND_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<CreativeModeTab>> TAB_REGISTRIES = new HashMap<>();
    
    // 存储注册的对象（用于返回）
    private static final Map<String, Map<String, Supplier<?>>> REGISTERED_ITEMS = new HashMap<>();
    private static final Map<String, Map<String, Supplier<?>>> REGISTERED_BLOCKS = new HashMap<>();
    
    /**
     * 获取或创建模组的物品注册器
     */
    private static DeferredRegister<Item> getItemRegister(String modId) {
        return ITEM_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(Registries.ITEM, id));
    }
    
    /**
     * 获取或创建模组的方块注册器
     */
    private static DeferredRegister<Block> getBlockRegister(String modId) {
        return BLOCK_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(Registries.BLOCK, id));
    }
    
    /**
     * 获取或创建模组的实体注册器
     */
    private static DeferredRegister<EntityType<?>> getEntityRegister(String modId) {
        return ENTITY_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(Registries.ENTITY_TYPE, id));
    }
    
    /**
     * 获取或创建模组的配方注册器
     */
    private static DeferredRegister<RecipeSerializer<?>> getRecipeRegister(String modId) {
        return RECIPE_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, id));
    }
    
    /**
     * 获取或创建模组的声音注册器
     */
    private static DeferredRegister<SoundEvent> getSoundRegister(String modId) {
        return SOUND_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(Registries.SOUND_EVENT, id));
    }
    
    /**
     * 获取或创建模组的创意标签页注册器
     */
    private static DeferredRegister<CreativeModeTab> getTabRegister(String modId) {
        return TAB_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, id));
    }
    
    // ========== 物品注册 ==========
    
    public static final UnifiedRegistry.ItemRegistry ITEM = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String itemId = parts[1];
        
        DeferredRegister<Item> register = getItemRegister(modId);
        Supplier<Item> registered = register.register(itemId, () -> (Item) supplier.get());
        
        // 存储注册的对象供后续使用
        REGISTERED_ITEMS.computeIfAbsent(modId, k -> new HashMap<>())
                        .put(itemId, registered);
        
        System.out.println("[NeoForge] 注册物品: " + id);
    };
    
    // ========== 方块注册 ==========
    
    public static final UnifiedRegistry.BlockRegistry BLOCK = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String blockId = parts[1];
        
        DeferredRegister<Block> register = getBlockRegister(modId);
        Supplier<Block> registered = register.register(blockId, () -> (Block) supplier.get());
        
        REGISTERED_BLOCKS.computeIfAbsent(modId, k -> new HashMap<>())
                         .put(blockId, registered);
        
        System.out.println("[NeoForge] 注册方块: " + id);
    };
    
    // ========== 实体注册 ==========
    
    public static final UnifiedRegistry.EntityRegistry ENTITY = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String entityId = parts[1];
        
        DeferredRegister<EntityType<?>> register = getEntityRegister(modId);
        register.register(entityId, () -> (EntityType<?>) supplier.get());
        
        System.out.println("[NeoForge] 注册实体: " + id);
    };
    
    // ========== 配方注册 ==========
    
    public static final UnifiedRegistry.RecipeRegistry RECIPE = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String recipeId = parts[1];
        
        DeferredRegister<RecipeSerializer<?>> register = getRecipeRegister(modId);
        register.register(recipeId, () -> (RecipeSerializer<?>) supplier.get());
        
        System.out.println("[NeoForge] 注册配方序列化器: " + id);
    };
    
    // ========== 声音注册 ==========
    
    public static final UnifiedRegistry.SoundRegistry SOUND = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String soundId = parts[1];
        
        DeferredRegister<SoundEvent> register = getSoundRegister(modId);
        register.register(soundId, () -> (SoundEvent) supplier.get());
        
        System.out.println("[NeoForge] 注册声音: " + id);
    };
    
    // ========== 创意标签页注册 ==========
    
    public static final UnifiedRegistry.CreativeTabRegistry CREATIVE_TAB = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String tabId = parts[1];
        
        DeferredRegister<CreativeModeTab> register = getTabRegister(modId);
        register.register(tabId, () -> (CreativeModeTab) supplier.get());
        
        System.out.println("[NeoForge] 注册创意标签页: " + id);
    };
    
    // ========== 工具方法 ==========
    
    /**
     * 获取已注册的物品（供模组使用）
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> getItem(String modId, String itemId) {
        Map<String, Supplier<?>> items = REGISTERED_ITEMS.get(modId);
        if (items == null) return null;
        return (Supplier<T>) items.get(itemId);
    }
    
    /**
     * 获取已注册的方块
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> getBlock(String modId, String blockId) {
        Map<String, Supplier<?>> blocks = REGISTERED_BLOCKS.get(modId);
        if (blocks == null) return null;
        return (Supplier<T>) blocks.get(blockId);
    }
    
    /**
     * 注册所有 DeferredRegister 到事件总线
     * 需要在模组主类的构造方法中调用
     */
    public static void registerAllToBus(Object modEventBus) {
        // 遍历所有注册器并注册到事件总线
        for (DeferredRegister<Item> register : ITEM_REGISTRIES.values()) {
            register.register(modEventBus);
        }
        for (DeferredRegister<Block> register : BLOCK_REGISTRIES.values()) {
            register.register(modEventBus);
        }
        for (DeferredRegister<EntityType<?>> register : ENTITY_REGISTRIES.values()) {
            register.register(modEventBus);
        }
        for (DeferredRegister<RecipeSerializer<?>> register : RECIPE_REGISTRIES.values()) {
            register.register(modEventBus);
        }
        for (DeferredRegister<SoundEvent> register : SOUND_REGISTRIES.values()) {
            register.register(modEventBus);
        }
        for (DeferredRegister<CreativeModeTab> register : TAB_REGISTRIES.values()) {
            register.register(modEventBus);
        }
        System.out.println("[NeoForge] 已注册 " + 
            (ITEM_REGISTRIES.size() + BLOCK_REGISTRIES.size() + ENTITY_REGISTRIES.size()) + 
            " 个 DeferredRegister 到事件总线");
    }
}