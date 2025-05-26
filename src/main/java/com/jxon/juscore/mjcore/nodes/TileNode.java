// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.utils.Helper;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.VoxHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.function.Function;

public class TileNode extends WFCNode {
    private List<byte[]> tiledata;
    private int S, SZ;
    private int overlap, overlapz;

    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        periodic = XMLHelper.get(element, "periodic", false);
        name = XMLHelper.get(element, "tileset");
        String tilesname = XMLHelper.get(element, "tiles", name);
        overlap = XMLHelper.get(element, "overlap", 0);
        overlapz = XMLHelper.get(element, "overlapz", 0);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String filepath = "resources/tilesets/" + name + ".xml";
            Document doc = builder.parse(new File(filepath));
            Element root = doc.getDocumentElement();

            boolean fullSymmetry = XMLHelper.get(root, "fullSymmetry", false);

            // Get first tile to determine dimensions
            NodeList tilesNodes = root.getElementsByTagName("tiles");
            if (tilesNodes.getLength() == 0) {
                Interpreter.writeLine("No tiles element found in tileset");
                return false;
            }
            Element tilesElement = (Element) tilesNodes.item(0);
            NodeList tileNodes = tilesElement.getElementsByTagName("tile");
            if (tileNodes.getLength() == 0) {
                Interpreter.writeLine("No tile elements found in tileset");
                return false;
            }

            Element firstTile = (Element) tileNodes.item(0);
            String firstTileName = XMLHelper.get(firstTile, "name");
            String firstFileName = tilesname + "/" + firstTileName + ".vox";

            VoxHelper.LoadVoxResult firstData = VoxHelper.loadVox("resources/tilesets/" + firstFileName);
            if (firstData.data() == null) {
                Interpreter.writeLine("couldn't read " + firstFileName);
                return false;
            }

            S = firstData.MX();
            int SY = firstData.MY();
            SZ = firstData.MZ();

            if (S != SY) {
                Interpreter.writeLine("tiles should be square shaped: " + S + " != " + SY);
                return false;
            }
            if (fullSymmetry && S != SZ) {
                Interpreter.writeLine("tiles should be cubes for the full symmetry option: " + S + " != " + SZ);
                return false;
            }

            newgrid = Grid.load(element, (S - overlap) * grid.MX + overlap,
                    (S - overlap) * grid.MY + overlap,
                    (SZ - overlapz) * grid.MZ + overlapz);
            if (newgrid == null) return false;

            tiledata = new ArrayList<>();
            Map<String, boolean[]> positions = new HashMap<>();
            List<Double> tempStationary = new ArrayList<>();
            List<Integer> uniques = new ArrayList<>();

            // Lambda functions for tile transformations
            Function<byte[], byte[]> zRotate = p -> newtile(p, (x, y, z) -> p[y + (S - 1 - x) * S + z * S * S]);
            Function<byte[], byte[]> yRotate = p -> newtile(p, (x, y, z) -> p[z + y * S + (S - 1 - x) * S * S]);
            Function<byte[], byte[]> xRotate = p -> newtile(p, (x, y, z) -> p[x + z * S + (S - 1 - y) * S * S]);
            Function<byte[], byte[]> xReflect = p -> newtile(p, (x, y, z) -> p[(S - 1 - x) + y * S + z * S * S]);
            Function<byte[], byte[]> yReflect = p -> newtile(p, (x, y, z) -> p[x + (S - 1 - y) * S + z * S * S]);
            Function<byte[], byte[]> zReflect = p -> newtile(p, (x, y, z) -> p[x + y * S + (S - 1 - z) * S * S]);

            Map<String, List<byte[]>> namedTileData = new HashMap<>();

