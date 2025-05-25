// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.BiFunction;

public final class SymmetryHelper {
    
    private SymmetryHelper() {} // Prevent instantiation
    
    public static final Map<String, boolean[]> squareSubgroups;
    public static final Map<String, boolean[]> cubeSubgroups;
    
    static {
        squareSubgroups = new HashMap<>();
        squareSubgroups.put("()", new boolean[]{true, false, false, false, false, false, false, false});
        squareSubgroups.put("(x)", new boolean[]{true, true, false, false, false, false, false, false});
        squareSubgroups.put("(y)", new boolean[]{true, false, false, false, false, true, false, false});
        squareSubgroups.put("(x)(y)", new boolean[]{true, true, false, false, true, true, false, false});
        squareSubgroups.put("(xy+)", new boolean[]{true, false, true, false, true, false, true, false});
        squareSubgroups.put("(xy)", new boolean[]{true, true, true, true, true, true, true, true});
        
        cubeSubgroups = new HashMap<>();
        cubeSubgroups.put("()", AH.array1D(48, l -> l == 0));
        cubeSubgroups.put("(x)", AH.array1D(48, l -> l == 0 || l == 1));
        cubeSubgroups.put("(z)", AH.array1D(48, l -> l == 0 || l == 17));
        cubeSubgroups.put("(xy)", AH.array1D(48, l -> l < 8));
        cubeSubgroups.put("(xyz+)", AH.array1D(48, l -> l % 2 == 0));
        cubeSubgroups.put("(xyz)", AH.array1D(48, true));
    }
    
    public static <T> Iterable<T> squareSymmetries(T thing, Function<T, T> rotation, Function<T, T> reflection, 
                                                   BiFunction<T, T, Boolean> same, boolean[] subgroup) {
        @SuppressWarnings("unchecked")
        T[] things = (T[]) new Object[8];
        
        things[0] = thing;                      // e
        things[1] = reflection.apply(things[0]); // b
        things[2] = rotation.apply(things[0]);   // a
        things[3] = reflection.apply(things[2]); // ba
        things[4] = rotation.apply(things[2]);   // a2
        things[5] = reflection.apply(things[4]); // ba2
        things[6] = rotation.apply(things[4]);   // a3
        things[7] = reflection.apply(things[6]); // ba3
        
        List<T> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int finalI = i;
            if ((subgroup == null || subgroup[i]) &&
                result.stream().noneMatch(s -> same.apply(s, things[finalI]))) {
                result.add(things[i]);
            }
        }
        return result;
    }
    
    public static <T> Iterable<T> cubeSymmetries(T thing, Function<T, T> a, Function<T, T> b, Function<T, T> r,
                                                 BiFunction<T, T, Boolean> same, boolean[] subgroup) {
        @SuppressWarnings("unchecked")
        T[] s = (T[]) new Object[48];
        
        s[0] = thing;        // e
        s[1] = r.apply(s[0]);
        s[2] = a.apply(s[0]);      // a
        s[3] = r.apply(s[2]);
        s[4] = a.apply(s[2]);      // a2
        s[5] = r.apply(s[4]);
        s[6] = a.apply(s[4]);      // a3
        s[7] = r.apply(s[6]);
        s[8] = b.apply(s[0]);      // b
        s[9] = r.apply(s[8]);
        s[10] = b.apply(s[2]);     // b a
        s[11] = r.apply(s[10]);
        s[12] = b.apply(s[4]);     // b a2
        s[13] = r.apply(s[12]);
        s[14] = b.apply(s[6]);     // b a3
        s[15] = r.apply(s[14]);
        s[16] = b.apply(s[8]);     // b2
        s[17] = r.apply(s[16]);
        s[18] = b.apply(s[10]);    // b2 a
        s[19] = r.apply(s[18]);
        s[20] = b.apply(s[12]);    // b2 a2
        s[21] = r.apply(s[20]);
        s[22] = b.apply(s[14]);    // b2 a3
        s[23] = r.apply(s[22]);
        s[24] = b.apply(s[16]);    // b3
        s[25] = r.apply(s[24]);
        s[26] = b.apply(s[18]);    // b3 a
        s[27] = r.apply(s[26]);
        s[28] = b.apply(s[20]);    // b3 a2
        s[29] = r.apply(s[28]);
        s[30] = b.apply(s[22]);    // b3 a3
        s[31] = r.apply(s[30]);
        s[32] = a.apply(s[8]);     // a b
        s[33] = r.apply(s[32]);
        s[34] = a.apply(s[10]);    // a b a
        s[35] = r.apply(s[34]);
        s[36] = a.apply(s[12]);    // a b a2
        s[37] = r.apply(s[36]);
        s[38] = a.apply(s[14]);    // a b a3
        s[39] = r.apply(s[38]);
        s[40] = a.apply(s[24]);    // a3 b a2 = a b3
        s[41] = r.apply(s[40]);
        s[42] = a.apply(s[26]);    // a3 b a3 = a b3 a
        s[43] = r.apply(s[42]);
        s[44] = a.apply(s[28]);    // a3 b = a b3 a2
        s[45] = r.apply(s[44]);
        s[46] = a.apply(s[30]);    // a3 b a = a b3 a3
        s[47] = r.apply(s[46]);
        
        List<T> result = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            int finalI = i;
            if ((subgroup == null || subgroup[i]) &&
                result.stream().noneMatch(t -> same.apply(t, s[finalI]))) {
                result.add(s[i]);
            }
        }
        return result;
    }
    
    public static boolean[] getSymmetry(boolean d2, String s, boolean[] dflt) {
        if (s == null) {
            return dflt;
        }
        
        Map<String, boolean[]> subgroups = d2 ? squareSubgroups : cubeSubgroups;
        return subgroups.get(s);
    }
}