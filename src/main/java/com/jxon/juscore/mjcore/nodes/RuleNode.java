// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.*;
import com.jxon.juscore.mjcore.models.Field;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.models.Observation;
import com.jxon.juscore.mjcore.models.Rule;
import com.jxon.juscore.mjcore.utils.AH;
import com.jxon.juscore.mjcore.utils.Search;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class RuleNode extends Node {
    public Rule[] rules;
    public int counter, steps;
    
    protected List<RuleMatch> matches;
    protected int matchCount, lastMatchedTurn;
    protected boolean[][] matchMask;
    
    protected int[][] potentials;
    public Field[] fields;
    protected Observation[] observations;
    protected double temperature;
    
    protected boolean search, futureComputed;
    protected int[] future;
    protected byte[][] trajectory;
    
    private int limit;
    private double depthCoefficient;
    
    public boolean[] last;
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        String symmetryString = XMLHelper.get(element, "symmetry", (String) null);
        boolean[] symmetry = SymmetryHelper.getSymmetry(grid.MZ == 1, symmetryString, parentSymmetry);
        if (symmetry == null) {
            Interpreter.writeLine("unknown symmetry " + symmetryString + " at line " + XMLHelper.getLineNumber(element));
            return false;
        }

        List<Rule> ruleList = new ArrayList<>();
        List<Element> ruleElements = XMLHelper.getDirectChildElements(element, "rule");
        if (ruleElements.isEmpty()) {
            ruleElements = List.of(element);
        }
        
        for (Element ruleElement : ruleElements) {
            Rule rule = Rule.load(ruleElement, grid, grid);
            if (rule == null) {
                return false;
            }
            rule.original = true;
            
            String ruleSymmetryString = XMLHelper.get(ruleElement, "symmetry", (String) null);
            boolean[] ruleSymmetry = SymmetryHelper.getSymmetry(grid.MZ == 1, ruleSymmetryString, symmetry);
            if (ruleSymmetry == null) {
                Interpreter.writeLine("unknown symmetry " + ruleSymmetryString + " at line " + XMLHelper.getLineNumber(ruleElement));
                return false;
            }
            
            for (Rule r : rule.symmetries(ruleSymmetry, grid.MZ == 1)) {
                ruleList.add(r);
            }
        }
        
        rules = ruleList.toArray(new Rule[0]);
        last = new boolean[rules.length];
        
        steps = XMLHelper.get(element, "steps", 0);
        
        temperature = XMLHelper.get(element, "temperature", 0.0);
        List<Element> fieldElements = XMLHelper.getElementsByTagName(element, "field");
        if (!fieldElements.isEmpty()) {
            fields = new Field[grid.C];
            for (Element fieldElement : fieldElements) {
                char c = XMLHelper.get(fieldElement, "for", Character.class);
                Byte value = grid.values.get(c);
                if (value != null) {
                    fields[value] = new Field(fieldElement, grid);
                } else {
                    Interpreter.writeLine("unknown field value " + c + " at line " + XMLHelper.getLineNumber(fieldElement));
                    return false;
                }
            }
            potentials = AH.array2D(grid.C, grid.state.length, 0);
        }
        
        List<Element> observeElements = XMLHelper.getElementsByTagName(element, "observe");
        if (!observeElements.isEmpty()) {
            observations = new Observation[grid.C];
            for (Element observeElement : observeElements) {
                byte value = grid.values.get(XMLHelper.get(observeElement, "value", Character.class));
                String from = XMLHelper.get(observeElement, "from", String.valueOf(grid.characters[value]));
                String to = XMLHelper.get(observeElement, "to");
                observations[value] = new Observation(from.charAt(0), to, grid);
            }
            
            search = XMLHelper.get(element, "search", false);
            if (search) {
                limit = XMLHelper.get(element, "limit", -1);
                depthCoefficient = XMLHelper.get(element, "depthCoefficient", 0.5);
            } else {
                potentials = AH.array2D(grid.C, grid.state.length, 0);
            }
            future = new int[grid.state.length];
        }
        
        return true;
    }
    
    @Override
    public void reset() {
        lastMatchedTurn = -1;
        counter = 0;
        futureComputed = false;

        Arrays.fill(last, false);
    }
    
    protected void add(int r, int x, int y, int z, boolean[] maskr) {
        maskr[x + y * grid.MX + z * grid.MX * grid.MY] = true;
        
        RuleMatch match = new RuleMatch(r, x, y, z);
        if (matchCount < matches.size()) {
            matches.set(matchCount, match);
        } else {
            matches.add(match);
        }
        matchCount++;
    }
    
    @Override
    public boolean go() {
        Arrays.fill(last, false);
        
        if (steps > 0 && counter >= steps) {
            return false;
        }
        
        int MX = grid.MX, MY = grid.MY, MZ = grid.MZ;
        if (observations != null && !futureComputed) {
            if (!Observation.computeFutureSetPresent(future, grid.state, observations)) {
                return false;
            } else {
                futureComputed = true;
                if (search) {
                    trajectory = null;
                    int TRIES = limit < 0 ? 1 : 20;
                    for (int k = 0; k < TRIES && trajectory == null; k++) {
                        trajectory = Search.run(grid.state, future, rules, grid.MX, grid.MY, grid.MZ,
                                               grid.C, this instanceof AllNode, limit, depthCoefficient, ip.random.nextInt());
                    }
                    if (trajectory == null) {
                        System.out.println("SEARCH RETURNED NULL");
                    }
                } else {
                    Observation.computeBackwardPotentials(potentials, future, MX, MY, MZ, rules);
                }
            }
        }
        
        if (lastMatchedTurn >= 0) {
            for (int n = ip.first.get(lastMatchedTurn); n < ip.changes.size(); n++) {
                Rule.Tuple3 change = ip.changes.get(n);
                int x = change.x(), y = change.y(), z = change.z();
                byte value = grid.state[x + y * MX + z * MX * MY];
                
                for (int r = 0; r < rules.length; r++) {
                    Rule rule = rules[r];
                    boolean[] maskr = matchMask[r];
                    Rule.Tuple3[] shifts = rule.ishifts[value];
                    
                    for (Rule.Tuple3 shift : shifts) {
                        int sx = x - shift.x();
                        int sy = y - shift.y();
                        int sz = z - shift.z();
                        
                        if (sx < 0 || sy < 0 || sz < 0 || 
                            sx + rule.IMX > MX || sy + rule.IMY > MY || sz + rule.IMZ > MZ) {
                            continue;
                        }
                        
                        int si = sx + sy * MX + sz * MX * MY;
                        if (!maskr[si] && grid.matches(rule, sx, sy, sz)) {
                            add(r, sx, sy, sz, maskr);
                        }
                    }
                }
            }
        } else {
            matchCount = 0;
            for (int r = 0; r < rules.length; r++) {
                Rule rule = rules[r];
                boolean[] maskr = matchMask != null ? matchMask[r] : null;
                
                for (int z = rule.IMZ - 1; z < MZ; z += rule.IMZ) {
                    for (int y = rule.IMY - 1; y < MY; y += rule.IMY) {
                        for (int x = rule.IMX - 1; x < MX; x += rule.IMX) {
                            Rule.Tuple3[] shifts = rule.ishifts[grid.state[x + y * MX + z * MX * MY]];
                            for (Rule.Tuple3 shift : shifts) {
                                int sx = x - shift.x();
                                int sy = y - shift.y();
                                int sz = z - shift.z();
                                
                                if (sx < 0 || sy < 0 || sz < 0 || 
                                    sx + rule.IMX > MX || sy + rule.IMY > MY || sz + rule.IMZ > MZ) {
                                    continue;
                                }
                                
                                if (grid.matches(rule, sx, sy, sz)) {
                                    add(r, sx, sy, sz, maskr);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (fields != null) {
            boolean anySuccess = false, anyComputation = false;
            for (int c = 0; c < fields.length; c++) {
                Field field = fields[c];
                if (field != null && (counter == 0 || field.recompute)) {
                    boolean success = field.compute(potentials[c], grid);
                    if (!success && field.essential) {
                        return false;
                    }
                    anySuccess |= success;
                    anyComputation = true;
                }
            }
            return !anyComputation || anySuccess;
        }
        
        return true;
    }

    // Helper class for rule matches
        public record RuleMatch(int r, int x, int y, int z) {
    }
}