package com.moonlightgames.moonlightbay;

import com.moonlightgames.moonlightbay.api.*;
import com.moonlightgames.moonlightbay.platform.LoaderDetector;
import com.moonlightgames.moonlightbay.platform.neoforge.NeoForgeRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("moonlightbay")
public class MoonlightBay {
    
    public MoonlightBay() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册所有 DeferredRegister 到事件总线
        NeoForgeRegistries.registerAllToBus(modBus);
        
        modBus.addListener(this::commonSetup);
        
        // 输出启动信息
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        String version = LoaderDetector.getLoaderVersion();
        System.out.println("[Moonlight Bay] 初始化完成");
        System.out.println("[Moonlight Bay] 检测到加载器: " + loader);
        System.out.println("[Moonlight Bay] 加载器版本: " + version);
        
        // 检查光影
        if (ShaderAdapter.isShaderAvailable()) {
            System.out.println("[Moonlight Bay] 检测到光影: " + ShaderAdapter.getCurrentShader());
        }
        
        // 初始化配置
        ConfigHelper.init("moonlightbay");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UnifiedRegistry.init();
            System.out.println("[Moonlight Bay] 通用设置完成");
        });
    }
}