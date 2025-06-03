// 修改 src/main/java/com/jxon/juscore/gui/tabs/ModelConfigTab.java
package com.jxon.juscore.gui.tabs;

import com.jxon.juscore.config.MarkovWorldConfig;
import com.jxon.juscore.gui.widgets.TreeFileWidget;
import com.jxon.juscore.gui.widgets.XmlInputDialog;
import com.jxon.juscore.resources.ResourceManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.List;
import java.util.ArrayList;

public class ModelConfigTab implements Element, Drawable, Selectable {
    private final MarkovWorldConfig config;
    private final Runnable onConfigChanged;
    private final List<ClickableWidget> children = new ArrayList<>();

    // 模型来源选择
    private CyclingButtonWidget<MarkovWorldConfig.ModelSource> sourceButton;
    private ButtonWidget browseFileButton;
    private ButtonWidget xmlCodeButton;
    private ButtonWidget templateButton;

    // 模型尺寸设置
    private TextFieldWidget lengthField;
    private TextFieldWidget widthField;
    private TextFieldWidget heightField;
    private TextFieldWidget maxStepField;

    // 验证和预览
    private ButtonWidget validateButton;
    private TextWidget statusText;

    private TreeFileWidget fileWidget;
    private boolean showFileWidget = false;
    private List<String> availableTemplates;
    private int currentTemplateIndex = 0;

    private int x, y, width, height;

    public ModelConfigTab(MarkovWorldConfig config, Runnable onConfigChanged) {
        this.config = config;
        this.onConfigChanged = onConfigChanged;
        this.availableTemplates = ResourceManager.getSystemTemplates();
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        initializeWidgets();
    }

    private void initializeWidgets() {
        children.clear();

        int currentY = y + 10;
        int labelWidth = 120;
        int fieldWidth = 100;
        int spacing = 25;

        // 模型来源标题
        TextWidget sourceTitle = new TextWidget(x, currentY, width, 20,
                Text.translatable("gui.juscore.model.source"),
                MinecraftClient.getInstance().textRenderer);
        children.add(sourceTitle);
        currentY += 25;

        // 模型来源选择按钮
        sourceButton = CyclingButtonWidget.builder(this::getSourceText)
                .values(MarkovWorldConfig.ModelSource.values())
                .initially(config.getModelSource())
                .build(x, currentY, 200, 20, Text.translatable("gui.juscore.model.source"));
        // 修复：使用正确的回调方式
        children.add(sourceButton);
        currentY += spacing;

        // 系统模板按钮
        templateButton = ButtonWidget.builder(
                Text.literal(getTemplateButtonText()),
                button -> cycleTemplate()
        ).dimensions(x, currentY, 300, 20).build();
        children.add(templateButton);
        currentY += spacing;

        // 文件浏览按钮
        browseFileButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.model.browse"),
                button -> toggleFileWidget()
        ).dimensions(x, currentY, 150, 20).build();
        browseFileButton.visible = false;
        children.add(browseFileButton);

