// src/main/java/com/jxon/juscore/resources/ResourceManager.java
package com.jxon.juscore.resources;

import com.jxon.juscore.Juscore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("markov_junior");
    private static final Path MODELS_DIR = CONFIG_DIR.resolve("models");
    private static final Path TEMPLATES_DIR = MODELS_DIR.resolve("templates");
    private static final Path USER_DIR = MODELS_DIR.resolve("user");
    private static final Path BLOCK_MAPPINGS_DIR = CONFIG_DIR.resolve("block_mappings");
    private static final Path RESOURCES_DIR = CONFIG_DIR.resolve("resources");

    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        try {
            createDirectories();
            extractSystemResources();
            initialized = true;
            Juscore.LOGGER.info("Resource manager initialized successfully");
        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to initialize resource manager", e);
        }
    }

    private static void createDirectories() throws IOException {
        Files.createDirectories(TEMPLATES_DIR);
        Files.createDirectories(USER_DIR);
        Files.createDirectories(BLOCK_MAPPINGS_DIR);
        Files.createDirectories(RESOURCES_DIR);
    }

    private static void extractSystemResources() {
        try {
            // 从JAR中提取系统模板和资源
            extractFromJar("templates/", TEMPLATES_DIR);
            extractFromJar("resources/", RESOURCES_DIR);
            extractDefaultBlockMappings();
        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to extract system resources", e);
        }
    }

    private static void extractFromJar(String resourcePath, Path targetDir) throws IOException {
        // 获取当前mod的JAR文件
        Path modJar = getModJarPath();
        if (modJar == null) {
            Juscore.LOGGER.warn("Could not locate mod JAR file");
            return;
        }

        try (JarFile jarFile = new JarFile(modJar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith("assets/juscore/" + resourcePath) && !entry.isDirectory()) {
                    String relativePath = entryName.substring(("assets/juscore/" + resourcePath).length());
                    Path targetFile = targetDir.resolve(relativePath);

                    // 只有文件不存在时才提取（保护用户修改）
                    if (!Files.exists(targetFile)) {
                        Files.createDirectories(targetFile.getParent());

                        try (InputStream is = jarFile.getInputStream(entry);
                             OutputStream os = Files.newOutputStream(targetFile)) {
                            is.transferTo(os);
                        }
                    }
                }
            }
        }
    }

    private static Path getModJarPath() {
        // 尝试从类加载器获取JAR路径
        try {
            String classPath = ResourceManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            Path path = Paths.get(classPath);
            if (Files.exists(path) && path.toString().endsWith(".jar")) {
                return path;
            }
        } catch (Exception e) {
            Juscore.LOGGER.debug("Could not get JAR path from protection domain", e);
        }

        return null;
    }

    private static void extractDefaultBlockMappings() throws IOException {
        Path defaultMapping = BLOCK_MAPPINGS_DIR.resolve("default.json");
        if (!Files.exists(defaultMapping)) {
            String defaultMappingContent = createDefaultMappingJson();
            Files.writeString(defaultMapping, defaultMappingContent);
        }
    }

    private static String createDefaultMappingJson() {
        return """
            {
                "name": "Default Block Mapping",
                "description": "Default character to block mappings",
                "mappings": {
                    "B": "minecraft:stone",
                    "W": "minecraft:oak_log",
                    "R": "minecraft:bricks",
                    "G": "minecraft:grass_block",
                    "Y": "minecraft:sand",
                    "U": "minecraft:water",
                    "A": "minecraft:air",
                    "*": "minecraft:air"
                }
            }
            """;
    }

    public static List<String> getSystemTemplates() {
        List<String> templates = new ArrayList<>();

        try {
            if (Files.exists(TEMPLATES_DIR)) {
                Files.walk(TEMPLATES_DIR)
                        .filter(path -> path.toString().endsWith(".xml"))
                        .map(path -> TEMPLATES_DIR.relativize(path).toString())
                        .forEach(templates::add);
            }
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to list system templates", e);
        }

        // 如果没有模板，添加一些默认项
        if (templates.isEmpty()) {
            templates.add("basic/tower.xml");
            templates.add("basic/house.xml");
            templates.add("basic/maze.xml");
        }

        return templates;
    }

    public static List<String> getUserFiles() {
        List<String> files = new ArrayList<>();

        try {
            if (Files.exists(USER_DIR)) {
                Files.walk(USER_DIR)
                        .filter(path -> path.toString().endsWith(".xml"))
                        .map(path -> USER_DIR.relativize(path).toString())
                        .forEach(files::add);
            }
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to list user files", e);
        }

        return files;
    }

    public static String loadFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        Path file = USER_DIR.resolve(filePath);
        return loadFile(file);
    }

    // 修改loadTemplate方法以支持默认模板
    public static String loadTemplate(String templatePath) {
        // 首先尝试从默认模板获取
        String defaultTemplate = DefaultTemplates.getTemplate(templatePath);
        if (defaultTemplate != null) {
            return defaultTemplate;
        }

        // 然后尝试从文件系统加载
        Path templateFile = TEMPLATES_DIR.resolve(templatePath);
        return loadFile(templateFile);
    }

    private static String loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                return Files.readString(file);
            }
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to load file: " + file, e);
        }
        return null;
    }

    public static boolean saveUserFile(String filename, String content) {
        try {
            Path file = USER_DIR.resolve(filename);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
            return true;
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to save user file: " + filename, e);
            return false;
        }
    }

    public static List<String> getBlockMappingTemplates() {
        List<String> templates = new ArrayList<>();

        try {
            if (Files.exists(BLOCK_MAPPINGS_DIR)) {
                Files.walk(BLOCK_MAPPINGS_DIR)
                        .filter(path -> path.toString().endsWith(".json"))
                        .map(path -> BLOCK_MAPPINGS_DIR.relativize(path).toString())
                        .forEach(templates::add);
            }
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to list block mapping templates", e);
        }

        return templates;
    }

    public static Map<Character, String> loadBlockMapping(String templateName) {
        Path templateFile = BLOCK_MAPPINGS_DIR.resolve(templateName);
        Map<Character, String> mapping = new HashMap<>();

        try {
            if (Files.exists(templateFile)) {
                String content = Files.readString(templateFile);
                // 解析JSON格式的映射文件
                parseBlockMappingJson(content, mapping);
            }
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to load block mapping: " + templateName, e);
        }

        return mapping;
    }

    private static void parseBlockMappingJson(String content, Map<Character, String> mapping) {
        // 简单的JSON解析（生产环境建议使用专业的JSON库）
        try {
            String[] lines = content.split("\n");
            boolean inMappings = false;

            for (String line : lines) {
                line = line.trim();
                if (line.contains("\"mappings\"")) {
                    inMappings = true;
                    continue;
                }

                if (inMappings && line.contains("\":")) {
                    String[] parts = line.split("\":");
                    if (parts.length >= 2) {
                        String key = parts[0].replace("\"", "").trim();
                        String value = parts[1].replace("\"", "").replace(",", "").trim();

                        if (key.length() == 1) {
                            mapping.put(key.charAt(0), value);
                        }
                    }
                }

                if (inMappings && line.contains("}")) {
                    break;
                }
            }
        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to parse block mapping JSON", e);
        }
    }

    public static boolean saveBlockMapping(String templateName, Map<Character, String> mapping) {
        try {
            Path templateFile = BLOCK_MAPPINGS_DIR.resolve(templateName);
            String json = createBlockMappingJson(templateName, mapping);
            Files.writeString(templateFile, json);
            return true;
        } catch (IOException e) {
            Juscore.LOGGER.error("Failed to save block mapping: " + templateName, e);
            return false;
        }
    }

    private static String createBlockMappingJson(String name, Map<Character, String> mapping) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"").append(name).append("\",\n");
        json.append("  \"description\": \"Custom block mapping\",\n");
        json.append("  \"mappings\": {\n");

        List<Map.Entry<Character, String>> entries = new ArrayList<>(mapping.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Character, String> entry = entries.get(i);
            json.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            if (i < entries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  }\n");
        json.append("}\n");

        return json.toString();
    }

    public static void resetSystemFiles() {
        try {
            // 删除系统模板和资源目录
            if (Files.exists(TEMPLATES_DIR)) {
                Files.walk(TEMPLATES_DIR)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                Juscore.LOGGER.error("Failed to delete: " + path, e);
                            }
                        });
            }

            if (Files.exists(RESOURCES_DIR)) {
                Files.walk(RESOURCES_DIR)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                Juscore.LOGGER.error("Failed to delete: " + path, e);
                            }
                        });
            }

            // 重新提取
            extractSystemResources();
            Juscore.LOGGER.info("System files reset successfully");

        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to reset system files", e);
        }
    }

    public static Path getConfigDir() {
        return CONFIG_DIR;
    }

    public static Path getModelsDir() {
        return MODELS_DIR;
    }

    public static Path getUserDir() {
        return USER_DIR;
    }
}