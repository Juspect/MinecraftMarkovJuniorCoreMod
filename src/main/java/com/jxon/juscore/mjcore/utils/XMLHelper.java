// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public final class XMLHelper {
    
    private XMLHelper() {} // Prevent instantiation
    
    public static <T> T get(Element element, String attribute, Class<T> type) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Element " + element.getTagName() + " didn't have attribute " + attribute);
        }
        return convertFromString(value, type);
    }
    
    public static <T> T get(Element element, String attribute, T defaultValue, Class<T> type) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return convertFromString(value, type);
    }
    
    public static String get(Element element, String attribute) {
        return get(element, attribute, String.class);
    }
    
    public static String get(Element element, String attribute, String defaultValue) {
        return get(element, attribute, defaultValue, String.class);
    }
    
    public static int get(Element element, String attribute, int defaultValue) {
        return get(element, attribute, defaultValue, Integer.class);
    }
    
    public static double get(Element element, String attribute, double defaultValue) {
        return get(element, attribute, defaultValue, Double.class);
    }
    
    public static boolean get(Element element, String attribute, boolean defaultValue) {
        return get(element, attribute, defaultValue, Boolean.class);
    }
    
    public static char get(Element element, String attribute, char defaultValue) {
        return get(element, attribute, defaultValue, Character.class);
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T convertFromString(String value, Class<T> type) {
        if (type == String.class) {
            return (T) value;
        } else if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (type == Double.class) {
            return (T) Double.valueOf(value);
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (type == Character.class) {
            return (T) Character.valueOf(value.charAt(0));
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
    
    public static int getLineNumber(Element element) {
        // In Java DOM, line numbers are not directly available
        // This would require a SAX parser or custom implementation
        return -1; // Return -1 to indicate unavailable
    }
    
    public static List<Element> elements(Element element, String... names) {
        List<Element> result = new ArrayList<>();
        NodeList children = element.getChildNodes();
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String tagName = childElement.getTagName();
                if (Arrays.asList(names).contains(tagName)) {
                    result.add(childElement);
                }
            }
        }
        return result;
    }
    
    public static List<Element> myDescendants(Element element, String... tags) {
        List<Element> result = new ArrayList<>();
        Queue<Element> queue = new LinkedList<>();
        queue.offer(element);
        
        while (!queue.isEmpty()) {
            Element e = queue.poll();
            if (e != element) {
                result.add(e);
            }
            
            NodeList children = e.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) child;
                    String tagName = childElement.getTagName();
                    if (Arrays.asList(tags).contains(tagName)) {
                        queue.offer(childElement);
                    }
                }
            }
        }
        return result;
    }
    
    public static List<Element> getChildElements(Element element) {
        List<Element> result = new ArrayList<>();
        NodeList children = element.getChildNodes();
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) child);
            }
        }
        return result;
    }
    
    public static List<Element> getElementsByTagName(Element element, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = element.getElementsByTagName(tagName);
        
        for (int i = 0; i < nodes.getLength(); i++) {
            result.add((Element) nodes.item(i));
        }
        return result;
    }
}