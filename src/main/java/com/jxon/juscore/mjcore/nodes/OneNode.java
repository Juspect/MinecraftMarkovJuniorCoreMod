// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.models.Field;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.models.Observation;
import com.jxon.juscore.mjcore.models.Rule;
import com.jxon.juscore.mjcore.utils.AH;
import org.w3c.dom.Element;
import java.util.ArrayList;

public class OneNode extends RuleNode {
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        if (!super.load(element, parentSymmetry, grid)) {
            return false;
        }
        matches = new ArrayList<>();
        matchMask = AH.array2D(rules.length, grid.state.length, false);
        return true;
    }
    
    @Override
    public void reset() {
        super.reset();
        if (matchCount != 0) {
            AH.set2D(matchMask, false);
            matchCount = 0;
        }
    }
    
    private void apply(Rule rule, int x, int y, int z) {
        int MX = grid.MX, MY = grid.MY;
        
        for (int dz = 0; dz < rule.OMZ; dz++) {
            for (int dy = 0; dy < rule.OMY; dy++) {
                for (int dx = 0; dx < rule.OMX; dx++) {
                    byte newValue = rule.output[dx + dy * rule.OMX + dz * rule.OMX * rule.OMY];
                    if (newValue != (byte) 0xff) {
                        int sx = x + dx;
                        int sy = y + dy;
                        int sz = z + dz;
                        int si = sx + sy * MX + sz * MX * MY;
                        byte oldValue = grid.state[si];
                        if (newValue != oldValue) {
                            grid.state[si] = newValue;
                            ip.changes.add(new Rule.Tuple3(sx, sy, sz));
                        }
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
        
        RandomMatchResult result = randomMatch(ip.random);
        if (result.r < 0) {
            return false;
        } else {
            last[result.r] = true;
            apply(rules[result.r], result.x, result.y, result.z);
            counter++;
            return true;
        }
    }
    
    private RandomMatchResult randomMatch(java.util.Random random) {
        if (potentials != null) {
            if (observations != null && Observation.isGoalReached(grid.state, future)) {
                futureComputed = false;
                return new RandomMatchResult(-1, -1, -1, -1);
            }
            
            double max = -1000.0;
            int argmax = -1;
            
            double firstHeuristic = 0.0;
            boolean firstHeuristicComputed = false;
            
            for (int k = 0; k < matchCount; k++) {
                RuleMatch match = matches.get(k);
                int r = match.r(), x = match.x(), y = match.y(), z = match.z();
                int i = x + y * grid.MX + z * grid.MX * grid.MY;
                
                if (!grid.matches(rules[r], x, y, z)) {
                    matchMask[r][i] = false;
                    matches.set(k, matches.get(matchCount - 1));
                    matchCount--;
                    k--;
                } else {
                    Integer heuristic = Field.deltaPointwise(grid.state, rules[r], x, y, z, fields, potentials, grid.MX, grid.MY);
                    if (heuristic == null) {
                        continue;
                    }
                    
                    double h = heuristic;
                    if (!firstHeuristicComputed) {
                        firstHeuristic = h;
                        firstHeuristicComputed = true;
                    }
                    
                    double u = random.nextDouble();
                    double key = temperature > 0 ? Math.pow(u, Math.exp((h - firstHeuristic) / temperature)) : -h + 0.001 * u;
                    if (key > max) {
                        max = key;
                        argmax = k;
                    }
                }
            }
            
            if (argmax >= 0) {
                RuleMatch match = matches.get(argmax);
                return new RandomMatchResult(match.r(), match.x(), match.y(), match.z());
            } else {
                return new RandomMatchResult(-1, -1, -1, -1);
            }
        } else {
            while (matchCount > 0) {
                int matchIndex = random.nextInt(matchCount);
                
                RuleMatch match = matches.get(matchIndex);
                int r = match.r(), x = match.x(), y = match.y(), z = match.z();
                int i = x + y * grid.MX + z * grid.MX * grid.MY;
                
                matchMask[r][i] = false;
                matches.set(matchIndex, matches.get(matchCount - 1));
                matchCount--;
                
                if (grid.matches(rules[r], x, y, z)) {
                    return new RandomMatchResult(r, x, y, z);
                }
            }
            return new RandomMatchResult(-1, -1, -1, -1);
        }
    }
    
    private static class RandomMatchResult {
        public final int r, x, y, z;
        
        public RandomMatchResult(int r, int x, int y, int z) {
            this.r = r;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}