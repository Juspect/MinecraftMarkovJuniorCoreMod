// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.utils.Graphics;
import com.jxon.juscore.mjcore.utils.Helper;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;

import java.util.*;
import java.util.function.Function;

public class OverlapNode extends WFCNode {
    private byte[][] patterns;

    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        if (grid.MZ != 1) {
            Interpreter.writeLine("overlapping model currently works only for 2d");
            return false;
        }

        N = XMLHelper.get(element, "n", 3);

        String symmetryString = XMLHelper.get(element, "symmetry", (String) null);
        boolean[] symmetry = SymmetryHelper.getSymmetry(true, symmetryString, parentSymmetry);
        if (symmetry == null) {
            Interpreter.writeLine("unknown symmetry " + symmetryString + " at line " + XMLHelper.getLineNumber(element));
            return false;
        }

        boolean periodicInput = XMLHelper.get(element, "periodicInput", true);

        newgrid = Grid.load(element, grid.MX, grid.MY, grid.MZ);
        if (newgrid == null) return false;
        periodic = true;

        name = XMLHelper.get(element, "sample");
        Graphics.LoadBitmapResult bitmapResult = Graphics.loadBitmap("resources/samples/" + name + ".png");
        if (bitmapResult.data == null) {
            Interpreter.writeLine("couldn't read sample " + name);
            return false;
        }

        int[] bitmap = bitmapResult.data;
        int SMX = bitmapResult.width;
        int SMY = bitmapResult.height;

        Helper.OrdsResult ordsResult = Helper.ords(bitmap);
        byte[] sample = ordsResult.result;
        int C = ordsResult.count;

        if (C > newgrid.C) {
            Interpreter.writeLine("there were more than " + newgrid.C + " colors in the sample");
            return false;
        }

        long W = Helper.power(C, N * N);

        // Function to convert index to pattern
        Function<Long, byte[]> patternFromIndex = ind -> {
            long residue = ind;
            long power = W;
            byte[] result = new byte[N * N];
            for (int i = 0; i < result.length; i++) {
                power /= C;
                int count = 0;
                while (residue >= power) {
                    residue -= power;
                    count++;
                }
                result[i] = (byte) count;
            }
            return result;
        };

        Map<Long, Integer> weights = new HashMap<>();
        List<Long> ordering = new ArrayList<>();

        int ymax = periodicInput ? grid.MY : grid.MY - N + 1;
        int xmax = periodicInput ? grid.MX : grid.MX - N + 1;

        for (int y = 0; y < ymax; y++) {
            for (int x = 0; x < xmax; x++) {
                final int fx = x, fy = y;
                byte[] pattern = Helper.pattern((dx, dy) -> sample[(fx + dx) % SMX + ((fy + dy) % SMY) * SMX], N);

                Iterable<byte[]> symmetries = SymmetryHelper.squareSymmetries(pattern,
                        q -> Helper.rotated(q, N), q -> Helper.reflected(q, N),
                        (q1, q2) -> false, symmetry);

                for (byte[] p : symmetries) {
                    long ind = Helper.index(p, C);
                    // 修正：与C#版本逻辑一致
                    if (weights.containsKey(ind)) {
                        weights.put(ind, weights.get(ind) + 1);
                    } else {
                        weights.put(ind, 1);
                        ordering.add(ind);
                    }
                }
            }
        }

        P = weights.size();
        System.out.println("number of patterns P = " + P);

        patterns = new byte[P][];
        this.weights = new double[P];
        int counter = 0;
        for (long w : ordering) {
            patterns[counter] = patternFromIndex.apply(w);
            this.weights[counter] = weights.get(w);
            counter++;
        }

        // Function to check if patterns agree
        Function3<byte[], byte[], Integer, Integer, Boolean> agrees = (p1, p2, dx, dy) -> {
            int xmin = dx < 0 ? 0 : dx;
            int xmax2 = dx < 0 ? dx + N : N;
            int ymin = dy < 0 ? 0 : dy;
            int ymax2 = dy < 0 ? dy + N : N;

            for (int y = ymin; y < ymax2; y++) {
                for (int x = xmin; x < xmax2; x++) {
                    if (p1[x + N * y] != p2[x - dx + N * (y - dy)]) {
                        return false;
                    }
                }
            }
            return true;
        };

        propagator = new int[4][][];
        for (int d = 0; d < 4; d++) {
            propagator[d] = new int[P][];
            for (int t = 0; t < P; t++) {
                List<Integer> list = new ArrayList<>();
                for (int t2 = 0; t2 < P; t2++) {
                    if (agrees.apply(patterns[t], patterns[t2], DX[d], DY[d])) {
                        list.add(t2);
                    }
                }
                propagator[d][t] = list.stream().mapToInt(Integer::intValue).toArray();
            }
        }

        // Initialize map
        map = new HashMap<>();
        List<Element> ruleElements = XMLHelper.getDirectChildElements(element, "rule");
        for (Element ruleElement : ruleElements) {
            char input = XMLHelper.get(ruleElement, "in", Character.class);
            String[] outputs = XMLHelper.get(ruleElement, "out").split("\\|");
            boolean[] position = new boolean[P];

            for (String outputStr : outputs) {
                byte outputValue = newgrid.values.get(outputStr.charAt(0));
                for (int t = 0; t < P; t++) {
                    if (patterns[t][0] == outputValue) {
                        position[t] = true;
                    }
                }
            }
            map.put(grid.values.get(input), position);
        }

        if (!map.containsKey((byte) 0)) {
            boolean[] allTrue = new boolean[P];
            Arrays.fill(allTrue, true);
            map.put((byte) 0, allTrue);
        }

        return super.load(element, parentSymmetry, grid);
    }

    @Override
    protected void updateState() {
        int MX = newgrid.MX, MY = newgrid.MY;
        int[][] votes = AH.array2D(newgrid.state.length, newgrid.C, 0);

        for (int i = 0; i < wave.data.length; i++) {
            boolean[] w = wave.data[i];
            int x = i % MX, y = i / MX;

            for (int p = 0; p < P; p++) {
                if (w[p]) {
                    byte[] pattern = patterns[p];
                    for (int dy = 0; dy < N; dy++) {
                        int ydy = y + dy;
                        if (ydy >= MY) ydy -= MY;

                        for (int dx = 0; dx < N; dx++) {
                            int xdx = x + dx;
                            if (xdx >= MX) xdx -= MX;

                            byte value = pattern[dx + dy * N];
                            votes[xdx + ydy * MX][value]++;
                        }
                    }
                }
            }
        }

        Random r = new Random();
        for (int i = 0; i < votes.length; i++) {
            double max = -1.0;
            byte argmax = (byte) 0xff;
            int[] v = votes[i];

            for (byte c = 0; c < v.length; c++) {
                double value = v[c] + 0.1 * r.nextDouble();
                if (value > max) {
                    argmax = c;
                    max = value;
                }
            }
            newgrid.state[i] = argmax;
        }
    }

    @FunctionalInterface
    private interface Function3<T, U, V, W, R> {
        R apply(T t, U u, V v, W w);
    }
}