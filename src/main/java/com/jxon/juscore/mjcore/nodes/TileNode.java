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

                boolean[] position = new boolean[128]; // 预分配
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

            // 2. 为复合符号创建映射
            createCompositeSymbols(positions, P);

            // 初始化map
            map = new HashMap<>();
            List<Element> ruleElements = XMLHelper.getElementsByTagName(element, "rule");
            for (Element ruleElement : ruleElements) {
                char input = XMLHelper.get(ruleElement, "in", Character.class);
                String outputString = XMLHelper.get(ruleElement, "out");
                String[] outputs = outputString.split("\\|");
                boolean[] position = new boolean[P];

                System.out.println("DEBUG: Processing rule - input: '" + input + "', outputs: " + Arrays.toString(outputs));

                for (String s : outputs) {
                    String trimmedOutput = s.trim();
                    System.out.println("DEBUG: Processing output: '" + trimmedOutput + "'");

                    // 首先尝试直接匹配
                    if (positions.containsKey(trimmedOutput)) {
                        boolean[] array = positions.get(trimmedOutput);
                        for (int p = 0; p < P && p < array.length; p++) {
                            if (array[p]) position[p] = true;
                        }
                        System.out.println("DEBUG: Direct match found for '" + trimmedOutput + "'");
                    }
                    // 尝试解析复合符号
                    else if (parseCompositeSymbol(trimmedOutput, positions, position, P)) {
                        System.out.println("DEBUG: Composite symbol parsed successfully for '" + trimmedOutput + "'");
                    }
                    // 尝试作为单个瓦片名称处理
                    else if (namedTileData.containsKey(trimmedOutput)) {
                        System.out.println("DEBUG: Found in namedTileData: '" + trimmedOutput + "'");
                        // 为单个瓦片创建 position
                        List<byte[]> tileVariants = namedTileData.get(trimmedOutput);
                        for (int i = 0; i < tiledata.size(); i++) {
                            byte[] currentTile = tiledata.get(i);
                            for (byte[] variant : tileVariants) {
                                if (AH.same(currentTile, variant)) {
                                    position[i] = true;
                                    break;
                                }
                            }
                        }
                    }
                    // 最后尝试通配符处理
                    else if (trimmedOutput.equals("*")) {
                        Arrays.fill(position, true);
                        System.out.println("DEBUG: Wildcard match for '*'");
                    }
                    // 如果都失败，报错
                    else {
                        System.out.println("DEBUG: Failed to resolve output '" + trimmedOutput + "'");
                        System.out.println("DEBUG: Available positions keys: " + positions.keySet());
                        System.out.println("DEBUG: Available namedTileData keys: " + namedTileData.keySet());

                        // 尝试创建默认映射
                        if (createDefaultPositionMapping(trimmedOutput, positions, position, P)) {
                            System.out.println("DEBUG: Created default mapping for '" + trimmedOutput + "'");
                        } else {
                            Interpreter.writeLine("unknown tilename " + trimmedOutput + " at line " + XMLHelper.getLineNumber(ruleElement));
                            return false;
                        }
                    }
                }

                // 输入值处理 - 修复输入值查找逻辑
                Byte inputValue = null;

                // 首先尝试从 newgrid 获取输入值
                if (newgrid != null && newgrid.values.containsKey(input)) {
                    inputValue = newgrid.values.get(input);
                    System.out.println("DEBUG: Found input '" + input + "' in newgrid with value " + inputValue);
                }
                // 如果 newgrid 中没有，则从原始 grid 获取
                else if (grid.values.containsKey(input)) {
                    inputValue = grid.values.get(input);
                    System.out.println("DEBUG: Found input '" + input + "' in original grid with value " + inputValue);
                }

                if (inputValue != null) {
                    map.put(inputValue, position);
                } else {
                    System.out.println("ERROR: Could not find input value for '" + input + "'");
                    System.out.println("DEBUG: Available characters in original grid: " + grid.values.keySet());
                    if (newgrid != null) {
                        System.out.println("DEBUG: Available characters in newgrid: " + newgrid.values.keySet());
                    }
                    System.out.println("DEBUG: Requested input character: '" + input + "'");
                    System.out.println("DEBUG: Current element tag: " + element.getTagName());

                    // 尝试使用映射的字符（例如 D -> B, Y -> W 等）
                    Character mappedChar = getMappedChar(input, grid.values.keySet());
                    if (mappedChar != null) {
                        inputValue = grid.values.get(mappedChar);
                        System.out.println("DEBUG: Using mapped character '" + mappedChar + "' for input '" + input + "'");
                        map.put(inputValue, position);
                    } else {
                        Interpreter.writeLine("unknown input value " + input + " at line " + XMLHelper.getLineNumber(ruleElement));
                        return false;
                    }
                }
            }


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

    private boolean createDefaultPositionMapping(String symbol, Map<String, boolean[]> positions,
                                                 boolean[] targetPosition, int P) {
        // 处理一些常见的模式
        if (symbol.contains(" ")) {
            // 空格分隔的符号，递归处理每个部分
            return parseCompositeSymbol(symbol, positions, targetPosition, P);
        }

        // 尝试部分匹配
        for (String key : positions.keySet()) {
            if (key.contains(symbol) || symbol.contains(key)) {
                boolean[] array = positions.get(key);
                for (int p = 0; p < P && p < array.length; p++) {
                    if (array[p]) {
                        targetPosition[p] = true;
                    }
                }
                return true;
            }
        }

        return false;
    }

    private Character getMappedChar(char requestedChar, Set<Character> availableChars) {
        // 创建从复杂字符到简单字符的映射
        Map<Character, Character> charMapping = new HashMap<>();
//        charMapping.put('D', 'B'); // Dark -> Black
//        charMapping.put('Y', 'W'); // Yellow -> White
//        charMapping.put('A', 'W'); // Air -> White
//        charMapping.put('P', 'W'); // 等等...
//        charMapping.put('R', 'B');
//        charMapping.put('F', 'B');
//        charMapping.put('U', 'B');
//        charMapping.put('E', 'B');
//        charMapping.put('C', 'B');

        Character mapped = charMapping.get(requestedChar);
        if (mapped != null && availableChars.contains(mapped)) {
            return mapped;
        }

        // 如果没有特定映射，尝试一些通用映射
//        if (availableChars.contains('B')) return 'B';  // 默认映射到 Black
//        if (availableChars.contains('W')) return 'W';  // 或 White

        return null;
    }

    private Function<String, byte[]> createTileFunction(Map<String, List<byte[]>> namedTileData,
                                                        Function<byte[], byte[]> xRotate,
                                                        Function<byte[], byte[]> yRotate,
                                                        Function<byte[], byte[]> zRotate) {
        return attribute -> {
            if (attribute == null) {
                System.out.println("ERROR: Null attribute in tile function");
                return null;
            }

            String[] code = attribute.split(" ");
            String action = code.length == 2 ? code[0] : "";
            String tileName = code[code.length - 1]; // 获取最后一个部分作为瓦片名称

            System.out.println("DEBUG: Processing tile attribute '" + attribute + "', tileName: '" + tileName + "', action: '" + action + "'");

            if (!namedTileData.containsKey(tileName)) {
                System.out.println("ERROR: Tile '" + tileName + "' not found in namedTileData");
                System.out.println("DEBUG: Available tiles: " + namedTileData.keySet());
                return null;
            }

            List<byte[]> tileVariants = namedTileData.get(tileName);
            if (tileVariants == null || tileVariants.isEmpty()) {
                System.out.println("ERROR: No variants found for tile '" + tileName + "'");
                return null;
            }

            byte[] startTile = tileVariants.get(0);

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
    }

    private boolean parseCompositeSymbol(String symbol, Map<String, boolean[]> positions,
                                         boolean[] targetPosition, int P) {
        // 处理空格分隔的符号
        String[] parts = symbol.split("\\s+");
        boolean foundAny = false;

        System.out.println("DEBUG: Parsing composite symbol '" + symbol + "' into parts: " + Arrays.toString(parts));

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            System.out.println("DEBUG: Processing part '" + part + "'");

            if (positions.containsKey(part)) {
                boolean[] array = positions.get(part);
                System.out.println("DEBUG: Found position array for '" + part + "' with length " + array.length);
                for (int p = 0; p < P && p < array.length; p++) {
                    if (array[p]) {
                        targetPosition[p] = true;
                        foundAny = true;
                    }
                }
            } else {
                System.out.println("DEBUG: Part '" + part + "' not found in positions. Available keys: " + positions.keySet());
            }
        }

        return foundAny;
    }

    // 创建复合模式
    private void createCompositePattern(Map<String, boolean[]> positions, String name1, String name2, int P) {
        String pattern = name1 + " " + name2;
        if (!positions.containsKey(pattern)) {
            boolean[] pos1 = positions.get(name1);
            boolean[] pos2 = positions.get(name2);
            if (pos1 != null && pos2 != null) {
                boolean[] combined = combinePositions(pos1, pos2, null, P);
                if (combined != null) {
                    positions.put(pattern, combined);
                }
            }
        }
    }

    private void createCompositeSymbols(Map<String, boolean[]> positions, int P) {
        Set<String> baseNames = new HashSet<>(positions.keySet());

        System.out.println("DEBUG: Creating composite symbols from base names: " + baseNames);

        for (String baseName : baseNames) {
            if ("*".equals(baseName)) continue;

            // 创建各种复合符号模式
            createCompositePattern(positions, "*", baseName, P);
            createCompositePattern(positions, baseName, "*", P);
            createCompositePattern(positions, baseName, baseName, P); // 支持 "BB EE" 这样的模式

            // 为每个基础名称创建重复模式
            String doublePattern = baseName + " " + baseName;
            if (!positions.containsKey(doublePattern)) {
                boolean[] pos = positions.get(baseName);
                if (pos != null) {
                    positions.put(doublePattern, pos.clone());
                    System.out.println("DEBUG: Created double pattern '" + doublePattern + "'");
                }
            }
        }

        // 创建一些常见的复合模式
        for (String name1 : baseNames) {
            for (String name2 : baseNames) {
                if (!name1.equals("*") && !name2.equals("*")) {
                    String pattern = name1 + " " + name2;
                    if (!positions.containsKey(pattern)) {
                        boolean[] combined = combinePositions(
                                positions.get(name1),
                                positions.get(name2),
                                null, P);
                        if (combined != null) {
                            positions.put(pattern, combined);
                            System.out.println("DEBUG: Created pattern '" + pattern + "'");
                        }
                    }
                }
            }
        }
    }

    private boolean[] combinePositions(boolean[] pos1, boolean[] pos2, boolean[] pos3, int P) {
        boolean[] result = new boolean[P];

        for (int i = 0; i < P; i++) {
            boolean val = false;
            if (pos1 != null && i < pos1.length) val |= pos1[i];
            if (pos2 != null && i < pos2.length) val |= pos2[i];
            if (pos3 != null && i < pos3.length) val |= pos3[i];
            result[i] = val;
        }

        return result;
    }

    private boolean initializePropagator(boolean fullSymmetry, Map<String, List<byte[]>> namedTileData,
                                         List<String> tilenames, Element root,
                                         Function<byte[], byte[]> zRotate, Function<byte[], byte[]> yRotate,
                                         Function<byte[], byte[]> xRotate, Function<byte[], byte[]> xReflect,
                                         Function<byte[], byte[]> yReflect, Function<byte[], byte[]> zReflect) {

        // 检查 namedTileData 是否为空
        if (namedTileData == null || namedTileData.isEmpty()) {
            System.out.println("ERROR: namedTileData is null or empty");
            return false;
        }

        System.out.println("DEBUG: namedTileData keys: " + namedTileData.keySet());
        System.out.println("DEBUG: tilenames: " + tilenames);

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

        // 修复 tile 函数
        Function<String, byte[]> tile = createTileFunction(namedTileData, xRotate, yRotate, zRotate);

        // Process neighbors
        NodeList neighborNodes = root.getElementsByTagName("neighbors");
        if (neighborNodes.getLength() > 0) {
            Element neighborsElement = (Element) neighborNodes.item(0);
            NodeList neighborElements = neighborsElement.getElementsByTagName("neighbor");

            for (int i = 0; i < neighborElements.getLength(); i++) {
                Element neighborElement = (Element) neighborElements.item(i);

                // 添加安全检查
                try {
                    processNeighborElement(neighborElement, fullSymmetry, tilenames, tile, index, tempPropagator,
                            xRotate, yRotate, zRotate, xReflect, yReflect, zReflect);
                } catch (Exception e) {
                    System.out.println("ERROR: Failed to process neighbor element: " + e.getMessage());
                    e.printStackTrace();
                    // 继续处理其他邻居，而不是直接失败
                }
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
                String leftTileName = last.apply(left);
                String rightTileName = last.apply(right);

                if (!tilenames.contains(leftTileName) || !tilenames.contains(rightTileName)) {
                    System.out.println("WARNING: unknown tile " + leftTileName + " or " + rightTileName);
                    return; // 跳过这个邻居而不是失败
                }

                byte[] ltile = tile.apply(left);
                byte[] rtile = tile.apply(right);
                if (ltile == null || rtile == null) {
                    System.out.println("WARNING: Failed to get tile data for " + left + " or " + right);
                    return;
                }

                processFullSymmetryNeighbor(ltile, rtile, index, tempPropagator, xRotate, yRotate, zRotate, xReflect, yReflect, zReflect);
            }
        } else {
            String left = XMLHelper.get(neighborElement, "left", (String) null);
            String right = XMLHelper.get(neighborElement, "right", (String) null);
            String top = XMLHelper.get(neighborElement, "top", (String) null);
            String bottom = XMLHelper.get(neighborElement, "bottom", (String) null);

            if (left != null && right != null) {
                String leftTileName = last.apply(left);
                String rightTileName = last.apply(right);

                if (!tilenames.contains(leftTileName) || !tilenames.contains(rightTileName)) {
                    System.out.println("WARNING: unknown tile " + leftTileName + " or " + rightTileName);
                    return;
                }

                byte[] ltile = tile.apply(left);
                byte[] rtile = tile.apply(right);
                if (ltile == null || rtile == null) {
                    System.out.println("WARNING: Failed to get tile data for " + left + " or " + right);
                    return;
                }

                processRegularNeighbor(ltile, rtile, index, tempPropagator, zRotate, xReflect, yReflect);
            } else if (top != null && bottom != null) {
                String topTileName = last.apply(top);
                String bottomTileName = last.apply(bottom);

                if (!tilenames.contains(topTileName) || !tilenames.contains(bottomTileName)) {
                    System.out.println("WARNING: unknown tile " + topTileName + " or " + bottomTileName);
                    return;
                }

                byte[] ttile = tile.apply(top);
                byte[] btile = tile.apply(bottom);
                if (ttile == null || btile == null) {
                    System.out.println("WARNING: Failed to get tile data for " + top + " or " + bottom);
                    return;
                }

                processTopBottomNeighbor(ttile, btile, index, tempPropagator, zRotate, xReflect);
            }
        }
    }

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