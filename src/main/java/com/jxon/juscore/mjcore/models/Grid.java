// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.models;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        String transparentString = XMLHelper.get(element, "transparent", (String) null);
        if (transparentString != null) {
            g.transparent = g.wave(transparentString);
        }
        
        // Handle union elements
        List<Element> unions = XMLHelper.myDescendants(element, "markov", "sequence", "union");
        g.waves.put('*', (1 << g.C) - 1);
        
        for (Element union : unions) {
            if (union.getTagName().equals("union")) {
                char symbol = XMLHelper.get(union, "symbol", Character.class);
                if (g.waves.containsKey(symbol)) {
                    Interpreter.writeLine("repeating union type " + symbol + " at line " + XMLHelper.getLineNumber(union));
                    return null;
                } else {
                    int w = g.wave(XMLHelper.get(union, "values"));
                    g.waves.put(symbol, w);
                }
            }
        }
        
        g.state = new byte[MX * MY * MZ];
        g.statebuffer = new byte[MX * MY * MZ];
        g.mask = new boolean[MX * MY * MZ];
        g.folder = XMLHelper.get(element, "folder", (String) null);
        
        return g;
    }
    
    public void clear() {
        for (int i = 0; i < state.length; i++) {
            state[i] = 0;
        }
    }
    
    public int wave(String values) {
        int sum = 0;
        for (int k = 0; k < values.length(); k++) {
            sum += 1 << this.values.get(values.charAt(k));
        }
        return sum;
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