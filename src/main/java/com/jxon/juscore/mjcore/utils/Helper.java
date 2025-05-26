// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

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

    public static boolean[] patternboolean(BiFunction<Integer, Integer, Boolean> f, int N) {
        boolean[] result = new boolean[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = f.apply(x, y);
            }
        }
        return result;
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

    // 确保byte数组索引计算与C#版本一致 - 关键：注意索引顺序
    public static long index(byte[] p, int C) {
        long result = 0;
        long power = 1;
        for (int i = 0; i < p.length; i++, power *= C) {
            // 关键修正：确保与C#版本一致 - 从末尾开始计算
            result += p[p.length - 1 - i] * power;
        }
        return result;
    }

    // 补充缺失的Power方法重载，确保大数计算正确
    public static long power(int a, int n) {
        long product = 1;
        for (int i = 0; i < n; i++) {
            product *= a;
        }
        return product;
    }

    // 确保MaxPositiveIndex方法与C#版本行为一致
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

    // 补充缺失的Pattern方法泛型版本
    public static <T> T[] pattern(Function<Integer, Function<Integer, T>> f, int N, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) java.lang.reflect.Array.newInstance(clazz, N * N);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = f.apply(x).apply(y);
            }
        }
        return result;
    }

    // 补充Lambda友好的Pattern方法
    public static byte[] pattern(BiFunction<Integer, Integer, Byte> f, int N) {
        byte[] result = new byte[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = f.apply(x, y);
            }
        }
        return result;
    }

    public static boolean[] patternBoolean(BiFunction<Integer, Integer, Boolean> f, int N) {
        boolean[] result = new boolean[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                result[x + y * N] = f.apply(x, y);
            }
        }
        return result;
    }

    // 确保Rotated和Reflected方法与C#版本的数学计算一致
    public static byte[] rotated(byte[] p, int N) {
        return pattern((x, y) -> p[N - 1 - y + x * N], N);
    }

    public static boolean[] rotated(boolean[] p, int N) {
        return patternBoolean((x, y) -> p[N - 1 - y + x * N], N);
    }

    public static byte[] reflected(byte[] p, int N) {
        return pattern((x, y) -> p[N - 1 - x + y * N], N);
    }

    public static boolean[] reflected(boolean[] p, int N) {
        return patternBoolean((x, y) -> p[N - 1 - x + y * N], N);
    }

    // 补充Split方法的完整实现
    public static String[][] split(String s, char s1, char s2) {
        if (s == null || s.isEmpty()) {
            return new String[0][];
        }

        String[] split = s.split(Pattern.quote(String.valueOf(s1)));
        String[][] result = new String[split.length][];
        for (int k = 0; k < result.length; k++) {
            result[k] = split[k].split(Pattern.quote(String.valueOf(s2)));
        }
        return result;
    }

    // 补充NonZeroPositions方法的边界检查
    public static byte[] nonZeroPositions(int w) {
        List<Byte> positions = new ArrayList<>();
        for (byte p = 0; p < 32; p++, w >>>= 1) {
            if ((w & 1) == 1) {
                positions.add(p);
            }
        }

        byte[] result = new byte[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            result[i] = positions.get(i);
        }
        return result;
    }
}

