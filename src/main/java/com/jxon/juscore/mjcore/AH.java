// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import java.util.Arrays;
import java.util.function.Function;

public final class AH {
    
    private AH() {} // Prevent instantiation
    
    public static int[][][] array3D(int MX, int MY, int MZ, int value) {
        int[][][] result = new int[MX][][];
        for (int x = 0; x < result.length; x++) {
            int[][] resultx = new int[MY][];
            result[x] = resultx;
            for (int y = 0; y < resultx.length; y++) {
                int[] row = new int[MZ];
                Arrays.fill(row, value);
                resultx[y] = row;
            }
        }
        return result;
    }
    
    public static <T> T[][] array2D(int MX, int MY, T value) {
        @SuppressWarnings("unchecked")
        T[][] result = (T[][]) new Object[MX][];
        for (int x = 0; x < result.length; x++) {
            @SuppressWarnings("unchecked")
            T[] row = (T[]) new Object[MY];
            Arrays.fill(row, value);
            result[x] = row;
        }
        return result;
    }
    
    public static int[][] array2D(int MX, int MY, int value) {
        int[][] result = new int[MX][];
        for (int x = 0; x < result.length; x++) {
            int[] row = new int[MY];
            Arrays.fill(row, value);
            result[x] = row;
        }
        return result;
    }
    
    public static boolean[][] array2D(int MX, int MY, boolean value) {
        boolean[][] result = new boolean[MX][];
        for (int x = 0; x < result.length; x++) {
            boolean[] row = new boolean[MY];
            Arrays.fill(row, value);
            result[x] = row;
        }
        return result;
    }
    
    public static <T> T[] array1D(int length, T value) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[length];
        Arrays.fill(result, value);
        return result;
    }
    
    public static int[] array1D(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }
    
    public static boolean[] array1D(int length, boolean value) {
        boolean[] result = new boolean[length];
        Arrays.fill(result, value);
        return result;
    }
    
    public static boolean[] array1D(int length, Function<Integer, Boolean> f) {
        boolean[] result = new boolean[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = f.apply(i);
        }
        return result;
    }
    
    public static char[] flatArray3D(int MX, int MY, int MZ, TriFunction<Integer, Integer, Integer, Character> f) {
        char[] result = new char[MX * MY * MZ];
        for (int z = 0; z < MZ; z++) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    result[z * MX * MY + y * MX + x] = f.apply(x, y, z);
                }
            }
        }
        return result;
    }
    
    public static <T> void set2D(T[][] a, T value) {
        for (T[] ts : a) {
            Arrays.fill(ts, value);
        }
    }
    
    public static void set2D(int[][] a, int value) {
        for (int[] ints : a) {
            Arrays.fill(ints, value);
        }
    }
    
    public static void set2D(boolean[][] a, boolean value) {
        for (boolean[] booleans : a) {
            Arrays.fill(booleans, value);
        }
    }
    
    public static boolean same(byte[] t1, byte[] t2) {
        return Arrays.equals(t1, t2);
    }
    
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}