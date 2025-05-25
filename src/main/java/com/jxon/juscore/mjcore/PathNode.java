// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class PathNode extends Node {
    public int start, finish, substrate;
    public byte value;
    private boolean inertia, longest, edges, vertices;
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        String startSymbols = XMLHelper.get(element, "from");
        start = grid.wave(startSymbols);
        value = grid.values.get(XMLHelper.get(element, "color", startSymbols.charAt(0)));
        finish = grid.wave(XMLHelper.get(element, "to"));
        inertia = XMLHelper.get(element, "inertia", false);
        longest = XMLHelper.get(element, "longest", false);
        edges = XMLHelper.get(element, "edges", false);
        vertices = XMLHelper.get(element, "vertices", false);
        substrate = grid.wave(XMLHelper.get(element, "on"));
        return true;
    }
    
    @Override
    public void reset() {}
    
    @Override
    public boolean go() {
        Queue<PathQueueItem> frontier = new LinkedList<>();
        List<Rule.Tuple3> startPositions = new ArrayList<>();
        int[] generations = AH.array1D(grid.state.length, -1);
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;
        
        for (int z = 0; z < MZ; z++) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    int i = x + y * MX + z * MX * MY;
                    generations[i] = -1;
                    
                    byte s = grid.state[i];
                    if ((start & (1 << s)) != 0) {
                        startPositions.add(new Rule.Tuple3(x, y, z));
                    }
                    if ((finish & (1 << s)) != 0) {
                        generations[i] = 0;
                        frontier.offer(new PathQueueItem(0, x, y, z));
                    }
                }
            }
        }
        
        if (startPositions.isEmpty() || frontier.isEmpty()) {
            return false;
        }
        
        while (!frontier.isEmpty()) {
            PathQueueItem item = frontier.poll();
            int t = item.t, x = item.x, y = item.y, z = item.z;
            
            for (Rule.Tuple3 direction : getDirections(x, y, z, MX, MY, MZ, edges, vertices)) {
                push(t, x + direction.x(), y + direction.y(), z + direction.z(), generations, frontier, MX, MY, MZ);
            }
        }
        
        if (startPositions.stream().noneMatch(p -> generations[p.x() + p.y() * MX + p.z() * MX * MY] > 0)) {
            return false;
        }
        
        Random localRandom = new Random(ip.random.nextInt());
        double min = MX * MY * MZ, max = -2;
        Rule.Tuple3 argmin = new Rule.Tuple3(-1, -1, -1), argmax = new Rule.Tuple3(-1, -1, -1);
        
        for (Rule.Tuple3 p : startPositions) {
            int g = generations[p.x() + p.y() * MX + p.z() * MX * MY];
            if (g == -1) continue;
            
            double dg = g;
            double noise = 0.1 * localRandom.nextDouble();
            
            if (dg + noise < min) {
                min = dg + noise;
                argmin = p;
            }
            
            if (dg + noise > max) {
                max = dg + noise;
                argmax = p;
            }
        }
        
        Rule.Tuple3 pen = longest ? argmax : argmin;
        int penx = pen.x(), peny = pen.y(), penz = pen.z();
        
        Rule.Tuple3 dir = direction(penx, peny, penz, 0, 0, 0, generations, localRandom);
        penx += dir.x();
        peny += dir.y();
        penz += dir.z();
        
        while (generations[penx + peny * MX + penz * MX * MY] != 0) {
            grid.state[penx + peny * MX + penz * MX * MY] = value;
            ip.changes.add(new Rule.Tuple3(penx, peny, penz));
            dir = direction(penx, peny, penz, dir.x(), dir.y(), dir.z(), generations, localRandom);
            penx += dir.x();
            peny += dir.y();
            penz += dir.z();
        }
        
        return true;
    }
    
    private void push(int t, int x, int y, int z, int[] generations, Queue<PathQueueItem> frontier, int MX, int MY, int MZ) {
        if (x < 0 || y < 0 || z < 0 || x >= MX || y >= MY || z >= MZ) return;
        
        int i = x + y * MX + z * MX * MY;
        byte v = grid.state[i];
        if (generations[i] == -1 && ((substrate & (1 << v)) != 0 || (start & (1 << v)) != 0)) {
            if ((substrate & (1 << v)) != 0) {
                frontier.offer(new PathQueueItem(t, x, y, z));
            }
            generations[i] = t;
        }
    }
    
    private Rule.Tuple3 direction(int x, int y, int z, int dx, int dy, int dz, int[] generations, Random random) {
        List<Rule.Tuple3> candidates = new ArrayList<>();
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;
        int g = generations[x + y * MX + z * MX * MY];
        
        if (!vertices && !edges) {
            if (dx != 0 || dy != 0 || dz != 0) {
                int cx = x + dx, cy = y + dy, cz = z + dz;
                if (inertia && cx >= 0 && cy >= 0 && cz >= 0 && cx < MX && cy < MY && cz < MZ && 
                    generations[cx + cy * MX + cz * MX * MY] == g - 1) {
                    return new Rule.Tuple3(dx, dy, dz);
                }
            }
            
            if (x > 0 && generations[x - 1 + y * MX + z * MX * MY] == g - 1) 
                candidates.add(new Rule.Tuple3(-1, 0, 0));
            if (x < MX - 1 && generations[x + 1 + y * MX + z * MX * MY] == g - 1) 
                candidates.add(new Rule.Tuple3(1, 0, 0));
            if (y > 0 && generations[x + (y - 1) * MX + z * MX * MY] == g - 1) 
                candidates.add(new Rule.Tuple3(0, -1, 0));
            if (y < MY - 1 && generations[x + (y + 1) * MX + z * MX * MY] == g - 1) 
                candidates.add(new Rule.Tuple3(0, 1, 0));
            if (z > 0 && generations[x + y * MX + (z - 1) * MX * MY] == g - 1) 
                candidates.add(new Rule.Tuple3(0, 0, -1));
            if (z < MZ - 1 && generations[x + y * MX + (z + 1) * MX * MY] == g - 1) 
                candidates.add(new Rule.Tuple3(0, 0, 1));
            
            return RandomHelper.random(candidates, random);
        } else {
            for (Rule.Tuple3 p : getDirections(x, y, z, MX, MY, MZ, edges, vertices)) {
                if (x + p.x() >= 0 && y + p.y() >= 0 && z + p.z() >= 0 &&
                    x + p.x() < MX && y + p.y() < MY && z + p.z() < MZ &&
                    generations[x + p.x() + (y + p.y()) * MX + (z + p.z()) * MX * MY] == g - 1) {
                    candidates.add(p);
                }
            }
            
            Rule.Tuple3 result = new Rule.Tuple3(-1, -1, -1);
            
            if (inertia && (dx != 0 || dy != 0 || dz != 0)) {
                double maxScalar = -4;
                for (Rule.Tuple3 c : candidates) {
                    double noise = 0.1 * random.nextDouble();
                    double cos = (c.x() * dx + c.y() * dy + c.z() * dz) /
                               Math.sqrt((c.x() * c.x() + c.y() * c.y() + c.z() * c.z()) * (dx * dx + dy * dy + dz * dz));
                    
                    if (cos + noise > maxScalar) {
                        maxScalar = cos + noise;
                        result = c;
                    }
                }
            } else {
                result = RandomHelper.random(candidates, random);
            }
            
            return result;
        }
    }
    
    private static List<Rule.Tuple3> getDirections(int x, int y, int z, int MX, int MY, int MZ, boolean edges, boolean vertices) {
        List<Rule.Tuple3> result = new ArrayList<>();
        
        if (MZ == 1) {
            if (x > 0) result.add(new Rule.Tuple3(-1, 0, 0));
            if (x < MX - 1) result.add(new Rule.Tuple3(1, 0, 0));
            if (y > 0) result.add(new Rule.Tuple3(0, -1, 0));
            if (y < MY - 1) result.add(new Rule.Tuple3(0, 1, 0));
            
            if (edges) {
                if (x > 0 && y > 0) result.add(new Rule.Tuple3(-1, -1, 0));
                if (x > 0 && y < MY - 1) result.add(new Rule.Tuple3(-1, 1, 0));
                if (x < MX - 1 && y > 0) result.add(new Rule.Tuple3(1, -1, 0));
                if (x < MX - 1 && y < MY - 1) result.add(new Rule.Tuple3(1, 1, 0));
            }
        } else {
            if (x > 0) result.add(new Rule.Tuple3(-1, 0, 0));
            if (x < MX - 1) result.add(new Rule.Tuple3(1, 0, 0));
            if (y > 0) result.add(new Rule.Tuple3(0, -1, 0));
            if (y < MY - 1) result.add(new Rule.Tuple3(0, 1, 0));
            if (z > 0) result.add(new Rule.Tuple3(0, 0, -1));
            if (z < MZ - 1) result.add(new Rule.Tuple3(0, 0, 1));
            
            if (edges) {
                if (x > 0 && y > 0) result.add(new Rule.Tuple3(-1, -1, 0));
                if (x > 0 && y < MY - 1) result.add(new Rule.Tuple3(-1, 1, 0));
                if (x < MX - 1 && y > 0) result.add(new Rule.Tuple3(1, -1, 0));
                if (x < MX - 1 && y < MY - 1) result.add(new Rule.Tuple3(1, 1, 0));
                
                if (x > 0 && z > 0) result.add(new Rule.Tuple3(-1, 0, -1));
                if (x > 0 && z < MZ - 1) result.add(new Rule.Tuple3(-1, 0, 1));
                if (x < MX - 1 && z > 0) result.add(new Rule.Tuple3(1, 0, -1));
                if (x < MX - 1 && z < MZ - 1) result.add(new Rule.Tuple3(1, 0, 1));
                
                if (y > 0 && z > 0) result.add(new Rule.Tuple3(0, -1, -1));
                if (y > 0 && z < MZ - 1) result.add(new Rule.Tuple3(0, -1, 1));
                if (y < MY - 1 && z > 0) result.add(new Rule.Tuple3(0, 1, -1));
                if (y < MY - 1 && z < MZ - 1) result.add(new Rule.Tuple3(0, 1, 1));
            }
            
            if (vertices) {
                if (x > 0 && y > 0 && z > 0) result.add(new Rule.Tuple3(-1, -1, -1));
                if (x > 0 && y > 0 && z < MZ - 1) result.add(new Rule.Tuple3(-1, -1, 1));
                if (x > 0 && y < MY - 1 && z > 0) result.add(new Rule.Tuple3(-1, 1, -1));
                if (x > 0 && y < MY - 1 && z < MZ - 1) result.add(new Rule.Tuple3(-1, 1, 1));
                if (x < MX - 1 && y > 0 && z > 0) result.add(new Rule.Tuple3(1, -1, -1));
                if (x < MX - 1 && y > 0 && z < MZ - 1) result.add(new Rule.Tuple3(1, -1, 1));
                if (x < MX - 1 && y < MY - 1 && z > 0) result.add(new Rule.Tuple3(1, 1, -1));
                if (x < MX - 1 && y < MY - 1 && z < MZ - 1) result.add(new Rule.Tuple3(1, 1, 1));
            }
        }
        
        return result;
    }
    
    private static class PathQueueItem {
        public final int t, x, y, z;
        
        public PathQueueItem(int t, int x, int y, int z) {
            this.t = t;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}