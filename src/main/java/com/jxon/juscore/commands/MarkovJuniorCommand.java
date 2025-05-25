package com.jxon.juscore.commands;

import com.jxon.juscore.Juscore;
import com.jxon.juscore.generator.MarkovJuniorGenerator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * MarkovJunior 指令处理类
 */
public class MarkovJuniorCommand {

    /**
     * 注册指令
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mj")
                .requires(source -> source.hasPermissionLevel(2)) // 需要OP权限

                // /mj help - 显示帮助
                .then(CommandManager.literal("help")
                        .executes(MarkovJuniorCommand::executeHelp))

                // /mj reload - 重新加载配置
                .then(CommandManager.literal("reload")
                        .executes(MarkovJuniorCommand::executeReload))

                // /mj list - 列出可用模板
                .then(CommandManager.literal("list")
                        .executes(MarkovJuniorCommand::executeList))

                // /mj size <size> <dimension> <xml_or_template> [seed]
                .then(CommandManager.literal("size")
                        .then(CommandManager.argument("size", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("dimension", IntegerArgumentType.integer(2, 3))
                                        .then(CommandManager.argument("xml_or_template", StringArgumentType.greedyString())
                                                .executes(MarkovJuniorCommand::executeSizeRandom)
                                                .then(CommandManager.argument("seed", IntegerArgumentType.integer())
                                                        .executes(MarkovJuniorCommand::executeSizeWithSeed))))))

                // /mj <length> <width> <height> <xml_or_template> [seed]
                .then(CommandManager.argument("length", IntegerArgumentType.integer(1))
                        .then(CommandManager.argument("width", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("height", IntegerArgumentType.integer(1))
                                        .then(CommandManager.argument("xml_or_template", StringArgumentType.greedyString())
                                                .executes(MarkovJuniorCommand::executeDimensionsRandom)
                                                .then(CommandManager.argument("seed", IntegerArgumentType.integer())
                                                        .executes(MarkovJuniorCommand::executeDimensionsWithSeed))))))
        );
    }

    /**
     * 显示帮助信息
     */
    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== MarkovJunior Commands ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/mj help - Show this help").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/mj reload - Reload configuration").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/mj list - List available templates").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/mj size <size> <d> <xml|template> [seed]").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("  - Generate cube structure (d=2 for 2D, d=3 for 3D)").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("/mj <length> <width> <height> <xml|template> [seed]").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("  - Generate with specific dimensions").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Examples:").formatted(Formatting.AQUA), false);
        source.sendFeedback(() -> Text.literal("  /mj 20 2 cave").formatted(Formatting.GREEN), false);
        source.sendFeedback(() -> Text.literal("  /mj 10 15 5 dungeon 12345").formatted(Formatting.GREEN), false);
        source.sendFeedback(() -> Text.literal("  /mj size 25 3 <one><rule in='B' out='W'/></one>").formatted(Formatting.GREEN), false);

        return 1;
    }

    /**
     * 重新加载配置
     */
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            Juscore.reloadConfig();
            source.sendFeedback(() -> Text.literal("MarkovJunior configuration reloaded!").formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to reload configuration: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    /**
     * 列出可用模板
     */
    private static int executeList(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        var config = Juscore.getConfig();

        source.sendFeedback(() -> Text.literal("=== Available Templates ===").formatted(Formatting.GOLD), false);

        if (config.templates.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No templates found. Check your config!").formatted(Formatting.RED), false);
        } else {
            config.templates.keySet().forEach(name -> {
                source.sendFeedback(() -> Text.literal("- " + name).formatted(Formatting.YELLOW), false);
            });
        }

        return 1;
    }

    /**
     * 执行size指令（随机种子）
     */
    private static int executeSizeRandom(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        int dimension = IntegerArgumentType.getInteger(context, "dimension");
        String xmlOrTemplate = StringArgumentType.getString(context, "xml_or_template");
        int seed = new Random().nextInt();

        return executeGeneration(context, size, size, dimension == 2 ? 1 : size, xmlOrTemplate, seed);
    }

    /**
     * 执行size指令（指定种子）
     */
    private static int executeSizeWithSeed(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        int dimension = IntegerArgumentType.getInteger(context, "dimension");
        String xmlOrTemplate = StringArgumentType.getString(context, "xml_or_template");
        int seed = IntegerArgumentType.getInteger(context, "seed");

        return executeGeneration(context, size, size, dimension == 2 ? 1 : size, xmlOrTemplate, seed);
    }

    /**
     * 执行dimensions指令（随机种子）
     */
    private static int executeDimensionsRandom(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int length = IntegerArgumentType.getInteger(context, "length");
        int width = IntegerArgumentType.getInteger(context, "width");
        int height = IntegerArgumentType.getInteger(context, "height");
        String xmlOrTemplate = StringArgumentType.getString(context, "xml_or_template");
        int seed = new Random().nextInt();

        return executeGeneration(context, length, width, height, xmlOrTemplate, seed);
    }

    /**
     * 执行dimensions指令（指定种子）
     */
    private static int executeDimensionsWithSeed(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int length = IntegerArgumentType.getInteger(context, "length");
        int width = IntegerArgumentType.getInteger(context, "width");
        int height = IntegerArgumentType.getInteger(context, "height");
        String xmlOrTemplate = StringArgumentType.getString(context, "xml_or_template");
        int seed = IntegerArgumentType.getInteger(context, "seed");

        return executeGeneration(context, length, width, height, xmlOrTemplate, seed);
    }

    /**
     * 执行生成操作
     */
    private static int executeGeneration(CommandContext<ServerCommandSource> context,
                                         int length, int width, int height,
                                         String xmlOrTemplate, int seed) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        var config = Juscore.getConfig();

        // 验证尺寸
        if (!config.isValidSize(Math.max(Math.max(length, width), height))) {
            source.sendError(Text.literal("Size too large! Maximum: " + config.maxSize).formatted(Formatting.RED));
            return 0;
        }

        // 获取玩家位置
        ServerPlayerEntity player = source.getPlayerOrThrow();
        BlockPos pos = player.getBlockPos();

        // 发送开始消息
        source.sendFeedback(() -> Text.literal("Starting MarkovJunior generation...")
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("Dimensions: " + length + "x" + width + "x" + height)
                .formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("Seed: " + seed)
                .formatted(Formatting.GRAY), false);

        // 异步执行生成
        MarkovJuniorGenerator.generateAsync(
                source.getWorld(),
                pos,
                length, width, height,
                xmlOrTemplate,
                seed,
                result -> {
                    if (result.success()) {
                        source.sendFeedback(() -> Text.literal("Generation completed! Placed " + result.blocksPlaced() + " blocks.")
                                .formatted(Formatting.GREEN), true);
                        if (config.enableDebug) {
                            source.sendFeedback(() -> Text.literal("Generation took " + result.executionTime() + "ms")
                                    .formatted(Formatting.GRAY), false);
                        }
                    } else {
                        source.sendError(Text.literal("Generation failed: " + result.error())
                                .formatted(Formatting.RED));
                    }
                }
        );

        return 1;
    }
}