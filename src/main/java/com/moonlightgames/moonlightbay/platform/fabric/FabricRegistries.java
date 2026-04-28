package com.moonlightgames.moonlightbay.platform.fabric;

import com.moonlightgames.moonlightbay.api.UnifiedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;

public final class FabricRegistries {
    
    private FabricRegistries() {}
    
    // 存储注册的对象
    private static final Map<String, Map<String, Object>> REGISTERED_ITEMS = new HashMap<>();
    private static final Map<String, Map<String, Object>> REGISTERED_BLOCKS = new HashMap<>();
    
    // ========== 物品注册 ==========
    
    public static final UnifiedRegistry.ItemRegistry ITEM = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String itemId = parts[1];
        
        ResourceLocation location = new ResourceLocation(modId, itemId);
        Item item = (Item) supplier.get();
        
        // Fabric 使用原生的 Registry.register
        Item registered = Registry.register(BuiltInRegistries.ITEM, location, item);
        
        // 存储注册的对象
        REGISTERED_ITEMS.computeIfAbsent(modId, k -> new HashMap<>())
                        .put(itemId, registered);
        
        System.out.println("[Fabric] 注册物品: " + id);
    };
    
    // ========== 方块注册 ==========
    
    public static final UnifiedRegistry.BlockRegistry BLOCK = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String blockId = parts[1];
        
        ResourceLocation location = new ResourceLocation(modId, blockId);
        Block block = (Block) supplier.get();
        
        Block registered = Registry.register(BuiltInRegistries.BLOCK, location, block);
        
        REGISTERED_BLOCKS.computeIfAbsent(modId, k -> new HashMap<>())
                         .put(blockId, registered);
        
        System.out.println("[Fabric] 注册方块: " + id);
    };
    
    // ========== 实体注册 ==========
    
    public static final UnifiedRegistry.EntityRegistry ENTITY = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String entityId = parts[1];
        
        ResourceLocation location = new ResourceLocation(modId, entityId);
        EntityType<?> entityType = (EntityType<?>) supplier.get();
        
        Registry.register(BuiltInRegistries.ENTITY_TYPE, location, entityType);
        
        System.out.println("[Fabric] 注册实体: " + id);
    };
    
    // ========== 配方注册 ==========
    
    public static final UnifiedRegistry.RecipeRegistry RECIPE = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String recipeId = parts[1];
        
        ResourceLocation location = new ResourceLocation(modId, recipeId);
        RecipeSerializer<?> serializer = (RecipeSerializer<?>) supplier.get();
        
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, location, serializer);
        
        System.out.println("[Fabric] 注册配方序列化器: " + id);
    };
    
    // ========== 声音注册 ==========
    
    public static final UnifiedRegistry.SoundRegistry SOUND = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String soundId = parts[1];
        
        ResourceLocation location = new ResourceLocation(modId, soundId);
        SoundEvent soundEvent = (SoundEvent) supplier.get();
        
        Registry.register(BuiltInRegistries.SOUND_EVENT, location, soundEvent);
        
        System.out.println("[Fabric] 注册声音: " + id);
    };
    
    // ========== 创意标签页注册 ==========
    
    public static final UnifiedRegistry.CreativeTabRegistry CREATIVE_TAB = (id, supplier) -> {
        String[] parts = id.split(":");
        String modId = parts[0];
        String tabId = parts[1];
        
        ResourceLocation location = new ResourceLocation(modId, tabId);
        CreativeModeTab tab = (CreativeModeTab) supplier.get();
        
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, location, tab);
        
        System.out.println("[Fabric] 注册创意标签页: " + id);
    };
    
    // ========== 工具方法 ==========
    
    @SuppressWarnings("unchecked")
    public static <T> T getItem(String modId, String itemId) {
        Map<String, Object> items = REGISTERED_ITEMS.get(modId);
        if (items == null) return null;
        return (T) items.get(itemId);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getBlock(String modId, String blockId) {
        Map<String, Object> blocks = REGISTERED_BLOCKS.get(modId);
        if (blocks == null) return null;
        return (T) blocks.get(blockId);
    }
}