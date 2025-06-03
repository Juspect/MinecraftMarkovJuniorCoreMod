// 修改 src/main/java/com/jxon/juscore/gui/tabs/AdvancedOptionsTab.java
package com.jxon.juscore.gui.tabs;

import com.jxon.juscore.config.MarkovWorldConfig;
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

public class AdvancedOptionsTab implements Element, Drawable, Selectable {
    private final MarkovWorldConfig config;
    private final Runnable onConfigChanged;
    private final List<ClickableWidget> children = new ArrayList<>();

    // 截取设置
    private CheckboxWidget enableClippingCheckbox;
    private TextFieldWidget clippingXMinField;
    private TextFieldWidget clippingXMaxField;
    private TextFieldWidget clippingYMinField;
    private TextFieldWidget clippingYMaxField;
    private TextFieldWidget clippingZMinField;
    private TextFieldWidget clippingZMaxField;

    // 系统设置
    private TextFieldWidget cacheLimitField;
    private CheckboxWidget enableChunkPreviewCheckbox;

    private int x, y, width, height;

    public AdvancedOptionsTab(MarkovWorldConfig config, Runnable onConfigChanged) {
        this.config = config;
        this.onConfigChanged = onConfigChanged;
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
        int fieldWidth = 80;
        int spacing = 25;

        // 截取设置标题
        TextWidget clippingTitle = new TextWidget(x, currentY, width, 20,
                Text.translatable("gui.juscore.advanced.clipping"),
                MinecraftClient.getInstance().textRenderer);
        children.add(clippingTitle);
        currentY += 30;

        // 启用截取
        enableClippingCheckbox = new CheckboxWidget(
                x, currentY, 200, 20,
                Text.translatable("gui.juscore.advanced.enable_clipping"),
                config.isEnableClipping()
        );

//                CheckboxWidget.builder(
//                        Text.translatable("gui.juscore.advanced.enable_clipping"),
//                        MinecraftClient.getInstance().textRenderer)
//                .checked(config.isEnableClipping())
//                .build(x, currentY, 200, 20);
        children.add(enableClippingCheckbox);
        currentY += spacing;

        // X范围
        TextWidget xLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.advanced.x_range"),
                MinecraftClient.getInstance().textRenderer);
        children.add(xLabel);

        clippingXMinField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.x_min"));
        clippingXMinField.setText(String.valueOf(config.getClippingXMin()));
        clippingXMinField.setChangedListener(value -> {
            try {
                config.setClippingXMin(Integer.parseInt(value));
                onConfigChanged.run();
            } catch (NumberFormatException ignored) {}
        });
        children.add(clippingXMinField);

        clippingXMaxField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth + fieldWidth + 10, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.x_max"));
        clippingXMaxField.setText(String.valueOf(config.getClippingXMax()));
        clippingXMaxField.setChangedListener(value -> {
            try {
                config.setClippingXMax(Integer.parseInt(value));
                onConfigChanged.run();
            } catch (NumberFormatException ignored) {}
        });
        children.add(clippingXMaxField);
        currentY += spacing;

        // Y范围
        TextWidget yLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.advanced.y_range"),
                MinecraftClient.getInstance().textRenderer);
        children.add(yLabel);

        clippingYMinField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.y_min"));
        clippingYMinField.setText(String.valueOf(config.getClippingYMin()));
        clippingYMinField.setChangedListener(value -> {
            try {
                config.setClippingYMin(Integer.parseInt(value));
                onConfigChanged.run();
            } catch (NumberFormatException ignored) {}
        });
        children.add(clippingYMinField);

        clippingYMaxField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth + fieldWidth + 10, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.y_max"));
        clippingYMaxField.setText(String.valueOf(config.getClippingYMax()));
        clippingYMaxField.setChangedListener(value -> {
            try {
                config.setClippingYMax(Integer.parseInt(value));
                onConfigChanged.run();
            } catch (NumberFormatException ignored) {}
        });
        children.add(clippingYMaxField);
        currentY += spacing;

        // Z范围
        TextWidget zLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.advanced.z_range"),
                MinecraftClient.getInstance().textRenderer);
        children.add(zLabel);

        clippingZMinField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.z_min"));
        clippingZMinField.setText(String.valueOf(config.getClippingZMin()));
        clippingZMinField.setChangedListener(value -> {
            try {
                config.setClippingZMin(Integer.parseInt(value));
                onConfigChanged.run();
            } catch (NumberFormatException ignored) {}
        });
        children.add(clippingZMinField);

        clippingZMaxField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth + fieldWidth + 10, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.z_max"));
        clippingZMaxField.setText(String.valueOf(config.getClippingZMax()));
        clippingZMaxField.setChangedListener(value -> {
            try {
                config.setClippingZMax(Integer.parseInt(value));
                onConfigChanged.run();
            } catch (NumberFormatException ignored) {}
        });
        children.add(clippingZMaxField);
        currentY += spacing + 20;

        // 系统设置标题
        TextWidget systemTitle = new TextWidget(x, currentY, width, 20,
                Text.translatable("gui.juscore.advanced.system"),
                MinecraftClient.getInstance().textRenderer);
        children.add(systemTitle);
        currentY += 25;

        // 缓存限制
        TextWidget cacheLabel = new TextWidget(x, currentY, labelWidth, 20,
                Text.translatable("gui.juscore.advanced.cache_limit"),
                MinecraftClient.getInstance().textRenderer);
        children.add(cacheLabel);

        cacheLimitField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + labelWidth, currentY, fieldWidth, 20, Text.translatable("gui.juscore.advanced.cache_limit"));
        cacheLimitField.setText(String.valueOf(config.getCacheLimit()));
        cacheLimitField.setChangedListener(value -> {
            try {
                int limit = Integer.parseInt(value);
                if (limit > 0) {
                    config.setCacheLimit(limit);
                    onConfigChanged.run();
                }
            } catch (NumberFormatException ignored) {}
        });
        children.add(cacheLimitField);
        currentY += spacing;

        // 区块预览
        enableChunkPreviewCheckbox = new CheckboxWidget(
                x, currentY, 300, 20,
                Text.translatable("gui.juscore.advanced.chunk_preview"),
                config.isEnableChunkPreview()
        );

//                CheckboxWidget.builder(
//                        Text.translatable("gui.juscore.advanced.chunk_preview"),
//                        MinecraftClient.getInstance().textRenderer)
//                .checked(config.isEnableChunkPreview())
//                .build(x, currentY, 300, 20);
        children.add(enableChunkPreviewCheckbox);

        updateClippingFieldStates();
    }

    // 手动处理复选框事件
    public void handleCheckboxChanges() {
        config.setEnableClipping(enableClippingCheckbox.isChecked());
        config.setEnableChunkPreview(enableChunkPreviewCheckbox.isChecked());
        updateClippingFieldStates();
        onConfigChanged.run();
    }

    private void updateClippingFieldStates() {
        boolean enabled = config.isEnableClipping();
        clippingXMinField.active = enabled;
        clippingXMaxField.active = enabled;
        clippingYMinField.active = enabled;
        clippingYMaxField.active = enabled;
        clippingZMinField.active = enabled;
        clippingZMaxField.active = enabled;
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
        boolean result = false;
        for (ClickableWidget child : children) {
            if (child.visible && child.mouseClicked(mouseX, mouseY, button)) {
                result = true;
            }
        }
        // 检查复选框变化
        if (result) {
            handleCheckboxChanges();
        }
        return result;
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