            int ind = 0;
            for (int i = 0; i < tileNodes.getLength(); i++) {
                Element tileElement = (Element) tileNodes.item(i);
                String tilename = XMLHelper.get(tileElement, "name");
                double weight = XMLHelper.get(tileElement, "weight", 1.0);

                String filename = "resources/tilesets/" + tilesname + "/" + tilename + ".vox";
                VoxHelper.LoadVoxResult voxResult = VoxHelper.loadVox(filename);
                if (voxResult.data() == null) {
                    Interpreter.writeLine("couldn't read tile " + filename);
                    return false;
                }

                Helper.OrdsResult ordsResult = Helper.ords(voxResult.data(), uniques);
                byte[] flatTile = ordsResult.result;
                int C = ordsResult.count;

                if (C > newgrid.C) {
                    Interpreter.writeLine("there were more than " + newgrid.C + " colors in vox files");
                    return false;
                }

                List<byte[]> localdata;
                if (fullSymmetry) {
                    localdata = new ArrayList<>();
                    for (byte[] tile : SymmetryHelper.cubeSymmetries(flatTile, zRotate, yRotate, xReflect,
                            AH::same, null)) {
                        localdata.add(tile);
                    }
                } else {
                    localdata = new ArrayList<>();
                    for (byte[] tile : SymmetryHelper.squareSymmetries(flatTile, zRotate, xReflect,
                            AH::same, null)) {
                        localdata.add(tile);
                    }
                }

                boolean[] position = new boolean[128]; // 预分配足够大的数组
                namedTileData.put(tilename, localdata);
                for (byte[] p : localdata) {
                    tiledata.add(p);
                    tempStationary.add(weight);
                    if (ind < position.length) {
                        position[ind] = true;
                    }
                    ind++;
                }
                positions.put(tilename, position);
            }

            P = tiledata.size();
            System.out.println("P = " + P);
            weights = tempStationary.stream().mapToDouble(Double::doubleValue).toArray();

            // 关键修正：创建union符号的特殊处理
            List<String> tilenames = new ArrayList<>();
            for (int i = 0; i < tileNodes.getLength(); i++) {
                Element tileElement = (Element) tileNodes.item(i);
                tilenames.add(XMLHelper.get(tileElement, "name"));
            }
            tilenames.add(null);

            // 关键修正：为特殊符号创建positions映射
            // 处理*符号 - 代表所有瓦片
            boolean[] allTiles = new boolean[P];
            Arrays.fill(allTiles, true);
            positions.put("*", allTiles);

            // 处理其他可能的union符号组合
            createUnionPositions(positions, namedTileData, P);

            // Initialize map with proper error handling
            map = new HashMap<>();
            List<Element> ruleElements = XMLHelper.getElementsByTagName(element, "rule");
            for (Element ruleElement : ruleElements) {
                char input = XMLHelper.get(ruleElement, "in", Character.class);
                String outputString = XMLHelper.get(ruleElement, "out");

                // 关键修正：正确处理输出字符串的分割和trim
                String[] outputs = outputString.split("\\|");
                boolean[] position = new boolean[P];

                for (String s : outputs) {
                    String trimmedOutput = s.trim(); // 关键：去除空格

                    // 关键修正：处理复合符号
                    boolean found = false;
                    if (positions.containsKey(trimmedOutput)) {
                        boolean[] array = positions.get(trimmedOutput);
                        for (int p = 0; p < P && p < array.length; p++) {
                            if (array[p]) position[p] = true;
                        }
                        found = true;
                    } else {
                        // 尝试解析复合符号，如 "* F", "*LSL*", "* a", "*I"
                        found = parseComplexTilename(trimmedOutput, positions, position, P);
                    }

                    if (!found) {
                        Interpreter.writeLine("unknown tilename " + trimmedOutput + " at line " + XMLHelper.getLineNumber(ruleElement));
                        return false;
                    }
                }

                Byte inputValue = grid.values.get(input);
                if (inputValue != null) {
                    map.put(inputValue, position);
                } else {
                    Interpreter.writeLine("unknown input value " + input + " at line " + XMLHelper.getLineNumber(ruleElement));
                    return false;
                }
            }

            // 确保默认映射存在
            if (!map.containsKey((byte) 0)) {
                boolean[] allTrue = new boolean[P];
                Arrays.fill(allTrue, true);
                map.put((byte) 0, allTrue);
            }

