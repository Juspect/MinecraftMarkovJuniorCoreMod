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
import java.util.stream.IntStream;

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

                boolean[] position = new boolean[128];
                namedTileData.put(tilename, localdata);
                for (byte[] p : localdata) {
                    tiledata.add(p);
                    tempStationary.add(weight);
                    position[ind] = true;
                    ind++;
                }
                positions.put(tilename, position);
            }

            P = tiledata.size();
            System.out.println("P = " + P);
            weights = tempStationary.stream().mapToDouble(Double::doubleValue).toArray();

            // Initialize map
            map = new HashMap<>();
            List<Element> ruleElements = XMLHelper.getElementsByTagName(element, "rule");
            for (Element ruleElement : ruleElements) {
                char input = XMLHelper.get(ruleElement, "in", Character.class);
                String[] outputs = XMLHelper.get(ruleElement, "out").split("\\|");
                boolean[] position = new boolean[P];

                for (String s : outputs) {
                    boolean[] array = positions.get(s);
                    if (array == null) {
                        Interpreter.writeLine("unknown tilename " + s + " at line " + XMLHelper.getLineNumber(ruleElement));
                        return false;
                    }
                    for (int p = 0; p < P; p++) {
                        if (array[p]) position[p] = true;
                    }
                }
                map.put(grid.values.get(input), position);
            }
            if (!map.containsKey((byte) 0)) {
                boolean[] allTrue = new boolean[P];
                Arrays.fill(allTrue, true);
                map.put((byte) 0, allTrue);
            }

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

            List<String> tilenames = new ArrayList<>();
            for (int i = 0; i < tileNodes.getLength(); i++) {
                Element tileElement = (Element) tileNodes.item(i);
                tilenames.add(XMLHelper.get(tileElement, "name"));
            }
            tilenames.add(null);

            // Process neighbors
            NodeList neighborNodes = root.getElementsByTagName("neighbors");
            if (neighborNodes.getLength() > 0) {
                Element neighborsElement = (Element) neighborNodes.item(0);
                NodeList neighborElements = neighborsElement.getElementsByTagName("neighbor");

                for (int i = 0; i < neighborElements.getLength(); i++) {
                    Element neighborElement = (Element) neighborElements.item(i);

                    if (fullSymmetry) {
                        String left = XMLHelper.get(neighborElement, "left", (String) null);
                        String right = XMLHelper.get(neighborElement, "right", (String) null);

                        if (left != null && right != null) {
                            if (!tilenames.contains(last.apply(left)) || !tilenames.contains(last.apply(right))) {
                                Interpreter.writeLine("unknown tile " + last.apply(left) + " or " + last.apply(right) +
                                        " at line " + XMLHelper.getLineNumber(neighborElement));
                                return false;
                            }

                            byte[] ltile = tile.apply(left);
                            byte[] rtile = tile.apply(right);
                            if (ltile == null || rtile == null) return false;

                            List<byte[]> lsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(ltile, xRotate, yReflect,
                                    (p1, p2) -> false, null)) {
                                lsym.add(t);
                            }

                            List<byte[]> rsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(rtile, xRotate, yReflect,
                                    (p1, p2) -> false, null)) {
                                rsym.add(t);
                            }

                            for (int j = 0; j < lsym.size(); j++) {
                                tempPropagator[0][index.apply(lsym.get(j))][index.apply(rsym.get(j))] = true;
                                tempPropagator[0][index.apply(xReflect.apply(rsym.get(j)))][index.apply(xReflect.apply(lsym.get(j)))] = true;
                            }

                            // Additional transformations for 3D
                            byte[] dtile = zRotate.apply(ltile);
                            byte[] utile = zRotate.apply(rtile);

                            List<byte[]> dsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(dtile, yRotate, zReflect,
                                    (p1, p2) -> false, null)) {
                                dsym.add(t);
                            }

                            List<byte[]> usym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(utile, yRotate, zReflect,
                                    (p1, p2) -> false, null)) {
                                usym.add(t);
                            }

                            for (int j = 0; j < dsym.size(); j++) {
                                tempPropagator[1][index.apply(dsym.get(j))][index.apply(usym.get(j))] = true;
                                tempPropagator[1][index.apply(yReflect.apply(usym.get(j)))][index.apply(yReflect.apply(dsym.get(j)))] = true;
                            }

                            byte[] btile = yRotate.apply(ltile);
                            byte[] ttile = yRotate.apply(rtile);

                            List<byte[]> bsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(btile, zRotate, xReflect,
                                    (p1, p2) -> false, null)) {
                                bsym.add(t);
                            }

                            List<byte[]> tsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(ttile, zRotate, xReflect,
                                    (p1, p2) -> false, null)) {
                                tsym.add(t);
                            }

                            for (int j = 0; j < bsym.size(); j++) {
                                tempPropagator[4][index.apply(bsym.get(j))][index.apply(tsym.get(j))] = true;
                                tempPropagator[4][index.apply(zReflect.apply(tsym.get(j)))][index.apply(zReflect.apply(bsym.get(j)))] = true;
                            }
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
                                return false;
                            }

                            byte[] ltile = tile.apply(left);
                            byte[] rtile = tile.apply(right);
                            if (ltile == null || rtile == null) return false;

                            tempPropagator[0][index.apply(ltile)][index.apply(rtile)] = true;
                            tempPropagator[0][index.apply(yReflect.apply(ltile))][index.apply(yReflect.apply(rtile))] = true;
                            tempPropagator[0][index.apply(xReflect.apply(rtile))][index.apply(xReflect.apply(ltile))] = true;
                            tempPropagator[0][index.apply(yReflect.apply(xReflect.apply(rtile)))][index.apply(yReflect.apply(xReflect.apply(ltile)))] = true;

                            byte[] dtile = zRotate.apply(ltile);
                            byte[] utile = zRotate.apply(rtile);

                            tempPropagator[1][index.apply(dtile)][index.apply(utile)] = true;
                            tempPropagator[1][index.apply(xReflect.apply(dtile))][index.apply(xReflect.apply(utile))] = true;
                            tempPropagator[1][index.apply(yReflect.apply(utile))][index.apply(yReflect.apply(dtile))] = true;
                            tempPropagator[1][index.apply(xReflect.apply(yReflect.apply(utile)))][index.apply(xReflect.apply(yReflect.apply(dtile)))] = true;
                        } else if (top != null && bottom != null) {
                            if (!tilenames.contains(last.apply(top)) || !tilenames.contains(last.apply(bottom))) {
                                Interpreter.writeLine("unknown tile " + last.apply(top) + " or " + last.apply(bottom) +
                                        " at line " + XMLHelper.getLineNumber(neighborElement));
                                return false;
                            }

                            byte[] ttile = tile.apply(top);
                            byte[] btile = tile.apply(bottom);
                            if (ttile == null || btile == null) return false;

                            List<byte[]> tsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(ttile, zRotate, xReflect,
                                    (p1, p2) -> false, null)) {
                                tsym.add(t);
                            }

                            List<byte[]> bsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(btile, zRotate, xReflect,
                                    (p1, p2) -> false, null)) {
                                bsym.add(t);
                            }

                            for (int j = 0; j < tsym.size(); j++) {
                                tempPropagator[4][index.apply(bsym.get(j))][index.apply(tsym.get(j))] = true;
                            }
                        }
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

            return super.load(element, parentSymmetry, grid);

        } catch (Exception e) {
            Interpreter.writeLine("Error loading tileset: " + e.getMessage());
            return false;
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
                                newgrid.state[sx + sy * newgrid.MX + sz * newgrid.MX * newgrid.MY] = argmax;
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