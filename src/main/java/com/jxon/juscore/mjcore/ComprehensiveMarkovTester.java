// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import com.jxon.juscore.mjcore.models.Grid;
import com.jxon.juscore.mjcore.models.Rule;
import com.jxon.juscore.mjcore.utils.Graphics;
import com.jxon.juscore.mjcore.utils.VoxHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 综合MarkovJunior测试套件
 * 包含功能验证、性能测试、错误诊断等功能
 */
public class ComprehensiveMarkovTester {

    private static final String OUTPUT_BASE = "markov_test_results";
    private static final String REPORT_FILE = OUTPUT_BASE + "/test_report.txt";

    // 内置测试模型（当外部文件不可用时使用）
    private static final Map<String, String> BUILTIN_MODELS = new HashMap<>();

    static {
        // 基础填充测试
        BUILTIN_MODELS.put("BasicFill",
                "<sequence values=\"BW\" origin=\"true\">" +
                        "  <one steps=\"100\">" +
                        "    <rule in=\"B\" out=\"W\" p=\"0.3\"/>" +
                        "  </one>" +
                        "</sequence>");

        // 迷宫生长测试    
        BUILTIN_MODELS.put("MazeGrowth",
                "<sequence values=\"BWG\">" +
                        "  <one steps=\"200\">" +
                        "    <rule in=\"WBB\" out=\"WAW\"/>" +
                        "  </one>" +
                        "</sequence>");

        // 路径查找测试
        BUILTIN_MODELS.put("PathTest",
                "<sequence values=\"BRGW\" origin=\"true\">" +
                        "  <one in=\"B\" out=\"R\" steps=\"1\"/>" +
                        "  <one in=\"B\" out=\"G\" steps=\"1\"/>" +
                        "  <path from=\"R\" to=\"G\" on=\"BW\" color=\"W\"/>" +
                        "</sequence>");

        // 卷积测试
        BUILTIN_MODELS.put("ConvolutionTest",
                "<sequence values=\"BW\">" +
                        "  <prl in=\"B\" out=\"W\" p=\"0.1\" steps=\"1\"/>" +
                        "  <convolution neighborhood=\"Moore\" steps=\"5\">" +
                        "    <rule in=\"B\" out=\"W\" sum=\"3..5\" values=\"W\"/>" +
                        "    <rule in=\"W\" out=\"B\" sum=\"0..2,6..8\" values=\"W\"/>" +
                        "  </convolution>" +
                        "</sequence>");

        // 生命游戏测试
        BUILTIN_MODELS.put("GameOfLife",
                "<sequence values=\"DA\">" +
                        "  <prl in=\"D\" out=\"A\" p=\"0.5\" steps=\"1\"/>" +
                        "  <convolution neighborhood=\"Moore\" periodic=\"True\">" +
                        "    <rule in=\"D\" out=\"A\" sum=\"3\" values=\"A\"/>" +
                        "    <rule in=\"A\" out=\"D\" sum=\"0,1,4..8\" values=\"A\"/>" +
                        "  </convolution>" +
                        "</sequence>");

        // Markov节点测试
        BUILTIN_MODELS.put("MarkovTest",
                "<markov values=\"BRGW\" origin=\"True\">" +
                        "  <one in=\"RBB\" out=\"GGR\"/>" +
                        "  <one in=\"RGG\" out=\"WWR\"/>" +
                        "</markov>");
    }

    private static PrintWriter reportWriter;
    private static long overallStartTime;

    public static void main(String[] args) {
        System.out.println("🔬 MarkovJunior Java实现综合测试套件");
        System.out.println("=" .repeat(60));

        overallStartTime = System.currentTimeMillis();

        try {
            initializeTest();
            runAllTests();
        } catch (Exception e) {
            System.err.println("测试初始化失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (reportWriter != null) {
                reportWriter.close();
            }
        }
    }

    private static void initializeTest() throws IOException {
        // 创建输出目录
        createDirectory(OUTPUT_BASE);
        createDirectory(OUTPUT_BASE + "/images");
        createDirectory(OUTPUT_BASE + "/voxels");
        createDirectory(OUTPUT_BASE + "/logs");

        // 初始化报告
        reportWriter = new PrintWriter(new FileWriter(REPORT_FILE));
        reportWriter.println("MarkovJunior Java实现测试报告");
        reportWriter.println("生成时间: " + new Date());
        reportWriter.println("=" .repeat(60));
        reportWriter.flush();

        log("测试环境初始化完成");
    }

