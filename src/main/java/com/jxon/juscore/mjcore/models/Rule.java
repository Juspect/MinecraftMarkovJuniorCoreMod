// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.models;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.utils.Graphics;
import com.jxon.juscore.mjcore.utils.Helper;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.VoxHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;

public class Rule {
    public int IMX, IMY, IMZ, OMX, OMY, OMZ;
    public int[] input;
    public byte[] output, binput;

    public double p;
    public Tuple3[][] ishifts, oshifts;

    public boolean original;

    public Rule(int[] input, int IMX, int IMY, int IMZ, byte[] output, int OMX, int OMY, int OMZ, int C, double p) {
        this.input = input;
        this.output = output;
        this.IMX = IMX;
        this.IMY = IMY;
        this.IMZ = IMZ;
        this.OMX = OMX;
        this.OMY = OMY;
        this.OMZ = OMZ;
        this.p = p;

        @SuppressWarnings("unchecked")
        List<Tuple3>[] lists = new List[C];
        for (int c = 0; c < C; c++) {
            lists[c] = new ArrayList<>();
        }

        for (int z = 0; z < IMZ; z++) {
            for (int y = 0; y < IMY; y++) {
                for (int x = 0; x < IMX; x++) {
                    int w = input[x + y * IMX + z * IMX * IMY];
                    for (int c = 0; c < C; c++, w >>>= 1) {
                        if ((w & 1) == 1) {
                            lists[c].add(new Tuple3(x, y, z));
                        }
                    }
                }
            }
        }

        ishifts = new Tuple3[C][];
        for (int c = 0; c < C; c++) {
            ishifts[c] = lists[c].toArray(new Tuple3[0]);
        }

        if (OMX == IMX && OMY == IMY && OMZ == IMZ) {
            for (int c = 0; c < C; c++) {
                lists[c].clear();
            }

            for (int z = 0; z < OMZ; z++) {
                for (int y = 0; y < OMY; y++) {
                    for (int x = 0; x < OMX; x++) {
                        byte o = output[x + y * OMX + z * OMX * OMY];
                        if (o != (byte) 0xff) {
                            lists[o].add(new Tuple3(x, y, z));
                        } else {
                            for (int c = 0; c < C; c++) {
                                lists[c].add(new Tuple3(x, y, z));
                            }
                        }
                    }
                }
            }

            oshifts = new Tuple3[C][];
            for (int c = 0; c < C; c++) {
                oshifts[c] = lists[c].toArray(new Tuple3[0]);
            }
        }

        int wildcard = (1 << C) - 1;
        binput = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            int w = input[i];
            binput[i] = w == wildcard ? (byte) 0xff : (byte) Integer.numberOfTrailingZeros(w);
        }
    }

    public Rule zRotated() {
        int[] newinput = new int[input.length];
        for (int z = 0; z < IMZ; z++) {
            for (int y = 0; y < IMX; y++) {
                for (int x = 0; x < IMY; x++) {
                    newinput[x + y * IMY + z * IMX * IMY] = input[IMX - 1 - y + x * IMX + z * IMX * IMY];
                }
            }
        }

        byte[] newoutput = new byte[output.length];
        for (int z = 0; z < OMZ; z++) {
            for (int y = 0; y < OMX; y++) {
                for (int x = 0; x < OMY; x++) {
                    newoutput[x + y * OMY + z * OMX * OMY] = output[OMX - 1 - y + x * OMX + z * OMX * OMY];
                }
            }
        }

        return new Rule(newinput, IMY, IMX, IMZ, newoutput, OMY, OMX, OMZ, ishifts.length, p);
    }

    public Rule yRotated() {
        int[] newinput = new int[input.length];
        for (int z = 0; z < IMX; z++) {
            for (int y = 0; y < IMY; y++) {
                for (int x = 0; x < IMZ; x++) {
                    newinput[x + y * IMZ + z * IMZ * IMY] = input[IMX - 1 - z + y * IMX + x * IMX * IMY];
                }
            }
        }

        byte[] newoutput = new byte[output.length];
        for (int z = 0; z < OMX; z++) {
            for (int y = 0; y < OMY; y++) {
                for (int x = 0; x < OMZ; x++) {
                    newoutput[x + y * OMZ + z * OMZ * OMY] = output[OMX - 1 - z + y * OMX + x * OMX * OMY];
                }
            }
        }

        return new Rule(newinput, IMZ, IMY, IMX, newoutput, OMZ, OMY, OMX, ishifts.length, p);
    }

    public Rule reflected() {
        int[] newinput = new int[input.length];
        for (int z = 0; z < IMZ; z++) {
            for (int y = 0; y < IMY; y++) {
                for (int x = 0; x < IMX; x++) {
                    newinput[x + y * IMX + z * IMX * IMY] = input[IMX - 1 - x + y * IMX + z * IMX * IMY];
                }
            }
        }

        byte[] newoutput = new byte[output.length];
        for (int z = 0; z < OMZ; z++) {
            for (int y = 0; y < OMY; y++) {
                for (int x = 0; x < OMX; x++) {
                    newoutput[x + y * OMX + z * OMX * OMY] = output[OMX - 1 - x + y * OMX + z * OMX * OMY];
                }
            }
        }

        return new Rule(newinput, IMX, IMY, IMZ, newoutput, OMX, OMY, OMZ, ishifts.length, p);
    }

    public static boolean same(Rule a1, Rule a2) {
        if (a1.IMX != a2.IMX || a1.IMY != a2.IMY || a1.IMZ != a2.IMZ ||
                a1.OMX != a2.OMX || a1.OMY != a2.OMY || a1.OMZ != a2.OMZ) {
            return false;
        }

        for (int i = 0; i < a1.IMX * a1.IMY * a1.IMZ; i++) {
            if (a1.input[i] != a2.input[i]) {
                return false;
            }
        }

        for (int i = 0; i < a1.OMX * a1.OMY * a1.OMZ; i++) {
            if (a1.output[i] != a2.output[i]) {
                return false;
            }
        }

        return true;
    }

    public Iterable<Rule> symmetries(boolean[] symmetry, boolean d2) {
        if (d2) {
            return SymmetryHelper.squareSymmetries(this, Rule::zRotated, Rule::reflected, Rule::same, symmetry);
        } else {
            return SymmetryHelper.cubeSymmetries(this, Rule::zRotated, Rule::yRotated, Rule::reflected, Rule::same, symmetry);
        }
    }

    public record LoadResourceResult(char[] data, int MX, int MY, int MZ) {
    }

    public static LoadResourceResult loadResource(String filename, String legend, boolean d2) {
        if (legend == null) {
            Interpreter.writeLine("no legend for " + filename);
            return new LoadResourceResult(null, -1, -1, -1);
        }

        if (d2) {
            // Load 2D bitmap
            Graphics.LoadBitmapResult bitmapResult = Graphics.loadBitmap(filename);
            LoadResourceResult loadResourceResult = new LoadResourceResult(null, bitmapResult.width(), bitmapResult.height(), bitmapResult.depth());
            if (bitmapResult.data() == null) {
                Interpreter.writeLine("couldn't read " + filename);
                return loadResourceResult;
            }

            Helper.OrdsResult ordsResult = Helper.ords(bitmapResult.data());
            byte[] ords = ordsResult.result();
            int amount = ordsResult.count();

            if (amount > legend.length()) {
                Interpreter.writeLine("the amount of colors " + amount + " in " + filename + " is more than " + legend.length());
                return loadResourceResult;
            }

            char[] result = new char[ords.length];
            for (int i = 0; i < ords.length; i++) {
                result[i] = legend.charAt(ords[i]);
            }

            return new LoadResourceResult(result, bitmapResult.width(), bitmapResult.height(), bitmapResult.depth());
        } else {
            // Load 3D vox file
            VoxHelper.LoadVoxResult voxResult = VoxHelper.loadVox(filename);
            LoadResourceResult resourceResult = new LoadResourceResult(null, voxResult.MX(), voxResult.MY(), voxResult.MZ());
            if (voxResult.data() == null) {
                Interpreter.writeLine("couldn't read " + filename);
                return resourceResult;
            }

            Helper.OrdsResult ordsResult = Helper.ords(voxResult.data());
            byte[] ords = ordsResult.result();
            int amount = ordsResult.count();

            if (amount > legend.length()) {
                Interpreter.writeLine("the amount of colors " + amount + " in " + filename + " is more than " + legend.length());
                return resourceResult;
            }

            char[] result = new char[ords.length];
            for (int i = 0; i < ords.length; i++) {
                result[i] = legend.charAt(ords[i]);
            }

            return new LoadResourceResult(result, voxResult.MX(), voxResult.MY(), voxResult.MZ());
        }
    }

    private static LoadResourceResult parse(String s) {
        String[][] lines = Helper.split(s, ' ', '/');
        int MX = lines[0][0].length();
        int MY = lines[0].length;
        int MZ = lines.length;
        char[] result = new char[MX * MY * MZ];

        for (int z = 0; z < MZ; z++) {
            String[] linesz = lines[MZ - 1 - z];
            if (linesz.length != MY) {
                Interpreter.write("non-rectangular pattern");
                return new LoadResourceResult(null, -1, -1, -1);
            }

            for (int y = 0; y < MY; y++) {
                String lineszy = linesz[y];
                if (lineszy.length() != MX) {
                    Interpreter.write("non-rectangular pattern");
                    return new LoadResourceResult(null, -1, -1, -1);
                }
                for (int x = 0; x < MX; x++) {
                    result[x + y * MX + z * MX * MY] = lineszy.charAt(x);
                }
            }
        }

        return new LoadResourceResult(result, MX, MY, MZ);
    }

    public static Rule load(Element element, Grid gin, Grid gout) {
        int lineNumber = XMLHelper.getLineNumber(element);

        String inString = XMLHelper.get(element, "in", (String) null);
        String outString = XMLHelper.get(element, "out", (String) null);
        String finString = XMLHelper.get(element, "fin", (String) null);
        String foutString = XMLHelper.get(element, "fout", (String) null);
        String fileString = XMLHelper.get(element, "file", (String) null);
        String legend = XMLHelper.get(element, "legend", (String) null);

        char[] inRect, outRect;
        int IMX, IMY, IMZ, OMX, OMY, OMZ;

        if (fileString == null) {
            if (inString == null && finString == null) {
                Interpreter.writeLine("no input in a rule at line " + lineNumber);
                return null;
            }
            if (outString == null && foutString == null) {
                Interpreter.writeLine("no output in a rule at line " + lineNumber);
                return null;
            }

            LoadResourceResult inResult = inString != null ? parse(inString) :
                    loadResource(filepath(finString, gout), legend, gin.MZ == 1);
            if (inResult.data == null) {
                Interpreter.writeLine(" in input at line " + lineNumber);
                return null;
            }
            inRect = inResult.data;
            IMX = inResult.MX;
            IMY = inResult.MY;
            IMZ = inResult.MZ;

            LoadResourceResult outResult = outString != null ? parse(outString) :
                    loadResource(filepath(foutString, gout), legend, gin.MZ == 1);
            if (outResult.data == null) {
                Interpreter.writeLine(" in output at line " + lineNumber);
                return null;
            }
            outRect = outResult.data;
            OMX = outResult.MX;
            OMY = outResult.MY;
            OMZ = outResult.MZ;

            if (gin == gout && (OMZ != IMZ || OMY != IMY || OMX != IMX)) {
                Interpreter.writeLine("non-matching pattern sizes at line " + lineNumber);
                return null;
            }
        } else {
            if (inString != null || finString != null || outString != null || foutString != null) {
                Interpreter.writeLine("rule at line " + lineNumber + " already contains a file attribute");
                return null;
            }

            LoadResourceResult result = loadResource(filepath(fileString, gout), legend, gin.MZ == 1);
            if (result.data == null) {
                Interpreter.writeLine(" in a rule at line " + lineNumber);
                return null;
            }

            int FX = result.MX, FY = result.MY, FZ = result.MZ;
            if (FX % 2 != 0) {
                Interpreter.writeLine("odd width " + FX + " in " + fileString);
                return null;
            }

            IMX = OMX = FX / 2;
            IMY = OMY = FY;
            IMZ = OMZ = FZ;

            inRect = AH.flatArray3D(FX / 2, FY, FZ, (x, y, z) -> result.data[x + y * FX + z * FX * FY]);
            outRect = AH.flatArray3D(FX / 2, FY, FZ, (x, y, z) -> result.data[x + FX / 2 + y * FX + z * FX * FY]);
        }

        int[] input = new int[inRect.length];
        for (int i = 0; i < inRect.length; i++) {
            char c = inRect[i];
            Integer value = gin.waves.get(c);
            if (value == null) {
                Interpreter.writeLine("input code " + c + " at line " + lineNumber + " is not found in codes");
                return null;
            }
            input[i] = value;
        }

        byte[] output = new byte[outRect.length];
        for (int o = 0; o < outRect.length; o++) {
            char c = outRect[o];
            if (c == '*') {
                output[o] = (byte) 0xff;
            } else {
                Byte value = gout.values.get(c);
                if (value == null) {
                    Interpreter.writeLine("output code " + c + " at line " + lineNumber + " is not found in codes");
                    return null;
                }
                output[o] = value;
            }
        }

        double p = XMLHelper.get(element, "p", 1.0);
        return new Rule(input, IMX, IMY, IMZ, output, OMX, OMY, OMZ, gin.C, p);
    }

    private static String filepath(String name, Grid gout) {
        String result = "resources/rules/";
        if (gout.folder != null) {
            result += gout.folder + "/";
        }
        result += name;
        result += gout.MZ == 1 ? ".png" : ".vox";
        return result;
    }

    // Helper class for 3-tuples
    public record Tuple3(int x, int y, int z) {
    }
}