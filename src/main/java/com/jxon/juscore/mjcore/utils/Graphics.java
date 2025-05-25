// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public final class Graphics {
    
    private Graphics() {} // Prevent instantiation
    
    public static class LoadBitmapResult {
        public final int[] data;
        public final int width, height, depth;
        
        public LoadBitmapResult(int[] data, int width, int height, int depth) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }
    
    public static LoadBitmapResult loadBitmap(String filename) {
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            int width = image.getWidth();
            int height = image.getHeight();
            int[] result = new int[width * height];
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result[x + y * width] = image.getRGB(x, y);
                }
            }
            
            return new LoadBitmapResult(result, width, height, 1);
        } catch (IOException e) {
            return new LoadBitmapResult(null, -1, -1, -1);
        }
    }
    
    public static void saveBitmap(int[] data, int width, int height, String filename) {
        if (width <= 0 || height <= 0 || data.length != width * height) {
            System.out.println("ERROR: wrong image width * height = " + width + " * " + height);
            return;
        }
        
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, data[x + y * width]);
                }
            }
            
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            ImageIO.write(image, extension.equals("jpg") || extension.equals("jpeg") ? "jpg" : "png", new File(filename));
        } catch (IOException e) {
            System.out.println("Error saving image: " + e.getMessage());
        }
    }
    
    public static class RenderResult {
        public final int[] bitmap;
        public final int width, height;
        
        public RenderResult(int[] bitmap, int width, int height) {
            this.bitmap = bitmap;
            this.width = width;
            this.height = height;
        }
    }
    
    public static RenderResult render(byte[] state, int MX, int MY, int MZ, int[] colors, int pixelsize, int MARGIN) {
        return MZ == 1 || !true ? // iso parameter would be here
                bitmapRender(state, MX, MY, colors, pixelsize, MARGIN) : 
                isometricRender(state, MX, MY, MZ, colors, pixelsize, MARGIN);
    }
    
    public static RenderResult bitmapRender(byte[] state, int MX, int MY, int[] colors, int pixelsize, int MARGIN) {
        int WIDTH = MARGIN + MX * pixelsize;
        int HEIGHT = MY * pixelsize;
        int TOTALWIDTH = WIDTH;
        int TOTALHEIGHT = HEIGHT;
        
        int[] bitmap = new int[TOTALWIDTH * TOTALHEIGHT];
        int BACKGROUND = 0xFF222222; // Default background color
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = BACKGROUND;
        }
        
        int DX = (TOTALWIDTH - WIDTH) / 2;
        int DY = (TOTALHEIGHT - HEIGHT) / 2;
        
        for (int y = 0; y < MY; y++) {
            for (int x = 0; x < MX; x++) {
                int c = colors[state[x + y * MX]];
                for (int dy = 0; dy < pixelsize; dy++) {
                    for (int dx = 0; dx < pixelsize; dx++) {
                        int SX = DX + x * pixelsize + dx;
                        int SY = DY + y * pixelsize + dy;
                        if (SX < 0 || SX >= TOTALWIDTH - MARGIN || SY < 0 || SY >= TOTALHEIGHT) {
                            continue;
                        }
                        bitmap[MARGIN + SX + SY * TOTALWIDTH] = c;
                    }
                }
            }
        }
        
        return new RenderResult(bitmap, TOTALWIDTH, TOTALHEIGHT);
    }
    
    private static final Map<Integer, Sprite> sprites = new HashMap<>();
    
    public static RenderResult isometricRender(byte[] state, int MX, int MY, int MZ, int[] colors, int blocksize, int MARGIN) {
        @SuppressWarnings("unchecked")
        List<Voxel>[] voxels = new List[MX + MY + MZ - 2];
        @SuppressWarnings("unchecked")
        List<Voxel>[] visibleVoxels = new List[MX + MY + MZ - 2];
        
        for (int i = 0; i < voxels.length; i++) {
            voxels[i] = new ArrayList<>();
            visibleVoxels[i] = new ArrayList<>();
        }
        
        boolean[] visible = new boolean[MX * MY * MZ];
        
        for (int z = 0; z < MZ; z++) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    int i = x + y * MX + z * MX * MY;
                    byte value = state[i];
                    visible[i] = value != 0;
                    if (value != 0) {
                        voxels[x + y + z].add(new Voxel(colors[value], x, y, z));
                    }
                }
            }
        }
        
        boolean[][] hash = AH.array2D(MX + MY - 1, MX + MY + 2 * MZ - 3, false);
        for (int i = voxels.length - 1; i >= 0; i--) {
            List<Voxel> voxelsi = voxels[i];
            for (int j = 0; j < voxelsi.size(); j++) {
                Voxel s = voxelsi.get(j);
                int u = s.x - s.y + MY - 1;
                int v = s.x + s.y - 2 * s.z + 2 * MZ - 2;
                if (!hash[u][v]) {
                    boolean X = s.x == 0 || !visible[(s.x - 1) + s.y * MX + s.z * MX * MY];
                    boolean Y = s.y == 0 || !visible[s.x + (s.y - 1) * MX + s.z * MX * MY];
                    boolean Z = s.z == 0 || !visible[s.x + s.y * MX + (s.z - 1) * MX * MY];
                    
                    s.edges[0] = s.y == MY - 1 || !visible[s.x + (s.y + 1) * MX + s.z * MX * MY];
                    s.edges[1] = s.x == MX - 1 || !visible[s.x + 1 + s.y * MX + s.z * MX * MY];
                    s.edges[2] = X || (s.y != MY - 1 && visible[s.x - 1 + (s.y + 1) * MX + s.z * MX * MY]);
                    s.edges[3] = X || (s.z != MZ - 1 && visible[s.x - 1 + s.y * MX + (s.z + 1) * MX * MY]);
                    s.edges[4] = Y || (s.x != MX - 1 && visible[s.x + 1 + (s.y - 1) * MX + s.z * MX * MY]);
                    s.edges[5] = Y || (s.z != MZ - 1 && visible[s.x + (s.y - 1) * MX + (s.z + 1) * MX * MY]);
                    s.edges[6] = Z || (s.x != MX - 1 && visible[s.x + 1 + s.y * MX + (s.z - 1) * MX * MY]);
                    s.edges[7] = Z || (s.y != MY - 1 && visible[s.x + (s.y + 1) * MX + (s.z - 1) * MX * MY]);
                    
                    visibleVoxels[i].add(s);
                    hash[u][v] = true;
                }
            }
        }
        
        int FITWIDTH = (MX + MY) * blocksize;
        int FITHEIGHT = ((MX + MY) / 2 + MZ) * blocksize;
        int WIDTH = FITWIDTH + 2 * blocksize;
        int HEIGHT = FITHEIGHT + 2 * blocksize;
        
        int[] screen = new int[(MARGIN + WIDTH) * HEIGHT];
        int BACKGROUND = 0xFF222222;
        for (int i = 0; i < screen.length; i++) {
            screen[i] = BACKGROUND;
        }
        
        Sprite sprite = sprites.get(blocksize);
        if (sprite == null) {
            sprite = new Sprite(blocksize);
            sprites.put(blocksize, sprite);
        }
        
        for (int i = 0; i < visibleVoxels.length; i++) {
            for (Voxel s : visibleVoxels[i]) {
                int u = blocksize * (s.x - s.y);
                int v = (blocksize * (s.x + s.y) / 2 - blocksize * s.z);
                int positionx = WIDTH / 2 + u - blocksize;
                int positiony = (HEIGHT - FITHEIGHT) / 2 + (MZ - 1) * blocksize + v;
                
                RGB rgb = toRGB(s.color);
                blit(sprite.cube, sprite.width, sprite.height, positionx, positiony, rgb.r, rgb.g, rgb.b, screen, MARGIN + WIDTH);
                for (int j = 0; j < 8; j++) {
                    if (s.edges[j]) {
                        blit(sprite.edges[j], sprite.width, sprite.height, positionx, positiony, rgb.r, rgb.g, rgb.b, screen, MARGIN + WIDTH);
                    }
                }
            }
        }
        
        return new RenderResult(screen, MARGIN + WIDTH, HEIGHT);
    }
    
    private static void blit(int[] sprite, int SX, int SY, int x, int y, int r, int g, int b, int[] screen, int screenWidth) {
        for (int dy = 0; dy < SY; dy++) {
            for (int dx = 0; dx < SX; dx++) {
                int grayscale = sprite[dx + dy * SX];
                if (grayscale < 0) {
                    continue;
                }
                int R = (int) ((float) r * (float) grayscale / 256.0f);
                int G = (int) ((float) g * (float) grayscale / 256.0f);
                int B = (int) ((float) b * (float) grayscale / 256.0f);
                int X = x + dx;
                int Y = y + dy;
                if (X >= 0 && X < screenWidth && Y >= 0 && Y < screen.length / screenWidth) {
                    screen[X + Y * screenWidth] = toInt(R, G, B);
                }
            }
        }
    }
    
    private static int toInt(int r, int g, int b) {
        return (0xff << 24) + (r << 16) + (g << 8) + b;
    }
    
    private static RGB toRGB(int i) {
        int r = (i & 0xff0000) >> 16;
        int g = (i & 0xff00) >> 8;
        int b = i & 0xff;
        return new RGB(r, g, b);
    }
    
    private static class RGB {
        public final int r, g, b;
        
        public RGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
    
    private static class Voxel {
        public int color;
        public int x, y, z;
        public boolean[] edges;
        
        public Voxel(int color, int x, int y, int z) {
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
            this.edges = new boolean[8];
        }
    }
    
    private static class Sprite {
        public int[] cube;
        public int[][] edges;
        public int width, height;
        
        private static final int c1 = 215, c2 = 143, c3 = 71, black = 0, transparent = -1;
        
        public Sprite(int size) {
            width = 2 * size;
            height = 2 * size - 1;
            
            cube = texture(size, (x, y) -> f(x, y, size));
            edges = new int[8][];
            edges[0] = texture(size, (x, y) -> x == 1 && y <= 0 ? c1 : transparent);
            edges[1] = texture(size, (x, y) -> x == 0 && y <= 0 ? c1 : transparent);
            edges[2] = texture(size, (x, y) -> x == 1 - size && 2 * y < size && 2 * y >= -size ? black : transparent);
            edges[3] = texture(size, (x, y) -> x <= 0 && y == x / 2 + size - 1 ? black : transparent);
            edges[4] = texture(size, (x, y) -> x == size && 2 * y < size && 2 * y >= -size ? black : transparent);
            edges[5] = texture(size, (x, y) -> x > 0 && y == -(x + 1) / 2 + size ? black : transparent);
            edges[6] = texture(size, (x, y) -> x > 0 && y == (x + 1) / 2 - size ? black : transparent);
            edges[7] = texture(size, (x, y) -> x <= 0 && y == -x / 2 - size + 1 ? black : transparent);
        }
        
        private int[] texture(int size, java.util.function.BiFunction<Integer, Integer, Integer> f) {
            int[] result = new int[width * height];
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    result[i + j * width] = f.apply(i - size + 1, size - j - 1);
                }
            }
            return result;
        }
        
        private static int f(int x, int y, int size) {
            if (2 * y - x >= 2 * size || 2 * y + x > 2 * size || 2 * y - x < -2 * size || 2 * y + x <= -2 * size) {
                return transparent;
            } else if (x > 0 && 2 * y < x) {
                return c3;
            } else if (x <= 0 && 2 * y <= -x) {
                return c2;
            } else {
                return c1;
            }
        }
    }
}