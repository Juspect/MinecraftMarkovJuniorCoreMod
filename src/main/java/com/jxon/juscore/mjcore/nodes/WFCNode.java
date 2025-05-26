// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.utils.RandomHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.Random;

public abstract class WFCNode extends Branch {
    protected Wave wave;
    protected int[][][] propagator;
    protected int P, N = 1;
    
    private WFCStackItem[] stack;
    private int stacksize;
    
    protected double[] weights;
    private double[] weightLogWeights;
    private double sumOfWeights, sumOfWeightLogWeights, startingEntropy;
    
    protected Grid newgrid;
    private Wave startwave;
    
    protected Map<Byte, boolean[]> map;
    protected boolean periodic, shannon;
    
    private double[] distribution;
    private int tries;
    
    public String name;
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        shannon = XMLHelper.get(element, "shannon", false);
        tries = XMLHelper.get(element, "tries", 1000);
        
        wave = new Wave(grid.state.length, P, propagator.length, shannon);
        startwave = new Wave(grid.state.length, P, propagator.length, shannon);
        stack = new WFCStackItem[wave.data.length * P];
        
        sumOfWeights = sumOfWeightLogWeights = startingEntropy = 0;
        
        if (shannon) {
            weightLogWeights = new double[P];
            
            for (int t = 0; t < P; t++) {
                weightLogWeights[t] = weights[t] * Math.log(weights[t]);
                sumOfWeights += weights[t];
                sumOfWeightLogWeights += weightLogWeights[t];
            }
            
            startingEntropy = Math.log(sumOfWeights) - sumOfWeightLogWeights / sumOfWeights;
        }
        
