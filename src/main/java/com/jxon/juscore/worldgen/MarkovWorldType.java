// src/main/java/com/jxon/juscore/worldgen/MarkovWorldType.java
package com.jxon.juscore.worldgen;

import com.jxon.juscore.Juscore;
import com.jxon.juscore.config.MarkovWorldConfig;
import com.jxon.juscore.gui.MarkovConfigScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

public class MarkovWorldType {
    public static final Identifier MARKOV_WORLD_ID = new Identifier(Juscore.MOD_ID, "markov_world");
    public static final RegistryKey<DimensionType> MARKOV_DIMENSION_TYPE =
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, MARKOV_WORLD_ID);

    // 存储当前的配置
    private static MarkovWorldConfig currentConfig = new MarkovWorldConfig();

    public static void register() {
        // 注册维度类型
        registerDimensionType();

        // 注册世界类型按钮处理
        registerWorldTypeHandler();

        Juscore.LOGGER.info("Markov World Type registered");
    }

    private static void registerDimensionType() {
        // 这里需要在数据包中定义维度类型
        // 暂时使用代码注册的方式
    }

    private static void registerWorldTypeHandler() {
        // 监听创建世界界面的事件
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof CreateWorldScreen createWorldScreen) {
                // 添加我们的世界类型处理逻辑
                handleCreateWorldScreen(createWorldScreen);
            }
        });
    }

    private static void handleCreateWorldScreen(CreateWorldScreen screen) {
        // 这里需要通过反射或者mixin来添加我们的世界类型选项
        // 具体实现需要根据Minecraft的GUI结构来调整
    }

    public static MarkovWorldConfig getCurrentConfig() {
        return currentConfig;
    }

    public static void setCurrentConfig(MarkovWorldConfig config) {
        currentConfig = config;
    }

    // 打开配置界面
    public static void openConfigScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new MarkovConfigScreen(client.currentScreen, currentConfig));
    }
}