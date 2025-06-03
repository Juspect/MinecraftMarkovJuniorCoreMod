// src/main/java/com/jxon/juscore/worldgen/mapping/BlockStateMapper.java
package com.jxon.juscore.worldgen.mapping;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockStateMapper {
    private final Map<Character, String> characterMapping;
    private final Map<Character, BlockState> cachedStates;
    private static final Pattern BLOCK_STATE_PATTERN = Pattern.compile("([^\\[]+)(?:\\[([^\\]]+)\\])?");

    public BlockStateMapper(Map<Character, String> mapping) {
        this.characterMapping = new HashMap<>(mapping);
        this.cachedStates = new HashMap<>();
        precomputeStates();
    }

    private void precomputeStates() {
        for (Map.Entry<Character, String> entry : characterMapping.entrySet()) {
            char character = entry.getKey();
            String stateString = entry.getValue();

            BlockState state = parseBlockState(stateString);
            if (state != null) {
                cachedStates.put(character, state);
            }
        }
    }

    public BlockState getBlockState(byte value, int worldX, int worldY, int worldZ) {
        if (value == 0) {
            return Blocks.AIR.getDefaultState(); // 默认空气
        }

        // 将byte转换为字符
        char character = (char) value;

        BlockState state = cachedStates.get(character);
        if (state != null) {
            return state;
        }

        // 如果没有映射，返回石头作为默认值
        return Blocks.STONE.getDefaultState();
    }

    private BlockState parseBlockState(String stateString) {
        try {
            Matcher matcher = BLOCK_STATE_PATTERN.matcher(stateString.trim());
            if (!matcher.matches()) {
                return null;
            }

            String blockId = matcher.group(1);
            String properties = matcher.group(2);

            // 解析方块ID
            Identifier id = new Identifier(blockId);
            Block block = Registries.BLOCK.get(id);
            if (block == Blocks.AIR && !blockId.equals("minecraft:air")) {
                return null; // 方块不存在
            }

            BlockState state = block.getDefaultState();

            // 解析属性
            if (properties != null && !properties.isEmpty()) {
                state = parseProperties(state, properties);
            }

            return state;

        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private BlockState parseProperties(BlockState state, String properties) {
        String[] pairs = properties.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length != 2) {
                continue;
            }

            String propertyName = keyValue[0].trim();
            String propertyValue = keyValue[1].trim();

            // 查找属性
            Property<?> property = null;
            for (Property<?> prop : state.getProperties()) {
                if (prop.getName().equals(propertyName)) {
                    property = prop;
                    break;
                }
            }

            if (property != null) {
                // 设置属性值
                state = setPropertyValue(state, property, propertyValue);
            }
        }

        return state;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        try {
            T propertyValue = property.parse(value).orElse(null);
            if (propertyValue != null) {
                return state.with(property, propertyValue);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return state;
    }

    public void updateMapping(Map<Character, String> newMapping) {
        characterMapping.clear();
        characterMapping.putAll(newMapping);
        cachedStates.clear();
        precomputeStates();
    }
}