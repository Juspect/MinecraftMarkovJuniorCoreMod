// src/main/java/com/jxon/juscore/config/MarkovWorldConfig.java
package com.jxon.juscore.config;

import com.jxon.juscore.resources.DefaultTemplates;
import com.jxon.juscore.resources.ResourceManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public class MarkovWorldConfig {
    // 模型配置
    public enum ModelSource {
        SYSTEM_TEMPLATE,
        FILE_BROWSER,
        XML_CODE
    }

    private ModelSource modelSource = ModelSource.SYSTEM_TEMPLATE;
    private String selectedTemplate = "basic/tower.xml";
    private String xmlFilePath = "";
    private String xmlContent = "";

    // 模型尺寸
    private int modelLength = 16;
    private int modelWidth = 16;
    private int modelHeight = 16;
    private int maxStep = 1000;

    // 平铺配置
    private boolean tilingX = true;
    private boolean tilingY = false;
    private boolean tilingZ = true;
    private int boundaryX = 2;
    private int boundaryY = 0;
    private int boundaryZ = 2;

    // 截取设置
    private boolean enableClipping = false;
    private int clippingXMin = -64, clippingXMax = 64;
    private int clippingYMin = 0, clippingYMax = 256;
    private int clippingZMin = -64, clippingZMax = 64;

    // 方块映射
    private Map<Character, String> blockMapping = new HashMap<>();

    // 系统设置
    private int cacheLimit = 512; // MB
    private boolean enableChunkPreview = true;
    private boolean enableDebugInfo = false;

    public MarkovWorldConfig() {
        initializeDefaultMapping();
    }

    private void initializeDefaultMapping() {
        blockMapping.put('B', "minecraft:stone");
        blockMapping.put('W', "minecraft:oak_log");
        blockMapping.put('R', "minecraft:bricks");
        blockMapping.put('G', "minecraft:grass_block");
        blockMapping.put('Y', "minecraft:sand");
        blockMapping.put('U', "minecraft:water");
        blockMapping.put('*', "minecraft:air");
    }

    // Getters and setters
    public ModelSource getModelSource() { return modelSource; }
    public void setModelSource(ModelSource modelSource) { this.modelSource = modelSource; }

    public String getSelectedTemplate() { return selectedTemplate; }
    public void setSelectedTemplate(String selectedTemplate) { this.selectedTemplate = selectedTemplate; }

    public String getXmlContent() { return xmlContent; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }

    public int getModelLength() { return modelLength; }
    public void setModelLength(int modelLength) { this.modelLength = modelLength; }

    public int getModelWidth() { return modelWidth; }
    public void setModelWidth(int modelWidth) { this.modelWidth = modelWidth; }

    public int getModelHeight() { return modelHeight; }
    public void setModelHeight(int modelHeight) { this.modelHeight = modelHeight; }

    public int getMaxStep() { return maxStep; }
    public void setMaxStep(int maxStep) { this.maxStep = maxStep; }

    public boolean isTilingX() { return tilingX; }
    public void setTilingX(boolean tilingX) { this.tilingX = tilingX; }

    public boolean isTilingY() { return tilingY; }
    public void setTilingY(boolean tilingY) { this.tilingY = tilingY; }

    public boolean isTilingZ() { return tilingZ; }
    public void setTilingZ(boolean tilingZ) { this.tilingZ = tilingZ; }

    public int getBoundaryX() { return boundaryX; }
    public void setBoundaryX(int boundaryX) { this.boundaryX = boundaryX; }

    public int getBoundaryY() { return boundaryY; }
    public void setBoundaryY(int boundaryY) { this.boundaryY = boundaryY; }

    public int getBoundaryZ() { return boundaryZ; }
    public void setBoundaryZ(int boundaryZ) { this.boundaryZ = boundaryZ; }

    public Map<Character, String> getBlockMapping() { return blockMapping; }
    public void setBlockMapping(Map<Character, String> blockMapping) { this.blockMapping = blockMapping; }

    public boolean isEnableClipping() { return enableClipping; }
    public void setEnableClipping(boolean enableClipping) { this.enableClipping = enableClipping; }

    // 在 MarkovWorldConfig.java 中添加缺失的方法
    public int getClippingXMin() { return clippingXMin; }
    public void setClippingXMin(int clippingXMin) { this.clippingXMin = clippingXMin; }

    public int getClippingXMax() { return clippingXMax; }
    public void setClippingXMax(int clippingXMax) { this.clippingXMax = clippingXMax; }

    public int getClippingYMin() { return clippingYMin; }
    public void setClippingYMin(int clippingYMin) { this.clippingYMin = clippingYMin; }

    public int getClippingYMax() { return clippingYMax; }
    public void setClippingYMax(int clippingYMax) { this.clippingYMax = clippingYMax; }

    public int getClippingZMin() { return clippingZMin; }
    public void setClippingZMin(int clippingZMin) { this.clippingZMin = clippingZMin; }

    public int getClippingZMax() { return clippingZMax; }
    public void setClippingZMax(int clippingZMax) { this.clippingZMax = clippingZMax; }

    public int getCacheLimit() { return cacheLimit; }
    public void setCacheLimit(int cacheLimit) { this.cacheLimit = cacheLimit; }

    public boolean isEnableChunkPreview() { return enableChunkPreview; }
    public void setEnableChunkPreview(boolean enableChunkPreview) { this.enableChunkPreview = enableChunkPreview; }

    public boolean isEnableDebugInfo() { return enableDebugInfo; }
    public void setEnableDebugInfo(boolean enableDebugInfo) { this.enableDebugInfo = enableDebugInfo; }

    public String getXmlFilePath() { return xmlFilePath; }
    public void setXmlFilePath(String xmlFilePath) { this.xmlFilePath = xmlFilePath; }

    // 修改isValid方法
    public boolean isValid() {
        return modelLength > 0 && modelWidth > 0 && modelHeight > 0 &&
                maxStep > 0 && getEffectiveXmlContent() != null && !getEffectiveXmlContent().trim().isEmpty();
    }

    // 添加获取有效XML内容的方法
    public String getEffectiveXmlContent() {
        switch (modelSource) {
            case SYSTEM_TEMPLATE:
                return DefaultTemplates.getTemplate(selectedTemplate);
            case FILE_BROWSER:
                return ResourceManager.loadFile(xmlFilePath);
            case XML_CODE:
                return xmlContent;
            default:
                return xmlContent;
        }
    }

    // 生成缓存键
    public String generateCacheKey(int markovSeed) {
        String content = xmlContent + blockMapping.toString();
        String hash = Integer.toHexString(content.hashCode()); // 简化的哈希
        return hash + "_" + modelLength + "_" + modelWidth + "_" + modelHeight + "_" + markovSeed;
    }
}