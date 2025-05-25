// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.models.Rule;
import org.w3c.dom.Element;

public class ParallelNode extends RuleNode {
    private byte[] newstate;
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        if (!super.load(element, parentSymmetry, grid)) {
            return false;
        }
        newstate = new byte[grid.state.length];
        return true;
    }
    
    @Override
    protected void add(int r, int x, int y, int z, boolean[] maskr) {
        Rule rule = rules[r];
        if (ip.random.nextDouble() > rule.p) {
            return;
        }
        
        last[r] = true;
        int MX = grid.MX, MY = grid.MY;
        
        for (int dz = 0; dz < rule.OMZ; dz++) {
            for (int dy = 0; dy < rule.OMY; dy++) {
                for (int dx = 0; dx < rule.OMX; dx++) {
                    byte newvalue = rule.output[dx + dy * rule.OMX + dz * rule.OMX * rule.OMY];
                    int idi = x + dx + (y + dy) * MX + (z + dz) * MX * MY;
                    if (newvalue != (byte) 0xff && newvalue != grid.state[idi]) {
                        newstate[idi] = newvalue;
                        ip.changes.add(new Rule.Tuple3(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
        matchCount++;
    }
    
    @Override
    public boolean go() {
        if (!super.go()) {
            return false;
        }
        
        for (int n = ip.first.get(ip.counter); n < ip.changes.size(); n++) {
            Rule.Tuple3 change = ip.changes.get(n);
            int x = change.x(), y = change.y(), z = change.z();
            int i = x + y * grid.MX + z * grid.MX * grid.MY;
            grid.state[i] = newstate[i];
        }
        
        counter++;
        return matchCount > 0;
    }
}