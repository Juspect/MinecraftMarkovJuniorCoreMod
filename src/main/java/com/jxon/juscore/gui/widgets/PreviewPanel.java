// src/main/java/com/jxon/juscore/gui/widgets/PreviewPanel.java
package com.jxon.juscore.gui.widgets;

import com.jxon.juscore.config.MarkovWorldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class PreviewPanel extends ClickableWidget {
    private final MarkovWorldConfig config;
    private boolean generating = false;

    public PreviewPanel(int x, int y, int width, int height, MarkovWorldConfig config) {
        super(x, y, width, height, Text.translatable("gui.juscore.preview"));
        this.config = config;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制边框
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFF666666);

        // 绘制内容
        String text = generating ? "Generating..." : "Preview will be shown here";
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + getHeight() / 2;

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                text, textX, textY, 0xFFFFFF);
    }

    public void generatePreview() {
        generating = true;
        // 这里暂时只是设置状态，实际预览在第二阶段实现

        // 模拟生成过程
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 模拟生成时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                generating = false;
            }
        }).start();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // 预留点击处理
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}