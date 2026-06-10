# AGENTS.md

## 项目名称

设备日均资费计算器 / Daily Cost Calculator

## 项目目标

本项目使用 Java 开发一个 Windows 桌面软件，用于管理个人设备、一次性附属配件、日均摊销、目标换新周期、统计分析、图表展示和本地数据导入导出。

最终目标是生成一个可以在 Windows 上安装和运行的 `.exe` 程序。用户安装后可以双击运行，不需要额外安装 JDK/JRE。

## 技术栈要求

必须使用：

* Java 21
* JavaFX
* Maven
* Jackson
* JUnit 5
* jpackage

禁止使用：

* Electron
* Python
* WebView 套壳
* 数据库服务器
* 联网功能
* 云端 API
* 需要管理员权限才能运行的设计

本项目必须是纯本地桌面软件。

## 项目结构要求

请保持标准 Maven 项目结构：

```text
daily-cost-calculator-java/
├─ AGENTS.md
├─ PLANS.md
├─ pom.xml
├─ README.md
├─ package-windows.bat
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/dailycost/
│  │  │     ├─ MainApp.java
│  │  │     ├─ controller/
│  │  │     ├─ model/
│  │  │     ├─ service/
│  │  │     ├─ storage/
│  │  │     ├─ util/
│  │  │     └─ view/
│  │  └─ resources/
│  │     ├─ app.css
│  │     ├─ theme-dark-purple.css
│  │     ├─ theme-light.css
│  │     └─ icons/
│  └─ test/
│     └─ java/
│        └─ com/dailycost/
```

不要把所有代码写在一个 `MainApp` 类里。

推荐职责划分：

* `model`：Device、Accessory、AppData、AppSettings 等数据模型。
* `service`：摊销计算、统计分析、目标换新、CSV 解析、业务编排。
* `storage`：JSON 本地持久化、备份导入导出。
* `controller`：JavaFX 页面事件处理。
* `view`：界面构建、弹窗、图表组件。
* `util`：金额格式化、日期格式化、路径工具、校验工具。

## 核心数据模型

### Device

