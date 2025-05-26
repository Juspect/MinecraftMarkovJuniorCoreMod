// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.models;

import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Field {
    public boolean recompute, inversed, essential;
    public int zero, substrate;
    
    public Field(Element element, Grid grid) {
        recompute = XMLHelper.get(element, "recompute", false);
        essential = XMLHelper.get(element, "essential", false);
        String on = XMLHelper.get(element, "on");
        substrate = grid.wave(on);
        
        String zeroSymbols = XMLHelper.get(element, "from", (String) null);
        if (zeroSymbols != null) {
            inversed = true;
        } else {
            zeroSymbols = XMLHelper.get(element, "to");
        }
        zero = grid.wave(zeroSymbols);
    }
    
    public boolean compute(int[] potential, Grid grid) {
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;
        Queue<FieldQueueItem> front = new LinkedList<>();
        
        int ix = 0, iy = 0, iz = 0;
        for (int i = 0; i < grid.state.length; i++) {
            potential[i] = -1;
            byte value = grid.state[i];
            if ((zero & (1 << value)) != 0) {
                potential[i] = 0;
                front.offer(new FieldQueueItem(0, ix, iy, iz));
            }
            
            ix++;
            if (ix == MX) {
                ix = 0;
                iy++;
                if (iy == MY) {
                    iy = 0;
                    iz++;
                }
            }
        }
        
        if (front.isEmpty()) {
            return false;
        }
        
        while (!front.isEmpty()) {
            FieldQueueItem item = front.poll();
            int t = item.t, x = item.x, y = item.y, z = item.z;
            List<Rule.Tuple3> neighbors = getNeighbors(x, y, z, MX, MY, MZ);
            
            for (Rule.Tuple3 neighbor : neighbors) {
                int nx = neighbor.x(), ny = neighbor.y(), nz = neighbor.z();
                int i = nx + ny * grid.MX + nz * grid.MX * grid.MY;
                byte v = grid.state[i];
                if (potential[i] == -1 && (substrate & (1 << v)) != 0) {
                    front.offer(new FieldQueueItem(t + 1, nx, ny, nz));
                    potential[i] = t + 1;
                }
            }
        }
        
        return true;
    }
    
    private static List<Rule.Tuple3> getNeighbors(int x, int y, int z, int MX, int MY, int MZ) {
        List<Rule.Tuple3> result = new ArrayList<>();
        
        if (x > 0) result.add(new Rule.Tuple3(x - 1, y, z));
        if (x < MX - 1) result.add(new Rule.Tuple3(x + 1, y, z));
        if (y > 0) result.add(new Rule.Tuple3(x, y - 1, z));
        if (y < MY - 1) result.add(new Rule.Tuple3(x, y + 1, z));
        if (z > 0) result.add(new Rule.Tuple3(x, y, z - 1));
        if (z < MZ - 1) result.add(new Rule.Tuple3(x, y, z + 1));
        
        return result;
    }
    
    public static Integer deltaPointwise(byte[] state, Rule rule, int x, int y, int z, 
                                        Field[] fields, int[][] potentials, int MX, int MY) {
        int sum = 0;
        int dz = 0, dy = 0, dx = 0;
        
        for (int di = 0; di < rule.input.length; di++) {
            byte newValue = rule.output[di];
            if (newValue != (byte) 0xff && (rule.input[di] & (1 << newValue)) == 0) {
                int i = x + dx + (y + dy) * MX + (z + dz) * MX * MY;
                int newPotential = potentials[newValue][i];
                if (newPotential == -1) {
                    return null;
                }
                
                byte oldValue = state[i];
                int oldPotential = potentials[oldValue][i];
                sum += newPotential - oldPotential;
                
                if (fields != null) {
                    Field oldField = fields[oldValue];
                    if (oldField != null && oldField.inversed) {
                        sum += 2 * oldPotential;
                    }
                    Field newField = fields[newValue];
                    if (newField != null && newField.inversed) {
                        sum -= 2 * newPotential;
                    }
                }
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
        return sum;
    }

    private record FieldQueueItem(int t, int x, int y, int z) {
    }
}