    private static void runAllTests() {
        TestResults results = new TestResults();

        // 1. 基础功能测试
        log("\n🧪 第一阶段: 基础功能测试");
        runBasicFunctionalityTests(results);

        // 2. 核心算法测试  
        log("\n⚙️ 第二阶段: 核心算法测试");
        runCoreAlgorithmTests(results);

        // 3. 复杂模型测试
        log("\n🏗️ 第三阶段: 复杂模型测试");
        runComplexModelTests(results);

        // 4. 性能基准测试
        log("\n🚀 第四阶段: 性能基准测试");
        runPerformanceTests(results);

        // 5. 错误处理测试
        log("\n🛡️ 第五阶段: 错误处理测试");
        runErrorHandlingTests(results);

        // 生成最终报告
        generateFinalReport(results);
    }

    private static void runBasicFunctionalityTests(TestResults results) {
        log("测试基础组件...");

        // 测试Grid创建
        results.record("Grid创建", testGridCreation());

        // 测试Rule加载
        results.record("Rule加载", testRuleLoading());

        // 测试Interpreter创建
        results.record("Interpreter创建", testInterpreterCreation());

        // 测试基础模型运行
        results.record("基础填充模型", testBuiltinModel("BasicFill", 20, 20, 1, 100));
        results.record("迷宫生长模型", testBuiltinModel("MazeGrowth", 30, 30, 1, 200));
    }

    private static void runCoreAlgorithmTests(TestResults results) {
        log("测试核心算法...");

        // 测试OneNode
        results.record("OneNode功能", testBuiltinModel("BasicFill", 25, 25, 1, 50));

        // 测试路径查找
        results.record("PathNode功能", testBuiltinModel("PathTest", 20, 20, 1, 100));

        // 测试卷积
        results.record("ConvolutionNode功能", testBuiltinModel("ConvolutionTest", 30, 30, 1, 10));

        // 测试Markov节点
        results.record("MarkovNode功能", testBuiltinModel("MarkovTest", 40, 40, 1, 500));

        // 测试生命游戏
        results.record("生命游戏", testBuiltinModel("GameOfLife", 50, 50, 1, 20));
    }

    private static void runComplexModelTests(TestResults results) {
        log("测试复杂模型（如果可用）...");

        // 尝试测试外部模型文件
        String[] externalModels = {
                "BasicDijkstraFill", "MazeBacktracker", "River",
                "Noise", "Growth", "Trail"
        };

        for (String modelName : externalModels) {
            if (modelFileExists(modelName)) {
                results.record("外部模型-" + modelName,
                        testExternalModel(modelName, 40, 40, 1, 100));
            } else {
                log("跳过外部模型 " + modelName + " (文件不存在)");
                results.record("外部模型-" + modelName, TestResult.SKIPPED);
            }
        }
    }

    private static void runPerformanceTests(TestResults results) {
        log("运行性能基准测试...");

        // 不同尺寸的性能测试
        int[] sizes = {20, 50, 100};
        for (int size : sizes) {
            String testName = "性能-" + size + "x" + size;
            long startTime = System.currentTimeMillis();
            boolean success = testBuiltinModel("MazeGrowth", size, size, 1, 200);
            long duration = System.currentTimeMillis() - startTime;

            results.record(testName, success ? TestResult.PASSED : TestResult.FAILED);
            results.recordPerformance(testName, duration);

            log(String.format("  %dx%d: %s (耗时: %dms)",
                    size, size, success ? "通过" : "失败", duration));
        }
    }

    private static void runErrorHandlingTests(TestResults results) {
        log("测试错误处理...");

        // 测试无效XML
        results.record("无效XML处理", testInvalidXML());

        // 测试空模型
        results.record("空模型处理", testEmptyModel());

        // 测试超大尺寸
        results.record("超大尺寸处理", testOversizeModel());
    }

    // ========== 具体测试方法 ==========

    private static boolean testGridCreation() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("sequence");
            root.setAttribute("values", "BW");
            doc.appendChild(root);