        distribution = new double[P];
        return super.load(element, parentSymmetry, newgrid);
    }
    
    @Override
    public void reset() {
        super.reset();
        n = -1;
        firstgo = true;
    }
    
    private boolean firstgo = true;
    private Random random;

    @Override
    public boolean go() {
        if (n >= 0) {
            return super.go();
        }

        if (firstgo) {
            wave.init(propagator, sumOfWeights, sumOfWeightLogWeights, startingEntropy, shannon);

            for (int i = 0; i < wave.data.length; i++) {
                byte value = grid.state[i];
                boolean[] startWave = map.get(value);
                if (startWave != null) {
                    for (int t = 0; t < P; t++) {
                        if (!startWave[t]) {
                            ban(i, t);
                        }
                    }
                }
            }

            boolean firstSuccess = propagate();
            if (!firstSuccess) {
                System.out.println("initial conditions are contradictive");
                return false;
            }
            startwave.copyFrom(wave, propagator.length, shannon);

            Integer goodseed = goodSeed();
            if (goodseed == null) {
                return false;
            }

            random = new Random(goodseed);
            stacksize = 0;
            wave.copyFrom(startwave, propagator.length, shannon);
            firstgo = false;

            newgrid.clear();
            ip.grid = newgrid;
            return true;
        } else {
            int node = nextUnobservedNode(random);
            if (node >= 0) {
                observe(node, random);
                boolean success = propagate(); // 关键：检查propagate结果
                if (!success) {
                    System.out.println("WFC: Contradiction detected, restarting...");
                    // 重置并重试
                    stacksize = 0;
                    wave.copyFrom(startwave, propagator.length, shannon);
                    return true; // 继续尝试
                }
            } else {
                n++;
            }

            if (n >= 0 || ip.gif) {
                updateState();
            }
            return true;
        }
    }

    // 修正的propagate方法
    private boolean propagate() {
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;

        while (stacksize > 0) {
            WFCStackItem item = stack[stacksize - 1];
            stacksize--;

            int i1 = item.i, p1 = item.p;
            int x1 = i1 % MX, y1 = (i1 % (MX * MY)) / MX, z1 = i1 / (MX * MY);

            for (int d = 0; d < propagator.length; d++) {
                int dx = DX[d], dy = DY[d], dz = DZ[d];
                int x2 = x1 + dx, y2 = y1 + dy, z2 = z1 + dz;

                // 边界检查
                if (!periodic && (x2 < 0 || y2 < 0 || z2 < 0 ||
                        x2 + N > MX || y2 + N > MY || z2 + 1 > MZ)) {
                    continue;
                }

                // 周期性边界处理
                if (x2 < 0) x2 += MX;
                else if (x2 >= MX) x2 -= MX;
                if (y2 < 0) y2 += MY;
                else if (y2 >= MY) y2 -= MY;
                if (z2 < 0) z2 += MZ;
                else if (z2 >= MZ) z2 -= MZ;

                int i2 = x2 + y2 * MX + z2 * MX * MY;

                // 安全检查
                if (i2 < 0 || i2 >= wave.data.length) {
                    continue;
                }

                int[] p = propagator[d][p1];
                int[][] compat = wave.compatible[i2];

                for (int l = 0; l < p.length; l++) {
                    int t2 = p[l];
                    if (t2 >= 0 && t2 < compat.length) {
                        int[] comp = compat[t2];

                        comp[d]--;
                        if (comp[d] == 0) {
                            ban(i2, t2);
                        }
                    }
                }
            }
        }

        // 检查是否还有可能的状态
        boolean hasValidStates = false;
        for (int sumsOfOnes : wave.sumsOfOnes) {
            if (sumsOfOnes > 0) {
                hasValidStates = true;
                break;
            }
        }

        return hasValidStates;
    }

    // 修正的ban方法
    private void ban(int i, int t) {
        // 添加边界检查
        if (i < 0 || i >= wave.data.length || t < 0 || t >= P) {
            System.out.println("DEBUG: Invalid ban parameters: i=" + i + ", t=" + t + ", data.length=" + wave.data.length + ", P=" + P);
            return;
        }

        if (!wave.data[i][t]) {
            return; // 已经被禁用了
        }

        wave.data[i][t] = false;

        int[] comp = wave.compatible[i][t];
        for (int d = 0; d < propagator.length; d++) {
            comp[d] = 0;
        }

        if (stacksize < stack.length) {
            stack[stacksize] = new WFCStackItem(i, t);
            stacksize++;
        }

        wave.sumsOfOnes[i] -= 1;
        if (shannon && weights != null && wave.sumsOfWeights != null) {
            double sum = wave.sumsOfWeights[i];
            if (sum > 0) {
                wave.entropies[i] += wave.sumsOfWeightLogWeights[i] / sum - Math.log(sum);

                wave.sumsOfWeights[i] -= weights[t];
                wave.sumsOfWeightLogWeights[i] -= weightLogWeights[t];

                sum = wave.sumsOfWeights[i];
                if (sum > 0) {
                    wave.entropies[i] -= wave.sumsOfWeightLogWeights[i] / sum - Math.log(sum);
                }
            }
        }
    }
    // 修正的goodSeed方法，增加重试机制
    private Integer goodSeed() {
        for (int k = 0; k < tries; k++) {
            int observationsSoFar = 0;
            int seed = ip.random.nextInt();
            random = new Random(seed);
            stacksize = 0;
            wave.copyFrom(startwave, propagator.length, shannon);

            boolean success = true;
            while (success) {
                int node = nextUnobservedNode(random);
                if (node >= 0) {
                    observe(node, random);
                    observationsSoFar++;
                    success = propagate();
                    if (!success) {
                        System.out.println("CONTRADICTION on try " + k + " with " + observationsSoFar + " observations");
                        break;
                    }
                } else {
                    System.out.println("wfc found a good seed " + seed + " on try " + k + " with " + observationsSoFar + " observations");
                    return seed;
                }
            }
        }

        System.out.println("wfc failed to find a good seed in " + tries + " tries");
        return null;
    }
    
    private int nextUnobservedNode(Random random) {
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;
        double min = 1E+4;
        int argmin = -1;
        
        for (int z = 0; z < MZ; z++) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    if (!periodic && (x + N > MX || y + N > MY || z + 1 > MZ)) {
                        continue;
                    }
                    int i = x + y * MX + z * MX * MY;
                    int remainingValues = wave.sumsOfOnes[i];
                    double entropy = shannon ? wave.entropies[i] : remainingValues;
                    if (remainingValues > 1 && entropy <= min) {
                        double noise = 1E-6 * random.nextDouble();
                        if (entropy + noise < min) {
                            min = entropy + noise;
                            argmin = i;
                        }
                    }
                }
            }
        }
        return argmin;
    }
    
    private void observe(int node, Random random) {
        boolean[] w = wave.data[node];
        for (int t = 0; t < P; t++) {
            distribution[t] = w[t] ? weights[t] : 0.0;
        }
        int r = RandomHelper.random(distribution, random.nextDouble());
        for (int t = 0; t < P; t++) {
            if (w[t] != (t == r)) {
                ban(node, t);
            }
        }
    }

    protected abstract void updateState();
    
    protected static final int[] DX = {1, 0, -1, 0, 0, 0};
    protected static final int[] DY = {0, 1, 0, -1, 0, 0};
    protected static final int[] DZ = {0, 0, 0, 0, 1, -1};
    
    private static class WFCStackItem {
        public final int i, p;
        
        public WFCStackItem(int i, int p) {
            this.i = i;
            this.p = p;
        }
    }
}

