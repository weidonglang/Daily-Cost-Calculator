# Daily Cost Calculator

一个 Java 21 + JavaFX 桌面应用，用于管理个人设备资产、配件投入、日均摊销、目标换新、统计图表和本地 AI 辅助分析。

## 功能

- 设备与配件管理：添加、编辑、删除、排序，支持批量添加多种附件和数量，支持将旧设备标记为已换新/恢复使用。
- 日均成本计算：设备本体日均、配件日均、总投入、当前活跃日均、30 天平滑日均、平滑月摊、等效年化成本。
- 目标换新模型：目标日均、还需天数、预计达成日期、30 天自然下降、边际下降、换新指数。
- 统计分析：资产摊销、配件投入占比、未来 7/30/90/365 天预测、风险提醒、换新建议。
- 历史对比：显示当前日均相较昨天、3 天前、7 天前、30 天前的变化。
- 模型图表：日均排行、投入构成饼图、投入排名条形图、目标等待时间、未来日均曲线。
- 图表筛选：可选择单个设备、多个设备，并查看已选设备组合的未来日均合计曲线；未来曲线支持 7/30/90/365 天和自定义天数。
- 本地 AI 分析：调用本机 Ollama 的 `qwen3:8b`，基于本地统计数据生成分析建议；任意 AI 按钮都会自动切换到“统计分析”页显示输出。
- 多场景 AI：支持整体分析、目标换新分析、预算风险分析、图表所选设备分析、单设备分析。
- AI 语义约束：明确区分“自然摊薄下降”和“真实节省”，并说明换新指数越高代表越接近目标。
- AI 口径约束：已换新设备只作为历史记录，不计入活跃组合风险；预算建议优先参考 30 天平滑日均和平滑月摊。
- AI 缓存：保留上一次 AI 分析结果，重新打开应用后仍可查看；再次点击会生成并保存新结果。
- 数据管理：本地 JSON 自动保存，支持 JSON 备份导入/导出和 CSV 导入/导出。
- 主题：暗夜紫、浅色简洁、深色专业、科技蓝、护眼绿、高对比。
- Windows 打包：生成安装包、MSI、便携版 app-image。

## 技术栈

- Java 21
- JavaFX 21
- Maven 3.9+
- Jackson
- JUnit 5
- jpackage
- WiX Toolset 3.x（仅 Windows 安装包打包需要）

## 目录结构

```text
src/main/java/com/dailycost/
  model/      数据模型与快照
  service/    计算、导入导出、目标换新、Ollama 分析
  storage/    本地 JSON 存储
  util/       路径与格式化工具
  view/       JavaFX 主界面
src/main/resources/
  app.css
  theme-*.css
src/test/java/
  单元测试
```

## 开发环境

需要安装：

- JDK 21，并设置 `JAVA_HOME`
- Maven 3.9+，确保 `mvn` 在 PATH 中，或设置 `MAVEN_HOME`

Windows 安装包打包还需要：

- WiX Toolset 3.x
- 将 WiX 目录加入 PATH，或设置 `WIX_HOME`

## 运行

```bash
mvn javafx:run
```

Windows 也可以直接运行：

```bat
run-dev.bat
```

## 测试

```bash
mvn test
```

Windows 也可以直接运行：

```bat
test.bat
```

## 打包

先确认 `JAVA_HOME`、Maven、WiX 都可用，然后运行：

```bat
package-windows.bat
```

输出内容：

```text
dist/DailyCostCalculator-Setup.exe
dist/DailyCostCalculator-Setup.msi
dist/Install-to-D-daily.bat
dist-app/DailyCostCalculator/DailyCostCalculator.exe
```

说明：

- `DailyCostCalculator-Setup.exe` 是普通安装向导，支持选择安装目录。
- `Install-to-D-daily.bat` 会自动创建 `D:\daily\DailyCostCalculator`，并通过 MSI 安装到该目录。
- `dist-app/DailyCostCalculator/` 是便携版目录。
- `dist/`、`dist-app/`、`target/` 都是构建产物，不应提交到 GitHub。

## 清理本地产物

```bat
clean-artifacts.bat
```

会删除 `target/`、`dist/`、`dist-app/`、临时 jpackage 目录和安装日志。

## 本地 AI 分析

应用默认调用：

```text
http://localhost:11434
```

默认模型：

```text
qwen3:8b
```

使用前先启动 Ollama：

```bat
ollama run qwen3:8b
```

如果使用其他模型或端口，可以在“统计分析”页修改 Ollama 地址和模型名。

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
- 换新指数 = 目标日均 / 当前日均 × 100，未达成时限制到 1-99，已达成时为 100。
- 已换新设备：保留在历史记录、累计投入和明细表中，但不计入当前活跃日均、活跃组合达标数、活跃组合自然下降分析。
- 30 天平滑日均 = 当前所有使用中设备继续使用 30 天后的预测组合日均，用于削弱当天新购设备带来的短期峰值。
- 平滑月摊 = 30 天平滑日均 × 30.4167，比即时等效月租更适合做稳定预算参考。

## GitHub 上传注意

本仓库只应提交源码、资源、测试、文档和脚本。

不要提交：

- `target/`
- `dist/`
- `dist-app/`
- `tools/`
- `*.exe`
- `*.msi`
- `*.zip`
- IDE 配置和日志文件

这些内容已经写入 `.gitignore`。如果它们之前已经被 Git 跟踪，需要先从索引移除：

```bash
git rm -r --cached target dist dist-app tools
git rm --cached installer-run.log
git add .
git commit -m "Prepare project for GitHub"
```

## License

未指定许可证。公开发布前建议补充 `LICENSE` 文件。
