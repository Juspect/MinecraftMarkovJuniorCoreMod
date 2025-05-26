// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import com.jxon.juscore.mjcore.utils.Graphics;
import com.jxon.juscore.mjcore.utils.VoxHelper;
import com.jxon.juscore.mjcore.utils.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * MarkovJunior功能验证测试程序
 * 测试指定的模型以验证Java实现的完整性和正确性
 */
public class MarkovJuniorTester {

    // 测试模型配置
    private static final TestModel[] TEST_MODELS = {
//            new TestModel("DijkstraDungeon", 40, 40, 1, 4, 4, false, false, 0, 50000, -1),
//            new TestModel("FindLongCycle", 27, 27, 1, 1, 10, false, false, 0, 50000, -1),
//            new TestModel("DualRetraction3D", 32, 32, 32, 1, 4, true, true, 150, 1000, -1),
//            new TestModel("MazeMap", 30, 30, 1, 1, 4, false, true, 0, 1000, -1),
//            new TestModel("ChainMaze", 60, 60, 1, 1, 4, true, true, 150, 1000, -1),
//            new TestModel("GameOfLife", 120, 120, 1, 1, 4, false, false, 0, 100, -1),
//            new TestModel("River", 20, 20, 1, 1, 4, false, false, 0, 50000, -1),
//            new TestModel("BasicDijkstraFill", 60, 60, 1, 1, 4, true, true, 150, 1000, -1),
//            new TestModel("MazeBacktracker", 359, 359, 1, 1, 1, false, false, 0, 20000, -1),
//            new TestModel("WaveFlowers", 60, 60, 1, 1, 4, false, false, 0, 50000, 12345),
//            new TestModel("TileDungeon", 12, 12, 1, 1, 4, true, true, 150, 1000, -1),
            new TestModel("Apartemazements", 8, 8, 16, 1, 6, true, true, 150, 1000, -1),
//            new TestModel("StairsPath", 33, 33, 30, 1, 6, true, true, 150, 1000, -1)
    };

    public static void main(String[] args) {
        System.out.println("=== MarkovJunior Java实现功能验证测试 ===");
        System.out.println("测试开始时间: " + java.time.LocalDateTime.now());

        long overallStartTime = System.currentTimeMillis();

        // 创建主输出目录
        createOutputDirectory("test_output");

        // 加载调色板
        Map<Character, Integer> palette = loadPalette("resources/palette.xml");

        Random metaRandom = new Random(System.currentTimeMillis());

        int successCount = 0;
        int totalCount = TEST_MODELS.length;

        // 运行每个测试模型
        for (TestModel model : TEST_MODELS) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("测试模型: " + model.name);
            System.out.println("=".repeat(60));

            boolean success = runTestModel(model, metaRandom, palette);
            if (success) {
                successCount++;
                System.out.println("✅ " + model.name + " 测试成功!");
            } else {
                System.out.println("❌ " + model.name + " 测试失败!");
            }
        }

        long overallEndTime = System.currentTimeMillis();

        // 输出测试总结
        System.out.println("\n" + "=".repeat(80));
        System.out.println("测试总结");
        System.out.println("=".repeat(80));
        System.out.println("总测试数: " + totalCount);
        System.out.println("成功数: " + successCount);
        System.out.println("失败数: " + (totalCount - successCount));
        System.out.println("成功率: " + String.format("%.1f%%", (double)successCount/totalCount*100));
        System.out.println("总耗时: " + (overallEndTime - overallStartTime) + "ms");
        System.out.println("测试结束时间: " + java.time.LocalDateTime.now());

