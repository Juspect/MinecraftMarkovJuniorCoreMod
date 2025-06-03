// src/main/java/com/jxon/juscore/worldgen/tiling/TilingSystem.java
package com.jxon.juscore.worldgen.tiling;

import com.jxon.juscore.config.MarkovWorldConfig;

public class TilingSystem {
    private final boolean tilingX, tilingY, tilingZ;
    private final int boundaryX, boundaryY, boundaryZ;
    private final int modelLength, modelWidth, modelHeight;

    public TilingSystem(MarkovWorldConfig config) {
        this.tilingX = config.isTilingX();
        this.tilingY = config.isTilingY();
        this.tilingZ = config.isTilingZ();
        this.boundaryX = config.getBoundaryX();
        this.boundaryY = config.getBoundaryY();
        this.boundaryZ = config.getBoundaryZ();
        this.modelLength = config.getModelLength();
        this.modelWidth = config.getModelWidth();
        this.modelHeight = config.getModelHeight();
    }

    public ModelCoordinate worldToModel(int worldX, int worldY, int worldZ) {
        // X轴计算
        int modelIndexX, relativeX;
        if (tilingX) {
            int spanX = modelLength + boundaryX;
            modelIndexX = Math.floorDiv(worldX, spanX);
            relativeX = Math.floorMod(worldX, spanX);
            // 如果在边界区域，设置为-1表示空气
            if (relativeX >= modelLength) {
                relativeX = -1;
            }
        } else {
            modelIndexX = 0;
            relativeX = worldX;
        }

        // Y轴计算
        int modelIndexY, relativeY;
        if (tilingY) {
            int spanY = modelHeight + boundaryY;
            modelIndexY = Math.floorDiv(worldY, spanY);
            relativeY = Math.floorMod(worldY, spanY);
            if (relativeY >= modelHeight) {
                relativeY = -1;
            }
        } else {
            modelIndexY = 0;
            relativeY = worldY;
        }

        // Z轴计算
        int modelIndexZ, relativeZ;
        if (tilingZ) {
            int spanZ = modelWidth + boundaryZ;
            modelIndexZ = Math.floorDiv(worldZ, spanZ);
            relativeZ = Math.floorMod(worldZ, spanZ);
            if (relativeZ >= modelWidth) {
                relativeZ = -1;
            }
        } else {
            modelIndexZ = 0;
            relativeZ = worldZ;
        }

        return new ModelCoordinate(modelIndexX, modelIndexY, modelIndexZ,
                relativeX, relativeY, relativeZ);
    }

    public static class ModelCoordinate {
        private final int modelIndexX, modelIndexY, modelIndexZ;
        private final int relativeX, relativeY, relativeZ;

        public ModelCoordinate(int modelIndexX, int modelIndexY, int modelIndexZ,
                               int relativeX, int relativeY, int relativeZ) {
            this.modelIndexX = modelIndexX;
            this.modelIndexY = modelIndexY;
            this.modelIndexZ = modelIndexZ;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
            this.relativeZ = relativeZ;
        }

        public int getModelIndexX() { return modelIndexX; }
        public int getModelIndexY() { return modelIndexY; }
        public int getModelIndexZ() { return modelIndexZ; }
        public int getRelativeX() { return relativeX; }
        public int getRelativeY() { return relativeY; }
        public int getRelativeZ() { return relativeZ; }

        public boolean isInBoundary() {
            return relativeX == -1 || relativeY == -1 || relativeZ == -1;
        }
    }
}