package com.moonlightgames.moonlightbay;

import com.moonlightgames.moonlightbay.api.*;
import com.moonlightgames.moonlightbay.platform.LoaderDetector;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("moonlightbay")
public class MoonlightBay {
    
    public MoonlightBay() {
        // 获取事件总线
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册设置事件
        modBus.addListener(this::commonSetup);
        
        // 输出启动信息
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        String version = LoaderDetector.getLoaderVersion();
        System.out.println("[Moonlight Bay] 初始化完成");
        System.out.println("[Moonlight Bay] 检测到加载器: " + loader);
        System.out.println("[Moonlight Bay] 加载器版本: " + version);
        
        // 检测并修复冲突
        ConflictResolver.detectAndFix();
        
        // 初始化配置
        ConfigHelper.init("moonlightbay");
        
        // 检查光影状态
        if (ShaderAdapter.isShaderAvailable()) {
            System.out.println("[Moonlight Bay] 检测到光影加载器: " + ShaderAdapter.getCurrentShader());
            System.out.println("[Moonlight Bay] 光影版本: " + ShaderAdapter.getShaderVersion());
        }
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 初始化注册辅助器
            ItemRegistryHelper.init();
            BlockRegistryHelper.init();
            
            // 声明 Moonlight Bay 提供的能力
            CapabilityDeclaration.declare("moonlightbay.loader_info", () -> {
                return LoaderAdapter.getCurrentLoader();
            });
            
            CapabilityDeclaration.declare("moonlightbay.version_info", () -> {
                return LoaderDetector.getLoaderVersion();
            });
        });
    }
}