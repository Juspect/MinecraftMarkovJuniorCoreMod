// 修改 src/main/java/com/jxon/juscore/worldgen/MarkovChunkGenerator.java
package com.jxon.juscore.worldgen;

import com.jxon.juscore.Juscore;
import com.jxon.juscore.cache.CacheManager;
import com.jxon.juscore.config.MarkovWorldConfig;
import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.worldgen.mapping.BlockStateMapper;
import com.jxon.juscore.worldgen.tiling.TilingSystem;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.registry.RegistryKeys;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MarkovChunkGenerator extends ChunkGenerator {
    public static final Codec<MarkovChunkGenerator> CODEC = Codec.unit(MarkovChunkGenerator::new);

    private final MarkovWorldConfig config;
    private final TilingSystem tilingSystem;
    private final BlockStateMapper blockMapper;
    private final CacheManager cacheManager;

    public MarkovChunkGenerator() {

        // 创建一个固定的生物群系源（平原）
        super(null);
        this.config = MarkovWorldType.getCurrentConfig();
        this.tilingSystem = new TilingSystem(config);
        this.blockMapper = new BlockStateMapper(config.getBlockMapping());
        this.cacheManager = new CacheManager(config.getCacheLimit());
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {

    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // 不生成实体
    }

    @Override
    public int getWorldHeight() {
        return 384; // 标准世界高度
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return null;
    }

    private void generateChunkContent(Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        // 计算区块的世界坐标范围
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        // 为每个方块位置生成内容
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                generateColumn(chunk, x, z, worldX, worldZ);
            }
        }
    }

    private void generateColumn(Chunk chunk, int localX, int localZ, int worldX, int worldZ) {
        // 遍历整个Y轴
        for (int worldY = chunk.getBottomY(); worldY < chunk.getTopY(); worldY++) {
            BlockState blockState = generateBlockAt(worldX, worldY, worldZ);
            if (blockState != null) {
                chunk.setBlockState(new BlockPos(localX, worldY, localZ), blockState, false);
            }
        }
    }

    private BlockState generateBlockAt(int worldX, int worldY, int worldZ) {
        // 使用平铺系统计算模型坐标
        TilingSystem.ModelCoordinate coord = tilingSystem.worldToModel(worldX, worldY, worldZ);

        // 检查是否在截取范围内
        if (config.isEnableClipping() && !isInClippingRange(worldX, worldY, worldZ)) {
            return null; // 超出截取范围，保持空气
        }

        // 检查相对坐标是否在模型范围内
        if (!isInModelBounds(coord)) {
            return null; // 超出模型边界，保持空气
        }

        // 生成或获取模型数据
        byte[][][] modelData = getOrGenerateModel(coord.getModelIndexX(),
                coord.getModelIndexY(),
                coord.getModelIndexZ());

        if (modelData == null) {
            return null;
        }

        // 获取该位置的字符值
        byte value = modelData[coord.getRelativeX()][coord.getRelativeY()][coord.getRelativeZ()];

        // 转换为方块状态
        return blockMapper.getBlockState(value, worldX, worldY, worldZ);
    }

    private boolean isInClippingRange(int worldX, int worldY, int worldZ) {
        return worldX >= config.getClippingXMin() && worldX <= config.getClippingXMax() &&
                worldY >= config.getClippingYMin() && worldY <= config.getClippingYMax() &&
                worldZ >= config.getClippingZMin() && worldZ <= config.getClippingZMax();
    }

    private boolean isInModelBounds(TilingSystem.ModelCoordinate coord) {
        return coord.getRelativeX() >= 0 && coord.getRelativeX() < config.getModelLength() &&
                coord.getRelativeY() >= 0 && coord.getRelativeY() < config.getModelHeight() &&
                coord.getRelativeZ() >= 0 && coord.getRelativeZ() < config.getModelWidth();
    }

    private byte[][][] getOrGenerateModel(int modelIndexX, int modelIndexY, int modelIndexZ) {
        // 计算模型种子
        long worldSeed = getSeed(); // 从世界获取种子
        int markovSeed = calculateMarkovSeed(worldSeed, modelIndexX, modelIndexY, modelIndexZ);

        // 生成缓存键
        String cacheKey = config.generateCacheKey(markovSeed);

        // 尝试从缓存获取
        byte[][][] cached = cacheManager.getModel(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 生成新模型
        MarkovIntegrator.GenerationResult result = MarkovIntegrator.generateModel(config, markovSeed);
        if (result.isSuccess()) {
            byte[][][] newModel = result.getModelData();
            cacheManager.putModel(cacheKey, newModel);
            return newModel;
        } else {
            Juscore.LOGGER.warn("Failed to generate model: " + result.getErrorMessage());
            return createFallbackModel(); // 返回简单的备用模型
        }
    }

    private long getSeed() {
        // 获取世界种子，暂时使用固定值
        return 12345L;
    }

    private int calculateMarkovSeed(long worldSeed, int modelIndexX, int modelIndexY, int modelIndexZ) {
        // 使用Minecraft标准的种子计算方式
        Random random = new Random();
        long decorationSeed = (worldSeed + modelIndexX * modelIndexX * 4987142L +
                modelIndexX * 5947611L + modelIndexZ * modelIndexZ * 4392871L +
                modelIndexZ * 389711L) ^ 987234911L;
        random.setSeed(decorationSeed);
        return random.nextInt() ^ (modelIndexX * 73856093) ^
                (modelIndexY * 19349663) ^ (modelIndexZ * 83492791);
    }

    private byte[][][] createFallbackModel() {
        // 创建一个简单的备用模型（石头平台）
        int length = config.getModelLength();
        int width = config.getModelWidth();
        int height = config.getModelHeight();

        byte[][][] fallback = new byte[length][height][width];

        for (int x = 0; x < length; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < width; z++) {
                    if (y == 0) {
                        fallback[x][y][z] = (byte) 'B'; // 底部石头
                    } else {
                        fallback[x][y][z] = (byte) '*'; // 空气
                    }
                }
            }
        }

        return fallback;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return getSeaLevel();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return new VerticalBlockSample(world.getBottomY(), new BlockState[0]);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Markov World Generator");
        text.add("Model: " + config.getSelectedTemplate());
    }
}