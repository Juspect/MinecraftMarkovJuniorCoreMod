package com.jxon.juscore.generator;

import com.jxon.juscore.Juscore;
import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.Program;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * MarkovJunior 生成器
 * 负责执行生成算法并将结果应用到世界中
 */
public class MarkovJuniorGenerator {

    // 方块映射表
    private static final Map<Character, Block> BLOCK_MAPPING = new HashMap<>();

    static {
        initializeBlockMapping();
    }

    /**
     * 初始化方块映射
     */
    private static void initializeBlockMapping() {
        // 基础方块
        BLOCK_MAPPING.put('A', Blocks.AIR);
        BLOCK_MAPPING.put('S', Blocks.STONE);
        BLOCK_MAPPING.put('D', Blocks.DIRT);
        BLOCK_MAPPING.put('G', Blocks.GRASS_BLOCK);
        BLOCK_MAPPING.put('W', Blocks.WATER);
        BLOCK_MAPPING.put('B', Blocks.STONE_BRICKS);

        // 建筑方块
        BLOCK_MAPPING.put('F', Blocks.OAK_PLANKS);          // 地板
        BLOCK_MAPPING.put('R', Blocks.STONE_BRICKS);        // 房间/墙
        BLOCK_MAPPING.put('C', Blocks.CHEST);               // 宝箱
        BLOCK_MAPPING.put('P', Blocks.COBBLESTONE);         // 路径
        BLOCK_MAPPING.put('E', Blocks.OAK_DOOR);            // 门

        // 装饰方块
        BLOCK_MAPPING.put('T', Blocks.TORCH);               // 火把
        BLOCK_MAPPING.put('L', Blocks.LADDER);              // 梯子
        BLOCK_MAPPING.put('H', Blocks.HAY_BLOCK);           // 干草

        // 矿物方块
        BLOCK_MAPPING.put('I', Blocks.IRON_ORE);            // 铁矿
        BLOCK_MAPPING.put('O', Blocks.GOLD_ORE);            // 金矿
        BLOCK_MAPPING.put('M', Blocks.DIAMOND_ORE);         // 钻石矿

        // 其他
        BLOCK_MAPPING.put('X', Blocks.BEDROCK);             // 基岩
        BLOCK_MAPPING.put('N', Blocks.NETHERRACK);          // 下界岩
        BLOCK_MAPPING.put('Y', Blocks.GLOWSTONE);           // 荧石
        BLOCK_MAPPING.put('U', Blocks.DEEPSLATE);           // 深板岩
        BLOCK_MAPPING.put('V', Blocks.MOSS_BLOCK);          // 苔藓块
        BLOCK_MAPPING.put('Z', Blocks.SANDSTONE);           // 砂岩
    }

