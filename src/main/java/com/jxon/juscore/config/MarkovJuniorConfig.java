package com.jxon.juscore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jxon.juscore.Juscore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * MarkovJunior 配置管理类
 */
public class MarkovJuniorConfig {

    // 配置文件路径
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("markovjunior");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static final Path TEMPLATES_DIR = CONFIG_DIR.resolve("templates");

    // 默认配置
    public int maxSize = 256;
    public int maxSteps = 10000;
    public boolean enableAsync = true;
    public int asyncTimeout = 30; // 秒
    public boolean enableDebug = false;
    public boolean saveResults = false;
    public String resultsDir = "markovjunior_results";

    // 预定义模板
    public Map<String, String> templates = new HashMap<>();

    // Gson实例
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * 创建默认配置
     */
    public MarkovJuniorConfig() {
        initializeDefaultTemplates();
    }

    /**
     * 加载配置文件
     */
    public static MarkovJuniorConfig load() {
        try {
            // 确保配置目录存在
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            if (!Files.exists(TEMPLATES_DIR)) {
                Files.createDirectories(TEMPLATES_DIR);
            }

            MarkovJuniorConfig config;

            if (Files.exists(CONFIG_FILE)) {
                // 读取现有配置
                String json = Files.readString(CONFIG_FILE);
                config = GSON.fromJson(json, MarkovJuniorConfig.class);
                if (config == null) {
                    config = new MarkovJuniorConfig();
                }
            } else {
                // 创建默认配置
                config = new MarkovJuniorConfig();
                config.save();
            }

            // 加载模板文件
            config.loadTemplates();

            return config;
        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to load config, using defaults", e);
            return new MarkovJuniorConfig();
        }
    }

    /**
     * 保存配置文件
     */
    public void save() {
        try {
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * 加载模板文件
     */
    private void loadTemplates() {
        try {
            if (!Files.exists(TEMPLATES_DIR)) {
                createDefaultTemplateFiles();
                return;
            }

            Files.list(TEMPLATES_DIR)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .forEach(path -> {
                        try {
                            String name = path.getFileName().toString().replace(".xml", "");
                            String content = Files.readString(path);
                            templates.put(name, content);
                        } catch (IOException e) {
                            Juscore.LOGGER.warn("Failed to load template: " + path, e);
                        }
                    });

        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to load templates", e);
        }
    }

    /**
     * 创建默认模板文件
     */
    private void createDefaultTemplateFiles() {
        try {
            // 洞穴生成模板
            Path caveTemplate = TEMPLATES_DIR.resolve("cave.xml");
            Files.writeString(caveTemplate, """
                <?xml version="1.0" encoding="UTF-8"?>
                <sequence values="SR" origin="false">
                    <one steps="200">
                        <rule in="S" out="R" p="0.3"/>
                    </one>
                    <all>
                        <rule in="SSS/SRS/SSS" out="***/*R*/***"/>
                    </all>
                </sequence>
                """);

            // 地牢生成模板
            Path dungeonTemplate = TEMPLATES_DIR.resolve("dungeon.xml");
            Files.writeString(dungeonTemplate, """
                <?xml version="1.0" encoding="UTF-8"?>
                <markov values="WFDC">
                    <sequence>
                        <one steps="100">
                            <rule in="W" out="F"/>
                        </one>
                        <one steps="50">
                            <rule in="FF" out="FD" p="0.1"/>
                            <rule in="FFF/FFF/FFF" out="***/*C*/***" p="0.05"/>
                        </one>
                    </sequence>
                </markov>
                """);

            // 迷宫生成模板
            Path mazeTemplate = TEMPLATES_DIR.resolve("maze.xml");
            Files.writeString(mazeTemplate, """
                <?xml version="1.0" encoding="UTF-8"?>
                <sequence values="WP" origin="true">
                    <one>
                        <rule in="WPP" out="WWP"/>
                    </one>
                </sequence>
                """);

            // 村庄道路模板
            Path pathTemplate = TEMPLATES_DIR.resolve("village_path.xml");
            Files.writeString(pathTemplate, """
                <?xml version="1.0" encoding="UTF-8"?>
                <sequence values="GPSC">
                    <path from="G" to="G" on="GPS" color="P" inertia="true"/>
                    <one steps="30">
                        <rule in="PP" out="PC" p="0.1"/>
                    </one>
                </sequence>
                """);

            // 房屋生成模板
            Path houseTemplate = TEMPLATES_DIR.resolve("house.xml");
            Files.writeString(houseTemplate, """
                <?xml version="1.0" encoding="UTF-8"?>
                <sequence values="AWFD">
                    <one steps="50">
                        <rule in="A" out="W"/>
                    </one>
                    <all>
                        <rule in="WWW/WAW/WWW" out="***/*F*/**W"/>
                        <rule in="WW/WF" out="**/*D" p="0.2"/>
                    </all>
                </sequence>
                """);

            Juscore.LOGGER.info("Created default template files");

        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to create default templates", e);
        }
    }

    /**
     * 初始化默认模板
     */
    private void initializeDefaultTemplates() {
        templates.put("simple", """
            <one steps="100">
                <rule in="B" out="W"/>
            </one>
            """);

        templates.put("growth", """
            <sequence values="BW">
                <one steps="200">
                    <rule in="WB" out="WW"/>
                </one>
            </sequence>
            """);

        templates.put("maze_simple", """
            <sequence values="WB" origin="true">
                <one>
                    <rule in="WBB" out="WWB"/>
                </one>
            </sequence>
            """);
    }

    /**
     * 获取模板内容
     */
    public String getTemplate(String name) {
        return templates.get(name);
    }

    /**
     * 检查尺寸是否有效
     */
    public boolean isValidSize(int size) {
        return size > 0 && size <= maxSize;
    }

    /**
     * 检查步数是否有效
     */
    public boolean isValidSteps(int steps) {
        return steps > 0 && steps <= maxSteps;
    }
}