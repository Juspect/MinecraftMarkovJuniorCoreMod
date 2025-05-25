package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.utils.SymmetryHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Element;

import java.util.List;

public abstract class Branch extends Node {
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
