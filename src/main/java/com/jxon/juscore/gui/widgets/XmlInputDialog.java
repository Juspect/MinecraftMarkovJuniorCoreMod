// src/main/java/com/jxon/juscore/gui/widgets/XmlInputDialog.java
package com.jxon.juscore.gui.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class XmlInputDialog extends Screen {
    private final Consumer<String> onComplete;
    private final String initialContent;
    private TextFieldWidget textArea;

    public XmlInputDialog(String initialContent, Consumer<String> onComplete) {
        super(Text.translatable("gui.juscore.xml_input"));
        this.initialContent = initialContent != null ? initialContent : "";
        this.onComplete = onComplete;
    }

    @Override
    protected void init() {
        super.init();

        int dialogWidth = Math.min(this.width - 40, 600);
        int dialogHeight = Math.min(this.height - 40, 400);
        int dialogX = (this.width - dialogWidth) / 2;
        int dialogY = (this.height - dialogHeight) / 2;

        // 文本输入区域
        textArea = new TextFieldWidget(textRenderer, dialogX + 10, dialogY + 30,
                dialogWidth - 20, dialogHeight - 80, Text.translatable("gui.juscore.xml_content"));
        textArea.setText(initialContent);
        textArea.setMaxLength(65536); // 允许长文本
        addDrawableChild(textArea);

        // 确定按钮
        ButtonWidget okButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> {
                    onComplete.accept(textArea.getText());
                    close();
                }
        ).dimensions(dialogX + dialogWidth - 160, dialogY + dialogHeight - 30, 70, 20).build();
        addDrawableChild(okButton);

        // 取消按钮
        ButtonWidget cancelButton = ButtonWidget.builder(
                Text.translatable("gui.common.cancel"),
                button -> {
                    onComplete.accept(null);
                    close();
                }
        ).dimensions(dialogX + dialogWidth - 80, dialogY + dialogHeight - 30, 70, 20).build();
        addDrawableChild(cancelButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int dialogWidth = Math.min(this.width - 40, 600);
        int dialogHeight = Math.min(this.height - 40, 400);
        int dialogX = (this.width - dialogWidth) / 2;
        int dialogY = (this.height - dialogHeight) / 2;

        // 绘制对话框背景
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xCC000000);
        context.drawBorder(dialogX, dialogY, dialogWidth, dialogHeight, 0xFFFFFFFF);

        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, title,
                dialogX + dialogWidth / 2, dialogY + 10, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}