// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import java.util.HashMap;
import java.util.Map;

/**
 * Simplified GUI class for MarkovJunior
 * This is a placeholder implementation since full GUI functionality
 * would require extensive graphics libraries and may not be needed
 * in a Minecraft mod context
 */
public final class GUI {

    private GUI() {} // Prevent instantiation

    // Constants from original implementation
    private static final int S = 7;
    private static final int SMALL = 3;
    private static final int MAXWIDTH = 10;
    private static final int ZSHIFT = 2;
    private static final int HINDENT = 30;
    private static final int HGAP = 2;
    private static final int HARROW = 10;
    private static final int HLINE = 14;
    private static final int VSKIP = 2;
    private static final int SMALLVSKIP = 2;
    private static final int FONTSHIFT = 2;
    private static final int AFTERFONT = 4;
    private static final boolean DENSE = true;
    private static final boolean D3 = true;

    public static final int BACKGROUND = 0xFF222222;
    public static final int INACTIVE = 0xFF666666;
    public static final int ACTIVE = 0xFFFFFFFF;

    // Font and legend data (simplified)
    private static final char[] LEGEND = "ABCDEFGHIJKLMNOPQRSTUVWXYZ 12345abcdefghijklmnopqrstuvwxyz\u03bb67890{}[]()<>$*-+=/#_%^@\\&|~?'\"`!,.;:".toCharArray();
    private static final Map<Character, Byte> LEGEND_MAP = new HashMap<>();

    static {
        for (int i = 0; i < LEGEND.length; i++) {
            LEGEND_MAP.put(LEGEND[i], (byte) i);
        }
    }

    /**
     * Simplified draw method - in a real implementation this would render the GUI
     * For Minecraft mod purposes, this is mostly a placeholder
     */
    public static void draw(String name, Branch root, Branch current, int[] bitmap,
                            int WIDTH, int HEIGHT, Map<Character, Integer> palette) {
        // This would contain the full GUI rendering logic
        // For now, we just print some debug information
        System.out.println("GUI Draw called for: " + name);
        System.out.println("Bitmap size: " + WIDTH + "x" + HEIGHT);
        System.out.println("Current node: " + (current != null ? current.getClass().getSimpleName() : "null"));

        // In a full implementation, this would:
        // 1. Draw the bitmap background
        // 2. Render the node hierarchy
        // 3. Show current execution state
        // 4. Display rules and their states
        // 5. Handle user interaction
    }

    /**
     * Utility method to draw a rectangle (placeholder)
     */
    private static void drawRectangle(int[] bitmap, int x, int y, int width, int height,
                                      int color, int bitmapWidth, int bitmapHeight) {
        if (y + height > bitmapHeight) return;

        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int index = (x + dx) + (y + dy) * bitmapWidth;
                if (index >= 0 && index < bitmap.length) {
                    bitmap[index] = color;
                }
            }
        }
    }

    /**
     * Utility method to draw a square (placeholder)
     */
    private static void drawSquare(int[] bitmap, int x, int y, int size, char c,
                                   Map<Character, Integer> palette, int bitmapWidth, int bitmapHeight) {
        Integer color = palette.get(c);
        if (color != null) {
            drawRectangle(bitmap, x, y, size, size, color, bitmapWidth, bitmapHeight);
        }
    }

    /**
     * Utility method to draw text (placeholder)
     */
    private static int writeText(int[] bitmap, String text, int x, int y, int color,
                                 int bitmapWidth, int bitmapHeight) {
        // This would render text using font data
        // For now, just return the expected width
        return text.length() * 8; // Assuming 8 pixels per character
    }

    /**
     * Check if a rule node is currently active
     */
    private static boolean isActive(RuleNode node, int index) {
        if (node.last[index]) return true;

        for (int r = index + 1; r < node.rules.length; r++) {
            Rule rule = node.rules[r];
            if (rule.original) break;
            if (node.last[r]) return true;
        }
        return false;
    }

    /**
     * Helper class for node layout information
     */
    private static class NodeLayout {
        public final int level;
        public final int height;

        public NodeLayout(int level, int height) {
            this.level = level;
            this.height = height;
        }
    }

    /**
     * Get layout information for GUI rendering
     */
    public static Map<String, Object> getLayoutInfo(Branch root, Branch current) {
        Map<String, Object> info = new HashMap<>();
        info.put("root", root != null ? root.getClass().getSimpleName() : "null");
        info.put("current", current != null ? current.getClass().getSimpleName() : "null");
        info.put("nodes", root != null ? root.nodes.length : 0);
        return info;
    }

    /**
     * Simple text-based representation of the current state
     */
    public static String getTextRepresentation(Branch root, Branch current) {
        StringBuilder sb = new StringBuilder();
        sb.append("MarkovJunior State:\n");
        sb.append("Root: ").append(root != null ? root.getClass().getSimpleName() : "null").append("\n");
        sb.append("Current: ").append(current != null ? current.getClass().getSimpleName() : "null").append("\n");

        if (root != null) {
            sb.append("Nodes: ").append(root.nodes.length).append("\n");
            for (int i = 0; i < root.nodes.length; i++) {
                Node node = root.nodes[i];
                sb.append("  [").append(i).append("] ").append(node.getClass().getSimpleName());
                if (current != null && current.n == i) {
                    sb.append(" (ACTIVE)");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}