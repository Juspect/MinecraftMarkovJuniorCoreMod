// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

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
        try {
            if (type == String.class) {
                return (T) value;
            } else if (type == Integer.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Double.class) {
                return (T) Double.valueOf(value);
            } else if (type == Boolean.class) {
                // 确保布尔值解析与C#一致（大小写不敏感）
                return (T) Boolean.valueOf(value.toLowerCase());
            } else if (type == Character.class) {
                if (value.length() == 0) {
                    throw new IllegalArgumentException("Cannot convert empty string to Character");
                }
                return (T) Character.valueOf(value.charAt(0));
            }
            throw new IllegalArgumentException("Unsupported type: " + type);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type.getSimpleName(), e);
        }
    }
    
    public static int getLineNumber(Element element) {
        // In Java DOM, line numbers are not directly available
        // This would require a SAX parser or custom implementation
        return -1; // Return -1 to indicate unavailable
    }

    public static List<Element> elements(Element element, String... names) {
        List<Element> result = new ArrayList<>();
        NodeList children = element.getChildNodes();

        Set<String> nameSet = new HashSet<>(Arrays.asList(names));

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String tagName = childElement.getTagName();
                if (nameSet.contains(tagName)) {
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

        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));

        while (!queue.isEmpty()) {
            Element e = queue.poll();

            // 关键：不返回根元素本身，与C#版本行为一致
            if (e != element) {
                result.add(e);
            }

            // 遍历子元素，只添加匹配标签的元素到队列
            NodeList children = e.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) child;
                    String tagName = childElement.getTagName();
                    if (tagSet.contains(tagName)) {
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
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) node);
            }
        }
        return result;
    }
}