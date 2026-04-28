package com.moonlightgames.moonlightbay.platform.forge;

import com.moonlightgames.moonlightbay.api.*;
import com.moonlightgames.moonlightbay.platform.LoaderDetector;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("moonlightbay")
public class MoonlightBayForge {
    
    public MoonlightBayForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册所有 DeferredRegister 到事件总线
        ForgeRegistries.registerAllToBus(modBus);
        
        modBus.addListener(this::commonSetup);
        
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        String version = LoaderDetector.getLoaderVersion();
        System.out.println("[Moonlight Bay] 初始化完成");
        System.out.println("[Moonlight Bay] 检测到加载器: " + loader);
        System.out.println("[Moonlight Bay] 加载器版本: " + version);
        
        if (ShaderAdapter.isShaderAvailable()) {
            System.out.println("[Moonlight Bay] 检测到光影: " + ShaderAdapter.getCurrentShader());
        }
        
        ConfigHelper.init("moonlightbay");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UnifiedRegistry.init();
            System.out.println("[Moonlight Bay] 通用设置完成");
        });
    }
}