class Wave {
    public boolean[][] data;
    public int[][][] compatible;
    
    public int[] sumsOfOnes;
    public double[] sumsOfWeights, sumsOfWeightLogWeights, entropies;
    
    public Wave(int length, int P, int D, boolean shannon) {
        data = AH.array2D(length, P, true);
        compatible = AH.array3D(length, P, D, -1);
        sumsOfOnes = new int[length];
        
        if (shannon) {
            sumsOfWeights = new double[length];
            sumsOfWeightLogWeights = new double[length];
            entropies = new double[length];
        }
    }

    public void init(int[][][] propagator, double sumOfWeights, double sumOfWeightLogWeights,
                     double startingEntropy, boolean shannon) {
        int P = data[0].length;
        for (int i = 0; i < data.length; i++) {
            for (int p = 0; p < P; p++) {
                data[i][p] = true;
                for (int d = 0; d < propagator.length; d++) {
                    // 关键修正：确保 opposite 索引正确且在边界内
                    int oppositeDir = opposite[d];
                    if (oppositeDir >= 0 && oppositeDir < propagator.length &&
                            p < propagator[oppositeDir].length) {
                        compatible[i][p][d] = propagator[oppositeDir][p].length;
                    } else {
                        compatible[i][p][d] = 0;
                    }
                }
            }

            sumsOfOnes[i] = P;
            if (shannon && sumsOfWeights != null && sumsOfWeightLogWeights != null && entropies != null) {
                sumsOfWeights[i] = sumOfWeights;
                sumsOfWeightLogWeights[i] = sumOfWeightLogWeights;
                entropies[i] = startingEntropy;
            }
        }
    }
    
    public void copyFrom(Wave wave, int D, boolean shannon) {
        for (int i = 0; i < data.length; i++) {
            boolean[] datai = data[i], wavedatai = wave.data[i];
            for (int t = 0; t < datai.length; t++) {
                datai[t] = wavedatai[t];
                for (int d = 0; d < D; d++) {
                    compatible[i][t][d] = wave.compatible[i][t][d];
                }
            }
            
            sumsOfOnes[i] = wave.sumsOfOnes[i];
            
            if (shannon) {
                sumsOfWeights[i] = wave.sumsOfWeights[i];
                sumsOfWeightLogWeights[i] = wave.sumsOfWeightLogWeights[i];
                entropies[i] = wave.entropies[i];
            }
        }
    }
    
    private static final int[] opposite = {2, 3, 0, 1, 5, 4};
}