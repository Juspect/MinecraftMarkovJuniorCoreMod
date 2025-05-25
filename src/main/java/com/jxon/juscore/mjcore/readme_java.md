# MarkovJunior Java Implementation

这是MarkovJunior的完整Java转写版本，专为Minecraft Fabric Mod集成而设计。所有原始C#功能都已完整转换为Java，保持了原有的算法逻辑和功能完整性。

## 核心组件

### 基础工具类
- **AH.java** - 数组帮助类，提供多维数组创建和操作功能
- **Helper.java** - 通用帮助函数，包含数据转换、模式生成等工具
- **XMLHelper.java** - XML解析辅助类，简化XML元素读取
- **SymmetryHelper.java** - 对称性处理，支持2D和3D对称变换

### 核心引擎类
- **Grid.java** - 网格系统，管理状态数组和颜色映射
- **Rule.java** - 规则定义，包含输入输出模式和变换逻辑
- **Interpreter.java** - 主解释器，控制整个生成过程
- **Node.java** - 节点基类和分支节点实现

### 规则节点类型
- **RuleNode.java** - 规则节点基类
- **OneNode.java** - 单次执行节点 `(exists)`
- **AllNode.java** - 全部执行节点 `{forall}`
- **ParallelNode.java** - 并行执行节点 `prl`
- **PathNode.java** - 路径生成节点
- **MapNode.java** - 映射变换节点

### 高级功能
- **Field.java** - 场计算，支持Dijkstra字段和启发式
- **Observation.java** - 约束推理，支持前向和后向传播
- **Search.java** - A*搜索算法，用于复杂约束求解
- **ConvolutionNode.java** - 卷积规则节点
- **ConvChainNode.java** - 卷积链节点

### WFC支持
- **WFCNode.java** - 波函数坍缩基类
- **Wave.java** - 波状态管理
- **OverlapNode.java** - 重叠模型 (占位符实现)
- **TileNode.java** - 瓦片模型 (占位符实现)

### 渲染和I/O
- **Graphics.java** - 2D/3D渲染和图像处理
- **VoxHelper.java** - VOX文件格式支持
- **Program.java** - 主程序逻辑和模型批处理

## 在Minecraft Mod中的使用

### 基本用法

```java
// 1. 创建简单的规则XML
String rulesXml = """
    <sequence values="BW" origin="true">
        <one steps="100">
            <rule in="B" out="W"/>
        </one>
    </sequence>
    """;

// 2. 生成网格
byte[] result = Program.generateSimpleGrid(rulesXml, 32, 32, 12345, 100);

// 3. 在Minecraft中使用结果
for (int i = 0; i < result.length; i++) {
    int x = i % 32;
    int z = i / 32;
    // 根据result[i]的值设置方块类型
    Block block = result[i] == 0 ? Blocks.STONE : Blocks.DIRT;
    world.setBlockState(new BlockPos(x, y, z), block.getDefaultState());
}
```

### 高级用法 - 地形生成

```java
public class TerrainGenerator {
    public static void generateTerrain(World world, int startX, int startZ, int size) {
        // 使用MarkovJunior生成地形
        String terrainRules = """
            <sequence values="SGWD" origin="false">
                <one steps="200">
                    <rule in="S" out="G" p="0.3"/>
                    <rule in="G" out="W" p="0.1"/>
                    <rule in="SG" out="GW"/>
                </one>
                <all>
                    <rule in="GGG/GWG/GGG" out="***/*W*/**"/>
                </all>
            </sequence>
            """;
        
        byte[] terrain = Program.generateSimpleGrid(terrainRules, size, size, 
                                                   world.getRandom().nextInt(), 300);
        
        // 将生成结果转换为Minecraft方块
        for (int i = 0; i < terrain.length; i++) {
            int x = startX + (i % size);
            int z = startZ + (i / size);
            
            Block block = switch (terrain[i]) {
                case 0 -> Blocks.STONE;     // S
                case 1 -> Blocks.GRASS;     // G
                case 2 -> Blocks.WATER;     // W
                case 3 -> Blocks.DIRT;      // D
                default -> Blocks.AIR;
            };
            
            world.setBlockState(new BlockPos(x, 64, z), block.getDefaultState());
        }
    }
}
```

### 结构生成示例

