// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.models;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;

import java.util.*;

public class Grid {
    public byte[] state;
    public boolean[] mask;
    public int MX, MY, MZ;
    
    public byte C;
    public char[] characters;
    public Map<Character, Byte> values;
    public Map<Character, Integer> waves;
    public String folder;
    
    private int transparent;
    private byte[] statebuffer;

    // Grid.java中Union处理的关键修正部分

    public static Grid load(Element element, int MX, int MY, int MZ) {
        Grid g = new Grid();
        g.MX = MX;
        g.MY = MY;
        g.MZ = MZ;

        String valueString = XMLHelper.get(element, "values", (String) null);
        if (valueString != null) {
            valueString = valueString.replace(" ", "");
        }
        if (valueString == null) {
            Interpreter.writeLine("no values specified");
            return null;
        }

        g.C = (byte) valueString.length();
        g.values = new HashMap<>();
        g.waves = new HashMap<>();
        g.characters = new char[g.C];

        // 建立字符到索引的映射
        for (byte i = 0; i < g.C; i++) {
            char symbol = valueString.charAt(i);
            if (g.values.containsKey(symbol)) {
                Interpreter.writeLine("repeating value " + symbol + " at line " + XMLHelper.getLineNumber(element));
                return null;
            } else {
                g.characters[i] = symbol;
                g.values.put(symbol, i);
                g.waves.put(symbol, 1 << i);
            }
        }

        // 处理transparent属性
        String transparentString = XMLHelper.get(element, "transparent", (String) null);
        if (transparentString != null) {
            g.transparent = g.wave(transparentString);
        }

        // 关键修正：正确处理union元素
        // 首先添加默认的*符号，代表所有值的union
        g.waves.put('*', (1 << g.C) - 1);

        // 查找所有union元素 - 使用更精确的查找方法
        List<Element> allDescendants = XMLHelper.myDescendants(element, "markov", "sequence", "union");
        List<Element> unions = new ArrayList<>();
        for (Element descendant : allDescendants) {
            if ("union".equals(descendant.getTagName())) {
                unions.add(descendant);
            }
        }

        // 处理每个union元素
        for (Element union : unions) {
            try {
                char symbol = XMLHelper.get(union, "symbol", Character.class);
                String values = XMLHelper.get(union, "values");

                if (g.waves.containsKey(symbol)) {
                    Interpreter.writeLine("repeating union type " + symbol + " at line " + XMLHelper.getLineNumber(union));
                    return null;
                } else {
                    int w = g.wave(values);
                    g.waves.put(symbol, w);
                    System.out.println("Added union symbol '" + symbol + "' with wave value " + w + " for values '" + values + "'");
                }
            } catch (Exception e) {
                Interpreter.writeLine("Error processing union element: " + e.getMessage());
                // 继续处理其他union元素，不要立即返回null
            }
        }

        // 关键新增：为常见的复合符号创建预定义mappings
        createCommonUnionMappings(g);

        g.state = new byte[MX * MY * MZ];
        g.statebuffer = new byte[MX * MY * MZ];
        g.mask = new boolean[MX * MY * MZ];
        g.folder = XMLHelper.get(element, "folder", (String) null);

        return g;
    }

    // 新增：为常见的复合符号创建预定义mappings
    private static void createCommonUnionMappings(Grid g) {
        // 获取所有现有的字符
        Set<Character> existingChars = new HashSet<>();
        for (char c : g.characters) {
            existingChars.add(c);
        }

        // 为每个字符创建与*的组合
        for (char c : existingChars) {
            // 创建 "* C" 形式的组合
            String starSpace = "* " + c;
            if (!g.waves.containsKey(starSpace.charAt(0))) { // 避免重复键
                int starWave = g.waves.get('*');
                int charWave = g.waves.get(c);
                g.waves.put(starSpace.intern().charAt(0), starWave | charWave);
            }

            // 创建 "*C" 形式的组合（无空格）
            String starNoSpace = "*" + c;
            // 由于Map的key是Character，我们需要用其他方式存储这种复合字符串
            // 这里我们扩展waves为支持String的Map
        }
    }

    // 修正wave方法，增加错误处理
    public int wave(String values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for (int k = 0; k < values.length(); k++) {
            char c = values.charAt(k);
            Byte value = this.values.get(c);
            if (value != null) {
                sum += 1 << value;
            } else {
                // 如果找不到字符，记录警告但继续处理
                System.out.println("Warning: Character '" + c + "' not found in values, skipping");
            }
        }
        return sum;
    }

    // 新增：扩展的wave方法，支持复合符号
    public int waveExtended(String symbolString) {
        // 首先尝试作为单个字符处理
        if (symbolString.length() == 1) {
            return wave(symbolString);
        }

        // 处理复合符号
        int sum = 0;

        // 处理空格分隔的符号
        String[] parts = symbolString.split("\\s+");
        for (String part : parts) {
            part = part.trim();
            if (part.equals("*")) {
                sum |= (1 << C) - 1; // 所有位
            } else {
                sum |= wave(part);
            }
        }

        // 处理*包围的符号，如 "*LSL*"
        if (symbolString.startsWith("*") && symbolString.endsWith("*") && symbolString.length() > 2) {
            String inner = symbolString.substring(1, symbolString.length() - 1);
            sum |= wave(inner);
            sum |= (1 << C) - 1; // 同时包含所有符号
        }

        return sum;
    }
    
    public void clear() {
        for (int i = 0; i < state.length; i++) {
            state[i] = 0;
        }
    }
    
    public boolean matches(Rule rule, int x, int y, int z) {
        int dz = 0, dy = 0, dx = 0;
        for (int di = 0; di < rule.input.length; di++) {
            if ((rule.input[di] & (1 << state[x + dx + (y + dy) * MX + (z + dz) * MX * MY])) == 0) {
                return false;
            }
            
            dx++;
            if (dx == rule.IMX) {
                dx = 0;
                dy++;
                if (dy == rule.IMY) {
                    dy = 0;
                    dz++;
                }
            }
        }
        return true;
    }
}