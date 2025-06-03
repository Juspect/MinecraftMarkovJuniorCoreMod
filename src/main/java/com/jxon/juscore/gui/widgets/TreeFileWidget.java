// src/main/java/com/jxon/juscore/gui/widgets/TreeFileWidget.java
package com.jxon.juscore.gui.widgets;

import com.jxon.juscore.resources.ResourceManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class TreeFileWidget extends ClickableWidget {
    private final Consumer<String> onFileSelected;
    private List<String> files;

    public TreeFileWidget(int x, int y, int width, int height, Consumer<String> onFileSelected) {
        super(x, y, width, height, Text.translatable("gui.juscore.file_browser"));
        this.onFileSelected = onFileSelected;
        refreshFiles();
    }

    public void refreshFiles() {
        files = ResourceManager.getUserFiles();
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制边框
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFF666666);

        // 绘制文件列表（简化版）
        int y = getY() + 5;
        for (String file : files) {
            if (y < getY() + getHeight() - 15) {
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                        file, getX() + 5, y, 0xFFFFFF);
                y += 12;
            }
        }

        if (files.isEmpty()) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    "No user files found", getX() + 5, getY() + 20, 0x888888);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // 简化的点击处理
        int index = (int) ((mouseY - getY() - 5) / 12);
        if (index >= 0 && index < files.size()) {
            onFileSelected.accept(files.get(index));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}