// 修改 MarkovConfigScreen.java - 简化标签页处理
package com.jxon.juscore.gui;

import com.jxon.juscore.config.MarkovWorldConfig;
import com.jxon.juscore.gui.tabs.ModelConfigTab;
import com.jxon.juscore.gui.tabs.TilingConfigTab;
import com.jxon.juscore.gui.tabs.AdvancedOptionsTab;
import com.jxon.juscore.gui.widgets.PreviewPanel;
import com.jxon.juscore.worldgen.MarkovWorldType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class MarkovConfigScreen extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("textures/gui/options_background.png");

    private final Screen parent;
    private final MarkovWorldConfig config;

    // 简化的标签页管理
    private enum TabType {
        MODEL, TILING, ADVANCED
    }

    private TabType currentTab = TabType.MODEL;
    private Map<TabType, ButtonWidget> tabButtons = new HashMap<>();

    private ModelConfigTab modelTab;
    private TilingConfigTab tilingTab;
    private AdvancedOptionsTab advancedTab;

    // 预览面板
    private PreviewPanel previewPanel;

    // 控制按钮
    private ButtonWidget doneButton;
    private ButtonWidget cancelButton;
    private ButtonWidget previewButton;

    private int leftPanelWidth;
    private int rightPanelWidth;

    public MarkovConfigScreen(Screen parent, MarkovWorldConfig config) {
        super(Text.translatable("gui.juscore.markov_config.title"));
        this.parent = parent;
        this.config = new MarkovWorldConfig(); // 创建配置副本
        copyConfig(config, this.config);
    }

    @Override
    protected void init() {
        super.init();

        // 计算面板尺寸
        leftPanelWidth = (int) (this.width * 0.6f); // 60%给配置
        rightPanelWidth = this.width - leftPanelWidth - 20; // 剩余给预览

        // 初始化标签页按钮
        initializeTabButtons();

        // 初始化标签页内容
        initializeTabs();

        // 初始化预览面板
        initializePreview();

        // 初始化控制按钮
        initializeButtons();

        // 显示默认标签页
        showTab(currentTab);
    }

    private void initializeTabButtons() {
        int buttonWidth = (leftPanelWidth - 30) / 3;
        int startX = 10;
        int buttonY = 30;

        // 模型配置标签
        ButtonWidget modelButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.tab.model"),
                button -> showTab(TabType.MODEL)
        ).dimensions(startX, buttonY, buttonWidth, 20).build();
        tabButtons.put(TabType.MODEL, modelButton);
        this.addDrawableChild(modelButton);

        // 平铺设置标签
        ButtonWidget tilingButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.tab.tiling"),
                button -> showTab(TabType.TILING)
        ).dimensions(startX + buttonWidth, buttonY, buttonWidth, 20).build();
        tabButtons.put(TabType.TILING, tilingButton);
        this.addDrawableChild(tilingButton);

        // 高级选项标签
        ButtonWidget advancedButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.tab.advanced"),
                button -> showTab(TabType.ADVANCED)
        ).dimensions(startX + 2 * buttonWidth, buttonY, buttonWidth, 20).build();
        tabButtons.put(TabType.ADVANCED, advancedButton);
        this.addDrawableChild(advancedButton);
    }

    private void initializeTabs() {
        int contentY = 60;
        int contentHeight = this.height - contentY - 60;

        // 创建各个标签页
        modelTab = new ModelConfigTab(config, this::onConfigChanged);
        modelTab.setPosition(10, contentY, leftPanelWidth - 20, contentHeight);

        tilingTab = new TilingConfigTab(config, this::onConfigChanged);
        tilingTab.setPosition(10, contentY, leftPanelWidth - 20, contentHeight);

        advancedTab = new AdvancedOptionsTab(config, this::onConfigChanged);
        advancedTab.setPosition(10, contentY, leftPanelWidth - 20, contentHeight);
    }

    private void showTab(TabType tab) {
        // 隐藏所有标签页
        if (modelTab != null) {
            modelTab.getChildren().forEach(this::remove);
        }
        if (tilingTab != null) {
            tilingTab.getChildren().forEach(this::remove);
        }
        if (advancedTab != null) {
            advancedTab.getChildren().forEach(this::remove);
        }

        // 显示选中的标签页
        currentTab = tab;
        switch (tab) {
            case MODEL -> {
                this.addDrawable(modelTab);
//                this.addSelectable(modelTab);
                modelTab.getChildren().forEach(this::addDrawableChild);
            }
            case TILING -> {
                this.addDrawable(tilingTab);
//                this.addSelectable(tilingTab);
                tilingTab.getChildren().forEach(this::addDrawableChild);
            }
            case ADVANCED -> {
                this.addDrawable(advancedTab);
//                this.addSelectable(advancedTab);
                advancedTab.getChildren().forEach(this::addDrawableChild);
            }
        }

        // 更新按钮状态
        updateTabButtonStates();
    }


    private void updateTabButtonStates() {
        for (Map.Entry<TabType, ButtonWidget> entry : tabButtons.entrySet()) {
            ButtonWidget button = entry.getValue();
            // 简单的视觉反馈：禁用当前选中的按钮
            button.active = (entry.getKey() != currentTab);
        }
    }

    private void initializePreview() {
        int previewX = leftPanelWidth + 10;
        int previewY = 30;
        int previewHeight = this.height - previewY - 60;

        previewPanel = new PreviewPanel(previewX, previewY, rightPanelWidth, previewHeight, config);
        this.addDrawableChild(previewPanel);
    }

    private void initializeButtons() {
        int buttonY = this.height - 30;
        int buttonWidth = 80;

        // 预览按钮
        previewButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.button.preview"),
                button -> generatePreview()
        ).dimensions(10, buttonY, buttonWidth, 20).build();

        // 取消按钮
        cancelButton = ButtonWidget.builder(
                Text.translatable("gui.common.cancel"),
                button -> close()
        ).dimensions(this.width - 2 * buttonWidth - 10, buttonY, buttonWidth, 20).build();

        // 完成按钮
        doneButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> applyAndClose()
        ).dimensions(this.width - buttonWidth, buttonY, buttonWidth, 20).build();

        this.addDrawableChild(previewButton);
        this.addDrawableChild(cancelButton);
        this.addDrawableChild(doneButton);

        updateButtonStates();
    }

    private void onConfigChanged() {
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean configValid = config.isValid();
        previewButton.active = configValid;
        doneButton.active = configValid;
    }

    private void generatePreview() {
        if (config.isValid()) {
            previewPanel.generatePreview();
        }
    }

    private void applyAndClose() {
        if (config.isValid()) {
            // 应用配置
            MarkovWorldType.setCurrentConfig(config);
            close();
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制背景
        renderBackground(context);

        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, title, this.width / 2, 10, 0xFFFFFF);

        // 绘制分割线
        int separatorX = leftPanelWidth + 5;
        context.fill(separatorX, 30, separatorX + 1, this.height - 30, 0xFF666666);

        // 绘制子组件
        super.render(context, mouseX, mouseY, delta);

        // 绘制预览标题
        int previewTitleX = leftPanelWidth + 15;
        context.drawTextWithShadow(textRenderer,
                Text.translatable("gui.juscore.preview.title"),
                previewTitleX, 35, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void copyConfig(MarkovWorldConfig source, MarkovWorldConfig target) {
        target.setModelSource(source.getModelSource());
        target.setSelectedTemplate(source.getSelectedTemplate());
        target.setXmlContent(source.getXmlContent());
        target.setModelLength(source.getModelLength());
        target.setModelWidth(source.getModelWidth());
        target.setModelHeight(source.getModelHeight());
        target.setMaxStep(source.getMaxStep());
        target.setTilingX(source.isTilingX());
        target.setTilingY(source.isTilingY());
        target.setTilingZ(source.isTilingZ());
        target.setBoundaryX(source.getBoundaryX());
        target.setBoundaryY(source.getBoundaryY());
        target.setBoundaryZ(source.getBoundaryZ());
        target.setBlockMapping(new HashMap<>(source.getBlockMapping()));
        target.setEnableClipping(source.isEnableClipping());
    }
}