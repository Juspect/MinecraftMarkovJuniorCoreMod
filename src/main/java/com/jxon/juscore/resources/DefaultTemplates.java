// src/main/java/com/jxon/juscore/resources/DefaultTemplates.java
package com.jxon.juscore.resources;

public class DefaultTemplates {

    public static final String BASIC_TOWER = """
        <sequence values="BW*" origin="True">
            <one>
                <rule in="B" out="W"/>
            </one>
            <all>
                <rule in="W" out="B" p="0.3"/>
                <rule in="*" out="W" p="0.1"/>
            </all>
            <one steps="10">
                <rule in="BBB/BWB/BBB" out="***/B*B/***"/>
                <rule in="BBB/B*B/BBB" out="***/BWB/***"/>
            </one>
        </sequence>
        """;

    public static final String BASIC_HOUSE = """
        <sequence values="BW*" origin="True">
            <one>
                <rule in="*" out="B"/>
            </one>
            <all steps="5">
                <rule in="BBB/B*B/BBB" out="***/BWB/***"/>
                <rule in="***/*B*/***" out="BWB/W*W/BWB"/>
            </all>
        </sequence>
        """;

    public static final String BASIC_MAZE = """
        <sequence values="BW*">
            <prl in="*" out="B"/>
            <prl in="B" out="*" p="0.5"/>
            <all>
                <rule in="BB*/B**/B**" out="***/***/**B"/>
                <rule in="*BB/**B/**B" out="***/***/**B"/>
            </all>
        </sequence>
        """;

    public static String getTemplate(String name) {
        return switch (name) {
            case "basic/tower.xml" -> BASIC_TOWER;
            case "basic/house.xml" -> BASIC_HOUSE;
            case "basic/maze.xml" -> BASIC_MAZE;
            default -> BASIC_TOWER;
        };
    }
}