// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import org.apache.commons.lang3.function.TriFunction;

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

    public static boolean[][][] array3Dboolean(int MX, int MY, int MZ, boolean value) {
        boolean[][][] result = new boolean[MX][][];
        for (int x = 0; x < result.length; x++) {
            boolean[][] resultx = new boolean[MY][];
            result[x] = resultx;
            for (int y = 0; y < resultx.length; y++) {
                boolean[] row = new boolean[MZ];
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
                    // 关键：确保索引计算与C#版本一致 z * MX * MY + y * MX + x
                    result[z * MX * MY + y * MX + x] = f.apply(x, y, z);
                }
            }
        }
        return result;
    }

    public static byte[] flatArray3Dbyte(int MX, int MY, int MZ, TriFunction<Integer, Integer, Integer, Byte> f) {
        byte[] result = new byte[MX * MY * MZ];
        for (int z = 0; z < MZ; z++) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    result[z * MX * MY + y * MX + x] = f.apply(x, y, z);
                }
            }
        }
        return result;
    }

    // 添加缺失的泛型Array3D方法重载
    public static <T> T[][][] array3D(int MX, int MY, int MZ, T value) {
        @SuppressWarnings("unchecked")
        T[][][] result = (T[][][]) new Object[MX][][];
        for (int x = 0; x < result.length; x++) {
            @SuppressWarnings("unchecked")
            T[][] resultx = (T[][]) new Object[MY][];
            result[x] = resultx;
            for (int y = 0; y < resultx.length; y++) {
                @SuppressWarnings("unchecked")
                T[] row = (T[]) new Object[MZ];
                Arrays.fill(row, value);
                resultx[y] = row;
            }
        }
        return result;
    }

    // 确保Same方法的实现完全一致
    public static boolean same(byte[] t1, byte[] t2) {
        if (t1 == null && t2 == null) return true;
        if (t1 == null || t2 == null) return false;
        if (t1.length != t2.length) return false;

        for (int i = 0; i < t1.length; i++) {
            if (t1[i] != t2[i]) return false;
        }
        return true;
    }

    // 添加缺失的函数式接口支持
    public static int[] array1DFunc(int length, Function<Integer, Integer> f) {
        int[] result = new int[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = f.apply(i);
        }
        return result;
    }

    public static byte[] array1DByteFunc(int length, Function<Integer, Byte> f) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = f.apply(i);
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
}