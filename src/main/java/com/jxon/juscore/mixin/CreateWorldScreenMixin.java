// src/main/java/com/jxon/juscore/mixin/CreateWorldScreenMixin.java
package com.jxon.juscore.mixin;

import com.jxon.juscore.worldgen.MarkovWorldType;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    @Shadow @Final private WorldCreator worldCreator;

    @Inject(method = "init", at = @At("TAIL"))
    private void addMarkovWorldButton(CallbackInfo ci) {
        CreateWorldScreen screen = (CreateWorldScreen)(Object)this;

        // 添加Markov World按钮（简化实现，直接添加一个按钮）
        ButtonWidget markovButton = ButtonWidget.builder(
                Text.translatable("selectWorld.worldType.markov"),
                button -> MarkovWorldType.openConfigScreen()
        ).dimensions(screen.width / 2 - 155, 151, 150, 20).build();

        screen.addDrawableChild(markovButton);
    }
}