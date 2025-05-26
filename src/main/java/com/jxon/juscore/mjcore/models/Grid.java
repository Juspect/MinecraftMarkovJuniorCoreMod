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

        System.out.println("DEBUG: Loading grid values: '" + valueString + "'");

        g.C = (byte) valueString.length();
        g.values = new HashMap<>();
        g.waves = new HashMap<>();
        g.characters = new char[g.C];

        for (byte i = 0; i < g.C; i++) {
            char symbol = valueString.charAt(i);
            if (g.values.containsKey(symbol)) {
                Interpreter.writeLine("repeating value " + symbol + " at line " + XMLHelper.getLineNumber(element));
                return null;
            } else {
                g.characters[i] = symbol;
                g.values.put(symbol, i);
                g.waves.put(symbol, 1 << i);
                System.out.println("DEBUG: Added character '" + symbol + "' with value " + i);
            }
        }

        String transparentString = XMLHelper.get(element, "transparent", (String) null);
        if (transparentString != null) {
            g.transparent = g.wave(transparentString);
        }

        // Union处理
        g.waves.put('*', (1 << g.C) - 1);
        System.out.println("DEBUG: Added wildcard '*' with value " + ((1 << g.C) - 1));

        List<Element> allDescendants = XMLHelper.myDescendants(element, "markov", "sequence", "union");
        for (Element descendant : allDescendants) {
            if ("union".equals(descendant.getTagName())) {
                char symbol = XMLHelper.get(descendant, "symbol", Character.class);
                if (g.waves.containsKey(symbol)) {
                    Interpreter.writeLine("repeating union type " + symbol + " at line " + XMLHelper.getLineNumber(descendant));
                    return null;
                } else {
                    String unionValues = XMLHelper.get(descendant, "values");
                    System.out.println("DEBUG: Processing union symbol '" + symbol + "' with values '" + unionValues + "'");
                    int w = g.wave(unionValues);
                    g.waves.put(symbol, w);
                    System.out.println("DEBUG: Added union symbol '" + symbol + "' with wave value " + w);
                }
            }
        }

        // 输出所有已加载的字符用于调试
        System.out.println("DEBUG: All loaded characters and values:");
        for (Map.Entry<Character, Byte> entry : g.values.entrySet()) {
            System.out.println("  '" + entry.getKey() + "' -> " + entry.getValue());
        }

        g.state = new byte[MX * MY * MZ];
        g.statebuffer = new byte[MX * MY * MZ];
        g.mask = new boolean[MX * MY * MZ];
        g.folder = XMLHelper.get(element, "folder", (String) null);

        return g;
    }

    // 改进wave方法，添加错误处理
    public int wave(String values) {
        if (values == null || values.isEmpty()) {
            System.out.println("DEBUG: Empty values string in wave()");
            return 0;
        }

        System.out.println("DEBUG: Computing wave for values: '" + values + "'");
        int sum = 0;
        for (int k = 0; k < values.length(); k++) {
            char c = values.charAt(k);
            Byte value = this.values.get(c);
            if (value != null) {
                sum += 1 << value;
                System.out.println("DEBUG: Character '" + c + "' contributes " + (1 << value) + " to wave");
            } else {
                System.out.println("WARNING: Character '" + c + "' not found in values, available characters: " + this.values.keySet());
            }
        }
        System.out.println("DEBUG: Final wave value: " + sum);
        return sum;
    }

    public void clear() {
        Arrays.fill(state, (byte) 0);
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