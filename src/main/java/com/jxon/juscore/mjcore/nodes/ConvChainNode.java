// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.*;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.utils.Graphics;
import com.jxon.juscore.mjcore.utils.Helper;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;

import java.util.Arrays;

public class ConvChainNode extends Node {
    private int N;
    private double temperature;
    private double[] weights;

    public byte c0, c1;
    private boolean[] substrate;
    private byte substrateColor;
    private int counter, steps;

    public boolean[] sample;
    public int SMX, SMY;

    @Override
    protected boolean load(Element element, boolean[] symmetry, Grid grid) {
        if (grid.MZ != 1) {
            Interpreter.writeLine("convchain currently works only for 2d");
            return false;
        }

        String name = XMLHelper.get(element, "sample");
        String filename = "resources/samples/" + name + ".png";

        Graphics.LoadBitmapResult bitmapResult = Graphics.loadBitmap(filename);
        if (bitmapResult.data == null) {
            Interpreter.writeLine("couldn't load ConvChain sample " + filename);
            return false;
        }

        int[] bitmap = bitmapResult.data;
        SMX = bitmapResult.width;
        SMY = bitmapResult.height;

        sample = new boolean[bitmap.length];
        for (int i = 0; i < sample.length; i++) {
            // Check if pixel is white (0xFFFFFFFF) or close to white
            int color = bitmap[i];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            sample[i] = (r + g + b) > 128 * 3; // Consider bright pixels as true
        }

        N = XMLHelper.get(element, "n", 3);
        steps = XMLHelper.get(element, "steps", -1);
        temperature = XMLHelper.get(element, "temperature", 1.0);
        c0 = grid.values.get(XMLHelper.get(element, "black", Character.class));
        c1 = grid.values.get(XMLHelper.get(element, "white", Character.class));
        substrateColor = grid.values.get(XMLHelper.get(element, "on", Character.class));

        substrate = new boolean[grid.state.length];

        weights = new double[1 << (N * N)];
        for (int y = 0; y < SMY; y++) {
            for (int x = 0; x < SMX; x++) {
                final int finalX = x, finalY = y;
                boolean[] pattern = Helper.patternboolean((dx, dy) -> sample[(finalX + dx) % SMX + (finalY + dy) % SMY * SMX], N);

                Iterable<boolean[]> symmetries = SymmetryHelper.squareSymmetries(pattern,
                        q -> Helper.rotated(q, N), q -> Helper.reflected(q, N),
                        (q1, q2) -> false, symmetry);

                for (boolean[] q : symmetries) {
                    weights[Helper.index(q)] += 1;
                }
            }
        }

        for (int k = 0; k < weights.length; k++) {
            if (weights[k] <= 0) {
                weights[k] = 0.1;
            }
        }
        return true;
    }

    private void toggle(byte[] state, int i) {
        state[i] = state[i] == c0 ? c1 : c0;
    }

    @Override
    public boolean go() {
        if (steps > 0 && counter >= steps) {
            return false;
        }

        int MX = grid.MX, MY = grid.MY;
        byte[] state = grid.state;

        if (counter == 0) {
            boolean anySubstrate = false;
            for (int i = 0; i < substrate.length; i++) {
                if (state[i] == substrateColor) {
                    state[i] = ip.random.nextInt(2) == 0 ? c0 : c1;
                    substrate[i] = true;
                    anySubstrate = true;
                }
            }
            counter++;
            return anySubstrate;
        }

        for (int k = 0; k < state.length; k++) {
            int r = ip.random.nextInt(state.length);
            if (!substrate[r]) {
                continue;
            }

            int x = r % MX, y = r / MX;
            double q = 1;

            for (int sy = y - N + 1; sy <= y + N - 1; sy++) {
                for (int sx = x - N + 1; sx <= x + N - 1; sx++) {
                    int ind = 0, difference = 0;
                    for (int dy = 0; dy < N; dy++) {
                        for (int dx = 0; dx < N; dx++) {
                            int X = sx + dx;
                            if (X < 0) X += MX;
                            else if (X >= MX) X -= MX;

                            int Y = sy + dy;
                            if (Y < 0) Y += MY;
                            else if (Y >= MY) Y -= MY;

                            boolean value = state[X + Y * MX] == c1;
                            int power = 1 << (dy * N + dx);
                            ind += value ? power : 0;
                            if (X == x && Y == y) {
                                difference = value ? power : -power;
                            }
                        }
                    }

                    q *= weights[ind - difference] / weights[ind];
                }
            }

            if (q >= 1) {
                toggle(state, r);
                continue;
            }
            if (temperature != 1) {
                q = Math.pow(q, 1.0 / temperature);
            }
            if (q > ip.random.nextDouble()) {
                toggle(state, r);
            }
        }

        counter++;
        return true;
    }

    @Override
    public void reset() {
        Arrays.fill(substrate, false);
        counter = 0;
    }
}