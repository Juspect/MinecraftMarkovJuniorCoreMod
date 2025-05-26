// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)
// Minecraft Integration Example for MarkovJunior Java Implementation

package com.jxon.juscore.mjcore;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Minecraft集成示例类
 * 展示如何在Minecraft Fabric Mod中使用MarkovJunior Java实现
 */
public class MinecraftIntegration {

    // 预定义的生成规则模板
    private static final Map<String, String> GENERATION_TEMPLATES = new HashMap<>();
    
    static {
        // 简单的洞穴生成
        GENERATION_TEMPLATES.put("cave", """
            <sequence values="SR" origin="false">
                <one steps="200">
                    <rule in="S" out="R" p="0.3"/>
                </one>
                <all>
                    <rule in="SSS/SRS/SSS" out="***/*R*/***"/>
                </all>
            </sequence>
            """);
        
        // 地牢生成
        GENERATION_TEMPLATES.put("dungeon", """
            <markov values="WFDC">
                <sequence>
                    <one steps="100">
                        <rule in="W" out="F"/>
                    </one>
                    <one steps="50">
                        <rule in="FF" out="FD" p="0.1"/>
                        <rule in="FFF/FFF/FFF" out="***/*C*/***" p="0.05"/>
                    </one>
                </sequence>
            </markov>
            """);
        
        // 村庄道路
        GENERATION_TEMPLATES.put("village_path", """
            <sequence values="GPSC">
                <path from="G" to="G" on="GPS" color="P" inertia="true"/>
                <one steps="30">
                    <rule in="PP" out="PC" p="0.1"/>
                </one>
            </sequence>
            """);
    }

