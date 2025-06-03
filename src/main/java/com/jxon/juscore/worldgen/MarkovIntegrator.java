// src/main/java/com/jxon/juscore/worldgen/MarkovIntegrator.java
package com.jxon.juscore.worldgen;

import com.jxon.juscore.Juscore;
import com.jxon.juscore.config.MarkovWorldConfig;
import com.jxon.juscore.mjcore.Interpreter;
import com.jxon.juscore.mjcore.models.Grid;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

public class MarkovIntegrator {

    public static class GenerationResult {
        private final byte[][][] modelData;
        private final boolean success;
        private final String errorMessage;

        public GenerationResult(byte[][][] modelData) {
            this.modelData = modelData;
            this.success = true;
            this.errorMessage = null;
        }

        public GenerationResult(String errorMessage) {
            this.modelData = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public byte[][][] getModelData() { return modelData; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static GenerationResult generateModel(MarkovWorldConfig config, int seed) {
        try {
            // 验证配置
            if (!config.isValid()) {
                return new GenerationResult("Invalid configuration");
            }

            // 解析XML
            Interpreter interpreter = createInterpreter(config);
            if (interpreter == null) {
                return new GenerationResult("Failed to parse XML");
            }

            // 运行生成过程
            Interpreter.RunResult result = executeGeneration(interpreter, seed, config.getMaxStep());
            if (result == null) {
                return new GenerationResult("Generation failed");
            }

            // 转换为3D数组
            byte[][][] modelData = convertTo3DArray(result, config);
            return new GenerationResult(modelData);

        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to generate Markov model", e);
            return new GenerationResult("Generation error: " + e.getMessage());
        }
    }

    private static Interpreter createInterpreter(MarkovWorldConfig config) {
        try {
            String xmlContent = config.getXmlContent();
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return null;
            }

            // 解析XML文档
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
            Element rootElement = doc.getDocumentElement();

            // 创建Interpreter
            Interpreter interpreter = Interpreter.load(rootElement,
                    config.getModelLength(),
                    config.getModelWidth(),
                    config.getModelHeight());

            return interpreter;

        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to create interpreter", e);
            return null;
        }
    }

    private static Interpreter.RunResult executeGeneration(Interpreter interpreter, int seed, int maxSteps) {
        try {
            // 运行生成过程
            Iterable<Interpreter.RunResult> results = interpreter.run(seed, maxSteps, false);
            Iterator<Interpreter.RunResult> iterator = results.iterator();

            Interpreter.RunResult lastResult = null;
            while (iterator.hasNext()) {
                lastResult = iterator.next();
            }

            return lastResult;

        } catch (Exception e) {
            Juscore.LOGGER.error("Failed to execute generation", e);
            return null;
        }
    }

    private static byte[][][] convertTo3DArray(Interpreter.RunResult result, MarkovWorldConfig config) {
        byte[] flatData = result.state();
        int MX = config.getModelLength();
        int MY = config.getModelHeight();
        int MZ = config.getModelWidth();

        // 创建3D数组 [x][y][z]
        byte[][][] data3D = new byte[MX][MY][MZ];

        // 转换为字符
        char[] legend = result.legend();

        for (int i = 0; i < flatData.length && i < MX * MY * MZ; i++) {
            int x = i % MX;
            int y = (i / MX) % MY;
            int z = i / (MX * MY);

            if (x < MX && y < MY && z < MZ) {
                byte value = flatData[i];
                if (value >= 0 && value < legend.length) {
                    // 将字符值作为byte存储
                    data3D[x][y][z] = (byte) legend[value];
                } else {
                    data3D[x][y][z] = (byte) '*'; // 默认空气
                }
            }
        }

        return data3D;
    }

    public static boolean validateXml(String xmlContent) {
        try {
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

            // 基本的MarkovJunior格式检查
            return xmlContent.contains("<sequence") ||
                    xmlContent.contains("<markov") ||
                    xmlContent.contains("<one") ||
                    xmlContent.contains("<all") ||
                    xmlContent.contains("<wfc");

        } catch (Exception e) {
            return false;
        }
    }
}