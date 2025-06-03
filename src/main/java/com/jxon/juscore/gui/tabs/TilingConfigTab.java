// 修改 src/main/java/com/jxon/juscore/gui/tabs/TilingConfigTab.java
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

public class TilingConfigTab implements Element, Drawable, Selectable {
    private final MarkovWorldConfig config;
    private final Runnable onConfigChanged;
    private final List<ClickableWidget> children = new ArrayList<>();

    // 平铺轴选择
    private CheckboxWidget tilingXCheckbox;
    private CheckboxWidget tilingYCheckbox;
    private CheckboxWidget tilingZCheckbox;

    // 边界间距设置
    private TextFieldWidget boundaryXField;
    private TextFieldWidget boundaryYField;
    private TextFieldWidget boundaryZField;

    // 调试选项
    private CheckboxWidget debugInfoCheckbox;

    private int x, y, width, height;

    public TilingConfigTab(MarkovWorldConfig config, Runnable onConfigChanged) {
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
        int fieldWidth = 80;
        int spacing = 30;

        // 平铺设置标题
        TextWidget tilingTitle = new TextWidget(x, currentY, width, 20,
                Text.translatable("gui.juscore.tiling.title"),
                MinecraftClient.getInstance().textRenderer);
        children.add(tilingTitle);
        currentY += 30;

        // X轴平铺
        tilingXCheckbox = new CheckboxWidget(
                x, currentY, 200, 20,
                Text.translatable("gui.juscore.tiling.x_axis"),
                config.isTilingX()
        );

//                CheckboxWidget.builder(
//                        Text.translatable("gui.juscore.tiling.x_axis"),
//                        MinecraftClient.getInstance().textRenderer)
//                .checked(config.isTilingX())
//                .build(x, currentY, 200, 20);
        children.add(tilingXCheckbox);

        TextWidget boundaryXLabel = new TextWidget(x + 220, currentY, 100, 20,
                Text.translatable("gui.juscore.tiling.boundary"),
                MinecraftClient.getInstance().textRenderer);
        children.add(boundaryXLabel);

        boundaryXField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + 320, currentY, fieldWidth, 20, Text.translatable("gui.juscore.tiling.boundary_x"));
        boundaryXField.setText(String.valueOf(config.getBoundaryX()));
        boundaryXField.setChangedListener(value -> {
            try {
                int boundary = Integer.parseInt(value);
                if (boundary >= 0) {
                    config.setBoundaryX(boundary);
                    onConfigChanged.run();
                }
            } catch (NumberFormatException ignored) {}
        });
        children.add(boundaryXField);
        currentY += spacing;

        // Y轴平铺
        tilingYCheckbox = new CheckboxWidget(x, currentY, 200, 20,Text.translatable("gui.juscore.tiling.y_axis"),config.isTilingY());

//                CheckboxWidget.builder(
//                        Text.translatable("gui.juscore.tiling.y_axis"),
//                        MinecraftClient.getInstance().textRenderer)
//                .checked(config.isTilingY())
//                .build(x, currentY, 200, 20);
        children.add(tilingYCheckbox);

        TextWidget boundaryYLabel = new TextWidget(x + 220, currentY, 100, 20,
                Text.translatable("gui.juscore.tiling.boundary"),
                MinecraftClient.getInstance().textRenderer);
        children.add(boundaryYLabel);

        boundaryYField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + 320, currentY, fieldWidth, 20, Text.translatable("gui.juscore.tiling.boundary_y"));
        boundaryYField.setText(String.valueOf(config.getBoundaryY()));
        boundaryYField.setChangedListener(value -> {
            try {
                int boundary = Integer.parseInt(value);
                if (boundary >= 0) {
                    config.setBoundaryY(boundary);
                    onConfigChanged.run();
                }
            } catch (NumberFormatException ignored) {}
        });
        children.add(boundaryYField);
        currentY += spacing;

        // Z轴平铺
        tilingZCheckbox = new CheckboxWidget(
                x, currentY, 200, 20,
                Text.translatable("gui.juscore.tiling.z_axis"),
                config.isTilingZ()
        );

//                CheckboxWidget.builder(
//                        Text.translatable("gui.juscore.tiling.z_axis"),
//                        MinecraftClient.getInstance().textRenderer)
//                .checked(config.isTilingZ())
//                .build(x, currentY, 200, 20);
        children.add(tilingZCheckbox);

        TextWidget boundaryZLabel = new TextWidget(x + 220, currentY, 100, 20,
                Text.translatable("gui.juscore.tiling.boundary"),
                MinecraftClient.getInstance().textRenderer);
        children.add(boundaryZLabel);

        boundaryZField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x + 320, currentY, fieldWidth, 20, Text.translatable("gui.juscore.tiling.boundary_z"));
        boundaryZField.setText(String.valueOf(config.getBoundaryZ()));
        boundaryZField.setChangedListener(value -> {
            try {
                int boundary = Integer.parseInt(value);
                if (boundary >= 0) {
                    config.setBoundaryZ(boundary);
                    onConfigChanged.run();
                }
            } catch (NumberFormatException ignored) {}
        });
        children.add(boundaryZField);
        currentY += spacing + 20;

        // 调试选项
        TextWidget debugTitle = new TextWidget(x, currentY, width, 20,
                Text.translatable("gui.juscore.tiling.debug"),
                MinecraftClient.getInstance().textRenderer);
        children.add(debugTitle);
        currentY += 25;

        debugInfoCheckbox = new CheckboxWidget(
                x, currentY, 300, 20,
                Text.translatable("gui.juscore.tiling.debug_info"),
                config.isEnableDebugInfo()
        );

//                CheckboxWidget.builder(
//                        Text.translatable("gui.juscore.tiling.debug_info"),
//                        MinecraftClient.getInstance().textRenderer)
//                .checked(config.isEnableDebugInfo())
//                .build(x, currentY, 300, 20);
        children.add(debugInfoCheckbox);

        updateBoundaryFieldStates();
    }

    // 手动处理复选框事件
    public void handleCheckboxChanges() {
        config.setTilingX(tilingXCheckbox.isChecked());
        config.setTilingY(tilingYCheckbox.isChecked());
        config.setTilingZ(tilingZCheckbox.isChecked());
        config.setEnableDebugInfo(debugInfoCheckbox.isChecked());
        updateBoundaryFieldStates();
        onConfigChanged.run();
    }

    private void updateBoundaryFieldStates() {
        boundaryXField.active = config.isTilingX();
        boundaryYField.active = config.isTilingY();
        boundaryZField.active = config.isTilingZ();
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