        // XML代码按钮
        xmlCodeButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.model.xml_code"),
                button -> openXmlDialog()
        ).dimensions(x, currentY, 150, 20).build();
        xmlCodeButton.visible = false;
        children.add(xmlCodeButton);
        currentY += spacing;

        // 文件浏览器组件
        fileWidget = new TreeFileWidget(x, currentY, 400, 200, this::onFileSelected);
        fileWidget.visible = false;
        children.add(fileWidget);
        currentY += 200;

        // 模型尺寸设置
        currentY += 20;
        TextWidget dimensionsTitle = new TextWidget(x, currentY, width, 20,
                Text.translatable("gui.juscore.model.dimensions"),
                MinecraftClient.getInstance().textRenderer);
        children.add(dimensionsTitle);
        currentY += 25;

        // 长度
        TextWidget lengthLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.model.length"),
                MinecraftClient.getInstance().textRenderer);
        children.add(lengthLabel);

        lengthField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.model.length"));
        lengthField.setText(String.valueOf(config.getModelLength()));
        lengthField.setChangedListener(this::onLengthChanged);
        children.add(lengthField);
        currentY += spacing;

        // 宽度
        TextWidget widthLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.model.width"),
                MinecraftClient.getInstance().textRenderer);
        children.add(widthLabel);

        widthField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.model.width"));
        widthField.setText(String.valueOf(config.getModelWidth()));
        widthField.setChangedListener(this::onWidthChanged);
        children.add(widthField);
        currentY += spacing;

        // 高度
        TextWidget heightLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.model.height"),
                MinecraftClient.getInstance().textRenderer);
        children.add(heightLabel);

        heightField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.model.height"));
        heightField.setText(String.valueOf(config.getModelHeight()));
        heightField.setChangedListener(this::onHeightChanged);
        children.add(heightField);
        currentY += spacing;

        // MaxStep
        TextWidget maxStepLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.model.max_step"),
                MinecraftClient.getInstance().textRenderer);
        children.add(maxStepLabel);

        maxStepField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.model.max_step"));
        maxStepField.setText(String.valueOf(config.getMaxStep()));
        maxStepField.setChangedListener(this::onMaxStepChanged);
        children.add(maxStepField);
        currentY += spacing;

        // 验证按钮
        validateButton = ButtonWidget.builder(
                Text.translatable("gui.juscore.model.validate"),
                button -> validateXml()
        ).dimensions(x, currentY + 20, 100, 20).build();
        children.add(validateButton);

        // 状态文本
        statusText = new TextWidget(x + 110, currentY + 22, 200, 20,
                Text.translatable("gui.juscore.model.status.ready"),
                MinecraftClient.getInstance().textRenderer);
        children.add(statusText);

        updateVisibility();
    }

    private Text getSourceText(MarkovWorldConfig.ModelSource source) {
        return switch (source) {
            case SYSTEM_TEMPLATE -> Text.translatable("gui.juscore.model.source.template");
            case FILE_BROWSER -> Text.translatable("gui.juscore.model.source.file");
            case XML_CODE -> Text.translatable("gui.juscore.model.source.xml");
        };
    }

    private String getTemplateButtonText() {
        if (!availableTemplates.isEmpty() && currentTemplateIndex < availableTemplates.size()) {
            return availableTemplates.get(currentTemplateIndex);
        }
        return "No templates available";
    }

    private void cycleTemplate() {
        if (!availableTemplates.isEmpty()) {
            currentTemplateIndex = (currentTemplateIndex + 1) % availableTemplates.size();
            templateButton.setMessage(Text.literal(getTemplateButtonText()));
            onTemplateChanged(availableTemplates.get(currentTemplateIndex));
        }
    }

    // 手动处理源切换事件
    public void handleSourceCycle() {
        MarkovWorldConfig.ModelSource currentSource = sourceButton.getValue();
        onSourceChanged(currentSource);
    }

    private void onSourceChanged(MarkovWorldConfig.ModelSource source) {
        config.setModelSource(source);
        updateVisibility();
        onConfigChanged.run();
    }

    private void updateVisibility() {
        MarkovWorldConfig.ModelSource source = config.getModelSource();

        templateButton.visible = (source == MarkovWorldConfig.ModelSource.SYSTEM_TEMPLATE);
        browseFileButton.visible = (source == MarkovWorldConfig.ModelSource.FILE_BROWSER);
        xmlCodeButton.visible = (source == MarkovWorldConfig.ModelSource.XML_CODE);
        fileWidget.visible = showFileWidget && (source == MarkovWorldConfig.ModelSource.FILE_BROWSER);
    }

    private void onTemplateChanged(String template) {
        config.setSelectedTemplate(template);
        loadTemplateXml(template);
        onConfigChanged.run();
    }

    private void loadTemplateXml(String template) {
        String xmlContent = ResourceManager.loadTemplate(template);
        if (xmlContent != null) {
            config.setXmlContent(xmlContent);
            statusText.setMessage(Text.translatable("gui.juscore.model.status.loaded"));
        } else {
            statusText.setMessage(Text.translatable("gui.juscore.model.status.error"));
        }
    }

    private void toggleFileWidget() {
        showFileWidget = !showFileWidget;
        fileWidget.visible = showFileWidget;
        if (showFileWidget) {
            fileWidget.refreshFiles();
        }
    }

    private void onFileSelected(String filePath) {
        String xmlContent = ResourceManager.loadFile(filePath);
        if (xmlContent != null) {
            config.setXmlContent(xmlContent);
            config.setXmlFilePath(filePath);
            statusText.setMessage(Text.translatable("gui.juscore.model.status.loaded"));
            showFileWidget = false;
            fileWidget.visible = false;
            onConfigChanged.run();
        }
    }

    private void openXmlDialog() {
        XmlInputDialog dialog = new XmlInputDialog(config.getXmlContent(), this::onXmlInputComplete);
        MinecraftClient.getInstance().setScreen(dialog);
    }

    private void onXmlInputComplete(String xmlContent) {
        if (xmlContent != null && !xmlContent.trim().isEmpty()) {
            config.setXmlContent(xmlContent);
            statusText.setMessage(Text.translatable("gui.juscore.model.status.loaded"));
            onConfigChanged.run();
        }
    }

    private void onLengthChanged(String value) {
        try {
            int length = Integer.parseInt(value);
            if (length > 0) {
                config.setModelLength(length);
                onConfigChanged.run();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void onWidthChanged(String value) {
        try {
            int width = Integer.parseInt(value);
            if (width > 0) {
                config.setModelWidth(width);
                onConfigChanged.run();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void onHeightChanged(String value) {
        try {
            int height = Integer.parseInt(value);
            if (height > 0) {
                config.setModelHeight(height);
                onConfigChanged.run();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void onMaxStepChanged(String value) {
        try {
            int maxStep = Integer.parseInt(value);
            if (maxStep > 0) {
                config.setMaxStep(maxStep);
                onConfigChanged.run();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void validateXml() {
        String xmlContent = config.getEffectiveXmlContent();
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            statusText.setMessage(Text.translatable("gui.juscore.model.status.no_xml"));
            return;
        }

        if (xmlContent.contains("<sequence") || xmlContent.contains("<markov")) {
            statusText.setMessage(Text.translatable("gui.juscore.model.status.valid"));
        } else {
            statusText.setMessage(Text.translatable("gui.juscore.model.status.invalid"));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (Drawable child : children) {
            if (child instanceof ClickableWidget widget && widget.visible) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ClickableWidget child : children) {
            if (child.visible && child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickableWidget child : children) {
            if (child.visible && child.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (ClickableWidget child : children) {
            if (child.visible && child.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return false;
    }

    public List<ClickableWidget> getChildren() {
        return children;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }
}