    /**
     * 异步生成结构
     */
    public static void generateAsync(ServerWorld world, BlockPos center,
                                     int length, int width, int height,
                                     String xmlOrTemplate, int seed,
                                     Consumer<GenerationResult> callback) {

        var config = Juscore.getConfig();

        CompletableFuture.supplyAsync(() -> {
                    long startTime = System.currentTimeMillis();

                    try {
                        // 解析XML或获取模板
                        String xml = resolveXmlOrTemplate(xmlOrTemplate);
                        if (xml == null) {
                            return new GenerationResult(false, 0, 0, "Unknown template or invalid XML: " + xmlOrTemplate);
                        }

                        // 执行MarkovJunior生成
                        byte[] result = Program.generateSimpleGrid(xml, length, width, seed, config.maxSteps);
                        if (result == null || result.length == 0) {
                            return new GenerationResult(false, 0, 0, "Generation failed - no output");
                        }

                        // 应用到世界中
                        int blocksPlaced = applyToWorld(world, center, result, length, width, height);

                        long executionTime = System.currentTimeMillis() - startTime;
                        return new GenerationResult(true, blocksPlaced, executionTime, null);

                    } catch (Exception e) {
                        Juscore.LOGGER.error("Generation error", e);
                        long executionTime = System.currentTimeMillis() - startTime;
                        return new GenerationResult(false, 0, executionTime, e.getMessage());
                    }
                }).orTimeout(config.asyncTimeout, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        callback.accept(new GenerationResult(false, 0, 0, "Generation timeout or error: " + throwable.getMessage()));
                    } else {
                        callback.accept(result);
                    }
                });
    }

    /**
     * 解析XML或模板名称
     */
    private static String resolveXmlOrTemplate(String xmlOrTemplate) {
        var config = Juscore.getConfig();

        // 如果是模板名称，获取模板内容
        if (config.templates.containsKey(xmlOrTemplate)) {
            return config.templates.get(xmlOrTemplate);
        }

        // 如果包含XML标签，直接使用
        if (xmlOrTemplate.trim().startsWith("<")) {
            return xmlOrTemplate;
        }

        // 尝试作为内联XML解析
        if (!xmlOrTemplate.contains("<?xml")) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlOrTemplate;
        }

        return xmlOrTemplate;
    }

    /**
     * 将生成结果应用到Minecraft世界中
     */
    private static int applyToWorld(ServerWorld world, BlockPos center,
                                    byte[] result, int length, int width, int height) {
        int blocksPlaced = 0;
        int halfLength = length / 2;
        int halfWidth = width / 2;

        // 获取虚拟的字符映射（假设结果使用简单的字符映射）
        char[] virtualLegend = {'A', 'S', 'D', 'G', 'W', 'B', 'F', 'R', 'C', 'P', 'E', 'T', 'L', 'H', 'I', 'O'};

        for (int i = 0; i < result.length && i < length * width * height; i++) {
            int x = i % length;
            int y = (i / length) % height;
            int z = i / (length * height);

            BlockPos pos = center.add(x - halfLength, y, z - halfWidth);

            // 确保位置合理
            if (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopY()) {
                continue;
            }

            // 获取方块类型
            byte value = result[i];
            if (value >= 0 && value < virtualLegend.length) {
                char legend = virtualLegend[value];
                Block block = BLOCK_MAPPING.getOrDefault(legend, Blocks.STONE);

                // 只放置非空气方块（除非明确指定）
                if (block != Blocks.AIR || world.getBlockState(pos).getBlock() != Blocks.AIR) {
                    world.setBlockState(pos, block.getDefaultState());
                    blocksPlaced++;
                }
            }
        }

        return blocksPlaced;
    }

    /**
     * 高级生成方法 - 使用完整的Interpreter
     */
    public static void generateAdvancedAsync(ServerWorld world, BlockPos center,
                                             int length, int width, int height,
                                             String xmlContent, int seed, int steps,
                                             Consumer<GenerationResult> callback) {

        CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // 创建解释器
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

                Interpreter interpreter = Interpreter.load(doc.getDocumentElement(), length, width, height);
                if (interpreter == null) {
                    return new GenerationResult(false, 0, 0, "Failed to create interpreter");
                }

                // 执行生成
                Interpreter.RunResult lastResult = null;
                int stepCount = 0;
                for (Interpreter.RunResult result : interpreter.run(seed, steps, false)) {
                    lastResult = result;
                    stepCount++;
                    if (stepCount > steps) break;
                }

                if (lastResult == null) {
                    return new GenerationResult(false, 0, 0, "No generation result");
                }

                // 应用到世界
                int blocksPlaced = applyAdvancedToWorld(world, center, lastResult);

                long executionTime = System.currentTimeMillis() - startTime;
                return new GenerationResult(true, blocksPlaced, executionTime, null);

            } catch (Exception e) {
                Juscore.LOGGER.error("Advanced generation error", e);
                long executionTime = System.currentTimeMillis() - startTime;
                return new GenerationResult(false, 0, executionTime, e.getMessage());
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                callback.accept(new GenerationResult(false, 0, 0, "Generation error: " + throwable.getMessage()));
            } else {
                callback.accept(result);
            }
        });
    }

    /**
     * 应用高级生成结果到世界
     */
    private static int applyAdvancedToWorld(ServerWorld world, BlockPos center, Interpreter.RunResult result) {
        int blocksPlaced = 0;
        int halfX = result.FX / 2;
        int halfZ = result.FZ / 2;

        for (int i = 0; i < result.state.length; i++) {
            int x = i % result.FX;
            int y = (i % (result.FX * result.FY)) / result.FX;
            int z = i / (result.FX * result.FY);

            BlockPos pos = center.add(x - halfX, y, z - halfZ);

            // 边界检查
            if (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopY()) {
                continue;
            }

            // 获取字符并转换为方块
            byte stateValue = result.state[i];
            if (stateValue >= 0 && stateValue < result.legend.length) {
                char legend = result.legend[stateValue];
                Block block = BLOCK_MAPPING.getOrDefault(legend, Blocks.STONE);

                if (block != Blocks.AIR) {
                    world.setBlockState(pos, block.getDefaultState());
                    blocksPlaced++;
                }
            }
        }

        return blocksPlaced;
    }

    /**
     * 添加自定义方块映射
     */
    public static void addBlockMapping(char character, Block block) {
        BLOCK_MAPPING.put(character, block);
    }

    /**
     * 获取方块映射
     */
    public static Map<Character, Block> getBlockMapping() {
        return new HashMap<>(BLOCK_MAPPING);
    }

    /**
     * 生成结果记录
     */
    public record GenerationResult(boolean success, int blocksPlaced, long executionTime, String error) {}
}