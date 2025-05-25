// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Interpreter {
    public Branch root, current;
    public Grid grid;
    private Grid startgrid;
    
    private boolean origin;
    public Random random;
    
    public List<Rule.Tuple3> changes;
    public List<Integer> first;
    public int counter;
    
    public boolean gif;
    
    private Interpreter() {}
    
    public static Interpreter load(Element element, int MX, int MY, int MZ) {
        Interpreter ip = new Interpreter();
        ip.origin = XMLHelper.get(element, "origin", false);
        ip.grid = Grid.load(element, MX, MY, MZ);
        if (ip.grid == null) {
            System.out.println("failed to load grid");
            return null;
        }
        ip.startgrid = ip.grid;
        
        String symmetryString = XMLHelper.get(element, "symmetry", (String) null);
        boolean[] symmetry = SymmetryHelper.getSymmetry(ip.grid.MZ == 1, symmetryString, 
                                                        AH.array1D(ip.grid.MZ == 1 ? 8 : 48, true));
        if (symmetry == null) {
            writeLine("unknown symmetry " + symmetryString + " at line " + XMLHelper.getLineNumber(element));
            return null;
        }
        
        Node topnode = Node.factory(element, symmetry, ip, ip.grid);
        if (topnode == null) {
            return null;
        }
        ip.root = topnode instanceof Branch ? (Branch) topnode : new MarkovNode(topnode, ip);
        
        ip.changes = new ArrayList<>();
        ip.first = new ArrayList<>();
        return ip;
    }
    
    public Iterable<RunResult> run(int seed, int steps, boolean gif) {
        random = new Random(seed);
        grid = startgrid;
        grid.clear();
        if (origin) {
            grid.state[grid.MX / 2 + (grid.MY / 2) * grid.MX + (grid.MZ / 2) * grid.MX * grid.MY] = 1;
        }
        
        changes.clear();
        first.clear();
        first.add(0);
        
        root.reset();
        current = root;
        
        this.gif = gif;
        counter = 0;
        
        return new Iterable<RunResult>() {
            @Override
            public Iterator<RunResult> iterator() {
                return new Iterator<RunResult>() {
                    private boolean hasNext = true;
                    private boolean firstResult = true;
                    
                    @Override
                    public boolean hasNext() {
                        return hasNext;
                    }
                    
                    @Override
                    public RunResult next() {
                        if (firstResult && gif && current != null && (steps <= 0 || counter < steps)) {
                            firstResult = false;
                            System.out.println("[" + counter + "]");
                            return new RunResult(grid.state.clone(), grid.characters.clone(), grid.MX, grid.MY, grid.MZ);
                        }
                        
                        while (current != null && (steps <= 0 || counter < steps)) {
                            if (gif) {
                                System.out.println("[" + counter + "]");
                                RunResult result = new RunResult(grid.state.clone(), grid.characters.clone(), grid.MX, grid.MY, grid.MZ);
                                current.go();
                                counter++;
                                first.add(changes.size());
                                return result;
                            } else {
                                current.go();
                                counter++;
                                first.add(changes.size());
                            }
                        }
                        
                        hasNext = false;
                        return new RunResult(grid.state.clone(), grid.characters.clone(), grid.MX, grid.MY, grid.MZ);
                    }
                };
            }
        };
    }
    
    public static void writeLine(String s) {
        System.out.println(s);
    }
    
    public static void write(String s) {
        System.out.print(s);
    }
    
    public static class RunResult {
        public final byte[] state;
        public final char[] legend;
        public final int FX, FY, FZ;
        
        public RunResult(byte[] state, char[] legend, int FX, int FY, int FZ) {
            this.state = state;
            this.legend = legend;
            this.FX = FX;
            this.FY = FY;
            this.FZ = FZ;
        }
    }
}