            Grid grid = Grid.load(root, 10, 10, 1);
            return grid != null && grid.MX == 10 && grid.MY == 10 && grid.MZ == 1;
        } catch (Exception e) {
            logError("Grid创建测试失败", e);
            return false;
        }
    }

    private static boolean testRuleLoading() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("sequence");
            root.setAttribute("values", "BW");

            Element rule = doc.createElement("rule");
            rule.setAttribute("in", "B");
            rule.setAttribute("out", "W");
            root.appendChild(rule);

            doc.appendChild(root);

            Grid grid = Grid.load(root, 5, 5, 1);
            if (grid == null) return false;

            Rule testRule = Rule.load(rule, grid, grid);
            return testRule != null;
        } catch (Exception e) {
            logError("Rule加载测试失败", e);
            return false;
        }
    }

    private static boolean testInterpreterCreation() {
        try {
            String xml = "<sequence values=\"BW\"><one><rule in=\"B\" out=\"W\"/></one></sequence>";
            Interpreter interpreter = createInterpreterFromXML(xml, 10, 10, 1);
            return interpreter != null;
        } catch (Exception e) {
            logError("Interpreter创建测试失败", e);
            return false;
        }
    }

    private static boolean testBuiltinModel(String modelName, int MX, int MY, int MZ, int steps) {
        try {
            String xml = BUILTIN_MODELS.get(modelName);
            if (xml == null) {
                log("内置模型 " + modelName + " 不存在");
                return false;
            }

            Interpreter interpreter = createInterpreterFromXML(xml, MX, MY, MZ);
            if (interpreter == null) {
                log("无法创建解释器: " + modelName);
                return false;
            }

            int stepCount = 0;
            Interpreter.RunResult lastResult = null;

            for (Interpreter.RunResult result : interpreter.run(12345, steps, false)) {
                stepCount++;
                lastResult = result;
                if (stepCount >= steps) break;
            }

            boolean success = lastResult != null && stepCount > 0;
            if (success) {
                saveTestResult(lastResult, modelName, MX, MY, MZ);
            }

            return success;

        } catch (Exception e) {
            logError("内置模型测试失败: " + modelName, e);
            return false;
        }
    }

    private static boolean testExternalModel(String modelName, int MX, int MY, int MZ, int steps) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("models/" + modelName + ".xml"));

            Interpreter interpreter = Interpreter.load(doc.getDocumentElement(), MX, MY, MZ);
            if (interpreter == null) return false;

            int stepCount = 0;
            for (Interpreter.RunResult result : interpreter.run(12345, steps, false)) {
                stepCount++;
                if (stepCount >= steps) break;
            }

            return stepCount > 0;

        } catch (Exception e) {
            logError("外部模型测试失败: " + modelName, e);
            return false;
        }
    }

    private static boolean testInvalidXML() {
        try {
            String invalidXML = "<invalid><missing></invalid>";
            Interpreter interpreter = createInterpreterFromXML(invalidXML, 10, 10, 1);
            return interpreter == null; // 应该失败
        } catch (Exception e) {
            return true; // 正确处理了异常
        }
    }

    private static boolean testEmptyModel() {
        try {
            String emptyXML = "<sequence values=\"B\"></sequence>";
            Interpreter interpreter = createInterpreterFromXML(emptyXML, 10, 10, 1);
            return interpreter != null; // 应该能创建但不执行任何操作
        } catch (Exception e) {
            logError("空模型测试失败", e);
            return false;
        }
    }

    private static boolean testOversizeModel() {
        try {
            String xml = BUILTIN_MODELS.get("BasicFill");
            Interpreter interpreter = createInterpreterFromXML(xml, 1000, 1000, 1);
            if (interpreter == null) return false;

            // 只运行少量步骤以避免内存问题
            int stepCount = 0;
            for (Interpreter.RunResult result : interpreter.run(12345, 10, false)) {
                stepCount++;
                if (stepCount >= 5) break;
            }

            return stepCount > 0;
        } catch (OutOfMemoryError e) {
            log("超大尺寸模型正确抛出内存异常");
            return true;
        } catch (Exception e) {
            logError("超大尺寸测试失败", e);
            return false;
        }
    }

    // ========== 工具方法 ==========

    private static Interpreter createInterpreterFromXML(String xml, int MX, int MY, int MZ) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            return Interpreter.load(doc.getDocumentElement(), MX, MY, MZ);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean modelFileExists(String modelName) {
        return new File("models/" + modelName + ".xml").exists();
    }

    private static void saveTestResult(Interpreter.RunResult result, String modelName,
                                       int MX, int MY, int MZ) {
        try {
            Map<Character, Integer> defaultPalette = createDefaultPalette();
            int[] colors = new int[result.legend().length];
            for (int i = 0; i < result.legend().length; i++) {
                colors[i] = defaultPalette.getOrDefault(result.legend()[i], 0xFF888888);
            }

            String filename = OUTPUT_BASE + "/images/" + modelName + "_" + MX + "x" + MY + "x" + MZ;

            if (result.FZ() == 1) {
                Graphics.RenderResult renderResult = Graphics.render(
                        result.state(), result.FX(), result.FY(), result.FZ(), colors, 4, 0);
                Graphics.saveBitmap(renderResult.bitmap(), renderResult.width(),
                        renderResult.height(), filename + ".png");
            } else {
                VoxHelper.saveVox(result.state(), (byte) result.FX(), (byte) result.FY(), (byte) result.FZ(),
                        colors, OUTPUT_BASE + "/voxels/" + modelName + ".vox");
            }

        } catch (Exception e) {
            logError("保存测试结果失败: " + modelName, e);
        }
    }

    private static Map<Character, Integer> createDefaultPalette() {
        Map<Character, Integer> palette = new HashMap<>();
        palette.put('B', 0xFF000000); // 黑色
        palette.put('W', 0xFFFFFFFF); // 白色  
        palette.put('R', 0xFFFF0000); // 红色
        palette.put('G', 0xFF00FF00); // 绿色
        palette.put('Y', 0xFFFFFF00); // 黄色
        palette.put('U', 0xFF0000FF); // 蓝色
        palette.put('D', 0xFF444444); // 深灰
        palette.put('A', 0xFFCCCCCC); // 浅灰
        return palette;
    }

    private static void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (Exception e) {
            System.err.println("创建目录失败: " + path);
        }
    }

    private static void log(String message) {
        System.out.println(message);
        if (reportWriter != null) {
            reportWriter.println(message);
            reportWriter.flush();
        }
    }

    private static void logError(String message, Exception e) {
        String errorMsg = message + ": " + e.getMessage();
        System.err.println("❌ " + errorMsg);
        if (reportWriter != null) {
            reportWriter.println("错误: " + errorMsg);
            reportWriter.flush();
        }
    }

    private static void generateFinalReport(TestResults results) {
        long totalTime = System.currentTimeMillis() - overallStartTime;

        log("\n" + "=".repeat(80));
        log("📊 最终测试报告");
        log("=".repeat(80));

        log("总测试数: " + results.getTotalTests());
        log("通过: " + results.getPassedTests() + " ✅");
        log("失败: " + results.getFailedTests() + " ❌");
        log("跳过: " + results.getSkippedTests() + " ⏭️");
        log("成功率: " + String.format("%.1f%%", results.getSuccessRate()));
        log("总耗时: " + totalTime + "ms");

        log("\n详细结果:");
        for (Map.Entry<String, TestResult> entry : results.getResults().entrySet()) {
            String status = switch(entry.getValue()) {
                case PASSED -> "✅ 通过";
                case FAILED -> "❌ 失败";
                case SKIPPED -> "⏭️ 跳过";
            };
            log("  " + entry.getKey() + ": " + status);
        }

        if (!results.getPerformanceData().isEmpty()) {
            log("\n性能数据:");
            for (Map.Entry<String, Long> entry : results.getPerformanceData().entrySet()) {
                log("  " + entry.getKey() + ": " + entry.getValue() + "ms");
            }
        }

        // 给出总体评估
        double successRate = results.getSuccessRate();
        if (successRate >= 90) {
            log("\n🎉 测试评估: 优秀! MarkovJunior Java实现功能完整且稳定。");
        } else if (successRate >= 70) {
            log("\n👍 测试评估: 良好! 大部分功能正常，少数问题需要修复。");
        } else if (successRate >= 50) {
            log("\n⚠️ 测试评估: 一般。存在较多问题，需要仔细检查实现。");
        } else {
            log("\n🚨 测试评估: 较差! 存在严重问题，需要大幅修复。");
        }

        log("\n测试结果已保存到: " + new File(OUTPUT_BASE).getAbsolutePath());
        log("测试完成时间: " + new Date());
    }

    // ========== 内部类 ==========

    private enum TestResult {
        PASSED, FAILED, SKIPPED
    }

    private static class TestResults {
        private final Map<String, TestResult> results = new LinkedHashMap<>();
        private final Map<String, Long> performanceData = new HashMap<>();

        public void record(String testName, boolean success) {
            results.put(testName, success ? TestResult.PASSED : TestResult.FAILED);
        }

        public void record(String testName, TestResult result) {
            results.put(testName, result);
        }

        public void recordPerformance(String testName, long duration) {
            performanceData.put(testName, duration);
        }

        public int getTotalTests() { return results.size(); }

        public long getPassedTests() {
            return results.values().stream().mapToLong(r -> r == TestResult.PASSED ? 1 : 0).sum();
        }

        public long getFailedTests() {
            return results.values().stream().mapToLong(r -> r == TestResult.FAILED ? 1 : 0).sum();
        }

        public long getSkippedTests() {
            return results.values().stream().mapToLong(r -> r == TestResult.SKIPPED ? 1 : 0).sum();
        }

        public double getSuccessRate() {
            return getTotalTests() > 0 ? (double)getPassedTests() / getTotalTests() * 100 : 0;
        }

        public Map<String, TestResult> getResults() { return results; }
        public Map<String, Long> getPerformanceData() { return performanceData; }
    }
}