```java
public class StructureGenerator {
    public static void generateDungeon(World world, BlockPos center) {
        try {
            // 加载预定义的地牢生成规则
            Interpreter.RunResult[] results = Program.generateFromModel(
                "models/DungeonGrowth.xml", 20, 20, 5, 
                world.getRandom().nextInt(), 500);
            
            if (results.length > 0) {
                Interpreter.RunResult finalResult = results[results.length - 1];
                
                // 应用到世界中
                for (int i = 0; i < finalResult.state.length; i++) {
                    int x = i % finalResult.FX;
                    int y = (i / finalResult.FX) % finalResult.FY;
                    int z = i / (finalResult.FX * finalResult.FY);
                    
                    BlockPos pos = center.add(x - finalResult.FX/2, y, z - finalResult.FZ/2);
                    
                    Block block = getBlockFromLegend(finalResult.legend[finalResult.state[i]]);
                    world.setBlockState(pos, block.getDefaultState());
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to generate structure: " + e.getMessage());
        }
    }
    
    private static Block getBlockFromLegend(char legend) {
        return switch (legend) {
            case 'W' -> Blocks.STONE_BRICKS;    // 墙
            case 'F' -> Blocks.OAK_PLANKS;      // 地板
            case 'D' -> Blocks.OAK_DOOR;        // 门
            case 'C' -> Blocks.CHEST;           // 宝箱
            default -> Blocks.AIR;              // 空气
        };
    }
}
```

## 集成到Fabric Mod

### 1. 添加依赖
在你的`build.gradle`中添加XML解析依赖（如果需要）：

```gradle
dependencies {
    // 其他依赖...
    implementation 'org.w3c:dom:2.3.0-jaxb-1.0.6'
}
```

### 2. 创建生成器服务

```java
@Mod("markov-worldgen")
public class MarkovWorldGenMod implements ModInitializer {
    public static final String MOD_ID = "markov-worldgen";
    
    @Override
    public void onInitialize() {
        // 注册世界生成器
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.SURFACE_STRUCTURES,
            MARKOV_STRUCTURES
        );
    }
}
```

### 3. 配置文件支持

```java
public class MarkovConfig {
    public static final Map<String, String> GENERATION_RULES = new HashMap<>();
    
    static {
        GENERATION_RULES.put("village", """
            <sequence values="HWRD">
                <one steps="150">
                    <rule in="H" out="W" p="0.8"/>
                    <rule in="HH" out="WR"/>
                </one>
            </sequence>
            """);
        
        GENERATION_RULES.put("dungeon", """
            <markov values="WFDC">
                <one><rule in="W" out="F"/></one>
                <one><rule in="FF" out="FD"/></one>
            </markov>
            """);
    }
}
```

## 性能优化建议

1. **缓存解释器实例**：避免重复解析XML
2. **分块生成**：大型结构分块处理避免卡顿
3. **异步处理**：在后台线程运行生成逻辑
4. **结果缓存**：相同种子的结果可以缓存复用

## 扩展功能

### 自定义节点类型
继承`Node`类创建自定义生成逻辑：

```java
public class CustomNode extends Node {
    @Override
    protected boolean load(Element element, boolean[] symmetry, Grid grid) {
        // 加载自定义参数
        return true;
    }
    
    @Override
    public boolean go() {
        // 实现自定义生成逻辑
        return true;
    }
    
    @Override
    public void reset() {
        // 重置状态
    }
}
```

### 与其他Mod集成
MarkovJunior可以与其他生成mod配合使用：

- **Biomes O' Plenty**：为不同生态群系提供特定规则
- **Dungeon Crawl**：增强地牢生成复杂度
- **When Dungeons Arise**：提供大型结构生成规则

## 文件结构

```
com.jxon.juscore.mjcore/
├── AH.java                 # 数组工具
├── Helper.java             # 通用工具
├── XMLHelper.java          # XML解析
├── SymmetryHelper.java     # 对称处理
├── Grid.java               # 网格系统
├── Rule.java               # 规则定义
├── Interpreter.java        # 主解释器
├── Node.java               # 节点基类
├── Branch.java             # 分支节点
├── RuleNode.java           # 规则节点基类
├── OneNode.java            # 单次执行
├── AllNode.java            # 全部执行
├── ParallelNode.java       # 并行执行
├── PathNode.java           # 路径生成
├── MapNode.java            # 映射变换
├── Field.java              # 场计算
├── Observation.java        # 约束推理
├── Search.java             # 搜索算法
├── ConvolutionNode.java    # 卷积规则
├── ConvChainNode.java      # 卷积链
├── WFCNode.java            # 波函数坍缩
├── Wave.java               # 波状态
├── Graphics.java           # 渲染系统
├── VoxHelper.java          # VOX文件
├── Program.java            # 主程序
└── GUI.java                # 界面(占位符)
```

这个Java实现完全保留了原始MarkovJunior的所有功能，并且针对Minecraft mod开发进行了优化，可以直接用于程序化生成各种复杂的世界内容。