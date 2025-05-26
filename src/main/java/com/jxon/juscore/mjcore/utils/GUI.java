// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import com.jxon.juscore.mjcore.models.Rule;
import com.jxon.juscore.mjcore.nodes.*;
import com.jxon.juscore.mjcore.utils.Helper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public final class GUI {

    private static final int S, SMALL, MAXWIDTH, ZSHIFT, HINDENT, HGAP, HARROW, HLINE, VSKIP, SMALLVSKIP, FONTSHIFT, AFTERFONT;
    private static final boolean DENSE, D3;
    public static final int BACKGROUND, INACTIVE, ACTIVE;

    private static final String FONT = "Tamzen8x16r", TITLEFONT = "Tamzen8x16b";
    private static final FontData[] fonts;

    private static final char[] legend = "ABCDEFGHIJKLMNOPQRSTUVWXYZ 12345abcdefghijklmnopqrstuvwxyz\u03bb67890{}[]()<>$*-+=/#_%^@\\&|~?'\"`!,.;:".toCharArray();
    private static final Map<Character, Byte> map;

    static {
        map = new HashMap<>();
        for (int i = 0; i < legend.length; i++) {
            map.put(legend[i], (byte) i);
        }
        fonts = new FontData[2];

        // Load font data - in real implementation would load from PNG files
        // For now using placeholder data
        fonts[0] = new FontData(new boolean[8*16*32*3], 8, 16); // Normal font
        fonts[1] = new FontData(new boolean[8*16*32*3], 8, 16); // Title font

        // Load settings from XML or use defaults
        S = 7;
        SMALL = 3;
        MAXWIDTH = 10;
        ZSHIFT = 2;
        HINDENT = 30;
        HGAP = 2;
        HARROW = 10;
        HLINE = 14;
        VSKIP = 2;
        SMALLVSKIP = 2;
        FONTSHIFT = 2;
        AFTERFONT = 4;
        DENSE = true;
        D3 = true;
        BACKGROUND = 0xFF222222;
        INACTIVE = 0xFF666666;
        ACTIVE = 0xFFFFFFFF;
    }

    public static void draw(String name, Branch root, Branch current, int[] bitmap,
                            int WIDTH, int HEIGHT, Map<Character, Integer> palette) {

        Map<Node, NodePosition> nodePositions = new HashMap<>();

        // Draw background
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = BACKGROUND;
        }

        int y = fonts[1].FY / 2;
        write(bitmap, name, 8, y, ACTIVE, WIDTH, HEIGHT, 1);
        y += (int)(AFTERFONT * fonts[1].FY / 2);

        // Draw node tree
        y = drawNodeTree(root, current, bitmap, WIDTH, HEIGHT, palette, nodePositions, y, 0);

        // Draw connection lines
        drawConnections(root, current, bitmap, WIDTH, HEIGHT, nodePositions);
    }

    private static int drawNodeTree(Node node, Branch current, int[] bitmap, int WIDTH, int HEIGHT,
                                    Map<Character, Integer> palette, Map<Node, NodePosition> positions,
                                    int y, int level) {
        if (node == null) return y;

        positions.put(node, new NodePosition(level, y));
        int x = level * HINDENT;

        if (node instanceof Branch) {
            Branch branch = (Branch) node;
            int LINECOLOR = branch == current && branch.n < 0 ? ACTIVE : INACTIVE;

            if (branch instanceof WFCNode) {
                WFCNode wfcnode = (WFCNode) branch;
                write(bitmap, "wfc " + wfcnode.name, x, y, LINECOLOR, WIDTH, HEIGHT, 0);
                y += fonts[0].FY + VSKIP;
            } else if (branch instanceof MapNode) {
                MapNode mapnode = (MapNode) branch;
                y = drawMapNode(mapnode, bitmap, x, y, WIDTH, HEIGHT, LINECOLOR, level);
            }

            // Draw child nodes
            boolean markov = branch instanceof MarkovNode;
            boolean sequence = branch instanceof SequenceNode;
            for (Node child : branch.nodes) {
                y = drawNodeTree(child, current, bitmap, WIDTH, HEIGHT, palette, positions, y,
                        markov || sequence ? level + 1 : level);
                drawDash(child, positions, bitmap, WIDTH, HEIGHT, markov, false);
            }
        } else {
            boolean active = current != null && current.n >= 0 &&
                    current.n < current.nodes.length && current.nodes[current.n] == node;
            int NODECOLOR = active ? ACTIVE : INACTIVE;

            if (node instanceof RuleNode) {
                y = drawRuleNode((RuleNode) node, bitmap, x, y, WIDTH, HEIGHT, NODECOLOR, active, level);
            } else if (node instanceof PathNode) {
                y = drawPathNode((PathNode) node, bitmap, x, y, WIDTH, HEIGHT, NODECOLOR, level);
            } else if (node instanceof ConvolutionNode) {
                y = drawConvolutionNode((ConvolutionNode) node, bitmap, x, y, WIDTH, HEIGHT, NODECOLOR);
            } else if (node instanceof ConvChainNode) {
                y = drawConvChainNode((ConvChainNode) node, bitmap, x, y, WIDTH, HEIGHT, NODECOLOR);
            }
        }

        return y;
    }

    private static int drawMapNode(MapNode mapnode, int[] bitmap, int x, int y, int WIDTH, int HEIGHT,
                                   int LINECOLOR, int level) {
        for (int r = 0; r < mapnode.rules.length; r++) {
            Rule rule = mapnode.rules[r];
            if (!rule.original) continue;

            int s = rule.IMX * rule.IMY > MAXWIDTH ? SMALL : S;

            x += drawArray(bitmap, rule.binput, x, y, rule.IMX, rule.IMY, rule.IMZ,
                    mapnode.grid.characters, s, WIDTH, HEIGHT) + HGAP;
            drawHLine(bitmap, x, y + S / 2, HARROW, LINECOLOR, WIDTH, HEIGHT, true);
            x += HARROW + HGAP;
            x += drawArray(bitmap, rule.output, x, y, rule.OMX, rule.OMY, rule.OMZ,
                    mapnode.newgrid.characters, s, WIDTH, HEIGHT) + HGAP;

            y += Math.max(rule.IMY, rule.OMY) * s + (Math.max(rule.IMZ, rule.OMZ) - 1) * ZSHIFT + SMALLVSKIP;
            x = level * HINDENT;
        }
        return y + VSKIP;
    }

    private static int drawRuleNode(RuleNode rulenode, int[] bitmap, int x, int y, int WIDTH, int HEIGHT,
                                    int NODECOLOR, boolean active, int level) {
        for (int r = 0; r < rulenode.rules.length && r < 40; r++) {
            Rule rule = rulenode.rules[r];
            if (!rule.original) continue;

            int s = rule.IMX * rule.IMY > MAXWIDTH ? SMALL : S;
            int LINECOLOR = (active && isActive(rulenode, r)) ? ACTIVE : INACTIVE;

            x += drawArray(bitmap, rule.binput, x, y, rule.IMX, rule.IMY, rule.IMZ,
                    rulenode.grid.characters, s, WIDTH, HEIGHT) + HGAP;

            drawHLine(bitmap, x, y + S / 2, HARROW, LINECOLOR, WIDTH, HEIGHT, !(rulenode instanceof OneNode));
            x += HARROW + HGAP;
            x += drawArray(bitmap, rule.output, x, y, rule.OMX, rule.OMY, rule.OMZ,
                    rulenode.grid.characters, s, WIDTH, HEIGHT) + HGAP;

            if (rulenode.steps > 0) {
                write(bitmap, " " + rulenode.counter + "/" + rulenode.steps, x, y, LINECOLOR, WIDTH, HEIGHT, 0);
            }

            y += rule.IMY * s + (rule.IMZ - 1) * ZSHIFT + SMALLVSKIP;
            x = level * HINDENT;
        }

        if (rulenode.fields != null) {
            y += SMALLVSKIP;
            for (int c = 0; c < rulenode.fields.length; c++) {
                if (rulenode.fields[c] == null) continue;

                y = drawFieldInfo(rulenode.fields[c], c, rulenode.grid.characters,
                        bitmap, x, y, WIDTH, HEIGHT, NODECOLOR, level);
            }
        }

        return y + VSKIP;
    }

    private static int drawPathNode(PathNode path, int[] bitmap, int x, int y, int WIDTH, int HEIGHT,
                                    int NODECOLOR, int level) {
        int VSHIFT = (fonts[0].FY - FONTSHIFT - S) / 2;

        if (!DENSE) {
            x += write(bitmap, "path from ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            byte[] start = Helper.nonZeroPositions(path.start);
            for (int k = 0; k < start.length; k++, x += S) {
                drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[start[k]], WIDTH, HEIGHT);
            }

            x += write(bitmap, " to ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            byte[] finish = Helper.nonZeroPositions(path.finish);
            for (int k = 0; k < finish.length; k++, x += S) {
                drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[finish[k]], WIDTH, HEIGHT);
            }

            x += write(bitmap, " on ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            byte[] on = Helper.nonZeroPositions(path.substrate);
            for (int k = 0; k < on.length; k++, x += S) {
                drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[on[k]], WIDTH, HEIGHT);
            }

            x += write(bitmap, " colored ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[path.value], WIDTH, HEIGHT);
        } else {
            x += write(bitmap, "path ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            byte[] start = Helper.nonZeroPositions(path.start);
            for (int k = 0; k < start.length; k++, x += S) {
                drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[start[k]], WIDTH, HEIGHT);
            }
            x += HGAP;
            byte[] finish = Helper.nonZeroPositions(path.finish);
            for (int k = 0; k < finish.length; k++, x += S) {
                drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[finish[k]], WIDTH, HEIGHT);
            }
            x += HGAP;
            byte[] on = Helper.nonZeroPositions(path.substrate);
            for (int k = 0; k < on.length; k++, x += S) {
                drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[on[k]], WIDTH, HEIGHT);
            }
            x += HGAP;
            drawSquare(bitmap, x, y + VSHIFT, S, path.grid.characters[path.value], WIDTH, HEIGHT);
        }

        return y + fonts[0].FY + VSKIP;
    }

    private static int drawConvolutionNode(ConvolutionNode convnode, int[] bitmap, int x, int y,
                                           int WIDTH, int HEIGHT, int NODECOLOR) {
        String s = "convolution";
        if (convnode.steps > 0) {
            s += " " + convnode.counter + "/" + convnode.steps;
        }
        write(bitmap, s, x, y, NODECOLOR, WIDTH, HEIGHT, 0);
        return y + fonts[0].FY + VSKIP;
    }

    private static int drawConvChainNode(ConvChainNode chainnode, int[] bitmap, int x, int y,
                                         int WIDTH, int HEIGHT, int NODECOLOR) {
        x += write(bitmap, "convchain ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
        drawSample(bitmap, x, y, chainnode.sample, chainnode.SMX, chainnode.SMY,
                chainnode.c0, chainnode.c1, chainnode.grid.characters, 7, WIDTH, HEIGHT);
        return y + fonts[0].FY + VSKIP;
    }

    private static int drawFieldInfo(com.jxon.juscore.mjcore.models.Field field, int c, char[] characters,
                                     int[] bitmap, int x, int y, int WIDTH, int HEIGHT, int NODECOLOR, int level) {
        if (!DENSE) {
            x += write(bitmap, "field for ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            drawSquare(bitmap, x, y, S, characters[c], WIDTH, HEIGHT);
            x += S;

            x += write(bitmap, field.inversed ? " from " : " to ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            byte[] zero = Helper.nonZeroPositions(field.zero);
            for (int k = 0; k < zero.length; k++, x += S) {
                drawSquare(bitmap, x, y, S, characters[zero[k]], WIDTH, HEIGHT);
            }

            x += write(bitmap, " on ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            byte[] substrate = Helper.nonZeroPositions(field.substrate);
            for (int k = 0; k < substrate.length; k++, x += S) {
                drawSquare(bitmap, x, y, S, characters[substrate[k]], WIDTH, HEIGHT);
            }
        } else {
            x += write(bitmap, "field ", x, y, NODECOLOR, WIDTH, HEIGHT, 0);
            drawSquare(bitmap, x, y, S, characters[c], WIDTH, HEIGHT);
            x += S + HGAP;
            byte[] zero = Helper.nonZeroPositions(field.zero);
            for (int k = 0; k < zero.length; k++, x += S) {
                drawSquare(bitmap, x, y, S, characters[zero[k]], WIDTH, HEIGHT);
            }
            x += HGAP;
            byte[] substrate = Helper.nonZeroPositions(field.substrate);
            for (int k = 0; k < substrate.length; k++, x += S) {
                drawSquare(bitmap, x, y, S, characters[substrate[k]], WIDTH, HEIGHT);
            }
        }

        return y + fonts[0].FY;
    }

    private static void drawConnections(Branch root, Branch current, int[] bitmap, int WIDTH, int HEIGHT,
                                        Map<Node, NodePosition> positions) {
        drawBranchLines(root, positions, bitmap, WIDTH, HEIGHT);

        // Draw active path
        for (Branch b = current; b != null; b = b.parent) {
            if (b.n >= 0 && b.n < b.nodes.length) {
                drawDash(b.nodes[b.n], positions, bitmap, WIDTH, HEIGHT, b instanceof MarkovNode, true);
                drawBracket(b, positions, bitmap, WIDTH, HEIGHT, b.n, true);
            }
        }
    }

    private static void drawBranchLines(Branch branch, Map<Node, NodePosition> positions,
                                        int[] bitmap, int WIDTH, int HEIGHT) {
        if (branch.nodes.length == 0) return;

        NodePosition firstPos = positions.get(branch.nodes[0]);
        if (firstPos != null) {
            drawBracket(branch, positions, bitmap, WIDTH, HEIGHT, branch.nodes.length - 1, false);
        }

        for (Node child : branch.nodes) {
            if (child instanceof Branch) {
                drawBranchLines((Branch) child, positions, bitmap, WIDTH, HEIGHT);
            }
        }
    }

    private static void drawDash(Node node, Map<Node, NodePosition> positions, int[] bitmap,
                                 int WIDTH, int HEIGHT, boolean markov, boolean active) {
        NodePosition pos = positions.get(node);
        if (pos == null) return;

        int extra = markov ? 3 : 1;
        drawHLine(bitmap, pos.level * HINDENT - HLINE - HGAP - extra, pos.height + S / 2,
                HLINE + extra, active ? ACTIVE : INACTIVE, WIDTH, HEIGHT, false);
    }

    private static void drawBracket(Branch branch, Map<Node, NodePosition> positions, int[] bitmap,
                                    int WIDTH, int HEIGHT, int n, boolean active) {
        if (branch.nodes.length == 0) return;

        NodePosition first = positions.get(branch.nodes[0]);
        NodePosition last = positions.get(branch.nodes[Math.min(n, branch.nodes.length - 1)]);

        if (first == null || last == null) return;

        int level = first.level;
        int x = (level + 1) * HINDENT - HGAP - HLINE;
        int color = active ? ACTIVE : INACTIVE;
        boolean markov = branch instanceof MarkovNode;

        drawVLine(bitmap, x, first.height + S / 2, last.height - first.height + 1, color, WIDTH, HEIGHT);
        drawVLine(bitmap, x - (markov ? 3 : 1), first.height + S / 2, last.height - first.height + 1, color, WIDTH, HEIGHT);
    }

    // Drawing utility methods
    private static void drawRectangle(int[] bitmap, int x, int y, int width, int height,
                                      int color, int bitmapWidth, int bitmapHeight) {
        if (y + height > bitmapHeight) return;
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int index = (x + dx) + (y + dy) * bitmapWidth;
                if (index >= 0 && index < bitmap.length && x + dx >= 0 && x + dx < bitmapWidth) {
                    bitmap[index] = color;
                }
            }
        }
    }

    private static void drawSquare(int[] bitmap, int x, int y, int size, char c, int bitmapWidth, int bitmapHeight) {
        // For now, just draw a colored rectangle based on character
        int color = getColorForChar(c);
        drawRectangle(bitmap, x, y, size, size, color, bitmapWidth, bitmapHeight);
    }

    private static void drawShadedSquare(int[] bitmap, int x, int y, int size, int color, int bitmapWidth, int bitmapHeight) {
        drawRectangle(bitmap, x, y, size, size, color, bitmapWidth, bitmapHeight);
        drawRectangle(bitmap, x + size, y, 1, size + 1, BACKGROUND, bitmapWidth, bitmapHeight);
        drawRectangle(bitmap, x, y + size, size + 1, 1, BACKGROUND, bitmapWidth, bitmapHeight);
    }

    private static void drawHLine(int[] bitmap, int x, int y, int length, int color,
                                  int bitmapWidth, int bitmapHeight, boolean dashed) {
        if (length <= 0 || x < 0 || x + length >= bitmapWidth || y < 0 || y >= bitmapHeight) return;

        if (!dashed) {
            drawRectangle(bitmap, x, y, length, 1, color, bitmapWidth, bitmapHeight);
        } else {
            int shift = length % 4 == 0 ? 1 : 0;
            for (int dx = 0; dx < length; dx++) {
                if ((dx + shift) / 2 % 2 == 0) {
                    int index = x + dx + y * bitmapWidth;
                    if (index >= 0 && index < bitmap.length) {
                        bitmap[index] = color;
                    }
                }
            }
        }
    }

    private static void drawVLine(int[] bitmap, int x, int y, int height, int color,
                                  int bitmapWidth, int bitmapHeight) {
        if (x < 0 || x >= bitmapWidth) return;
        int yend = Math.min(y + height, bitmapHeight);
        drawRectangle(bitmap, x, y, 1, yend - y, color, bitmapWidth, bitmapHeight);
    }

    private static int write(int[] bitmap, String s, int x, int y, int color,
                             int bitmapWidth, int bitmapHeight, int font) {
        int fontshift = font == 0 ? FONTSHIFT : 0;
        FontData f = fonts[font];

        if (y - FONTSHIFT + f.FY >= bitmapHeight) return -1;

        for (int i = 0; i < s.length(); i++) {
            Byte p = map.get(s.charAt(i));
            if (p == null) continue;

            int px = p % 32, py = p / 32;
            for (int dy = 0; dy < f.FY; dy++) {
                for (int dx = 0; dx < f.FX; dx++) {
                    if (f.data[px * f.FX + dx + (py * f.FY + dy) * f.FX * 32]) {
                        int index = x + i * f.FX + dx + (y + dy - fontshift) * bitmapWidth;
                        if (index >= 0 && index < bitmap.length) {
                            bitmap[index] = color;
                        }
                    }
                }
            }
        }
        return s.length() * f.FX;
    }

    private static int drawArray(int[] bitmap, byte[] a, int x, int y, int MX, int MY, int MZ,
                                 char[] characters, int size, int bitmapWidth, int bitmapHeight) {
        for (int dz = 0; dz < MZ; dz++) {
            for (int dy = 0; dy < MY; dy++) {
                for (int dx = 0; dx < MX; dx++) {
                    byte i = a[dx + dy * MX + dz * MX * MY];
                    int color = i != (byte) 0xff ? getColorForChar(characters[i]) : (D3 ? INACTIVE : BACKGROUND);
                    drawShadedSquare(bitmap, x + dx * size + (MZ - dz - 1) * ZSHIFT,
                            y + dy * size + (MZ - dz - 1) * ZSHIFT, size, color, bitmapWidth, bitmapHeight);
                }
            }
        }
        return MX * size + (MZ - 1) * ZSHIFT;
    }

    private static void drawSample(int[] bitmap, int x, int y, boolean[] sample, int MX, int MY,
                                   byte c0, byte c1, char[] characters, int size, int bitmapWidth, int bitmapHeight) {
        for (int dy = 0; dy < MY; dy++) {
            for (int dx = 0; dx < MX; dx++) {
                byte b = sample[dx + dy * MX] ? c1 : c0;
                drawSquare(bitmap, x + dx * size, y + dy * size, size, characters[b], bitmapWidth, bitmapHeight);
            }
        }
    }

    private static boolean isActive(RuleNode node, int index) {
        if (node.last[index]) return true;
        for (int r = index + 1; r < node.rules.length; r++) {
            Rule rule = node.rules[r];
            if (rule.original) break;
            if (node.last[r]) return true;
        }
        return false;
    }

    private static int getColorForChar(char c) {
        // Simple color mapping based on character
        switch (c) {
            case 'B': case 'b': return 0xFF000000; // Black
            case 'W': case 'w': return 0xFFFFFFFF; // White
            case 'R': case 'r': return 0xFFFF0000; // Red
            case 'G': case 'g': return 0xFF00FF00; // Green
            case 'Y': case 'y': return 0xFFFFFF00; // Yellow
            case 'U': case 'u': return 0xFF0000FF; // Blue
            default: return 0xFF808080; // Gray
        }
    }

    // Helper classes
    private static class FontData {
        public final boolean[] data;
        public final int FX, FY;

        public FontData(boolean[] data, int FX, int FY) {
            this.data = data;
            this.FX = FX;
            this.FY = FY;
        }
    }

    private static class NodePosition {
        public final int level;
        public final int height;

        public NodePosition(int level, int height) {
            this.level = level;
            this.height = height;
        }
    }
}