```java
class Device {
    String id;
    String name;
    BigDecimal basePrice;
    LocalDate purchaseDate;
    BigDecimal targetDailyCost;
    List<Accessory> accessories;
    int sortOrder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

### Accessory

```java
class Accessory {
    String id;
    String name;
    BigDecimal price;
    LocalDate purchaseDate;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

### AppSettings

```java
class AppSettings {
    String themeName;
    BigDecimal defaultTargetDailyCost;
    int saveFormatVersion;
}
```

### AppData

```java
class AppData {
    int version;
    AppSettings settings;
    List<Device> devices;
}
```

## 数据保存要求

默认数据保存路径：

```text
C:\Users\{用户名}\AppData\Roaming\DailyCostCalculator\data.json
```

实现时不要硬编码用户名。必须使用 Java 系统属性或平台路径工具动态获取用户目录。

如果数据文件不存在，应自动创建目录和默认数据。

默认数据：

```json
{
  "version": 4,
  "settings": {
    "themeName": "暗夜紫",
    "defaultTargetDailyCost": 10,
    "saveFormatVersion": 4
  },
  "devices": []
}
```

要求：

* 启动时自动读取 data.json。
* 修改数据后自动保存。
* 保存失败必须弹窗提示。
* 读取失败必须提示并提供恢复或重新创建空数据的选择。
* 支持 JSON 备份导入。
* 支持 JSON 备份导出。
* 支持打开数据目录。
* 不得因为 JSON 损坏导致程序直接崩溃。

## 金额和日期计算要求

金额核心计算必须使用 `BigDecimal`。

禁止直接使用 `double` 进行金额核心计算。

允许在 JavaFX 图表展示时将 BigDecimal 转为 double，但不得影响核心计算结果。

日期使用：

* `LocalDate`
* `LocalDateTime`
* `DateTimeFormatter`

日期显示格式：

```text
yyyy-MM-dd
```

金额显示格式：

```text
保留两位小数
```

## 计算规则

### 已使用天数

```text
已使用天数 = 当前日期 - 购买日期 + 1
最小值为 1
```

购买当天算第 1 天。

### 设备本体日均

```text
设备本体日均 = 设备本体价格 / 设备本体已使用天数
```

### 配件日均

```text
单个配件日均 = 配件价格 / 配件已使用天数
配件合计日均 = 所有配件日均之和
```

### 设备当前总日均

```text
设备当前总日均 = 设备本体日均 + 配件合计日均
```

### 设备总投入

```text
设备总投入 = 设备本体价格 + 所有配件价格
```

### 全部累计投入

```text
全部累计投入 = 所有设备总投入之和
```

### 合计日均

```text
合计日均 = 所有设备当前总日均之和
```

### 等效月租

```text
等效月租 = 合计日均 × 30.4167
```

### 等效年化成本

```text
等效年化成本 = 合计日均 × 365
```

### 加权使用天数

因为设备和配件购买日期不同，所以设备整体使用天数使用加权模型：

```text
加权使用天数 = 设备总投入 / 当前总日均
```

### 目标所需总天数

```text
目标所需总天数 = 设备总投入 / 目标日均
```

### 还需天数

```text
还需天数 = 目标所需总天数 - 当前加权使用天数
```

如果当前总日均 <= 目标日均，则视为已达成。

### 预计达成日期

```text
预计达成日期 = 当前日期 + 还需天数
```

### 30 天下降

```text
当前日均 = 当前总日均
30天后日均 = 设备总投入 / (当前加权使用天数 + 30)
30天下降 = 当前日均 - 30天后日均
```

### 边际下降/天

```text
明日日均 = 设备总投入 / (当前加权使用天数 + 1)
边际下降/天 = 当前日均 - 明日日均
```

### 替换指数

替换指数范围为 0 到 100，数值越高表示越接近可以换新。

```text
progress = targetDailyCost / currentDailyCost
baseScore = progress × 100
```

限制规则：

```text
如果 currentDailyCost <= targetDailyCost，则替换指数 = 100
否则替换指数 = max(1, min(99, baseScore))
```

显示格式：

```text
12/100
54/100
```

### 建议文本

```text
如果 currentDailyCost <= targetDailyCost:
    已达到目标，可考虑换新
否则如果 replacementIndex >= 70:
    接近目标，可观望换新
否则:
    继续摊薄更划算
```

## 页面要求

软件包含 5 个主 Tab：

```text
设备总览
目标换新
统计分析
模型图表
导入导出
```

## 顶部区域要求

窗口标题：

```text
设备日均资费计算器
```

顶部大标题：

```text
设备日均资费计算器
```

副标题：

```text
设备摊销 × 目标换新 × 数据建模分析
```

顶部按钮：

```text
添加设备
批量导入
导出
主题下拉框
```

主题下拉框至少包含：

```text
暗夜紫
浅色简洁
```

## 顶部统计卡片

顶部显示 6 个统计卡片：

```text
设备
配件
累计投入
合计日均
等效月租
达成目标
```

示例：

```text
设备：8 个
配件：2 个
累计投入：30956.00 元
合计日均：3101.97 元/天
等效月租：94351.66 元/月
达成目标：0 台
```

## 设备总览 Tab

必须包含：

1. 计算规则说明条。
2. 快速总览表。
3. 设备卡片列表。
4. 每台设备的配件列表。
5. 添加、编辑、删除、上移、下移、设置目标等操作。

计算规则说明条文本：

```text
计算规则：日均 = 价格 ÷ 已使用天数；购买当天算第 1 天。配件按自己的购买日期计算后叠加到设备。
```

快速总览表字段：

```text
设备
总投入
当前日均
目标
还需
30天下降
替换指数
建议
```

设备卡片字段：

```text
设备名称
配件数量
设备本体：价格 | 购买日期 | 已使用 X 天 | 本体日均
配件合计：价格 | 配件数量 | 配件日均
目标换新：目标日均 | 还需多久 | 预计日期
边际下降：边际下降/天 | 再用30天预计下降
设备当前总日均
替换指数
建议
编辑
删除
上移
下移
+ 添加一次性附属配件
设置目标
```

配件列表字段：

```text
配件名称
价格
购买日期
已使用天数
配件日均
编辑
删除
```

## 目标换新 Tab

必须包含：

1. 批量目标输入框。
2. 单位：元/天。
3. 应用到全部按钮。
4. 快捷按钮：1 元、5 元、10 元。
5. 目标换新表格。

目标换新表格字段：

```text
设备
当前日均
目标日均
还需多久
达成日期
30天下降
替换指数
建议
```

点击“应用到全部”后，必须修改所有设备目标日均并保存。

## 统计分析 Tab

必须包含统计卡片：

```text
总资产摊销：合计日均 元/天
平均加权使用：X 天
目标达成率：已达成数量/设备总数
```

必须生成自动分析结论：

```text
当前日均最高的是“设备名”：X 元/天，占全部设备日均的 Y%。

累计投入最高的是“设备名”：X 元，占总投入的 Y%。

如果未来 30 天不新增设备/配件，组合合计日均预计自然下降约 X 元/天。

当前组合的等效年化使用成本约 X 元/年，可用于和“每年换新预算”做对比。
```

数学建模指标表字段：

```text
设备
总投入
加权使用
当前日均
目标日均
边际下降/天
替换指数
```

## 模型图表 Tab

使用 JavaFX Canvas 或 JavaFX Chart 实现。

不要引入复杂图表框架。

必须包含 4 个图表：

```text
日均排行
投入构成
目标等待时间
未来日均曲线
```

日均排行：

* 横向条形图。
* 按当前日均从高到低排序。
* 显示设备名和当前日均。

投入构成：

* 饼图或环形图。
* 显示每台设备总投入占比。
* 中心显示总投入。
* 图例显示设备名和百分比。

目标等待时间：

* 横向条形图。
* 显示每台设备距离目标日均还需要的天数。

未来日均曲线：

* 选取当前日均最高的设备。
* 展示未来 365 天内继续使用后的日均下降趋势。
* 横轴：0、30、60、90、180、270、365。
* 纵轴：预计日均 元/天。

## 导入导出 Tab

必须包含按钮：

```text
批量导入文本/CSV
导入 JSON 备份
导出 CSV/JSON
打开数据目录
```

必须显示当前数据文件信息：

```text
当前数据文件：C:\Users\...\AppData\Roaming\DailyCostCalculator\data.json
设备数量：X
配件数量：X
主题：暗夜紫
保存格式版本：4
```

## CSV 导入格式

支持以下格式：

```text
类型,名称/所属设备,价格/配件名称,购买日期/价格,配件购买日期或目标日均
设备,vivo x200 ultra,4999,2026-03-07,10
设备,巴塞利斯蛇V3pro 35K,758,2025-12-27,1
设备,Rog 魔霸7plus,8999,2023-06-13,10
配件,vivo x200 ultra,手机壳,39,2026-03-08
配件,vivo x200 ultra,钢化膜,25,2026-03-08
```

导入规则：

```text
类型 = 设备 或 配件
设备行：名称、价格、购买日期、目标日均
配件行：所属设备、配件名称、价格、购买日期
```

导入时必须：

* 校验日期格式。
* 校验金额大于 0。
* 配件所属设备必须存在。
* 导入前显示预览。
* 导入成功后自动保存。
* 导入失败时说明具体行号和原因。

## CSV 导出字段

CSV 明细至少包含：

```text
类型
设备名
配件名
价格
购买日期
已使用天数
当前日均
目标日均
```

## 弹窗要求

必须实现以下弹窗：

```text
添加设备弹窗
编辑设备弹窗
添加配件弹窗
编辑配件弹窗
设置目标弹窗
批量导入预览弹窗
删除确认弹窗
关于软件弹窗
```

添加设备字段：

```text
设备名称
设备价格
购买日期
目标日均
```

添加配件字段：

```text
所属设备
配件名称
配件价格
购买日期
```

输入校验规则：

```text
名称不能为空
价格必须大于 0
目标日均必须大于 0
日期不能晚于今天太多
如果日期晚于今天，需要二次确认
```

## UI 风格要求

主视觉参考“暗夜紫”。

暗夜紫建议颜色：

```text
顶部背景：#2F2A7E
主紫色：#7C3AED
强调粉色：#DB2777
背景浅紫：#F5F2FF
边框浅紫：#DDD6FE
正文深蓝：#050B3F
弱文本：#7C74A8
```

按钮风格：

```text
主按钮紫色填充
普通按钮白底紫边框
危险按钮淡红色
卡片白底浅紫边框
```

字体：

```text
默认使用 Microsoft YaHei UI
标题加粗
数字加粗
```

界面目标：

* 优先保证功能完整和稳定。
* 不要牺牲可用性追求过度动画。
* 表格、卡片、图表都要清晰。
* 窗口缩放时布局不应严重错乱。
* 长设备名要支持省略和 Tooltip。

## 示例数据

不要默认污染用户数据。

首次启动或空数据时，可以提供“导入示例数据”按钮。

示例数据：

```text
设备,vivo x200 utral,4999,2026-03-07,3.5
设备,巴塞利斯蛇V3pro 35K,758,2025-12-27,0.5
设备,Rog 魔霸7plus,9499,2023-06-13,5
设备,黑寡妇V4迷你,500,2025-04-21,0.25
设备,台式电脑5080+9900x+16G+5T+tufx670egp+G7显示器,12000,2025-05-20,3
设备,歌德G22,350,2026-06-10,0.1
设备,OPPO Enco X3,650,2026-06-10,0.4
设备,P275MV MAX,2000,2026-06-10,1
配件,vivo x200 utral,手机壳和膜,200,2026-04-27
```

## 单元测试要求

至少为以下内容编写 JUnit 5 测试：

```text
已使用天数计算
设备本体日均计算
配件日均计算
设备总日均计算
目标达成日期计算
30天下降计算
边际下降计算
替换指数计算
CSV 导入解析
JSON 保存与读取
```

每次完成一个阶段后，必须运行：

```bash
mvn test
```

如果测试失败，不要继续堆功能，必须先修复测试。

## 打包要求

必须提供：

```text
package-windows.bat
```

一键完成：

```text
mvn clean package
jpackage 打包
输出 Windows exe 安装包
```

目标输出目录：

```text
dist/
```

安装包名称：

```text
DailyCostCalculator-Setup.exe
```

打包要求：

* 安装后开始菜单可见。
* 安装后可创建桌面快捷方式。
* 程序图标可配置。
* 包含运行所需 Java runtime。
* 用户不需要单独安装 JDK/JRE。

`package-windows.bat` 可参考：

```bat
@echo off
set APP_NAME=DailyCostCalculator
set APP_VERSION=1.0.0

mvn clean package

jpackage ^
  --type exe ^
  --name %APP_NAME% ^
  --app-version %APP_VERSION% ^
  --input target ^
  --main-jar daily-cost-calculator.jar ^
  --main-class com.dailycost.MainApp ^
  --dest dist ^
  --win-menu ^
  --win-shortcut ^
  --vendor "DailyCostCalculator" ^
  --description "设备日均资费计算器"

pause
```

如果 JavaFX 依赖无法被 jpackage 正确识别，可以调整 Maven 配置或打包方式，但最终必须实现一键生成 Windows exe 安装包。

## README 要求

必须生成 README.md，包含：

```text
项目简介
功能列表
开发环境
运行方式
打包方式
数据保存位置
CSV 导入格式
计算公式说明
常见问题
```

## 开发顺序要求

必须按照 PLANS.md 的阶段顺序开发。

不要一开始就做复杂 UI。

开发优先级：

1. 核心模型与计算。
2. 本地 JSON 存储。
3. 主界面与设备总览。
4. 目标换新与统计分析。
5. 图表与导入导出。
6. 主题与 exe 打包。
7. 最终测试与文档。

## 验收标准

最终程序必须满足：

```text
可以添加、编辑、删除设备
可以添加、编辑、删除配件
可以正确计算当前日均、目标日均、还需时间、30天下降、替换指数
可以显示总投入、合计日均、等效月租、等效年化成本
可以批量设置目标日均
可以显示统计分析结论
可以显示 4 类图表
可以导入 CSV
可以导出 CSV/JSON
可以自动保存到 AppData
可以切换主题并保存
可以通过 package-windows.bat 打包成 Windows exe
重新打开软件后数据不丢失
```

## 代码质量要求

必须做到：

* 命名清晰。
* 注释适量。
* 服务层和界面层分离。
* 计算逻辑可测试。
* UI 不要和公式强绑定。
* 异常必须处理。
* 重要错误必须弹窗提示。
* 导入导出等耗时操作不能卡死 UI。
* 不要使用绝对路径写死用户目录。
* 不要引入不必要的重型依赖。
* 不要默认联网。
* 不要强制管理员权限运行。

## 每次提交或阶段完成前必须检查

阶段完成前必须确认：

```text
mvn test 通过
程序可以正常启动
数据可以保存和重新读取
核心页面没有明显空指针异常
新增功能写入 README 或注释说明
PLANS.md 中对应任务已更新状态
```
