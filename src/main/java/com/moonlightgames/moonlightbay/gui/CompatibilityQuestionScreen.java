package com.moonlightgames.moonlightbay.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 兼容性询问界面
 * 在客户端弹窗询问用户是否适配特定模组
 */
public class CompatibilityQuestionScreen extends Screen {
    
    private final String modName;
    private final String sourceLoader;
    private final String targetLoader;
    private final Runnable onAccept;
    private final Runnable onDeny;
    private final Runnable onRemember;
    
    private Button acceptButton;
    private Button denyButton;
    private Button rememberButton;
    
    // 用户是否已做出选择
    private boolean choiceMade = false;
    private boolean result = false;
    
    public CompatibilityQuestionScreen(String modName, 
                                        String sourceLoader, 
                                        String targetLoader,
                                        Runnable onAccept,
                                        Runnable onDeny,
                                        Runnable onRemember) {
        super(Component.literal("Moonlight Bay - 兼容性询问"));
        this.modName = modName;
        this.sourceLoader = sourceLoader;
        this.targetLoader = targetLoader;
        this.onAccept = onAccept;
        this.onDeny = onDeny;
        this.onRemember = onRemember;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 接受按钮（是，适配运行）
        acceptButton = Button.builder(
            Component.literal("§a是，适配运行"),
            button -> {
                choiceMade = true;
                result = true;
                if (onAccept != null) {
                    onAccept.run();
                }
                onClose();
            }
        ).bounds(centerX - 210, centerY + 50, 120, 20).build();
        
        // 拒绝按钮（否，禁用模组）
        denyButton = Button.builder(
            Component.literal("§c否，禁用模组"),
            button -> {
                choiceMade = true;
                result = false;
                if (onDeny != null) {
                    onDeny.run();
                }
                onClose();
            }
        ).bounds(centerX - 80, centerY + 50, 120, 20).build();
        
        // 记住选择并适配按钮
        rememberButton = Button.builder(
            Component.literal("§e记住并适配"),
            button -> {
                choiceMade = true;
                result = true;
                if (onRemember != null) {
                    onRemember.run();
                }
                if (onAccept != null) {
                    onAccept.run();
                }
                onClose();
            }
        ).bounds(centerX + 50, centerY + 50, 140, 20).build();
        
        addRenderableWidget(acceptButton);
        addRenderableWidget(denyButton);
        addRenderableWidget(rememberButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 标题（黄色）
        guiGraphics.drawCenteredString(
            this.font, 
            Component.literal("§6Moonlight Bay 兼容性检测"), 
            centerX, centerY - 80, 
            0xFFFFFF
        );
        
        // 问题描述
        guiGraphics.drawCenteredString(
            this.font, 
            Component.literal("模组 §e" + modName + "§r 是为 §c" + sourceLoader + "§r 开发的，"), 
            centerX, centerY - 40, 
            0xFFFFFF
        );
        
        guiGraphics.drawCenteredString(
            this.font, 
            Component.literal("而当前运行在 §a" + targetLoader + "§r 环境。"), 
            centerX, centerY - 20, 
            0xFFFFFF
        );
        
        guiGraphics.drawCenteredString(
            this.font, 
            Component.literal("是否尝试适配运行？"), 
            centerX, centerY, 
            0xFFFF00
        );
        
        // 提示信息（小字）
        guiGraphics.drawCenteredString(
            this.font, 
            Component.literal("§7如果适配失败，可在 config/moonlightbay.json 中调整设置"), 
            centerX, centerY + 100, 
            0xFFFFFF
        );
        
        // 调用父类方法渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;  // 弹窗时暂停游戏
    }
    
    @Override
    public void onClose() {
        super.onClose();
    }
    
    /**
     * 是否已做出选择
     */
    public boolean isChoiceMade() {
        return choiceMade;
    }
    
    /**
     * 获取选择结果
     */
    public boolean getResult() {
        return result;
    }
}