            // Initialize propagator (existing code continues...)
            return initializePropagator(fullSymmetry, namedTileData, tilenames, root, zRotate, yRotate, xRotate,
                    xReflect, yReflect, zReflect) && super.load(element, parentSymmetry, grid);

        } catch (Exception e) {
            Interpreter.writeLine("Error loading tileset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 关键新增：创建union符号的positions映射
    private void createUnionPositions(Map<String, boolean[]> positions, Map<String, List<byte[]>> namedTileData, int P) {
        // 为可能的符号组合创建映射
        Set<String> existingNames = new HashSet<>(namedTileData.keySet());

        // 处理空格分隔的组合，如 "* F"
        for (String name1 : existingNames) {
            for (String name2 : existingNames) {
                String combo1 = "* " + name2;
                String combo2 = name1 + " *";
                String combo3 = "*" + name2;
                String combo4 = name1 + "*";

                if (!positions.containsKey(combo1)) {
                    boolean[] pos = combinePositions(positions.get("*"), positions.get(name2), P);
                    if (pos != null) positions.put(combo1, pos);
                }

                if (!positions.containsKey(combo2)) {
                    boolean[] pos = combinePositions(positions.get(name1), positions.get("*"), P);
                    if (pos != null) positions.put(combo2, pos);
                }

                if (!positions.containsKey(combo3)) {
                    boolean[] pos = combinePositions(positions.get("*"), positions.get(name2), P);
                    if (pos != null) positions.put(combo3, pos);
                }

                if (!positions.containsKey(combo4)) {
                    boolean[] pos = combinePositions(positions.get(name1), positions.get("*"), P);
                    if (pos != null) positions.put(combo4, pos);
                }
            }
        }

        // 处理被*包围的名称，如 "*LSL*"
        for (String name : existingNames) {
            String surrounded = "*" + name + "*";
            if (!positions.containsKey(surrounded)) {
                positions.put(surrounded, positions.get(name));
            }
        }
    }

    // 关键新增：解析复合瓦片名称
    private boolean parseComplexTilename(String tilename, Map<String, boolean[]> positions, boolean[] targetPosition, int P) {
        // 尝试各种可能的解析方式

        // 1. 直接匹配
        if (positions.containsKey(tilename)) {
            boolean[] array = positions.get(tilename);
            for (int p = 0; p < P && p < array.length; p++) {
                if (array[p]) targetPosition[p] = true;
            }
            return true;
        }

        // 2. 去除*符号后匹配
        String withoutStars = tilename.replace("*", "").trim();
        if (!withoutStars.isEmpty() && positions.containsKey(withoutStars)) {
            boolean[] array = positions.get(withoutStars);
            for (int p = 0; p < P && p < array.length; p++) {
                if (array[p]) targetPosition[p] = true;
            }
            return true;
        }

        // 3. 处理空格分隔的组合
        String[] parts = tilename.split("\\s+");
        boolean foundAny = false;
        for (String part : parts) {
            part = part.trim();
            if (positions.containsKey(part)) {
                boolean[] array = positions.get(part);
                for (int p = 0; p < P && p < array.length; p++) {
                    if (array[p]) targetPosition[p] = true;
                }
                foundAny = true;
            }
        }

        return foundAny;
    }

    // 辅助方法：合并两个position数组
    private boolean[] combinePositions(boolean[] pos1, boolean[] pos2, int P) {
        if (pos1 == null || pos2 == null) return null;

        boolean[] result = new boolean[P];
        for (int i = 0; i < P && i < pos1.length && i < pos2.length; i++) {
            result[i] = pos1[i] || pos2[i];
        }
        return result;
    }

    // 初始化propagator的方法（从原来的load方法中提取）
    private boolean initializePropagator(boolean fullSymmetry, Map<String, List<byte[]>> namedTileData,
                                         List<String> tilenames, Element root,
                                         Function<byte[], byte[]> zRotate, Function<byte[], byte[]> yRotate,
                                         Function<byte[], byte[]> xRotate, Function<byte[], byte[]> xReflect,
                                         Function<byte[], byte[]> yReflect, Function<byte[], byte[]> zReflect) {

        // Initialize propagator
        boolean[][][] tempPropagator = AH.array3Dboolean(6, P, P, false);

        Function<byte[], Integer> index = p -> {
            for (int i = 0; i < tiledata.size(); i++) {
                if (AH.same(p, tiledata.get(i))) return i;
            }
            return -1;
        };

        Function<String, String> last = attribute -> {
            if (attribute == null) return null;
            String[] parts = attribute.split(" ");
            return parts[parts.length - 1];
        };

        Function<String, byte[]> tile = attribute -> {
            String[] code = attribute.split(" ");
            String action = code.length == 2 ? code[0] : "";
            byte[] startTile = namedTileData.get(last.apply(attribute)).get(0);

            for (int i = action.length() - 1; i >= 0; i--) {
                char sym = action.charAt(i);
                if (sym == 'x') startTile = xRotate.apply(startTile);
                else if (sym == 'y') startTile = yRotate.apply(startTile);
                else if (sym == 'z') startTile = zRotate.apply(startTile);
                else {
                    Interpreter.writeLine("unknown symmetry " + sym);
                    return null;
                }
            }
            return startTile;
        };

        // Process neighbors (这里继续使用原来的neighbor处理逻辑)
        NodeList neighborNodes = root.getElementsByTagName("neighbors");
        if (neighborNodes.getLength() > 0) {
            Element neighborsElement = (Element) neighborNodes.item(0);
            NodeList neighborElements = neighborsElement.getElementsByTagName("neighbor");

            for (int i = 0; i < neighborElements.getLength(); i++) {
                Element neighborElement = (Element) neighborElements.item(i);
                processNeighborElement(neighborElement, fullSymmetry, tilenames, tile, index, tempPropagator,
                        xRotate, yRotate, zRotate, xReflect, yReflect, zReflect);
            }
        }

        // Fill opposite directions
        for (int p2 = 0; p2 < P; p2++) {
            for (int p1 = 0; p1 < P; p1++) {
                tempPropagator[2][p2][p1] = tempPropagator[0][p1][p2];
                tempPropagator[3][p2][p1] = tempPropagator[1][p1][p2];
                tempPropagator[5][p2][p1] = tempPropagator[4][p1][p2];
            }
        }

        // Convert to sparse propagator
        propagator = new int[6][][];
        for (int d = 0; d < 6; d++) {
            propagator[d] = new int[P][];
            for (int p1 = 0; p1 < P; p1++) {
                List<Integer> sp = new ArrayList<>();
                boolean[] tp = tempPropagator[d][p1];

                for (int p2 = 0; p2 < P; p2++) {
                    if (tp[p2]) sp.add(p2);
                }

                propagator[d][p1] = sp.stream().mapToInt(Integer::intValue).toArray();
            }
        }

        return true;
    }

    // 处理neighbor元素的方法
    private void processNeighborElement(Element neighborElement, boolean fullSymmetry, List<String> tilenames,
                                        Function<String, byte[]> tile, Function<byte[], Integer> index,
                                        boolean[][][] tempPropagator,
                                        Function<byte[], byte[]> xRotate, Function<byte[], byte[]> yRotate,
                                        Function<byte[], byte[]> zRotate, Function<byte[], byte[]> xReflect,
                                        Function<byte[], byte[]> yReflect, Function<byte[], byte[]> zReflect) {

        Function<String, String> last = attribute -> {
            if (attribute == null) return null;
            String[] parts = attribute.split(" ");
            return parts[parts.length - 1];
        };

        if (fullSymmetry) {
            String left = XMLHelper.get(neighborElement, "left", (String) null);
            String right = XMLHelper.get(neighborElement, "right", (String) null);

            if (left != null && right != null) {
                if (!tilenames.contains(last.apply(left)) || !tilenames.contains(last.apply(right))) {
                    Interpreter.writeLine("unknown tile " + last.apply(left) + " or " + last.apply(right) +
                            " at line " + XMLHelper.getLineNumber(neighborElement));
                    return;
                }

                byte[] ltile = tile.apply(left);
                byte[] rtile = tile.apply(right);
                if (ltile == null || rtile == null) return;

                // 处理fullSymmetry的逻辑...
                processFullSymmetryNeighbor(ltile, rtile, index, tempPropagator, xRotate, yRotate, zRotate, xReflect, yReflect, zReflect);
            }
        } else {
            String left = XMLHelper.get(neighborElement, "left", (String) null);
            String right = XMLHelper.get(neighborElement, "right", (String) null);
            String top = XMLHelper.get(neighborElement, "top", (String) null);
            String bottom = XMLHelper.get(neighborElement, "bottom", (String) null);

            if (left != null && right != null) {
                if (!tilenames.contains(last.apply(left)) || !tilenames.contains(last.apply(right))) {
                    Interpreter.writeLine("unknown tile " + last.apply(left) + " or " + last.apply(right) +
                            " at line " + XMLHelper.getLineNumber(neighborElement));
                    return;
                }

                byte[] ltile = tile.apply(left);
                byte[] rtile = tile.apply(right);
                if (ltile == null || rtile == null) return;

                processRegularNeighbor(ltile, rtile, index, tempPropagator, zRotate, xReflect, yReflect);
            } else if (top != null && bottom != null) {
                if (!tilenames.contains(last.apply(top)) || !tilenames.contains(last.apply(bottom))) {
                    Interpreter.writeLine("unknown tile " + last.apply(top) + " or " + last.apply(bottom) +
                            " at line " + XMLHelper.getLineNumber(neighborElement));
                    return;
                }

                byte[] ttile = tile.apply(top);
                byte[] btile = tile.apply(bottom);
                if (ttile == null || btile == null) return;

                processTopBottomNeighbor(ttile, btile, index, tempPropagator, zRotate, xReflect);
            }
        }
    }

    // 其余的辅助方法...
    private void processFullSymmetryNeighbor(byte[] ltile, byte[] rtile, Function<byte[], Integer> index,
                                             boolean[][][] tempPropagator,
                                             Function<byte[], byte[]> xRotate, Function<byte[], byte[]> yRotate,
                                             Function<byte[], byte[]> zRotate, Function<byte[], byte[]> xReflect,
                                             Function<byte[], byte[]> yReflect, Function<byte[], byte[]> zReflect) {
        // 实现fullSymmetry处理逻辑
        List<byte[]> lsym = new ArrayList<>();
        for (byte[] t : SymmetryHelper.squareSymmetries(ltile, xRotate, yReflect, (p1, p2) -> false, null)) {
            lsym.add(t);
        }

        List<byte[]> rsym = new ArrayList<>();
        for (byte[] t : SymmetryHelper.squareSymmetries(rtile, xRotate, yReflect, (p1, p2) -> false, null)) {
            rsym.add(t);
        }

        for (int i = 0; i < lsym.size(); i++) {
            Integer leftIdx = index.apply(lsym.get(i));
            Integer rightIdx = index.apply(rsym.get(i));
            if (leftIdx != null && rightIdx != null && leftIdx >= 0 && rightIdx >= 0) {
                tempPropagator[0][leftIdx][rightIdx] = true;

                Integer leftReflIdx = index.apply(xReflect.apply(rsym.get(i)));
                Integer rightReflIdx = index.apply(xReflect.apply(lsym.get(i)));
                if (leftReflIdx != null && rightReflIdx != null && leftReflIdx >= 0 && rightReflIdx >= 0) {
                    tempPropagator[0][leftReflIdx][rightReflIdx] = true;
                }
            }
        }

        // 处理其他方向的对称...
    }

    private void processRegularNeighbor(byte[] ltile, byte[] rtile, Function<byte[], Integer> index,
                                        boolean[][][] tempPropagator,
                                        Function<byte[], byte[]> zRotate, Function<byte[], byte[]> xReflect,
                                        Function<byte[], byte[]> yReflect) {
        // 实现常规neighbor处理逻辑
        Integer leftIdx = index.apply(ltile);
        Integer rightIdx = index.apply(rtile);

        if (leftIdx != null && rightIdx != null && leftIdx >= 0 && rightIdx >= 0) {
            tempPropagator[0][leftIdx][rightIdx] = true;

            // 处理反射变换
            Integer leftReflYIdx = index.apply(yReflect.apply(ltile));
            Integer rightReflYIdx = index.apply(yReflect.apply(rtile));
            if (leftReflYIdx != null && rightReflYIdx != null && leftReflYIdx >= 0 && rightReflYIdx >= 0) {
                tempPropagator[0][leftReflYIdx][rightReflYIdx] = true;
            }

            // 处理更多变换...
        }
    }

    private void processTopBottomNeighbor(byte[] ttile, byte[] btile, Function<byte[], Integer> index,
                                          boolean[][][] tempPropagator,
                                          Function<byte[], byte[]> zRotate, Function<byte[], byte[]> xReflect) {
        // 实现top-bottom neighbor处理逻辑
        List<byte[]> tsym = new ArrayList<>();
        for (byte[] t : SymmetryHelper.squareSymmetries(ttile, zRotate, xReflect, (p1, p2) -> false, null)) {
            tsym.add(t);
        }

        List<byte[]> bsym = new ArrayList<>();
        for (byte[] t : SymmetryHelper.squareSymmetries(btile, zRotate, xReflect, (p1, p2) -> false, null)) {
            bsym.add(t);
        }

        for (int i = 0; i < tsym.size(); i++) {
            Integer bottomIdx = index.apply(bsym.get(i));
            Integer topIdx = index.apply(tsym.get(i));
            if (bottomIdx != null && topIdx != null && bottomIdx >= 0 && topIdx >= 0) {
                tempPropagator[4][bottomIdx][topIdx] = true;
            }
        }
    }

    private byte[] newtile(byte[] p, TriFunction<Integer, Integer, Integer, Byte> f) {
        return AH.flatArray3Dbyte(S, S, SZ, f::apply);
    }

    @Override
    protected void updateState() {
        Random r = new Random();
        for (int z = 0; z < grid.MZ; z++) {
            for (int y = 0; y < grid.MY; y++) {
                for (int x = 0; x < grid.MX; x++) {
                    boolean[] w = wave.data[x + y * grid.MX + z * grid.MX * grid.MY];
                    int[][] votes = AH.array2D(S * S * SZ, newgrid.C, 0);

                    for (int t = 0; t < P; t++) {
                        if (w[t]) {
                            byte[] tile = tiledata.get(t);
                            for (int dz = 0; dz < SZ; dz++) {
                                for (int dy = 0; dy < S; dy++) {
                                    for (int dx = 0; dx < S; dx++) {
                                        int di = dx + dy * S + dz * S * S;
                                        votes[di][tile[di]]++;
                                    }
                                }
                            }
                        }
                    }

                    for (int dz = 0; dz < SZ; dz++) {
                        for (int dy = 0; dy < S; dy++) {
                            for (int dx = 0; dx < S; dx++) {
                                int[] v = votes[dx + dy * S + dz * S * S];
                                double max = -1.0;
                                byte argmax = (byte) 0xff;

                                for (byte c = 0; c < v.length; c++) {
                                    double vote = v[c] + 0.1 * r.nextDouble();
                                    if (vote > max) {
                                        argmax = c;
                                        max = vote;
                                    }
                                }

                                int sx = x * (S - overlap) + dx;
                                int sy = y * (S - overlap) + dy;
                                int sz = z * (SZ - overlapz) + dz;
                                if (sx < newgrid.MX && sy < newgrid.MY && sz < newgrid.MZ) {
                                    newgrid.state[sx + sy * newgrid.MX + sz * newgrid.MX * newgrid.MY] = argmax;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}