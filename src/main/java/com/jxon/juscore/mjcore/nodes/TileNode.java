// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.utils.Helper;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.VoxHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.apache.commons.lang3.function.TriFunction;
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

            // CRITICAL FIX: Initialize namedTileData BEFORE processing tiles
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

                // CRITICAL FIX: Add to namedTileData
                namedTileData.put(tilename, localdata);

                boolean[] position = new boolean[128];
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

            // Build tilenames list
            List<String> tilenames = new ArrayList<>();
            for (int i = 0; i < tileNodes.getLength(); i++) {
                Element tileElement = (Element) tileNodes.item(i);
                tilenames.add(XMLHelper.get(tileElement, "name"));
            }
            tilenames.add(null);

            // Handle map initialization
            map = new HashMap<>();
            List<Element> ruleElements = XMLHelper.getDirectChildElements(element, "rule");
            for (Element ruleElement : ruleElements) {
                char input = XMLHelper.get(ruleElement, "in", Character.class);
                String outputString = XMLHelper.get(ruleElement, "out");
                String[] outputs = outputString.split("\\|");
                boolean[] position = new boolean[P];

                for (String s : outputs) {
                    String trimmedOutput = s.trim();

                    // Check if it's a known tile name
                    if (positions.containsKey(trimmedOutput)) {
                        boolean[] array = positions.get(trimmedOutput);
                        for (int p = 0; p < P && p < array.length; p++) {
                            if (array[p]) position[p] = true;
                        }
                    } else {
                        Interpreter.writeLine("unknown tilename " + trimmedOutput + " at line " + XMLHelper.getLineNumber(ruleElement));
                        return false;
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
            return initializePropagator(fullSymmetry, namedTileData, tilenames, root, zRotate, yRotate, xRotate,
                    xReflect, yReflect, zReflect) && super.load(element, parentSymmetry, grid);

        } catch (Exception e) {
            Interpreter.writeLine("Error loading tileset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

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

        // Create tile function
        Function<String, byte[]> tile = attribute -> {
            if (attribute == null) return null;

            String[] code = attribute.split(" ");
            String action = code.length == 2 ? code[0] : "";
            String tileName = last.apply(attribute);

            if (!namedTileData.containsKey(tileName)) {
                Interpreter.writeLine("unknown tile " + tileName);
                return null;
            }

            byte[] starttile = namedTileData.get(tileName).get(0);

            for (int i = action.length() - 1; i >= 0; i--) {
                char sym = action.charAt(i);
                if (sym == 'x') starttile = xRotate.apply(starttile);
                else if (sym == 'y') starttile = yRotate.apply(starttile);
                else if (sym == 'z') starttile = zRotate.apply(starttile);
                else {
                    Interpreter.writeLine("unknown symmetry " + sym);
                    return null;
                }
            }
            return starttile;
        };

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
                            Interpreter.writeLine("unknown tile " + last.apply(left) + " or " + last.apply(right));
                            return false;
                        }

                        byte[] ltile = tile.apply(left);
                        byte[] rtile = tile.apply(right);
                        if (ltile == null || rtile == null) return false;

                        // Process full symmetry
                        List<byte[]> lsym = new ArrayList<>();
                        for (byte[] t : SymmetryHelper.squareSymmetries(ltile, xRotate, yReflect, (p1, p2) -> false, null)) {
                            lsym.add(t);
                        }

                        List<byte[]> rsym = new ArrayList<>();
                        for (byte[] t : SymmetryHelper.squareSymmetries(rtile, xRotate, yReflect, (p1, p2) -> false, null)) {
                            rsym.add(t);
                        }

                        for (int j = 0; j < lsym.size(); j++) {
                            Integer li = index.apply(lsym.get(j));
                            Integer ri = index.apply(rsym.get(j));
                            if (li != null && ri != null && li >= 0 && ri >= 0) {
                                tempPropagator[0][li][ri] = true;
                                Integer lri = index.apply(xReflect.apply(rsym.get(j)));
                                Integer rli = index.apply(xReflect.apply(lsym.get(j)));
                                if (lri != null && rli != null && lri >= 0 && rli >= 0) {
                                    tempPropagator[0][lri][rli] = true;
                                }
                            }
                        }

                        // Process other directions
                        byte[] dtile = zRotate.apply(ltile);
                        byte[] utile = zRotate.apply(rtile);

                        List<byte[]> dsym = new ArrayList<>();
                        for (byte[] t : SymmetryHelper.squareSymmetries(dtile, yRotate, zReflect, (p1, p2) -> false, null)) {
                            dsym.add(t);
                        }

                        List<byte[]> usym = new ArrayList<>();
                        for (byte[] t : SymmetryHelper.squareSymmetries(utile, yRotate, zReflect, (p1, p2) -> false, null)) {
                            usym.add(t);
                        }

                        for (int j = 0; j < dsym.size(); j++) {
                            Integer di = index.apply(dsym.get(j));
                            Integer ui = index.apply(usym.get(j));
                            if (di != null && ui != null && di >= 0 && ui >= 0) {
                                tempPropagator[1][di][ui] = true;
                                Integer dri = index.apply(yReflect.apply(usym.get(j)));
                                Integer uri = index.apply(yReflect.apply(dsym.get(j)));
                                if (dri != null && uri != null && dri >= 0 && uri >= 0) {
                                    tempPropagator[1][dri][uri] = true;
                                }
                            }
                        }

                        byte[] btile = yRotate.apply(ltile);
                        byte[] ttile = yRotate.apply(rtile);

                        List<byte[]> bsym = new ArrayList<>();
                        for (byte[] t : SymmetryHelper.squareSymmetries(btile, zRotate, xReflect, (p1, p2) -> false, null)) {
                            bsym.add(t);
                        }

                        List<byte[]> tsym = new ArrayList<>();
                        for (byte[] t : SymmetryHelper.squareSymmetries(ttile, zRotate, xReflect, (p1, p2) -> false, null)) {
                            tsym.add(t);
                        }

                        for (int j = 0; j < bsym.size(); j++) {
                            Integer bi = index.apply(bsym.get(j));
                            Integer ti = index.apply(tsym.get(j));
                            if (bi != null && ti != null && bi >= 0 && ti >= 0) {
                                tempPropagator[4][bi][ti] = true;
                                Integer bri = index.apply(zReflect.apply(tsym.get(j)));
                                Integer tri = index.apply(zReflect.apply(bsym.get(j)));
                                if (bri != null && tri != null && bri >= 0 && tri >= 0) {
                                    tempPropagator[4][bri][tri] = true;
                                }
                            }
                        }
                    }
                } else {
                    String left = XMLHelper.get(neighborElement, "left", (String) null);
                    String right = XMLHelper.get(neighborElement, "right", (String) null);

                    if (left != null && right != null) {
                        if (!tilenames.contains(last.apply(left)) || !tilenames.contains(last.apply(right))) {
                            Interpreter.writeLine("unknown tile " + last.apply(left) + " or " + last.apply(right));
                            return false;
                        }

                        byte[] ltile = tile.apply(left);
                        byte[] rtile = tile.apply(right);
                        if (ltile == null || rtile == null) return false;

                        Integer li = index.apply(ltile);
                        Integer ri = index.apply(rtile);
                        if (li != null && ri != null && li >= 0 && ri >= 0) {
                            tempPropagator[0][li][ri] = true;

                            Integer lyi = index.apply(yReflect.apply(ltile));
                            Integer ryi = index.apply(yReflect.apply(rtile));
                            if (lyi != null && ryi != null && lyi >= 0 && ryi >= 0) {
                                tempPropagator[0][lyi][ryi] = true;
                            }

                            Integer lxi = index.apply(xReflect.apply(rtile));
                            Integer rxi = index.apply(xReflect.apply(ltile));
                            if (lxi != null && rxi != null && lxi >= 0 && rxi >= 0) {
                                tempPropagator[0][lxi][rxi] = true;
                            }

                            Integer lyxi = index.apply(yReflect.apply(xReflect.apply(rtile)));
                            Integer ryxi = index.apply(yReflect.apply(xReflect.apply(ltile)));
                            if (lyxi != null && ryxi != null && lyxi >= 0 && ryxi >= 0) {
                                tempPropagator[0][lyxi][ryxi] = true;
                            }
                        }

                        byte[] dtile = zRotate.apply(ltile);
                        byte[] utile = zRotate.apply(rtile);

                        Integer di = index.apply(dtile);
                        Integer ui = index.apply(utile);
                        if (di != null && ui != null && di >= 0 && ui >= 0) {
                            tempPropagator[1][di][ui] = true;

                            Integer dxi = index.apply(xReflect.apply(dtile));
                            Integer uxi = index.apply(xReflect.apply(utile));
                            if (dxi != null && uxi != null && dxi >= 0 && uxi >= 0) {
                                tempPropagator[1][dxi][uxi] = true;
                            }

                            Integer dyi = index.apply(yReflect.apply(utile));
                            Integer uyi = index.apply(yReflect.apply(dtile));
                            if (dyi != null && uyi != null && dyi >= 0 && uyi >= 0) {
                                tempPropagator[1][dyi][uyi] = true;
                            }

                            Integer dyxi = index.apply(xReflect.apply(yReflect.apply(utile)));
                            Integer uyxi = index.apply(xReflect.apply(yReflect.apply(dtile)));
                            if (dyxi != null && uyxi != null && dyxi >= 0 && uyxi >= 0) {
                                tempPropagator[1][dyxi][uyxi] = true;
                            }
                        }
                    } else {
                        String top = XMLHelper.get(neighborElement, "top", (String) null);
                        String bottom = XMLHelper.get(neighborElement, "bottom", (String) null);

                        if (top != null && bottom != null) {
                            if (!tilenames.contains(last.apply(top)) || !tilenames.contains(last.apply(bottom))) {
                                Interpreter.writeLine("unknown tile " + last.apply(top) + " or " + last.apply(bottom));
                                return false;
                            }

                            byte[] ttile = tile.apply(top);
                            byte[] btile = tile.apply(bottom);
                            if (ttile == null || btile == null) return false;

                            List<byte[]> tsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(ttile, zRotate, xReflect, (p1, p2) -> false, null)) {
                                tsym.add(t);
                            }

                            List<byte[]> bsym = new ArrayList<>();
                            for (byte[] t : SymmetryHelper.squareSymmetries(btile, zRotate, xReflect, (p1, p2) -> false, null)) {
                                bsym.add(t);
                            }

                            for (int j = 0; j < tsym.size(); j++) {
                                Integer bi = index.apply(bsym.get(j));
                                Integer ti = index.apply(tsym.get(j));
                                if (bi != null && ti != null && bi >= 0 && ti >= 0) {
                                    tempPropagator[4][bi][ti] = true;
                                }
                            }
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

        return true;
    }

    private byte[] newtile(byte[] p, TriFunction<Integer, Integer, Integer, Byte> f) {
        return AH.flatArray3Dbyte(S, S, SZ, f);
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
                                        if (di < tile.length) {
                                            votes[di][tile[di]]++;
                                        }
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


}