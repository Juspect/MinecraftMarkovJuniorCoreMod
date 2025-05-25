// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;

public class AllNode extends RuleNode {
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        if (!super.load(element, parentSymmetry, grid)) {
            return false;
        }
        matches = new ArrayList<>();
        matchMask = AH.array2D(rules.length, grid.state.length, false);
        return true;
    }
    
    private void fit(int r, int x, int y, int z, boolean[] newstate, int MX, int MY) {
        Rule rule = rules[r];
        for (int dz = 0; dz < rule.OMZ; dz++) {
            for (int dy = 0; dy < rule.OMY; dy++) {
                for (int dx = 0; dx < rule.OMX; dx++) {
                    byte value = rule.output[dx + dy * rule.OMX + dz * rule.OMX * rule.OMY];
                    if (value != (byte) 0xff && newstate[x + dx + (y + dy) * MX + (z + dz) * MX * MY]) {
                        return;
                    }
                }
            }
        }
        
        last[r] = true;
        for (int dz = 0; dz < rule.OMZ; dz++) {
            for (int dy = 0; dy < rule.OMY; dy++) {
                for (int dx = 0; dx < rule.OMX; dx++) {
                    byte newvalue = rule.output[dx + dy * rule.OMX + dz * rule.OMX * rule.OMY];
                    if (newvalue != (byte) 0xff) {
                        int sx = x + dx, sy = y + dy, sz = z + dz;
                        int i = sx + sy * MX + sz * MX * MY;
                        newstate[i] = true;
                        grid.state[i] = newvalue;
                        ip.changes.add(new Rule.Tuple3(sx, sy, sz));
                    }
                }
            }
        }
    }
    
    @Override
    public boolean go() {
        if (!super.go()) {
            return false;
        }
        lastMatchedTurn = ip.counter;
        
        if (trajectory != null) {
            if (counter >= trajectory.length) {
                return false;
            }
            System.arraycopy(trajectory[counter], 0, grid.state, 0, grid.state.length);
            counter++;
            return true;
        }
        
        if (matchCount == 0) {
            return false;
        }
        
        int MX = grid.MX, MY = grid.MY;
        if (potentials != null) {
            double firstHeuristic = 0;
            boolean firstHeuristicComputed = false;
            
            List<HeuristicPair> list = new ArrayList<>();
            for (int m = 0; m < matchCount; m++) {
                RuleMatch match = matches.get(m);
                int r = match.r(), x = match.x(), y = match.y(), z = match.z();
                Integer heuristic = Field.deltaPointwise(grid.state, rules[r], x, y, z, fields, potentials, grid.MX, grid.MY);
                if (heuristic != null) {
                    double h = heuristic;
                    if (!firstHeuristicComputed) {
                        firstHeuristic = h;
                        firstHeuristicComputed = true;
                    }
                    double u = ip.random.nextDouble();
                    double value = temperature > 0 ? Math.pow(u, Math.exp((h - firstHeuristic) / temperature)) : -h + 0.001 * u;
                    list.add(new HeuristicPair(m, value));
                }
            }
            
            list.sort((a, b) -> Double.compare(b.value, a.value));
            
            for (HeuristicPair pair : list) {
                RuleMatch match = matches.get(pair.index);
                int r = match.r(), x = match.x(), y = match.y(), z = match.z();
                matchMask[r][x + y * MX + z * MX * MY] = false;
                fit(r, x, y, z, grid.mask, MX, MY);
            }
        } else {
            int[] shuffle = new int[matchCount];
            for (int i = 0; i < shuffle.length; i++) {
                shuffle[i] = i;
            }
            RandomHelper.shuffle(shuffle, ip.random);

            for (int i : shuffle) {
                RuleMatch match = matches.get(i);
                int r = match.r(), x = match.x(), y = match.y(), z = match.z();
                matchMask[r][x + y * MX + z * MX * MY] = false;
                fit(r, x, y, z, grid.mask, MX, MY);
            }
        }
        
        for (int n = ip.first.get(lastMatchedTurn); n < ip.changes.size(); n++) {
            Rule.Tuple3 change = ip.changes.get(n);
            int x = change.x(), y = change.y(), z = change.z();
            grid.mask[x + y * MX + z * MX * MY] = false;
        }
        
        counter++;
        matchCount = 0;
        return true;
    }
    
    private static class HeuristicPair {
        public final int index;
        public final double value;
        
        public HeuristicPair(int index, double value) {
            this.index = index;
            this.value = value;
        }
    }
}