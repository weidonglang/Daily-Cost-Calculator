# 设备日均资费计算器

Daily Cost Calculator 是一个 Java 21 + JavaFX 桌面应用，用于管理个人设备、一次性配件、日均摊销、目标换新、统计分析、模型图表，以及本地 JSON/CSV 导入导出。

## 功能

- 设备和配件的添加、编辑、删除、上移、下移。
- 设备本体日均、配件日均、总投入、合计日均、等效月租、等效年化成本计算。
- 单台设置目标日均，批量设置全部设备目标日均。
- 目标等待时间、预计达成日期、30 天下降、边际下降、替换指数和建议文本。
- 统计分析结论与数学建模指标表。
- 日均排行、投入构成、目标等待时间、未来日均曲线图表。
- CSV 文本/文件导入，CSV 明细导出。
- JSON 备份导入/导出。
- 数据自动保存到用户 AppData。
- 暗夜紫和浅色简洁主题切换，主题随数据保存。

## 开发环境

- Java 21
- Maven 3.9+
- JavaFX 21
- Jackson
- JUnit 5
- jpackage

当前项目使用 `maven.compiler.release=21`，运行 Maven 前请确保当前终端的 `JAVA_HOME` 指向 JDK 21。

## 运行

```bash
mvn javafx:run
```

或先打包 jar：

```bash
mvn clean package
java -jar target/daily-cost-calculator.jar
```

## 测试

```bash
mvn test
```

## Windows 打包

```bat
package-windows.bat
```

脚本会执行 `mvn clean package`，再调用 `jpackage` 输出安装包到 `dist/DailyCostCalculator-Setup.exe`，同时输出便携版到 `dist-app/DailyCostCalculator/`。自动化运行时可设置：

```bat
set NO_PAUSE=1
package-windows.bat
```

注意：`dist/DailyCostCalculator-Setup.exe` 是安装包，不是应用本体。安装完成后请运行桌面快捷方式、开始菜单中的 `DailyCostCalculator`，或直接运行：

```text
C:\Program Files\DailyCostCalculator\DailyCostCalculator.exe
```

如果同版本已经安装，重复双击安装包可能只是快速修复/重新配置安装项，看起来像“等待一下然后没有反应”。需要直接运行应用本体，或先从“设置 > 应用”卸载后重新安装。

## 数据保存位置

```text
C:\Users\{用户名}\AppData\Roaming\DailyCostCalculator\data.json
```

卸载或重装程序不会主动删除该目录中的数据，除非用户手动删除。

## CSV 导入格式

```text
类型,名称/所属设备,价格/配件名称,购买日期/价格,配件购买日期或目标日均
设备,vivo x200 ultra,4999,2026-03-07,10
配件,vivo x200 ultra,手机壳,39,2026-03-08
```

规则：

- `设备` 行：设备名称、价格、购买日期、目标日均。
- `配件` 行：所属设备、配件名称、价格、购买日期。
- 日期格式必须是 `yyyy-MM-dd`。
- 金额必须大于 0。
- 配件所属设备必须存在于当前数据或同批导入的设备中。

## 核心公式

- 已使用天数 = 当前日期 - 购买日期 + 1，最小为 1。
- 设备本体日均 = 设备本体价格 / 设备本体已使用天数。
- 配件日均 = 配件价格 / 配件已使用天数。
- 设备当前总日均 = 设备本体日均 + 配件合计日均。
- 设备总投入 = 设备本体价格 + 配件价格总和。
- 加权使用天数 = 设备总投入 / 当前总日均。
- 目标所需总天数 = 设备总投入 / 目标日均。
- 还需天数 = 目标所需总天数 - 当前加权使用天数。
- 30 天下降 = 当前日均 - 设备总投入 / (当前加权使用天数 + 30)。
- 边际下降/天 = 当前日均 - 设备总投入 / (当前加权使用天数 + 1)。
- 替换指数 = 目标日均 / 当前日均 × 100，未达成时限制到 1-99，已达成时为 100。

## 常见问题

### 为什么 Maven 说 release 21 不支持？

当前终端使用的不是 JDK 21。将 `JAVA_HOME` 指向 JDK 21 后重新执行命令。

### JSON 损坏怎么办？

程序启动时会弹出可读错误提示。可以通过备份 JSON 导入恢复，或手动删除损坏的 `data.json` 让程序重新创建空数据。

### 金额计算是否使用 double？

核心金额计算使用 `BigDecimal`。图表展示时才会把数值转换为 JavaFX 需要的 double。

## 2026-06-10 功能补充

- Windows 安装包已启用安装目录选择，运行 `dist/DailyCostCalculator-Setup.exe` 时可以在安装向导里选择安装位置。
- 应用内“导入导出”页提供“卸载/应用设置”入口，会打开 Windows 应用设置，便于卸载或管理当前程序。
- 主窗口改为可调整大小，默认窗口增大到 `1400x900`，最小尺寸为 `1080x720`。
- 主题扩展为：暗夜紫、浅色简洁、深色专业、科技蓝、护眼绿、高对比。
- “统计分析”页增加配件投入占比、30 天自然下降、365 天预测日均，以及 7/30/90/365 天预测、风险提醒、换新建议。
- 本地 AI 分析使用 Ollama `qwen3:8b`，需要先在本机运行：

```bat
ollama run qwen3:8b
```

默认地址为 `http://localhost:11434`，也可以在“统计分析”页手动调整 Ollama 地址和模型名称。

## 2026-06-10 二次优化

- 新增 `dist\Install-to-D-daily.bat`，运行它会自动创建 `D:\daily\DailyCostCalculator` 并通过 MSI 安装到该目录；普通 `DailyCostCalculator-Setup.exe` 仍保留安装向导和目录选择器。
- AI 分析要求 Ollama 输出纯文本，界面会自动清理 Markdown 表格、标题符号和加粗符号。
- “模型图表”页支持勾选设备，未来日均曲线可查看单个设备、多个设备，并显示已选组合合计曲线。
- 主题样式扩展到标签页、表格、图表、输入框和卡片，主题切换会更明显。