    /**
     * 主要的结构生成方法
     * @param world Minecraft世界
     * @param center 生成中心位置
     * @param structureType 结构类型
     * @param size 生成尺寸
     * @param seed 随机种子
     */
    public static void generateStructure(World world, BlockPos center, String structureType, 
                                       int size, long seed) {
        try {
            String template = GENERATION_TEMPLATES.get(structureType);
            if (template == null) {
                System.out.println("Unknown structure type: " + structureType);
                return;
            }
            
            // 异步生成以避免阻塞主线程
            CompletableFuture.runAsync(() -> {
                try {
                    generateStructureAsync(world, center, template, size, seed);
                } catch (Exception e) {
                    System.out.println("Error generating structure: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.out.println("Failed to start structure generation: " + e.getMessage());
        }
    }
    
    private static void generateStructureAsync(World world, BlockPos center, String template, 
                                             int size, long seed) throws Exception {
        // 创建解释器
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(template.getBytes()));
        
        Interpreter interpreter = Interpreter.load(doc.getDocumentElement(), size, size, size/4 + 1);
        if (interpreter == null) {
            System.out.println("Failed to create interpreter");
            return;
        }
        
        // 生成结构
        Interpreter.RunResult lastResult = null;
        int stepCount = 0;
        for (Interpreter.RunResult result : interpreter.run((int)seed, 1000, false)) {
            lastResult = result;
            stepCount++;
            if (stepCount > 1000) break; // 防止无限循环
        }
        
        if (lastResult != null) {
            // 在主线程中应用结果到世界
            Interpreter.RunResult finalLastResult = lastResult;
            world.getServer().execute(() -> {
                applyResultToWorld(world, center, finalLastResult);
            });
        }
    }
    
    /**
     * 将MarkovJunior生成结果应用到Minecraft世界中
     */
    private static void applyResultToWorld(World world, BlockPos center, Interpreter.RunResult result) {
        try {
            int halfX = result.FX() / 2;
            int halfZ = result.FZ() / 2;
            
            for (int i = 0; i < result.state().length; i++) {
                int x = i % result.FX();
                int y = (i % (result.FX() * result.FY())) / result.FX();
                int z = i / (result.FX() * result.FY());
                
                BlockPos pos = center.add(x - halfX, y, z - halfZ);
                
                // 确保位置在合理范围内
                if (pos.getY() < 0 || pos.getY() > 255) continue;
                
                char legend = result.legend()[result.state()[i]];
                Block block = getBlockFromLegend(legend);
                
                if (block != Blocks.AIR) {
                    world.setBlockState(pos, block.getDefaultState());
                }
            }
        } catch (Exception e) {
            System.out.println("Error applying result to world: " + e.getMessage());
        }
    }
    
    /**
     * 根据字符图例获取对应的Minecraft方块
     */
    private static Block getBlockFromLegend(char legend) {
        return switch (legend) {
            // 基础方块
            case 'S' -> Blocks.STONE;
            case 'D' -> Blocks.DIRT;
            case 'G' -> Blocks.GRASS_BLOCK;
            case 'W' -> Blocks.WATER;
            case 'A' -> Blocks.AIR;
            
            // 结构方块
            case 'F' -> Blocks.OAK_PLANKS;          // 地板
            case 'R' -> Blocks.STONE_BRICKS;        // 房间/墙
            case 'C' -> Blocks.CHEST;               // 宝箱
            case 'P' -> Blocks.COBBLESTONE;         // 路径
            case 'B' -> Blocks.COBBLESTONE_WALL;    // 边界
            
            // 装饰方块
            case 'T' -> Blocks.TORCH;               // 火把
            case 'L' -> Blocks.LADDER;              // 梯子
            case 'E' -> Blocks.OAK_DOOR;            // 门
            
            // 矿物和特殊方块
            case 'I' -> Blocks.IRON_ORE;            // 铁矿
            case 'O' -> Blocks.GOLD_ORE;            // 金矿
            case 'M' -> Blocks.DIAMOND_ORE;         // 钻石矿
            
            default -> Blocks.AIR;
        };
    }
    
    /**
     * 生成地形的高级方法
     * 支持生物群系特定的生成规则
     */
    public static void generateTerrain(World world, Chunk chunk, Biome biome, Random random) {
        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;
        
        String terrainRule = getTerrainRuleForBiome(biome);
        if (terrainRule == null) return;
        
        try {
            byte[] terrain = Program.generateSimpleGrid(terrainRule, 16, 16, random.nextInt(), 200);
            
            for (int i = 0; i < terrain.length; i++) {
                int x = chunkX + (i % 16);
                int z = chunkZ + (i / 16);
                int y = world.getTopY() - 1;
                
                Block block = getTerrainBlock(terrain[i], biome);
                world.setBlockState(new BlockPos(x, y, z), block.getDefaultState());
            }
        } catch (Exception e) {
            System.out.println("Error generating terrain: " + e.getMessage());
        }
    }
    
    private static String getTerrainRuleForBiome(Biome biome) {
        // 根据生物群系返回不同的地形生成规则
        // 这里需要根据实际的生物群系类型来判断
        return """
            <sequence values="GDWS">
                <one steps="100">
                    <rule in="G" out="D" p="0.1"/>
                    <rule in="D" out="W" p="0.05"/>
                </one>
                <convolution neighborhood="Moore" steps="5">
                    <rule in="G" out="S" values="W" sum="3..5"/>
                </convolution>
            </sequence>
            """;
    }
    
    private static Block getTerrainBlock(byte value, Biome biome) {
        return switch (value) {
            case 0 -> Blocks.GRASS_BLOCK;   // G
            case 1 -> Blocks.DIRT;          // D
            case 2 -> Blocks.WATER;         // W
            case 3 -> Blocks.SAND;          // S
            default -> Blocks.STONE;
        };
    }
    
    /**
     * 工具方法：根据现有世界内容创建种子
     */
    public static long createSeedFromWorld(World world, BlockPos pos) {
        long seed = 0;
        
        // 基于周围方块创建种子
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos checkPos = pos.add(dx, 0, dz);
                Block block = world.getBlockState(checkPos).getBlock();
                seed = seed * 31 + Block.getRawIdFromState(block.getDefaultState());
            }
        }
        
        return seed;
    }
    
    /**
     * 配置类：存储不同类型结构的生成参数
     */
    public static class GenerationConfig {
        public final String structureType;
        public final int minSize, maxSize;
        public final double probability;
        public final boolean requiresFlat;
        
        public GenerationConfig(String structureType, int minSize, int maxSize, 
                              double probability, boolean requiresFlat) {
            this.structureType = structureType;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.probability = probability;
            this.requiresFlat = requiresFlat;
        }
        
        public static final GenerationConfig[] CONFIGS = {
            new GenerationConfig("cave", 8, 24, 0.1, false),
            new GenerationConfig("dungeon", 12, 32, 0.05, true),
            new GenerationConfig("village_path", 20, 50, 0.02, true)
        };
    }
    
    /**
     * 批量生成器：在区块生成时调用
     */
    public static void onChunkGenerate(World world, Chunk chunk, Random random) {
        BlockPos chunkCenter = new BlockPos(
            chunk.getPos().x * 16 + 8,
            world.getSeaLevel(),
            chunk.getPos().z * 16 + 8
        );
        
        for (GenerationConfig config : GenerationConfig.CONFIGS) {
            if (random.nextDouble() < config.probability) {
                int size = random.nextInt(config.maxSize - config.minSize + 1) + config.minSize;
                long seed = createSeedFromWorld(world, chunkCenter);
                
                generateStructure(world, chunkCenter, config.structureType, size, seed);
                break; // 每个区块只生成一个主要结构
            }
        }
    }
    
    /**
     * 调试工具：将生成结果保存为文本可视化
     */
    public static void debugVisualize(Interpreter.RunResult result, String filename) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(filename)) {
            writer.println("MarkovJunior Generation Result");
            writer.println("Size: " + result.FX() + "x" + result.FY() + "x" + result.FZ());
            writer.println("Legend: " + new String(result.legend()));
            writer.println();
            
            for (int z = 0; z < result.FZ(); z++) {
                writer.println("Layer " + z + ":");
                for (int y = result.FY() - 1; y >= 0; y--) {
                    for (int x = 0; x < result.FX(); x++) {
                        int index = x + y * result.FX() + z * result.FX() * result.FY();
                        char symbol = result.legend()[result.state()[index]];
                        writer.print(symbol + " ");
                    }
                    writer.println();
                }
                writer.println();
            }
        } catch (Exception e) {
            System.out.println("Failed to write debug visualization: " + e.getMessage());
        }
    }
}