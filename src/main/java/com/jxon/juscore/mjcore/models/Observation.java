// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.models;

import com.jxon.juscore.mjcore.utils.AH;

import java.util.LinkedList;
import java.util.Queue;

public class Observation {
    private final byte from;
    private final int to;
    
    public Observation(char from, String to, Grid grid) {
        this.from = grid.values.get(from);
        this.to = grid.wave(to);
    }
    
    public static boolean computeFutureSetPresent(int[] future, byte[] state, Observation[] observations) {
        boolean[] mask = new boolean[observations.length];
        for (int k = 0; k < observations.length; k++) {
            if (observations[k] == null) {
                mask[k] = true;
            }
        }
        
        for (int i = 0; i < state.length; i++) {
            byte value = state[i];
            Observation obs = observations[value];
            mask[value] = true;
            if (obs != null) {
                future[i] = obs.to;
                state[i] = obs.from;
            } else {
                future[i] = 1 << value;
            }
        }
        
        for (int k = 0; k < mask.length; k++) {
            if (!mask[k]) {
                return false;
            }
        }
        return true;
    }
    
    public static void computeForwardPotentials(int[][] potentials, byte[] state, int MX, int MY, int MZ, Rule[] rules) {
        AH.set2D(potentials, -1);
        for (int i = 0; i < state.length; i++) {
            potentials[state[i]][i] = 0;
        }
        computePotentials(potentials, MX, MY, MZ, rules, false);
    }
    
    public static void computeBackwardPotentials(int[][] potentials, int[] future, int MX, int MY, int MZ, Rule[] rules) {
        for (int c = 0; c < potentials.length; c++) {
            int[] potential = potentials[c];
            for (int i = 0; i < future.length; i++) {
                potential[i] = (future[i] & (1 << c)) != 0 ? 0 : -1;
            }
        }
        computePotentials(potentials, MX, MY, MZ, rules, true);
    }
    
    private static void computePotentials(int[][] potentials, int MX, int MY, int MZ, Rule[] rules, boolean backwards) {
        Queue<ObservationQueueItem> queue = new LinkedList<>();
        for (byte c = 0; c < potentials.length; c++) {
            int[] potential = potentials[c];
            for (int i = 0; i < potential.length; i++) {
                if (potential[i] == 0) {
                    queue.offer(new ObservationQueueItem(c, i % MX, (i % (MX * MY)) / MX, i / (MX * MY)));
                }
            }
        }
        
        boolean[][] matchMask = AH.array2D(rules.length, potentials[0].length, false);
        
        while (!queue.isEmpty()) {
            ObservationQueueItem item = queue.poll();
            byte value = item.value;
            int x = item.x, y = item.y, z = item.z;
            int i = x + y * MX + z * MX * MY;
            int t = potentials[value][i];
            
            for (int r = 0; r < rules.length; r++) {
                boolean[] maskr = matchMask[r];
                Rule rule = rules[r];
                Rule.Tuple3[] shifts = backwards ? rule.oshifts[value] : rule.ishifts[value];
                
                for (Rule.Tuple3 shift : shifts) {
                    int sx = x - shift.x();
                    int sy = y - shift.y();
                    int sz = z - shift.z();
                    
                    if (sx < 0 || sy < 0 || sz < 0 || 
                        sx + rule.IMX > MX || sy + rule.IMY > MY || sz + rule.IMZ > MZ) {
                        continue;
                    }
                    
                    int si = sx + sy * MX + sz * MX * MY;
                    if (!maskr[si] && forwardMatches(rule, sx, sy, sz, potentials, t, MX, MY, backwards)) {
                        maskr[si] = true;
                        applyForward(rule, sx, sy, sz, potentials, t, MX, MY, queue, backwards);
                    }
                }
            }
        }
    }
    
    private static boolean forwardMatches(Rule rule, int x, int y, int z, int[][] potentials, int t, int MX, int MY, boolean backwards) {
        int dz = 0, dy = 0, dx = 0;
        byte[] a = backwards ? rule.output : rule.binput;
        
        for (int di = 0; di < a.length; di++) {
            byte value = a[di];
            if (value != (byte) 0xff) {
                int current = potentials[value][x + dx + (y + dy) * MX + (z + dz) * MX * MY];
                if (current > t || current == -1) {
                    return false;
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
        return true;
    }
    
    private static void applyForward(Rule rule, int x, int y, int z, int[][] potentials, int t, int MX, int MY, 
                                    Queue<ObservationQueueItem> q, boolean backwards) {
        byte[] a = backwards ? rule.binput : rule.output;
        
        for (int dz = 0; dz < rule.IMZ; dz++) {
            int zdz = z + dz;
            for (int dy = 0; dy < rule.IMY; dy++) {
                int ydy = y + dy;
                for (int dx = 0; dx < rule.IMX; dx++) {
                    int xdx = x + dx;
                    int idi = xdx + ydy * MX + zdz * MX * MY;
                    int di = dx + dy * rule.IMX + dz * rule.IMX * rule.IMY;
                    byte o = a[di];
                    
                    if (o != (byte) 0xff && potentials[o][idi] == -1) {
                        potentials[o][idi] = t + 1;
                        q.offer(new ObservationQueueItem(o, xdx, ydy, zdz));
                    }
                }
            }
        }
    }
    
    public static boolean isGoalReached(byte[] present, int[] future) {
        for (int i = 0; i < present.length; i++) {
            if (((1 << present[i]) & future[i]) == 0) {
                return false;
            }
        }
        return true;
    }
    
    public static int forwardPointwise(int[][] potentials, int[] future) {
        int sum = 0;
        for (int i = 0; i < future.length; i++) {
            int f = future[i];
            int min = 1000, argmin = -1;
            
            for (int c = 0; c < potentials.length; c++, f >>>= 1) {
                int potential = potentials[c][i];
                if ((f & 1) == 1 && potential >= 0 && potential < min) {
                    min = potential;
                    argmin = c;
                }
            }
            
            if (argmin < 0) {
                return -1;
            }
            sum += min;
        }
        return sum;
    }
    
    public static int backwardPointwise(int[][] potentials, byte[] present) {
        int sum = 0;
        for (int i = 0; i < present.length; i++) {
            int potential = potentials[present[i]][i];
            if (potential < 0) {
                return -1;
            }
            sum += potential;
        }
        return sum;
    }
    
    private static class ObservationQueueItem {
        public final byte value;
        public final int x, y, z;
        
        public ObservationQueueItem(byte value, int x, int y, int z) {
            this.value = value;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}