        if (successCount == totalCount) {
            System.out.println("\n🎉 所有测试通过! MarkovJunior Java实现功能完整!");
        } else {
            System.out.println("\n⚠️  部分测试失败，请检查实现细节");
        }
    }

    private static boolean runTestModel(TestModel model, Random metaRandom, Map<Character, Integer> palette) {
        try {
            // 创建模型专用输出目录
            String outputDir = "test_output/" + model.name;
            createOutputDirectory(outputDir);

            System.out.println("配置: " + model.toString());
            System.out.println("输出目录: " + outputDir);

            // 加载模型XML文件
            String filename = "models/" + model.name + ".xml";
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document modelDoc;

            try {
                modelDoc = builder.parse(new File(filename));
            } catch (Exception e) {
                System.out.println("错误: 无法打开XML文件 " + filename);
                System.out.println("详细错误: " + e.getMessage());
                return false;
            }

            // 创建解释器
            Interpreter interpreter = Interpreter.load(modelDoc.getDocumentElement(),
                    model.MX, model.MY, model.MZ);
            if (interpreter == null) {
                System.out.println("错误: 无法创建解释器");
                return false;
            }

            System.out.println("解释器创建成功");

            // 准备自定义调色板（如果需要）
            Map<Character, Integer> customPalette = new HashMap<>(palette);

            // 运行模型生成
            for (int k = 0; k < model.amount; k++) {
                int seed = model.seed != -1 ? model.seed : metaRandom.nextInt();
                System.out.println("运行实例 " + (k+1) + "/" + model.amount + " (种子: " + seed + ")");

                long startTime = System.currentTimeMillis();
                int stepCounter = 0;
                Interpreter.RunResult lastResult = null;

                try {
                    for (Interpreter.RunResult result : interpreter.run(seed, model.steps, model.gif)) {
                        stepCounter++;
                        lastResult = result;

                        // 如果是GIF模式，保存每一步
                        if (model.gif) {
                            saveResult(result, customPalette, outputDir,
                                    model.name + "_step_" + String.format("%04d", stepCounter),
                                    model);
                        }

                        // 定期输出进度
                        if (stepCounter % 1000 == 0) {
                            System.out.println("  进度: " + stepCounter + " 步...");
                        }
                    }

                    // 保存最终结果
                    if (lastResult != null && !model.gif) {
                        String filename_out = model.name + "_" + seed + "_final";
                        saveResult(lastResult, customPalette, outputDir, filename_out, model);
                    }

                    long endTime = System.currentTimeMillis();
                    System.out.println("  完成: " + stepCounter + " 步, 耗时: " + (endTime - startTime) + "ms");

                } catch (Exception e) {
                    System.out.println("  运行时错误: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            System.out.println("模型测试异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void saveResult(Interpreter.RunResult result, Map<Character, Integer> palette,
                                   String outputDir, String filename, TestModel model) {
        try {
            // 准备颜色数组
            int[] colors = new int[result.legend.length];
            for (int c = 0; c < result.legend.length; c++) {
                colors[c] = palette.getOrDefault(result.legend[c], 0xFF888888); // 默认灰色
            }

            String outputPath = outputDir + "/" + filename;

            // 根据维数和iso设置决定输出格式
            if (result.FZ == 1 || model.iso) {
                // 2D渲染或等轴测渲染
                Graphics.RenderResult renderResult = Graphics.render(
                        result.state, result.FX, result.FY, result.FZ,
                        colors, model.pixelsize, model.gui
                );

                Graphics.saveBitmap(renderResult.bitmap, renderResult.width,
                        renderResult.height, outputPath + ".png");

            } else {
                // 3D VOX格式
                VoxHelper.saveVox(result.state, (byte)result.FX, (byte)result.FY, (byte)result.FZ,
                        colors, outputPath + ".vox");
            }

        } catch (Exception e) {
            System.out.println("保存结果时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createOutputDirectory(String path) {
        try {
            Path dir = Paths.get(path);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            System.out.println("创建输出目录失败: " + path);
            e.printStackTrace();
        }
    }

    private static Map<Character, Integer> loadPalette(String filename) {
        Map<Character, Integer> palette = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));

            var colorNodes = doc.getElementsByTagName("color");
            for (int i = 0; i < colorNodes.getLength(); i++) {
                Element colorElement = (Element) colorNodes.item(i);
                char symbol = XMLHelper.get(colorElement, "symbol", Character.class);
                String value = XMLHelper.get(colorElement, "value");
                palette.put(symbol, (255 << 24) + Integer.parseInt(value, 16));
            }

            System.out.println("调色板加载成功，包含 " + palette.size() + " 种颜色");

        } catch (Exception e) {
            System.out.println("调色板加载失败，使用默认调色板: " + e.getMessage());
            // 提供默认调色板
            palette.put('B', 0xFF000000); // 黑色
            palette.put('W', 0xFFFFFFFF); // 白色
            palette.put('R', 0xFFFF0000); // 红色
            palette.put('G', 0xFF00FF00); // 绿色
            palette.put('Y', 0xFFFFFF00); // 黄色
            palette.put('U', 0xFF0000FF); // 蓝色
            palette.put('D', 0xFF444444); // 深灰
            palette.put('A', 0xFFCCCCCC); // 浅灰
        }

        return palette;
    }

    // 测试模型配置类
    private static class TestModel {
        public final String name;
        public final int MX, MY, MZ;
        public final int amount;
        public final int pixelsize;
        public final boolean iso;
        public final boolean gif;
        public final int gui;
        public final int steps;
        public final int seed;

        public TestModel(String name, int MX, int MY, int MZ, int amount, int pixelsize,
                         boolean iso, boolean gif, int gui, int steps, int seed) {
            this.name = name;
            this.MX = MX;
            this.MY = MY;
            this.MZ = MZ;
            this.amount = amount;
            this.pixelsize = pixelsize;
            this.iso = iso;
            this.gif = gif;
            this.gui = gui;
            this.steps = steps;
            this.seed = seed;
        }

        @Override
        public String toString() {
            return String.format("尺寸=%dx%dx%d, 数量=%d, 像素大小=%d, ISO=%s, GIF=%s, 步数=%d",
                    MX, MY, MZ, amount, pixelsize, iso, gif, steps);
        }
    }

    /**
     * 独立的模型测试方法，可用于单独测试特定模型
     */
    public static boolean testSingleModel(String modelName, int MX, int MY, int MZ,
                                          int seed, int steps) {
        try {
            System.out.println("独立测试模型: " + modelName);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("models/" + modelName + ".xml"));

            Interpreter interpreter = Interpreter.load(doc.getDocumentElement(), MX, MY, MZ);
            if (interpreter == null) {
                System.out.println("无法创建解释器");
                return false;
            }

            int stepCount = 0;
            for (Interpreter.RunResult result : interpreter.run(seed, steps, false)) {
                stepCount++;
                if (stepCount % 100 == 0) {
                    System.out.println("步数: " + stepCount);
                }
            }

            System.out.println("测试完成，总步数: " + stepCount);
            return true;

        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 性能基准测试
     */
    public static void performanceBenchmark() {
        System.out.println("=== 性能基准测试 ===");

        String[] benchmarkModels = {"MazeGrowth", "BasicDijkstraFill", "GameOfLife"};
        int[] sizes = {50, 100, 200};

        for (String modelName : benchmarkModels) {
            System.out.println("\n测试模型: " + modelName);

            for (int size : sizes) {
                long startTime = System.currentTimeMillis();
                boolean success = testSingleModel(modelName, size, size, 1, 12345, 1000);
                long endTime = System.currentTimeMillis();

                System.out.printf("  尺寸 %dx%d: %s, 耗时: %dms%n",
                        size, size, success ? "成功" : "失败", endTime - startTime);
            }
        }
    }
}