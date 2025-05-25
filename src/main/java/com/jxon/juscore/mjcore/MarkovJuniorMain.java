// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Main class demonstrating the MarkovJunior Java implementation
 * This is a basic example showing how to use the converted Java classes
 */
public class MarkovJuniorMain {
    
    public static void main(String[] args) {
        try {
            // Example usage of the MarkovJunior Java implementation
            System.out.println("MarkovJunior Java Implementation");
            System.out.println("================================");
            
            // Create a simple example programmatically
            runSimpleExample();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void runSimpleExample() {
        try {
            // Create a simple XML document for testing
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Create root element with basic configuration
            Element root = doc.createElement("sequence");
            root.setAttribute("values", "BW"); // Black and White
            root.setAttribute("origin", "true");
            doc.appendChild(root);
            
            // Create a simple one node with a basic rule
            Element oneNode = doc.createElement("one");
            oneNode.setAttribute("steps", "100");
            root.appendChild(oneNode);
            
            Element rule = doc.createElement("rule");
            rule.setAttribute("in", "B");
            rule.setAttribute("out", "W");
            oneNode.appendChild(rule);
            
            // Load the interpreter
            Interpreter interpreter = Interpreter.load(root, 20, 20, 1);
            if (interpreter != null) {
                System.out.println("Interpreter loaded successfully!");
                
                // Run the interpreter
                Random metaRandom = new Random();
                int seed = metaRandom.nextInt();
                System.out.println("Running with seed: " + seed);
                
                int stepCount = 0;
                for (Interpreter.RunResult result : interpreter.run(seed, 100, false)) {
                    stepCount++;
                    if (stepCount <= 5) { // Only print first few steps
                        System.out.println("Step " + stepCount + ": Generated " + 
                                         result.FX + "x" + result.FY + " grid");
                        printGrid(result.state, result.FX, result.FY, result.legend);
                    }
                }
                
                System.out.println("Completed " + stepCount + " steps");
            } else {
                System.out.println("Failed to load interpreter");
            }
            
        } catch (Exception e) {
            System.out.println("Error in simple example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printGrid(byte[] state, int width, int height, char[] legend) {
        System.out.println("Grid state:");
        for (int y = 0; y < Math.min(height, 10); y++) { // Limit to 10 rows for display
            for (int x = 0; x < Math.min(width, 20); x++) { // Limit to 20 columns for display
                int index = x + y * width;
                if (index < state.length) {
                    char symbol = legend[state[index]];
                    System.out.print(symbol + " ");
                } else {
                    System.out.print("? ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
    
    /**
     * Example of creating a palette map (similar to the original C# implementation)
     */
    private static Map<Character, Integer> createDefaultPalette() {
        Map<Character, Integer> palette = new HashMap<>();
        palette.put('B', 0xFF000000); // Black
        palette.put('W', 0xFFFFFFFF); // White
        palette.put('R', 0xFFFF0000); // Red
        palette.put('G', 0xFF00FF00); // Green
        palette.put('B', 0xFF0000FF); // Blue
        palette.put('Y', 0xFFFFFF00); // Yellow
        // Add more colors as needed
        return palette;
    }
    
    /**
     * Utility method to demonstrate rule creation
     */
    private static void demonstrateRuleCreation() {
        System.out.println("Demonstrating rule creation:");
        
        // Create input and output patterns
        int[] input = {1}; // Wave for 'B' (black)
        byte[] output = {1}; // Value for 'W' (white)
        
        // Create a simple 1x1 rule that changes black to white
        Rule rule = new Rule(input, 1, 1, 1, output, 1, 1, 1, 2, 1.0);
        
        System.out.println("Created rule with IMX=" + rule.IMX + ", IMY=" + rule.IMY + ", IMZ=" + rule.IMZ);
        System.out.println("Output dimensions: OMX=" + rule.OMX + ", OMY=" + rule.OMY + ", OMZ=" + rule.OMZ);
    }
}