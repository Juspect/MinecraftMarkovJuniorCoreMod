// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class Program {

    private Program() {} // Prevent instantiation

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        // Create output directory
        Path outputDir = Paths.get("output");
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectory(outputDir);
            }
            // Clear existing files
            Files.list(outputDir).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    System.out.println("Failed to delete file: " + file);
                }
            });
        } catch (IOException e) {
            System.out.println("Failed to create/clear output directory: " + e.getMessage());
        }

        // Load palette
        Map<Character, Integer> palette = loadPalette("resources/palette.xml");

        Random meta = new Random();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("models.xml"));

            NodeList modelNodes = doc.getElementsByTagName("model");
            for (int i = 0; i < modelNodes.getLength(); i++) {
                Element modelElement = (Element) modelNodes.item(i);
                processModel(modelElement, meta, palette);
            }
        } catch (Exception e) {
            System.out.println("Error processing models: " + e.getMessage());
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("time = " + (endTime - startTime));
    }

    private static void processModel(Element modelElement, Random meta, Map<Character, Integer> palette) {
        try {
            String name = XMLHelper.get(modelElement, "name");
            int linearSize = XMLHelper.get(modelElement, "size", -1);
            int dimension = XMLHelper.get(modelElement, "d", 2);
            int MX = XMLHelper.get(modelElement, "length", linearSize);
            int MY = XMLHelper.get(modelElement, "width", linearSize);
            int MZ = XMLHelper.get(modelElement, "height", dimension == 2 ? 1 : linearSize);

            System.out.print(name + " > ");
            String filename = "models/" + name + ".xml";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document modelDoc;
            try {
                modelDoc = builder.parse(new File(filename));
            } catch (Exception e) {
                System.out.println("ERROR: couldn't open xml file " + filename);
                return;
            }

            Interpreter interpreter = Interpreter.load(modelDoc.getDocumentElement(), MX, MY, MZ);
            if (interpreter == null) {
                System.out.println("ERROR");
                return;
            }

            int amount = XMLHelper.get(modelElement, "amount", 2);
            int pixelsize = XMLHelper.get(modelElement, "pixelsize", 4);
            String seedString = XMLHelper.get(modelElement, "seeds", (String) null);
            int[] seeds = null;
            if (seedString != null) {
                String[] seedStrings = seedString.split(" ");
                seeds = new int[seedStrings.length];
                for (int j = 0; j < seedStrings.length; j++) {
                    seeds[j] = Integer.parseInt(seedStrings[j]);
                }
            }
            boolean gif = XMLHelper.get(modelElement, "gif", false);
            boolean iso = XMLHelper.get(modelElement, "iso", false);
            int steps = XMLHelper.get(modelElement, "steps", gif ? 1000 : 50000);
            int gui = XMLHelper.get(modelElement, "gui", 0);

            if (gif) {
                amount = 1;
            }

            // Custom palette for this model
            Map<Character, Integer> customPalette = new HashMap<>(palette);
            NodeList colorNodes = modelElement.getElementsByTagName("color");
            for (int j = 0; j < colorNodes.getLength(); j++) {
                Element colorElement = (Element) colorNodes.item(j);
                char symbol = XMLHelper.get(colorElement, "symbol", Character.class);
                String value = XMLHelper.get(colorElement, "value");
                customPalette.put(symbol, (255 << 24) + Integer.parseInt(value, 16));
            }

            for (int k = 0; k < amount; k++) {
                int seed = (seeds != null && k < seeds.length) ? seeds[k] : meta.nextInt();

                for (Interpreter.RunResult result : interpreter.run(seed, steps, gif)) {
                    int[] colors = new int[result.legend.length];
                    for (int c = 0; c < result.legend.length; c++) {
                        colors[c] = customPalette.get(result.legend[c]);
                    }

                    String outputname = gif ? ("output/" + interpreter.counter) : ("output/" + name + "_" + seed);

                    if (result.FZ == 1 || iso) {
                        Graphics.RenderResult renderResult = Graphics.render(result.state, result.FX, result.FY, result.FZ,
                                colors, pixelsize, gui);
                        // Note: GUI drawing would be implemented here if needed
                        Graphics.saveBitmap(renderResult.bitmap, renderResult.width, renderResult.height, outputname + ".png");
                    } else {
                        VoxHelper.saveVox(result.state, (byte) result.FX, (byte) result.FY, (byte) result.FZ,
                                colors, outputname + ".vox");
                    }
                }
                System.out.println("DONE");
            }
        } catch (Exception e) {
            System.out.println("Error processing model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<Character, Integer> loadPalette(String filename) {
        Map<Character, Integer> palette = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));

            NodeList colorNodes = doc.getElementsByTagName("color");
            for (int i = 0; i < colorNodes.getLength(); i++) {
                Element colorElement = (Element) colorNodes.item(i);
                char symbol = XMLHelper.get(colorElement, "symbol", Character.class);
                String value = XMLHelper.get(colorElement, "value");
                palette.put(symbol, (255 << 24) + Integer.parseInt(value, 16));
            }
        } catch (Exception e) {
            System.out.println("Error loading palette: " + e.getMessage());
            // Provide default palette
            palette.put('B', 0xFF000000); // Black
            palette.put('W', 0xFFFFFFFF); // White
            palette.put('R', 0xFFFF0000); // Red
            palette.put('G', 0xFF00FF00); // Green
            palette.put('Y', 0xFFFFFF00); // Yellow
        }

        return palette;
    }

    /**
     * Alternative main method for integration with Minecraft mods
     * This allows the MarkovJunior engine to be used as a library
     */
    public static Interpreter.RunResult[] generateFromModel(String modelPath, int MX, int MY, int MZ, int seed, int steps) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(modelPath));

            Interpreter interpreter = Interpreter.load(doc.getDocumentElement(), MX, MY, MZ);
            if (interpreter == null) {
                return new Interpreter.RunResult[0];
            }

            java.util.List<Interpreter.RunResult> results = new java.util.ArrayList<>();
            for (Interpreter.RunResult result : interpreter.run(seed, steps, false)) {
                results.add(result);
            }

            return results.toArray(new Interpreter.RunResult[0]);
        } catch (Exception e) {
            System.out.println("Error generating from model: " + e.getMessage());
            return new Interpreter.RunResult[0];
        }
    }

    /**
     * Utility method for Minecraft mod integration
     */
    public static byte[] generateSimpleGrid(String rulesXml, int width, int height, int seed, int steps) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(rulesXml.getBytes()));

            Interpreter interpreter = Interpreter.load(doc.getDocumentElement(), width, height, 1);
            if (interpreter == null) {
                return new byte[width * height];
            }

            Interpreter.RunResult lastResult = null;
            for (Interpreter.RunResult result : interpreter.run(seed, steps, false)) {
                lastResult = result;
            }

            return lastResult != null ? lastResult.state : new byte[width * height];
        } catch (Exception e) {
            System.out.println("Error generating simple grid: " + e.getMessage());
            return new byte[width * height];
        }
    }
}