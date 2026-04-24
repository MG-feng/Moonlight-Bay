package com.moonlightgames.moonlightbay.platform.forge;

import com.moonlightgames.moonlightbay.api.UnifiedRegistry;
import com.moonlightgames.moonlightbay.api.VersionAwareAdapter;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;

public final class ForgeRegistries {
    
    private ForgeRegistries() {}
    
    // 根据 Forge 版本选择不同的实现
    private static final boolean IS_LEGACY = 
        VersionAwareAdapter.getForgeVersionGroup() == VersionAwareAdapter.ForgeVersionGroup.LEGACY;
    
    // 存储每个模组的 DeferredRegister（新版 Forge 使用）
    private static final Map<String, DeferredRegister<Item>> ITEM_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<Block>> BLOCK_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<EntityType<?>>> ENTITY_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<RecipeSerializer<?>>> RECIPE_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<SoundEvent>> SOUND_REGISTRIES = new HashMap<>();
    private static final Map<String, DeferredRegister<CreativeModeTab>> TAB_REGISTRIES = new HashMap<>();
    
    // 存储注册的对象
    private static final Map<String, Map<String, Supplier<?>>> REGISTERED_ITEMS = new HashMap<>();
    
    // ========== 旧版 Forge 注册方式（1.7.10 - 1.12.2） ==========
    
    private static class LegacyForgeRegistry {
        // 旧版 Forge 使用 GameRegistry.register
        static void registerItem(String modId, String id, Supplier<Object> supplier) {
            // 实际代码需要调用 GameRegistry.registerItem
            // 由于旧版 Forge 的 API 差异较大，这里提供框架
            System.out.println("[Forge Legacy] 注册物品: " + modId + ":" + id);
            // 模组需要自行处理 GameRegistry.registerItem
        }
        
        static void registerBlock(String modId, String id, Supplier<Object> supplier) {
            System.out.println("[Forge Legacy] 注册方块: " + modId + ":" + id);
        }
        
        static void registerEntity(String modId, String id, Supplier<Object> supplier) {
            System.out.println("[Forge Legacy] 注册实体: " + modId + ":" + id);
        }
    }
    
    // ========== 新版 Forge 注册方式（1.13 - 1.20.1） ==========
    
    private static DeferredRegister<Item> getItemRegister(String modId) {
        return ITEM_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(ForgeRegistries.ITEMS, id));
    }
    
    private static DeferredRegister<Block> getBlockRegister(String modId) {
        return BLOCK_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(ForgeRegistries.BLOCKS, id));
    }
    
    private static DeferredRegister<EntityType<?>> getEntityRegister(String modId) {
        return ENTITY_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, id));
    }
    
    private static DeferredRegister<RecipeSerializer<?>> getRecipeRegister(String modId) {
        return RECIPE_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, id));
    }
    
    private static DeferredRegister<SoundEvent> getSoundRegister(String modId) {
        return SOUND_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, id));
    }
    
    private static DeferredRegister<CreativeModeTab> getTabRegister(String modId) {
        return TAB_REGISTRIES.computeIfAbsent(modId, id -> 
            DeferredRegister.create(ForgeRegistries.CREATIVE_MODE_TABS, id));
    }
    
    // ========== 物品注册 ==========
    
    public static final UnifiedRegistry.ItemRegistry ITEM = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String itemId = parts[1];
        
        if (IS_LEGACY) {
            // 旧版 Forge
            LegacyForgeRegistry.registerItem(modId, itemId, supplier);
        } else {
            // 新版 Forge
            DeferredRegister<Item> register = getItemRegister(modId);
            Supplier<Item> registered = register.register(itemId, () -> (Item) supplier.get());
            
            REGISTERED_ITEMS.computeIfAbsent(modId, k -> new HashMap<>())
                            .put(itemId, registered);
        }
        
        System.out.println("[Forge] 注册物品: " + id);
    };
    
    // ========== 方块注册 ==========
    
    public static final UnifiedRegistry.BlockRegistry BLOCK = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String blockId = parts[1];
        
        if (IS_LEGACY) {
            LegacyForgeRegistry.registerBlock(modId, blockId, supplier);
        } else {
            DeferredRegister<Block> register = getBlockRegister(modId);
            register.register(blockId, () -> (Block) supplier.get());
        }
        
        System.out.println("[Forge] 注册方块: " + id);
    };
    
    // ========== 实体注册 ==========
    
    public static final UnifiedRegistry.EntityRegistry ENTITY = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String entityId = parts[1];
        
        if (IS_LEGACY) {
            LegacyForgeRegistry.registerEntity(modId, entityId, supplier);
        } else {
            DeferredRegister<EntityType<?>> register = getEntityRegister(modId);
            register.register(entityId, () -> (EntityType<?>) supplier.get());
        }
        
        System.out.println("[Forge] 注册实体: " + id);
    };
    
    // ========== 配方注册 ==========
    
    public static final UnifiedRegistry.RecipeRegistry RECIPE = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String recipeId = parts[1];
        
        if (!IS_LEGACY) {
            DeferredRegister<RecipeSerializer<?>> register = getRecipeRegister(modId);
            register.register(recipeId, () -> (RecipeSerializer<?>) supplier.get());
        }
        
        System.out.println("[Forge] 注册配方序列化器: " + id);
    };
    
    // ========== 声音注册 ==========
    
    public static final UnifiedRegistry.SoundRegistry SOUND = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String soundId = parts[1];
        
        if (!IS_LEGACY) {
            DeferredRegister<SoundEvent> register = getSoundRegister(modId);
            register.register(soundId, () -> (SoundEvent) supplier.get());
        }
        
        System.out.println("[Forge] 注册声音: " + id);
    };
    
    // ========== 创意标签页注册 ==========
    
    public static final UnifiedRegistry.CreativeTabRegistry CREATIVE_TAB = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String tabId = parts[1];
        
        if (!IS_LEGACY) {
            DeferredRegister<CreativeModeTab> register = getTabRegister(modId);
            register.register(tabId, () -> (CreativeModeTab) supplier.get());
        }
        
        System.out.println("[Forge] 注册创意标签页: " + id);
    };
    
    // ========== 工具方法 ==========
    
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> getItem(String modId, String itemId) {
        Map<String, Supplier<?>> items = REGISTERED_ITEMS.get(modId);
        if (items == null) return null;
        return (Supplier<T>) items.get(itemId);
    }
    
    /**
     * 注册所有 DeferredRegister 到事件总线
     */
    public static void registerAllToBus(Object modEventBus) {
        if (IS_LEGACY) return; // 旧版不需要注册到事件总线
        
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
    }
}