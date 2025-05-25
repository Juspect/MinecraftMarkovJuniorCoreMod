// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import org.w3c.dom.Element;
import java.util.Arrays;
import java.util.List;

public abstract class Node {
    protected abstract boolean load(Element element, boolean[] symmetry, Grid grid);
    public abstract void reset();
    public abstract boolean go();
    
    protected Interpreter ip;
    public Grid grid;
    
    private static final String[] NODE_NAMES = {
        "one", "all", "prl", "markov", "sequence", "path", "map", 
        "convolution", "convchain", "wfc"
    };
    
    public static Node factory(Element element, boolean[] symmetry, Interpreter ip, Grid grid) {
        String nodeName = element.getTagName();
        if (!Arrays.asList(NODE_NAMES).contains(nodeName)) {
            Interpreter.writeLine("unknown node type \"" + nodeName + "\" at line " + XMLHelper.getLineNumber(element));
            return null;
        }
        
        Node result = null;
        switch (nodeName) {
            case "one":
                result = new OneNode();
                break;
            case "all":
                result = new AllNode();
                break;
            case "prl":
                result = new ParallelNode();
                break;
            case "markov":
                result = new MarkovNode();
                break;
            case "sequence":
                result = new SequenceNode();
                break;
            case "path":
                result = new PathNode();
                break;
            case "map":
                result = new MapNode();
                break;
            case "convolution":
                result = new ConvolutionNode();
                break;
            case "convchain":
                result = new ConvChainNode();
                break;
            case "wfc":
                String sample = XMLHelper.get(element, "sample", (String) null);
                String tileset = XMLHelper.get(element, "tileset", (String) null);
                if (sample != null) {
                    result = new OverlapNode();
                } else if (tileset != null) {
                    result = new TileNode();
                }
                break;
        }
        
        if (result == null) {
            return null;
        }
        
        result.ip = ip;
        result.grid = grid;
        boolean success = result.load(element, symmetry, grid);
        
        if (!success) {
            return null;
        }
        return result;
    }
}

abstract class Branch extends Node {
    public Branch parent;
    public Node[] nodes;
    public int n;
    
    @Override
    protected boolean load(Element element, boolean[] parentSymmetry, Grid grid) {
        String symmetryString = XMLHelper.get(element, "symmetry", (String) null);
        boolean[] symmetry = SymmetryHelper.getSymmetry(ip.grid.MZ == 1, symmetryString, parentSymmetry);
        if (symmetry == null) {
            Interpreter.writeLine("unknown symmetry " + symmetryString + " at line " + XMLHelper.getLineNumber(element));
            return false;
        }
        
        List<Element> xchildren = XMLHelper.elements(element, "one", "all", "prl", "markov", "sequence", "path", "map", "convolution", "convchain", "wfc");
        nodes = new Node[xchildren.size()];
        for (int c = 0; c < xchildren.size(); c++) {
            Node child = Node.factory(xchildren.get(c), symmetry, ip, grid);
            if (child == null) {
                return false;
            }
            if (child instanceof Branch) {
                Branch branch = (Branch) child;
                branch.parent = (branch instanceof MapNode || branch instanceof WFCNode) ? null : this;
            }
            nodes[c] = child;
        }
        return true;
    }
    
    @Override
    public boolean go() {
        for (; n < nodes.length; n++) {
            Node node = nodes[n];
            if (node instanceof Branch) {
                ip.current = (Branch) node;
            }
            if (node.go()) {
                return true;
            }
        }
        ip.current = ip.current.parent;
        reset();
        return false;
    }
    
    @Override
    public void reset() {
        for (Node node : nodes) {
            node.reset();
        }
        n = 0;
    }
}

class SequenceNode extends Branch {
    // Inherits all functionality from Branch
}

class MarkovNode extends Branch {
    
    public MarkovNode() {}
    
    public MarkovNode(Node child, Interpreter ip) {
        this.nodes = new Node[]{child};
        this.ip = ip;
        this.grid = ip.grid;
    }
    
    @Override
    public boolean go() {
        n = 0;
        return super.go();
    }
}