package com.jxon.juscore.mjcore.nodes;

import com.jxon.juscore.mjcore.Interpreter;

public class MarkovNode extends Branch {
    
    public MarkovNode() {}
    
    public MarkovNode(Node child, Interpreter ip) {
        this.nodes = new Node[]{child};
        this.ip = ip;
        this.grid = ip.grid;
    }
    
    @Override
    public boolean go() {
        System.out.println("DEBUG: MarkovNode.go() called, resetting n to 0");
        n = 0;
        boolean result = super.go();
        System.out.println("DEBUG: MarkovNode.go() returning " + result);
        return result;
    }
}
