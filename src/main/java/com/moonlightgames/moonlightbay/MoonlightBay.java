package com.moonlightgames.moonlightbay;

import com.moonlightgames.moonlightbay.api.*;
import com.moonlightgames.moonlightbay.platform.LoaderDetector;
import com.moonlightgames.moonlightbay.platform.neoforge.NeoForgeRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import java.nio.file.Paths;

@Mod("moonlightbay")
public class MoonlightBay {
    
    public MoonlightBay() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册所有 DeferredRegister 到事件总线
        NeoForgeRegistries.registerAllToBus(modBus);
        
        modBus.addListener(this::commonSetup);
        
        // ========== 1. 输出启动信息 ==========
        LoaderType loader = LoaderAdapter.getCurrentLoader();
        String version = LoaderDetector.getLoaderVersion();
        System.out.println("[Moonlight Bay] 初始化完成");
        System.out.println("[Moonlight Bay] 检测到加载器: " + loader);
        System.out.println("[Moonlight Bay] 加载器版本: " + version);
        
        // ========== 2. 检查光影 ==========
        if (ShaderAdapter.isShaderAvailable()) {
            System.out.println("[Moonlight Bay] 检测到光影: " + ShaderAdapter.getCurrentShader());
            System.out.println("[Moonlight Bay] 光影版本: " + ShaderAdapter.getShaderVersion());
        }
        
        // ========== 3. 初始化配置系统 ==========
        ConfigHelper.init("moonlightbay");
        System.out.println("[Moonlight Bay] 配置系统已初始化");
        
        // ========== 4. 初始化适配模式管理器 ==========
        AdapterModeManager.init();
        System.out.println("[Moonlight Bay] 适配模式: " + AdapterModeManager.getCurrentMode());
        
        // ========== 5. 初始化子文件夹加载器 ==========
        SubFolderLoader.init();
        
        // ========== 6. 扫描并加载子文件夹中的模组 ==========
        // 注意：这会扫描 mods 文件夹下的所有子文件夹
        try {
            SubFolderLoader.scanAndLoad(Paths.get("mods"));
        } catch (Exception e) {
            System.err.println("[Moonlight Bay] 扫描子文件夹失败: " + e.getMessage());
        }
        
        // ========== 7. 处理待加载队列（需要重启的模组） ==========
        SubFolderLoader.processPendingQueue();
        
        // ========== 8. 记录本模组调用了 Moonlight Bay ==========
        AdapterModeManager.registerCall("moonlightbay");
        
        System.out.println("[Moonlight Bay] 所有组件初始化完成");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 初始化统一注册器
            UnifiedRegistry.init();
            
            // 额外：可以在此时处理一些需要游戏启动后才做的事
            System.out.println("[Moonlight Bay] 通用设置完成");
        });
    }
}