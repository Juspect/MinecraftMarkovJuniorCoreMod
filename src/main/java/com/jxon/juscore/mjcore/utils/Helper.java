// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

public final class Helper {
    
    private Helper() {} // Prevent instantiation
    
    public static class OrdsResult {
        public final byte[] result;
        public final int count;
        
        public OrdsResult(byte[] result, int count) {
            this.result = result;
            this.count = count;
        }
    }
    
    public static OrdsResult ords(int[] data, List<Integer> uniques) {
        byte[] result = new byte[data.length];
        if (uniques == null) {
            uniques = new ArrayList<>();
        }
        
        for (int i = 0; i < data.length; i++) {
            int d = data[i];
            int ord = uniques.indexOf(d);
            if (ord == -1) {
                ord = uniques.size();
                uniques.add(d);
            }
            result[i] = (byte) ord;
        }
        return new OrdsResult(result, uniques.size());
    }
    
    public static OrdsResult ords(int[] data) {
        return ords(data, null);
    }
    
    public static String[][] split(String s, char s1, char s2) {
        String[] split = s.split(String.valueOf(s1));
        String[][] result = new String[split.length][];
        for (int k = 0; k < result.length; k++) {
            result[k] = split[k].split(String.valueOf(s2));
        }
        return result;
    }
    
    public static int power(int a, int n) {
        int product = 1;
        for (int i = 0; i < n; i++) {
            product *= a;
        }
        return product;
    }
    
    public static int index(boolean[] array) {
        int result = 0;
        int power = 1;
        for (int i = 0; i < array.length; i++, power *= 2) {
            if (array[i]) {
                result += power;
            }
        }
        return result;
    }
    
    public static long index(byte[] p, int C) {
        long result = 0;
        long power = 1;
        for (int i = 0; i < p.length; i++, power *= C) {
            result += p[p.length - 1 - i] * power;
        }
        return result;
    }
    
    public static byte[] nonZeroPositions(int w) {
        int amount = 0;
        int wcopy = w;
        for (byte p = 0; p < 32; p++, w >>>= 1) {
            if ((w & 1) == 1) {
                amount++;
            }
        }
        
        byte[] result = new byte[amount];
        amount = 0;
        for (byte p = 0; p < 32; p++, wcopy >>>= 1) {
            if ((wcopy & 1) == 1) {
                result[amount] = p;
                amount++;
            }
        }
        return result;
    }
    
    public static int maxPositiveIndex(int[] amounts) {
        int max = -1;
        int argmax = -1;
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            if (amount > 0 && amount > max) {
                max = amount;
                argmax = i;
            }
        }
        return argmax;
    }

    public static boolean[] patternboolean(BiFunction<Integer, Integer, Boolean> f, int N) {
        boolean[] result = new boolean[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = f.apply(x, y);
            }
        }
        return result;
    }
    
    public static byte[] pattern(BiFunction<Integer, Integer, Byte> f, int N) {
        byte[] result = new byte[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = f.apply(x, y);
            }
        }
        return result;
    }
    
    public static byte[] rotated(byte[] p, int N) {
        return pattern((x, y) -> p[N - 1 - y + x * N], N);
    }
    
    public static boolean[] rotated(boolean[] p, int N) {
        boolean[] result = new boolean[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = p[N - 1 - y + x * N];
            }
        }
        return result;
    }
    
    public static byte[] reflected(byte[] p, int N) {
        return pattern((x, y) -> p[N - 1 - x + y * N], N);
    }
    
    public static boolean[] reflected(boolean[] p, int N) {
        boolean[] result = new boolean[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = p[N - 1 - x + y * N];
            }
        }
        return result;
    }
}

