// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
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
