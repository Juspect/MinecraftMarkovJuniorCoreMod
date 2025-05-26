package com.jxon.juscore.mjcore.utils;

import java.util.List;
import java.util.Random;

public final class RandomHelper {

    private RandomHelper() {} // Prevent instantiation

    public static <T> T random(List<T> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }

    public static int random(double[] weights, double r) {
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
        }
        double threshold = r * sum;

        double partialSum = 0;
        for (int i = 0; i < weights.length; i++) {
            partialSum += weights[i];
            if (partialSum >= threshold) {
                return i;
            }
        }
        return 0;
    }

    // 修正：保持与C#原版完全一致的实现
    public static void shuffle(int[] array, Random random) {
        for (int i = 0; i < array.length; i++) {
            int j = random.nextInt(i + 1);
            int temp = array[i];  // 保存临时值
            array[i] = array[j];
            array[j] = temp;      // 使用临时值，而不是 i
        }
    }
}