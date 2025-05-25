// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvolutionNode extends Node {
    private ConvolutionRule[] rules;
    private int[] kernel;
    private boolean periodic;
    public int counter, steps;
    
    private int[][] sumfield;
    
    private static final Map<String, int[]> kernels2d = new HashMap<>();
    private static final Map<String, int[]> kernels3d = new HashMap<>();
    
    static {
        kernels2d.put("VonNeumann", new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0});
        kernels2d.put("Moore", new int[]{1, 1, 1, 1, 0, 1, 1, 1, 1});
        
        kernels3d.put("VonNeumann", new int[]{0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0});
        kernels3d.put("NoCorners", new int[]{0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0});
    }
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        List<Element> ruleElements = XMLHelper.getElementsByTagName(element, "rule");
        if (ruleElements.isEmpty()) {
            ruleElements = List.of(element);
        }
        
        rules = new ConvolutionRule[ruleElements.size()];
        for (int k = 0; k < rules.length; k++) {
            rules[k] = new ConvolutionRule();
            if (!rules[k].load(ruleElements.get(k), grid)) {
                return false;
            }
        }
        
        steps = XMLHelper.get(element, "steps", -1);
        periodic = XMLHelper.get(element, "periodic", false);
        String neighborhood = XMLHelper.get(element, "neighborhood");
        kernel = grid.MZ == 1 ? kernels2d.get(neighborhood) : kernels3d.get(neighborhood);
        
        sumfield = AH.array2D(grid.state.length, grid.C, 0);
        return true;
    }
    
    @Override
    public void reset() {
        counter = 0;
    }
    
    @Override
    public boolean go() {
        if (steps > 0 && counter >= steps) {
            return false;
        }
        
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;
        
        AH.set2D(sumfield, 0);
        if (MZ == 1) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    int[] sums = sumfield[x + y * MX];
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int sx = x + dx;
                            int sy = y + dy;
                            
                            if (periodic) {
                                if (sx < 0) sx += MX;
                                else if (sx >= MX) sx -= MX;
                                if (sy < 0) sy += MY;
                                else if (sy >= MY) sy -= MY;
                            } else if (sx < 0 || sy < 0 || sx >= MX || sy >= MY) {
                                continue;
                            }
                            
                            sums[grid.state[sx + sy * MX]] += kernel[dx + 1 + (dy + 1) * 3];
                        }
                    }
                }
            }
        } else {
            for (int z = 0; z < MZ; z++) {
                for (int y = 0; y < MY; y++) {
                    for (int x = 0; x < MX; x++) {
                        int[] sums = sumfield[x + y * MX + z * MX * MY];
                        for (int dz = -1; dz <= 1; dz++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    int sx = x + dx;
                                    int sy = y + dy;
                                    int sz = z + dz;
                                    
                                    if (periodic) {
                                        if (sx < 0) sx += MX;
                                        else if (sx >= MX) sx -= MX;
                                        if (sy < 0) sy += MY;
                                        else if (sy >= MY) sy -= MY;
                                        if (sz < 0) sz += MZ;
                                        else if (sz >= MZ) sz -= MZ;
                                    } else if (sx < 0 || sy < 0 || sz < 0 || sx >= MX || sy >= MY || sz >= MZ) {
                                        continue;
                                    }
                                    
                                    sums[grid.state[sx + sy * MX + sz * MX * MY]] += kernel[dx + 1 + (dy + 1) * 3 + (dz + 1) * 9];
                                }
                            }
                        }
                    }
                }
            }
        }
        
        boolean change = false;
        for (int i = 0; i < sumfield.length; i++) {
            int[] sums = sumfield[i];
            byte input = grid.state[i];
            for (int r = 0; r < rules.length; r++) {
                ConvolutionRule rule = rules[r];
                if (input == rule.input && rule.output != grid.state[i] && 
                    (rule.p == 1.0 || ip.random.nextInt() < rule.p * Integer.MAX_VALUE)) {
                    boolean success = true;
                    if (rule.sums != null) {
                        int sum = 0;
                        for (int c = 0; c < rule.values.length; c++) {
                            sum += sums[rule.values[c]];
                        }
                        success = rule.sums[sum];
                    }
                    if (success) {
                        grid.state[i] = rule.output;
                        change = true;
                        break;
                    }
                }
            }
        }
        
        counter++;
        return change;
    }
    
    private static class ConvolutionRule {
        public byte input, output;
        public byte[] values;
        public boolean[] sums;
        public double p;
        
        public boolean load(Element element, Grid grid) {
            input = grid.values.get(XMLHelper.get(element, "in", Character.class));
            output = grid.values.get(XMLHelper.get(element, "out", Character.class));
            p = XMLHelper.get(element, "p", 1.0);
            
            String valueString = XMLHelper.get(element, "values", (String) null);
            String sumsString = XMLHelper.get(element, "sum", (String) null);
            
            if (valueString != null && sumsString == null) {
                Interpreter.writeLine("missing \"sum\" attribute at line " + XMLHelper.getLineNumber(element));
                return false;
            }
            if (valueString == null && sumsString != null) {
                Interpreter.writeLine("missing \"values\" attribute at line " + XMLHelper.getLineNumber(element));
                return false;
            }
            
            if (valueString != null) {
                values = new byte[valueString.length()];
                for (int i = 0; i < valueString.length(); i++) {
                    values[i] = grid.values.get(valueString.charAt(i));
                }
                
                sums = new boolean[28];
                String[] intervals = sumsString.split(",");
                for (String s : intervals) {
                    for (int i : interval(s)) {
                        sums[i] = true;
                    }
                }
            }
            return true;
        }
        
        private static int[] interval(String s) {
            if (s.contains("..")) {
                String[] bounds = s.split("\\.\\.");
                int min = Integer.parseInt(bounds[0]);
                int max = Integer.parseInt(bounds[1]);
                int[] result = new int[max - min + 1];
                for (int i = 0; i < result.length; i++) {
                    result[i] = min + i;
                }
                return result;
            } else {
                return new int[]{Integer.parseInt(s)};
            }
        }
    }
}