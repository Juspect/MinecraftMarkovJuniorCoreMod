// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.*;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.models.Rule;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;

public class MapNode extends Branch {
    public Grid newgrid;
    public Rule[] rules;
    private int NX, NY, NZ, DX, DY, DZ;
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        String scaleString = XMLHelper.get(element, "scale", (String) null);
        if (scaleString == null) {
            Interpreter.writeLine("scale should be specified in map node");
            return false;
        }
        
        String[] scales = scaleString.split(" ");
        if (scales.length != 3) {
            Interpreter.writeLine("scale attribute \"" + scaleString + "\" should have 3 components separated by space");
            return false;
        }
        
        ScalePair nxdx = readScale(scales[0]);
        ScalePair nydy = readScale(scales[1]);
        ScalePair nzdz = readScale(scales[2]);
        
        NX = nxdx.numerator;
        DX = nxdx.denominator;
        NY = nydy.numerator;
        DY = nydy.denominator;
        NZ = nzdz.numerator;
        DZ = nzdz.denominator;
        
        newgrid = Grid.load(element, grid.MX * NX / DX, grid.MY * NY / DY, grid.MZ * NZ / DZ);
        if (newgrid == null) {
            return false;
        }
        
        if (!super.load(element, parentSymmetry, newgrid)) {
            return false;
        }
        
        boolean[] symmetry = SymmetryHelper.getSymmetry(grid.MZ == 1,
                                                        XMLHelper.get(element, "symmetry", (String) null), 
                                                        parentSymmetry);
        
        List<Rule> ruleList = new ArrayList<>();
        List<Element> ruleElements = XMLHelper.getElementsByTagName(element, "rule");
        for (Element ruleElement : ruleElements) {
            Rule rule = Rule.load(ruleElement, grid, newgrid);
            if (rule == null) {
                return false;
            }
            rule.original = true;
            
            for (Rule r : rule.symmetries(symmetry, grid.MZ == 1)) {
                ruleList.add(r);
            }
        }
        rules = ruleList.toArray(new Rule[0]);
        return true;
    }
    
    private static ScalePair readScale(String s) {
        if (!s.contains("/")) {
            return new ScalePair(Integer.parseInt(s), 1);
        } else {
            String[] nd = s.split("/");
            return new ScalePair(Integer.parseInt(nd[0]), Integer.parseInt(nd[1]));
        }
    }
    
    private static boolean matches(Rule rule, int x, int y, int z, byte[] state, int MX, int MY, int MZ) {
        for (int dz = 0; dz < rule.IMZ; dz++) {
            for (int dy = 0; dy < rule.IMY; dy++) {
                for (int dx = 0; dx < rule.IMX; dx++) {
                    int sx = x + dx;
                    int sy = y + dy;
                    int sz = z + dz;
                    
                    if (sx >= MX) sx -= MX;
                    if (sy >= MY) sy -= MY;
                    if (sz >= MZ) sz -= MZ;
                    
                    int inputWave = rule.input[dx + dy * rule.IMX + dz * rule.IMX * rule.IMY];
                    if ((inputWave & (1 << state[sx + sy * MX + sz * MX * MY])) == 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private static void apply(Rule rule, int x, int y, int z, byte[] state, int MX, int MY, int MZ) {
        for (int dz = 0; dz < rule.OMZ; dz++) {
            for (int dy = 0; dy < rule.OMY; dy++) {
                for (int dx = 0; dx < rule.OMX; dx++) {
                    int sx = x + dx;
                    int sy = y + dy;
                    int sz = z + dz;
                    
                    if (sx >= MX) sx -= MX;
                    if (sy >= MY) sy -= MY;
                    if (sz >= MZ) sz -= MZ;
                    
                    byte output = rule.output[dx + dy * rule.OMX + dz * rule.OMX * rule.OMY];
                    if (output != (byte) 0xff) {
                        state[sx + sy * MX + sz * MX * MY] = output;
                    }
                }
            }
        }
    }
    
    @Override
    public boolean go() {
        if (n >= 0) {
            return super.go();
        }
        
        newgrid.clear();
        for (Rule rule : rules) {
            for (int z = 0; z < grid.MZ; z++) {
                for (int y = 0; y < grid.MY; y++) {
                    for (int x = 0; x < grid.MX; x++) {
                        if (matches(rule, x, y, z, grid.state, grid.MX, grid.MY, grid.MZ)) {
                            apply(rule, x * NX / DX, y * NY / DY, z * NZ / DZ, 
                                 newgrid.state, newgrid.MX, newgrid.MY, newgrid.MZ);
                        }
                    }
                }
            }
        }
        
        ip.grid = newgrid;
        n++;
        return true;
    }
    
    @Override
    public void reset() {
        super.reset();
        n = -1;
    }
    
    private static class ScalePair {
        public final int numerator, denominator;
        
        public ScalePair(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }
}