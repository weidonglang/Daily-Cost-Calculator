package com.dailycost.view;

import com.dailycost.model.Accessory;
import com.dailycost.model.AccessoryCostSnapshot;
import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import com.dailycost.model.DeviceCostSnapshot;
import com.dailycost.model.SummarySnapshot;
import com.dailycost.service.CsvImportException;
import com.dailycost.service.CsvImportExportService;
import com.dailycost.service.DailyCostCalculatorService;
import com.dailycost.service.OllamaAnalysisService;
import com.dailycost.service.TargetPlanService;
import com.dailycost.storage.JsonDataStore;
import com.dailycost.storage.StorageException;
import com.dailycost.util.AppPaths;
import com.dailycost.util.FormatUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MainView {
    private static final String SAMPLE_CSV = """
            设备,vivo x200 utral,4999,2026-03-07,3.5
            设备,巴塞利斯蛇V3pro 35K,758,2025-12-27,0.5
            设备,Rog 魔霸7plus,9499,2023-06-13,5
            设备,黑寡妇V4迷你,500,2025-04-21,0.25
            设备,台式电脑5080+9900x+16G+5T+tufx670egp+G7显示器,12000,2025-05-20,3
            设备,歌德G22,350,2026-06-10,0.1
            设备,OPPO Enco X3,650,2026-06-10,0.4
            设备,P275MV MAX,2000,2026-06-10,1
            配件,vivo x200 utral,手机壳和膜,200,2026-04-27
            """;

    private final AppData appData;
    private final JsonDataStore dataStore;
    private final DailyCostCalculatorService calculatorService = new DailyCostCalculatorService();
    private final CsvImportExportService csvService = new CsvImportExportService();
    private final OllamaAnalysisService ollamaService = new OllamaAnalysisService();
    private final TargetPlanService targetPlanService = new TargetPlanService();
    private final BorderPane root = new BorderPane();
    private final HBox statsBar = new HBox(12);
    private final TableView<DeviceCostSnapshot> overviewTable = new TableView<>();
    private final TableView<DeviceCostSnapshot> targetTable = new TableView<>();
    private final TableView<DeviceCostSnapshot> modelTable = new TableView<>();
    private final VBox deviceCards = new VBox(12);
    private final VBox analysisBox = new VBox(12);
    private final VBox chartsBox = new VBox(14);
    private final VBox chartDeviceFilters = new VBox(8);
    private final VBox importExportInfo = new VBox(8);
    private final List<String> selectedChartDeviceIds = new ArrayList<>();
    private final ComboBox<String> futureRangeBox = new ComboBox<>(FXCollections.observableArrayList("7天", "30天", "90天", "365天", "自定义"));
    private final TextField customFutureDays = new TextField("365");
    private final TextField batchTarget = new TextField();
    private final TextArea aiAnalysis = new TextArea();
    private final TextField ollamaEndpoint = new TextField(OllamaAnalysisService.DEFAULT_ENDPOINT);
    private final TextField ollamaModel = new TextField(OllamaAnalysisService.DEFAULT_MODEL);
    private Scene scene;

    public MainView(AppData appData, JsonDataStore dataStore) {
        this.appData = appData;
        this.dataStore = dataStore;
        build();
        refresh();
    }

    public Parent getRoot() {
        return root;
    }

    public void attachScene(Scene scene) {
        this.scene = scene;
        applyTheme();
    }

    private void build() {
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
    }

    private Node buildHeader() {
        Label title = new Label("设备日均资费计算器");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("设备摊销 × 目标换新 × 数据建模分析");
        subtitle.getStyleClass().add("app-subtitle");

        Button addDeviceButton = primaryButton("添加设备");
        addDeviceButton.setOnAction(event -> showDeviceDialog(null).ifPresent(device -> {
            device.setSortOrder(appData.getDevices().size());
            appData.getDevices().add(device);
            saveAndRefresh();
        }));

        Button importButton = secondaryButton("批量导入");
        importButton.setOnAction(event -> showTextImportDialog(null));

        Button exportButton = secondaryButton("导出");
        exportButton.setOnAction(event -> exportCsv());

        ComboBox<String> themeBox = new ComboBox<>(FXCollections.observableArrayList("暗夜紫", "浅色简洁", "深色专业", "科技蓝", "护眼绿", "高对比"));
        themeBox.setValue(appData.getSettings().getThemeName());
        themeBox.setOnAction(event -> {
            appData.getSettings().setThemeName(themeBox.getValue());
            saveAndRefresh();
            applyTheme();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, addDeviceButton, importButton, exportButton, themeBox);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox titleRow = new HBox(16, new VBox(6, title, subtitle), spacer, actions);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(16, titleRow, statsBar);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(22));
        return header;
    }

    private Node buildTabs() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("设备总览", buildOverviewTab()));
        tabs.getTabs().add(new Tab("目标换新", buildTargetTab()));
        tabs.getTabs().add(new Tab("统计分析", buildStatisticsTab()));
        tabs.getTabs().add(new Tab("模型图表", buildChartsTab()));
        tabs.getTabs().add(new Tab("导入导出", buildImportExportTab()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        return tabs;
    }

    private Node buildOverviewTab() {
        configureOverviewTable();
        Label rule = new Label("计算规则：日均 = 价格 ÷ 已使用天数；购买当天算第 1 天。配件按自己的购买日期计算后叠加到设备。");
        rule.getStyleClass().add("notice");
        ScrollPane cardsScroll = new ScrollPane(deviceCards);
        cardsScroll.setFitToWidth(true);
        cardsScroll.getStyleClass().add("content-scroll");

        VBox top = new VBox(12, rule, overviewTable);
        VBox.setVgrow(overviewTable, Priority.ALWAYS);
        SplitPane splitPane = new SplitPane(top, cardsScroll);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.42);

        VBox content = new VBox(14, splitPane);
        content.setPadding(new Insets(18));
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        return content;
    }

    private Node buildTargetTab() {
        configureTargetTable();
        batchTarget.setPromptText("目标日均");
        batchTarget.setMaxWidth(140);
        Button apply = primaryButton("应用到全部");
        apply.setOnAction(event -> applyBatchTarget());
        Button one = secondaryButton("1 元");
        one.setOnAction(event -> batchTarget.setText("1"));
        Button five = secondaryButton("5 元");
        five.setOnAction(event -> batchTarget.setText("5"));
        Button ten = secondaryButton("10 元");
        ten.setOnAction(event -> batchTarget.setText("10"));
        HBox controls = new HBox(10, new Label("批量目标"), batchTarget, new Label("元/天"), apply, one, five, ten);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(14, titleLabel("目标换新模型"), controls, targetTable);
        content.setPadding(new Insets(18));
        return content;
    }

    private Node buildStatisticsTab() {
        configureModelTable();
        aiAnalysis.setPromptText("点击 AI 辅助分析后显示本地 qwen3:8b 生成的分析。");
        aiAnalysis.setWrapText(true);
        aiAnalysis.setPrefRowCount(12);
        aiAnalysis.setText(lastAiAnalysisText());
        ollamaEndpoint.setPrefWidth(240);
        ollamaModel.setPrefWidth(130);

        Button aiButton = primaryButton("整体分析");
        aiButton.setOnAction(event -> runAiAnalysis(aiButton, OllamaAnalysisService.AnalysisFocus.OVERALL, null));
        Button targetAi = secondaryButton("目标换新分析");
        targetAi.setOnAction(event -> runAiAnalysis(targetAi, OllamaAnalysisService.AnalysisFocus.REPLACEMENT, null));
        Button budgetAi = secondaryButton("预算风险分析");
        budgetAi.setOnAction(event -> runAiAnalysis(budgetAi, OllamaAnalysisService.AnalysisFocus.BUDGET, null));
        Button copyAi = secondaryButton("复制分析结果");
        copyAi.setOnAction(event -> copyToClipboard(aiAnalysis.getText()));

        HBox aiControls = new HBox(10, new Label("Ollama"), ollamaEndpoint, new Label("模型"), ollamaModel, aiButton, targetAi, budgetAi, copyAi);
        aiControls.setAlignment(Pos.CENTER_LEFT);
        VBox aiBox = new VBox(10, titleLabel("本地 AI 分析"), aiControls, aiAnalysis);
        aiBox.getStyleClass().add("device-card");

        ScrollPane scroll = new ScrollPane(new VBox(16, analysisBox, modelTable, aiBox));
        scroll.setFitToWidth(true);
        VBox content = new VBox(14, titleLabel("统计分析"), scroll);
        content.setPadding(new Insets(18));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return content;
    }

    private Node buildChartsTab() {
        ScrollPane scroll = new ScrollPane(chartsBox);
        scroll.setFitToWidth(true);
        VBox content = new VBox(14, titleLabel("模型图表"), scroll);
        content.setPadding(new Insets(18));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return content;
    }

    private Node buildImportExportTab() {
        Button textImport = primaryButton("批量导入文本/CSV");
        textImport.setOnAction(event -> showTextImportDialog(null));
        Button fileImport = secondaryButton("选择 CSV 文件导入");
        fileImport.setOnAction(event -> importCsvFile());
        Button importSample = secondaryButton("导入示例数据");
        importSample.setOnAction(event -> showTextImportDialog(SAMPLE_CSV));
        Button importJson = secondaryButton("导入 JSON 备份");
        importJson.setOnAction(event -> importJson());
        Button exportJson = secondaryButton("导出 JSON 备份");
        exportJson.setOnAction(event -> exportJson());
        Button exportCsv = secondaryButton("导出 CSV 明细");
        exportCsv.setOnAction(event -> exportCsv());
        Button openDir = secondaryButton("打开数据目录");
        openDir.setOnAction(event -> {
            try {
                AppPaths.openDataDirectory();
            } catch (RuntimeException e) {
                showError("打开数据目录失败", e.getMessage());
            }
        });
        Button uninstall = dangerButton("卸载/应用设置");
        uninstall.setOnAction(event -> {
            try {
                AppPaths.openWindowsAppsSettings();
            } catch (RuntimeException e) {
                showError("打开卸载入口失败", e.getMessage());
            }
        });
        HBox buttons = new HBox(10, textImport, fileImport, importSample, importJson, exportJson, exportCsv, openDir, uninstall);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(16, titleLabel("导入导出"), buttons, importExportInfo);
        content.setPadding(new Insets(18));
        return content;
    }

    private void configureOverviewTable() {
        setupTable(overviewTable, 360,`r`n                column("状态", this::deviceStatusText),`r`n                column("设备", DeviceCostSnapshot::name),
                column("总投入", s -> FormatUtil.yuan(s.totalInvestment())),
                column("当前日均", s -> FormatUtil.yuanPerDay(s.currentDailyCost())),
                column("目标", s -> FormatUtil.yuanPerDay(s.targetDailyCost())),
                column("还需", s -> s.targetPlan().achieved() ? "已达成" : FormatUtil.money(s.targetPlan().remainingDays()) + " 天"),
                column("30天下降", s -> FormatUtil.yuanPerDay(s.targetPlan().thirtyDayDecrease())),
                column("替换指数", s -> s.targetPlan().replacementIndex() + "/100"),
                column("建议", s -> s.targetPlan().advice())
        );
    }

    private void configureTargetTable() {
        setupTable(targetTable, 520,`r`n                column("状态", this::deviceStatusText),`r`n                column("设备", DeviceCostSnapshot::name),
                column("当前日均", s -> FormatUtil.yuanPerDay(s.currentDailyCost())),
                column("目标日均", s -> FormatUtil.yuanPerDay(s.targetDailyCost())),
                column("还需多久", s -> s.targetPlan().achieved() ? "已达成" : FormatUtil.money(s.targetPlan().remainingDays()) + " 天"),
                column("达成日期", s -> FormatUtil.date(s.targetPlan().estimatedDate())),
                column("30天下降", s -> FormatUtil.yuanPerDay(s.targetPlan().thirtyDayDecrease())),
                column("替换指数", s -> s.targetPlan().replacementIndex() + "/100"),
                column("建议", s -> s.targetPlan().advice())
        );
    }

    private void configureModelTable() {
        setupTable(modelTable, 360,`r`n                column("状态", this::deviceStatusText),`r`n                column("设备", DeviceCostSnapshot::name),
                column("总投入", s -> FormatUtil.yuan(s.totalInvestment())),
                column("加权使用", s -> FormatUtil.money(s.weightedUsedDays()) + " 天"),
                column("当前日均", s -> FormatUtil.yuanPerDay(s.currentDailyCost())),
                column("目标日均", s -> FormatUtil.yuanPerDay(s.targetDailyCost())),
                column("边际下降/天", s -> FormatUtil.yuanPerDay(s.targetPlan().marginalDecreasePerDay())),
                column("替换指数", s -> s.targetPlan().replacementIndex() + "/100")
        );
    }

    @SafeVarargs
    private final void setupTable(TableView<DeviceCostSnapshot> table, double height, TableColumn<DeviceCostSnapshot, String>... columns) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(height);
        table.getColumns().setAll(columns);
    }

    private TableColumn<DeviceCostSnapshot, String> column(String title, SnapshotValue value) {
        TableColumn<DeviceCostSnapshot, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.get(data.getValue())));
        return column;
    }

    private String deviceStatusText(DeviceCostSnapshot snapshot) {
        return snapshot.replaced()
                ? "已换新 " + FormatUtil.date(snapshot.replacementDate())
                : "使用中";
    }

    private void refresh() {
        appData.getDevices().sort(Comparator.comparingInt(Device::getSortOrder));
        SummarySnapshot summary = calculatorService.calculateSummary(appData, LocalDate.now());
        refreshStats(summary);
        overviewTable.setItems(FXCollections.observableArrayList(summary.devices()));
        targetTable.setItems(FXCollections.observableArrayList(summary.devices()));
        modelTable.setItems(FXCollections.observableArrayList(summary.devices()));
        refreshDeviceCards(summary.devices());
        refreshAnalysis(summary);
        refreshCharts(summary);
        refreshImportExportInfo(summary);
    }

    private void refreshStats(SummarySnapshot summary) {
        BigDecimal smoothDaily30 = activeProjectedPortfolioDaily(summary, 30);
        statsBar.getChildren().setAll(
                statCard("设备", summary.deviceCount() + " 台"),
                statCard("配件", summary.accessoryCount() + " 个"),
                statCard("累计投入", FormatUtil.yuan(summary.totalInvestment())),
                statCard("当前活跃日均", FormatUtil.yuanPerDay(summary.totalDailyCost())),
                statCard("30天平滑日均", FormatUtil.yuanPerDay(smoothDaily30)),
                statCard("平滑月摊", FormatUtil.yuan(smoothDaily30.multiply(new BigDecimal("30.4167"))) + "/月"),
                statCard("达成目标", summary.achievedTargetCount() + " 台")
        );
    }

    private void refreshDeviceCards(List<DeviceCostSnapshot> snapshots) {
        deviceCards.getChildren().clear();
        if (snapshots.isEmpty()) {
            VBox empty = new VBox(12, new Label("暂无设备。点击顶部“添加设备”开始记录。"), sampleButton());
            empty.getStyleClass().add("empty-state");
            deviceCards.getChildren().add(empty);
            return;
        }
        for (DeviceCostSnapshot snapshot : snapshots) {
            Device device = findDevice(snapshot.id());
            if (device != null) {
                deviceCards.getChildren().add(deviceCard(device, snapshot));
            }
        }
    }

    private void refreshAnalysis(SummarySnapshot summary) {
        TilePane cards = new TilePane(12, 12,
                plainStat("总资产摊销", FormatUtil.yuanPerDay(summary.totalDailyCost())),
                plainStat("平均加权使用", averageWeightedDays(summary) + " 天"),
                plainStat("目标达成率", summary.achievedTargetCount() + "/" + summary.deviceCount())
        );
        cards.getChildren().addAll(
                plainStat("配件投入占比", accessoryInvestmentRatio(summary)),
                plainStat("30天自然下降", FormatUtil.yuanPerDay(totalThirtyDayDecrease(summary))),
                plainStat("365天预测日均", FormatUtil.yuanPerDay(projectedPortfolioDaily(summary, 365)))
        );
        cards.getChildren().addAll(
                plainStat("较昨天变化", historicalChangeText(1)),
                plainStat("较3天前变化", historicalChangeText(3)),
                plainStat("较7天前变化", historicalChangeText(7)),
                plainStat("较30天前变化", historicalChangeText(30))
        );
        cards.setPrefColumns(3);

        Label text = new Label(buildExtendedAnalysisText(summary));
        text.setWrapText(true);
        text.getStyleClass().add("notice");
        analysisBox.getChildren().setAll(cards, text);
    }

    private void refreshCharts(SummarySnapshot summary) {
        chartsBox.getChildren().clear();
        if (summary.devices().isEmpty()) {
            chartsBox.getChildren().add(new Label("暂无数据，添加设备后显示图表。"));
            return;
        }
        refreshChartDeviceFilters(summary.devices());
        List<DeviceCostSnapshot> selectedDevices = selectedChartDevices(summary.devices());
        chartsBox.getChildren().addAll(
                chartCard("图表设备筛选", chartDeviceFilters),
                chartCard("日均排行", dailyRankingChart(selectedDevices)),
                chartCard("投入构成", investmentPieChart(selectedDevices)),
                chartCard("目标等待时间", targetWaitingChart(selectedDevices)),
                chartCard("未来日均曲线", futureDailyLineChart(selectedDevices))
        );
    }

    private void refreshChartDeviceFilters(List<DeviceCostSnapshot> devices) {
        normalizeChartSelection(devices);

        Button all = secondaryButton("全选");
        all.setOnAction(event -> {
            selectedChartDeviceIds.clear();
            devices.forEach(device -> selectedChartDeviceIds.add(device.id()));
            refreshCharts(calculatorService.calculateSummary(appData, LocalDate.now()));
        });

        Button topThree = secondaryButton("只看前三");
        topThree.setOnAction(event -> {
            selectedChartDeviceIds.clear();
            devices.stream()
                    .sorted(Comparator.comparing(DeviceCostSnapshot::currentDailyCost).reversed())
                    .limit(3)
                    .forEach(device -> selectedChartDeviceIds.add(device.id()));
            refreshCharts(calculatorService.calculateSummary(appData, LocalDate.now()));
        });
        Button chartAi = primaryButton("AI 解读所选设备");
        chartAi.setOnAction(event -> runAiAnalysis(
                chartAi,
                OllamaAnalysisService.AnalysisFocus.CHART_SELECTION,
                selectedChartDevices(devices)
        ));

        HBox actions = new HBox(8, all, topThree, chartAi);
        actions.setAlignment(Pos.CENTER_LEFT);

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Label("勾选要显示的设备；勾选多个时，未来日均曲线会额外显示组合合计。"));
        nodes.add(actions);
        for (DeviceCostSnapshot device : devices) {
            CheckBox box = new CheckBox(device.name());
            box.setSelected(selectedChartDeviceIds.contains(device.id()));
            box.setOnAction(event -> {
                if (box.isSelected()) {
                    if (!selectedChartDeviceIds.contains(device.id())) {
                        selectedChartDeviceIds.add(device.id());
                    }
                } else if (selectedChartDeviceIds.size() > 1) {
                    selectedChartDeviceIds.remove(device.id());
                } else {
                    box.setSelected(true);
                    return;
                }
                refreshCharts(calculatorService.calculateSummary(appData, LocalDate.now()));
            });
            nodes.add(box);
        }
        chartDeviceFilters.getChildren().setAll(nodes);
    }

    private void normalizeChartSelection(List<DeviceCostSnapshot> devices) {
        selectedChartDeviceIds.removeIf(id -> devices.stream().noneMatch(device -> device.id().equals(id)));
        if (selectedChartDeviceIds.isEmpty()) {
            devices.forEach(device -> selectedChartDeviceIds.add(device.id()));
        }
    }

    private List<DeviceCostSnapshot> selectedChartDevices(List<DeviceCostSnapshot> devices) {
        return devices.stream()
                .filter(device -> selectedChartDeviceIds.contains(device.id()))
                .toList();
    }

    private void refreshImportExportInfo(SummarySnapshot summary) {
        importExportInfo.getChildren().setAll(
                new Label("当前数据文件：" + dataStore.getDataFile()),
                new Label("设备数量：" + summary.deviceCount()),
                new Label("配件数量：" + summary.accessoryCount()),
                new Label("主题：" + appData.getSettings().getThemeName()),
                new Label("保存格式版本：" + appData.getSettings().getSaveFormatVersion())
        );
    }

    private Node deviceCard(Device device, DeviceCostSnapshot snapshot) {
        Label name = new Label(device.getName());
        name.getStyleClass().add("card-title");
        name.setTooltip(new Tooltip(device.getName()));
        name.setWrapText(true);

        Label summary = new Label("""
                配件数量：%d
                设备本体：%s | %s | 已使用 %d 天 | 本体日均 %s
                配件合计：%s | 配件日均 %s
                目标换新：目标 %s | %s | 预计 %s
                边际下降：%s | 再用30天预计下降 %s
                当前总日均：%s | 替换指数：%d/100 | %s
                """.formatted(
                device.getAccessories().size(),
                FormatUtil.yuan(device.getBasePrice()),
                FormatUtil.date(device.getPurchaseDate()),
                snapshot.baseUsedDays(),
                FormatUtil.yuanPerDay(snapshot.baseDailyCost()),
                FormatUtil.yuan(snapshot.accessoriesInvestment()),
                FormatUtil.yuanPerDay(snapshot.accessoriesDailyCost()),
                FormatUtil.yuanPerDay(snapshot.targetDailyCost()),
                snapshot.targetPlan().achieved() ? "已达成" : "还需 " + FormatUtil.money(snapshot.targetPlan().remainingDays()) + " 天",
                FormatUtil.date(snapshot.targetPlan().estimatedDate()),
                FormatUtil.yuanPerDay(snapshot.targetPlan().marginalDecreasePerDay()),
                FormatUtil.yuanPerDay(snapshot.targetPlan().thirtyDayDecrease()),
                FormatUtil.yuanPerDay(snapshot.currentDailyCost()),
                snapshot.targetPlan().replacementIndex(),
                snapshot.targetPlan().advice()
        ));
        summary.getStyleClass().add("card-body");

        VBox accessories = new VBox(6);
        for (Accessory accessory : device.getAccessories()) {
            accessories.getChildren().add(accessoryRow(device, accessory));
        }

        Button edit = secondaryButton("编辑");
        edit.setOnAction(event -> showDeviceDialog(device).ifPresent(updated -> {
            copyDevice(updated, device);
            saveAndRefresh();
        }));
        Button delete = dangerButton("删除");
        delete.setOnAction(event -> {
            if (confirm("删除设备", "确认删除“" + device.getName() + "”？该设备下的配件也会删除。")) {
                appData.getDevices().remove(device);
                normalizeSortOrder();
                saveAndRefresh();
            }
        });
        Button up = secondaryButton("上移");
        up.setOnAction(event -> moveDevice(device, -1));
        Button down = secondaryButton("下移");
        down.setOnAction(event -> moveDevice(device, 1));
        Button addAccessory = secondaryButton("+ 添加附件");
        addAccessory.setOnAction(event -> showAccessoryDialog(device, null).ifPresent(accessory -> {
            device.getAccessories().add(accessory);
            device.setUpdatedAt(LocalDateTime.now());
            saveAndRefresh();
        }));
        Button batchAccessory = secondaryButton("批量附件");
        batchAccessory.setOnAction(event -> showBatchAccessoryDialog(device));
        Button target = secondaryButton("设置目标");
        target.setOnAction(event -> showTargetDialog(device));
        Button replacement = device.isReplaced() ? secondaryButton("恢复使用") : dangerButton("标记已换新");
        replacement.setOnAction(event -> toggleReplacement(device));
        Button deviceAi = secondaryButton("AI 分析此设备");
        deviceAi.setOnAction(event -> runAiAnalysis(
                deviceAi,
                OllamaAnalysisService.AnalysisFocus.SINGLE_DEVICE,
                List.of(snapshot)
        ));

        HBox actions = new HBox(8, edit, delete, up, down, addAccessory, batchAccessory, target, replacement, deviceAi);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(10, name, summary, accessories, actions);
        card.getStyleClass().add("device-card");
        return card;
    }

    private Node accessoryRow(Device device, Accessory accessory) {
        DeviceCostSnapshot snapshot = calculatorService.calculateDevice(device, LocalDate.now());
        var accessorySnapshot = snapshot.accessories().stream()
                .filter(item -> item.id().equals(accessory.getId()))
                .findFirst();
        Label label = new Label("%s | %s | %s | 已使用 %s | %s".formatted(
                accessory.getName(),
                FormatUtil.yuan(accessory.getPrice()),
                FormatUtil.date(accessory.getPurchaseDate()),
                accessorySnapshot.map(item -> item.usedDays() + " 天").orElse("-"),
                accessorySnapshot.map(item -> FormatUtil.yuanPerDay(item.dailyCost())).orElse("-")
        ));
        label.setTooltip(new Tooltip(accessory.getName()));
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        Button edit = secondaryButton("编辑");
        edit.setOnAction(event -> showAccessoryDialog(device, accessory).ifPresent(updated -> {
            copyAccessory(updated, accessory);
            device.setUpdatedAt(LocalDateTime.now());
            saveAndRefresh();
        }));
        Button delete = dangerButton("删除");
        delete.setOnAction(event -> {
            if (confirm("删除配件", "确认删除配件“" + accessory.getName() + "”？")) {
                device.getAccessories().remove(accessory);
                device.setUpdatedAt(LocalDateTime.now());
                saveAndRefresh();
            }
        });
        HBox row = new HBox(8, label, edit, delete);
        row.getStyleClass().add("accessory-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Optional<Device> showDeviceDialog(Device source) {
        boolean editing = source != null;
        Dialog<Device> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑设备" : "添加设备");
        TextField name = new TextField(editing ? source.getName() : "");
        TextField price = new TextField(editing ? source.getBasePrice().toPlainString() : "");
        DatePicker purchaseDate = new DatePicker(editing ? source.getPurchaseDate() : LocalDate.now());
        TextField target = new TextField(editing ? source.getTargetDailyCost().toPlainString() : appData.getSettings().getDefaultTargetDailyCost().toPlainString());
        GridPane form = formGrid();
        form.addRow(0, new Label("设备名称"), name);
        form.addRow(1, new Label("设备价格"), price);
        form.addRow(2, new Label("购买日期"), purchaseDate);
        form.addRow(3, new Label("目标日均"), target);
        dialog.getDialogPane().setContent(form);
        ButtonType ok = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateDevice(name.getText(), price.getText(), target.getText(), purchaseDate.getValue());
            if (error != null) {
                showError("输入有误", error);
                event.consume();
            } else if (purchaseDate.getValue().isAfter(LocalDate.now()) && !confirm("确认未来日期", "购买日期晚于今天，确认继续保存？")) {
                event.consume();
            }
        });
        dialog.setResultConverter(type -> {
            if (type != ok) {
                return null;
            }
            Device device = editing ? cloneDevice(source) : new Device();
            device.setName(name.getText().trim());
            device.setBasePrice(new BigDecimal(price.getText().trim()));
            device.setPurchaseDate(purchaseDate.getValue());
            device.setTargetDailyCost(new BigDecimal(target.getText().trim()));
            device.setUpdatedAt(LocalDateTime.now());
            return device;
        });
        return dialog.showAndWait();
    }

    private Optional<Accessory> showAccessoryDialog(Device fixedOwner, Accessory source) {
        boolean editing = source != null;
        Dialog<Accessory> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑配件" : "添加一次性附属配件");
        ComboBox<Device> ownerBox = new ComboBox<>(FXCollections.observableArrayList(appData.getDevices()));
        ownerBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Device device) {
                return device == null ? "" : device.getName();
            }

            @Override
            public Device fromString(String string) {
                return appData.getDevices().stream().filter(device -> device.getName().equals(string)).findFirst().orElse(null);
            }
        });
        ownerBox.setValue(fixedOwner == null ? appData.getDevices().stream().findFirst().orElse(null) : fixedOwner);
        ownerBox.setDisable(fixedOwner != null);
        TextField name = new TextField(editing ? source.getName() : "");
        TextField price = new TextField(editing ? source.getPrice().toPlainString() : "");
        DatePicker purchaseDate = new DatePicker(editing ? source.getPurchaseDate() : LocalDate.now());
        GridPane form = formGrid();
        form.addRow(0, new Label("所属设备"), ownerBox);
        form.addRow(1, new Label("配件名称"), name);
        form.addRow(2, new Label("配件价格"), price);
        form.addRow(3, new Label("购买日期"), purchaseDate);
        dialog.getDialogPane().setContent(form);
        ButtonType ok = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateAccessory(ownerBox.getValue(), name.getText(), price.getText(), purchaseDate.getValue());
            if (error != null) {
                showError("输入有误", error);
                event.consume();
            } else if (purchaseDate.getValue().isAfter(LocalDate.now()) && !confirm("确认未来日期", "购买日期晚于今天，确认继续保存？")) {
                event.consume();
            }
        });
        dialog.setResultConverter(type -> {
            if (type != ok) {
                return null;
            }
            Accessory accessory = editing ? cloneAccessory(source) : new Accessory();
            accessory.setName(name.getText().trim());
            accessory.setPrice(new BigDecimal(price.getText().trim()));
            accessory.setPurchaseDate(purchaseDate.getValue());
            accessory.setUpdatedAt(LocalDateTime.now());
            if (!editing && fixedOwner == null) {
                ownerBox.getValue().getAccessories().add(accessory);
                saveAndRefresh();
                return null;
            }
            return accessory;
        });
        return dialog.showAndWait();
    }

    private void showBatchAccessoryDialog(Device device) {
        Dialog<List<Accessory>> dialog = new Dialog<>();
        dialog.setTitle("批量添加附件");
        TextArea rows = new TextArea("""
                手机壳,39,2,2026-06-12
                钢化膜,15,3,2026-06-12
                充电器,99,1,2026-06-12
                """);
        rows.setPrefRowCount(10);
        rows.setWrapText(false);
        CheckBox mergeQuantity = new CheckBox("同类数量合并为一条记录");
        mergeQuantity.setSelected(true);
        Label hint = new Label("每行格式：附件名称, 单价, 数量, 购买日期。数量和日期可省略，默认数量 1、日期今天。");
        hint.setWrapText(true);
        VBox content = new VBox(8, hint, rows, mergeQuantity);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        ButtonType ok = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                parseBatchAccessories(rows.getText(), mergeQuantity.isSelected());
            } catch (IllegalArgumentException e) {
                showError("批量附件格式有误", e.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(type -> type == ok ? parseBatchAccessories(rows.getText(), mergeQuantity.isSelected()) : null);
        dialog.showAndWait().ifPresent(accessories -> {
            device.getAccessories().addAll(accessories);
            device.setUpdatedAt(LocalDateTime.now());
            saveAndRefresh();
            showInfo("已添加 " + accessories.size() + " 条附件记录");
        });
    }

    private List<Accessory> parseBatchAccessories(String text, boolean mergeQuantity) {
        List<Accessory> accessories = new ArrayList<>();
        String[] lines = text == null ? new String[0] : text.split("\\R");
        int lineNumber = 0;
        for (String rawLine : lines) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.replace('，', ',').split(",");
            if (parts.length < 2) {
                throw new IllegalArgumentException("第 " + lineNumber + " 行至少需要：名称, 单价");
            }
            String name = parts[0].trim();
            String priceText = parts[1].trim();
            String quantityText = parts.length >= 3 ? parts[2].trim() : "1";
            String dateText = parts.length >= 4 ? parts[3].trim() : "";
            if (name.isBlank()) {
                throw new IllegalArgumentException("第 " + lineNumber + " 行附件名称不能为空");
            }
            if (!isPositiveDecimal(priceText)) {
                throw new IllegalArgumentException("第 " + lineNumber + " 行单价必须大于 0");
            }
            int quantity;
            try {
                quantity = quantityText.isBlank() ? 1 : Integer.parseInt(quantityText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("第 " + lineNumber + " 行数量必须是整数");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("第 " + lineNumber + " 行数量必须大于 0");
            }
            LocalDate purchaseDate;
            try {
                purchaseDate = dateText.isBlank() ? LocalDate.now() : LocalDate.parse(dateText);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("第 " + lineNumber + " 行日期必须是 yyyy-MM-dd");
            }
            BigDecimal unitPrice = new BigDecimal(priceText);
            if (mergeQuantity) {
                String mergedName = quantity > 1 ? name + " x" + quantity : name;
                accessories.add(new Accessory(mergedName, unitPrice.multiply(BigDecimal.valueOf(quantity)), purchaseDate));
            } else {
                for (int i = 1; i <= quantity; i++) {
                    String itemName = quantity > 1 ? name + " " + i + "/" + quantity : name;
                    accessories.add(new Accessory(itemName, unitPrice, purchaseDate));
                }
            }
        }
        if (accessories.isEmpty()) {
            throw new IllegalArgumentException("请至少填写一行附件");
        }
        return accessories;
    }

    private void showTargetDialog(Device device) {
        TextField target = new TextField(device.getTargetDailyCost().toPlainString());
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("设置目标日均");
        dialog.getDialogPane().setContent(new VBox(8, new Label("目标日均（元/天）"), target));
        ButtonType ok = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!isPositiveDecimal(target.getText())) {
                showError("输入有误", "目标日均必须大于 0");
                event.consume();
            }
        });
        dialog.setResultConverter(type -> type == ok ? new BigDecimal(target.getText().trim()) : null);
        dialog.showAndWait().ifPresent(value -> {
            device.setTargetDailyCost(value);
            device.setUpdatedAt(LocalDateTime.now());
            saveAndRefresh();
        });
    }

    private void applyBatchTarget() {
        if (!isPositiveDecimal(batchTarget.getText())) {
            showError("输入有误", "目标日均必须大于 0");
            return;
        }
        try {
            targetPlanService.applyTargetDailyCostToAll(appData, new BigDecimal(batchTarget.getText().trim()));
            saveAndRefresh();
        } catch (IllegalArgumentException e) {
            showError("输入有误", e.getMessage());
        }
    }

    private BarChart<Number, String> dailyRankingChart(List<DeviceCostSnapshot> devices) {
        NumberAxis xAxis = new NumberAxis();
        CategoryAxis yAxis = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(xAxis, yAxis);
        styleChart(chart);
        chart.setLegendVisible(false);
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        devices.stream().sorted(Comparator.comparing(DeviceCostSnapshot::currentDailyCost).reversed()).forEach(snapshot ->
                series.getData().add(new XYChart.Data<>(snapshot.currentDailyCost().doubleValue(), snapshot.name())));
        chart.getData().add(series);
        chart.setPrefHeight(Math.max(260, devices.size() * 42));
        return chart;
    }

    private PieChart investmentPieChart(List<DeviceCostSnapshot> devices) {
        PieChart chart = new PieChart();
        styleChart(chart);
        BigDecimal totalInvestment = devices.stream()
                .map(DeviceCostSnapshot::totalInvestment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (DeviceCostSnapshot snapshot : devices) {
            BigDecimal percent = totalInvestment.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : snapshot.totalInvestment().multiply(new BigDecimal("100")).divide(totalInvestment, 2, RoundingMode.HALF_UP);
            chart.getData().add(new PieChart.Data(snapshot.name() + " " + percent + "%", snapshot.totalInvestment().doubleValue()));
        }
        chart.setTitle("总投入：" + FormatUtil.yuan(totalInvestment));
        chart.setPrefHeight(360);
        return chart;
    }

    private BarChart<Number, String> targetWaitingChart(List<DeviceCostSnapshot> devices) {
        NumberAxis xAxis = new NumberAxis();
        CategoryAxis yAxis = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(xAxis, yAxis);
        styleChart(chart);
        chart.setLegendVisible(false);
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        devices.forEach(snapshot -> series.getData().add(new XYChart.Data<>(snapshot.targetPlan().remainingDays().doubleValue(), snapshot.name())));
        chart.getData().add(series);
        chart.setPrefHeight(Math.max(260, devices.size() * 42));
        return chart;
    }

    private LineChart<Number, Number> futureDailyLineChart(List<DeviceCostSnapshot> devices) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("未来天数");
        yAxis.setLabel("日均成本");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        styleChart(chart);
        chart.setLegendVisible(true);
        int[] days = {0, 30, 60, 90, 180, 270, 365};
        for (DeviceCostSnapshot device : devices) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(device.name());
            for (int day : days) {
                BigDecimal daily = projectedDeviceDaily(device, day);
                series.getData().add(new XYChart.Data<>(day, daily.doubleValue()));
            }
            chart.getData().add(series);
        }
        if (devices.size() > 1) {
            XYChart.Series<Number, Number> aggregate = new XYChart.Series<>();
            aggregate.setName("已选组合合计");
            for (int day : days) {
                BigDecimal daily = devices.stream()
                        .map(device -> projectedDeviceDaily(device, day))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                aggregate.getData().add(new XYChart.Data<>(day, daily.doubleValue()));
            }
            chart.getData().add(aggregate);
        }
        chart.setPrefHeight(360);
        return chart;
    }

    private Node chartCard(String title, Node chart) {
        VBox box = new VBox(8, titleLabel(title), chart);
        box.getStyleClass().add("device-card");
        return box;
    }

    private void styleChart(Node chart) {
        chart.setStyle("-fx-text-fill: -app-text;");
    }

    private void showTextImportDialog(String preset) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("批量导入文本/CSV");
        TextArea textArea = new TextArea(preset == null ? "" : preset);
        textArea.setPrefSize(760, 360);
        textArea.setPromptText("粘贴 CSV 内容");
        dialog.getDialogPane().setContent(textArea);
        ButtonType preview = new ButtonType("预览并导入", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(preview, ButtonType.CANCEL);
        dialog.setResultConverter(type -> type == preview ? textArea.getText() : null);
        dialog.showAndWait().ifPresent(this::importCsvText);
    }

    private void importCsvText(String text) {
        try {
            CsvImportExportService.ImportResult result = csvService.parse(text, appData);
            if (confirm("导入预览", "将导入设备 " + result.deviceCount() + " 台，配件 " + result.accessoryCount() + " 个。确认导入？")) {
                csvService.applyImport(appData, result);
                saveAndRefresh();
            }
        } catch (CsvImportException e) {
            showError("导入失败", e.getMessage());
        }
    }

    private void importCsvFile() {
        FileChooser chooser = fileChooser("选择 CSV 文件", "CSV 文件", "*.csv");
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            importCsvText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            showError("读取 CSV 失败", e.getMessage());
        }
    }

    private void exportCsv() {
        FileChooser chooser = fileChooser("导出 CSV 明细", "CSV 文件", "*.csv");
        chooser.setInitialFileName("daily-cost-details.csv");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), csvService.exportDetails(appData, LocalDate.now()), StandardCharsets.UTF_8);
            showInfo("CSV 已导出：" + file);
        } catch (IOException e) {
            showError("导出 CSV 失败", e.getMessage());
        }
    }

    private void exportJson() {
        FileChooser chooser = fileChooser("导出 JSON 备份", "JSON 文件", "*.json");
        chooser.setInitialFileName("daily-cost-backup.json");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            dataStore.exportBackup(file.toPath(), appData);
            showInfo("JSON 备份已导出：" + file);
        } catch (StorageException e) {
            showError("导出 JSON 失败", e.getMessage());
        }
    }

    private void importJson() {
        FileChooser chooser = fileChooser("导入 JSON 备份", "JSON 文件", "*.json");
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null || !confirm("导入 JSON", "导入后会替换当前 data.json，确认继续？")) {
            return;
        }
        try {
            AppData imported = dataStore.importBackup(file.toPath());
            appData.setVersion(imported.getVersion());
            appData.setSettings(imported.getSettings());
            appData.setDevices(imported.getDevices());
            saveAndRefresh();
        } catch (StorageException e) {
            showError("导入 JSON 失败", e.getMessage());
        }
    }

    private FileChooser fileChooser(String title, String extensionName, String pattern) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extensionName, pattern));
        return chooser;
    }

    private void applyTheme() {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().removeIf(item -> item.contains("theme-"));
        scene.getStylesheets().add(resource(themeFile(appData.getSettings().getThemeName())));
    }

    private String themeFile(String themeName) {
        return switch (themeName) {
            case "浅色简洁" -> "/theme-light.css";
            case "深色专业" -> "/theme-dark-professional.css";
            case "科技蓝" -> "/theme-tech-blue.css";
            case "护眼绿" -> "/theme-comfort-green.css";
            case "高对比" -> "/theme-high-contrast.css";
            default -> "/theme-dark-purple.css";
        };
    }

    private String resource(String path) {
        return getClass().getResource(path).toExternalForm();
    }

    private void runAiAnalysis(Button button, OllamaAnalysisService.AnalysisFocus focus, List<DeviceCostSnapshot> selectedDevices) {
        SummarySnapshot summary = calculatorService.calculateSummary(appData, LocalDate.now());
        button.setDisable(true);
        aiAnalysis.setText("正在调用本地 Ollama，请稍候...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return ollamaService.analyze(ollamaEndpoint.getText(), ollamaModel.getText(), summary, focus, selectedDevices);
            }
        };
        task.setOnSucceeded(event -> {
            String result = formatAiOutput(task.getValue());
            String timestamp = FormatUtil.date(LocalDate.now()) + " " + java.time.LocalTime.now().withNano(0);
            appData.getSettings().setLastAiAnalysis("[" + focus.title() + "]" + System.lineSeparator() + result);
            appData.getSettings().setLastAiAnalysisAt(timestamp);
            try {
                dataStore.save(appData);
            } catch (StorageException e) {
                showError("保存 AI 分析失败", e.getMessage());
            }
            aiAnalysis.setText(lastAiAnalysisText());
            button.setDisable(false);
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            aiAnalysis.setText(error == null ? "AI 分析失败" : error.getMessage());
            button.setDisable(false);
        });

        Thread thread = new Thread(task, "ollama-analysis");
        thread.setDaemon(true);
        thread.start();
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String lastAiAnalysisText() {
        String text = appData.getSettings().getLastAiAnalysis();
        if (text.isBlank()) {
            return "";
        }
        String time = appData.getSettings().getLastAiAnalysisAt();
        return (time.isBlank() ? "上次 AI 分析" : "上次 AI 分析：" + time)
                + System.lineSeparator()
                + System.lineSeparator()
                + text;
    }

    private String formatAiOutput(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean previousBlank = false;
        for (String rawLine : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine.trim()
                    .replace("**", "")
                    .replace("__", "");
            line = line.replaceFirst("^#{1,6}\\s*", "");
            line = line.replaceFirst("^[-*]\\s+", "- ");
            if (line.matches("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?$")) {
                continue;
            }
            if (line.startsWith("|") && line.endsWith("|")) {
                String[] cells = line.substring(1, line.length() - 1).split("\\|");
                List<String> cleanedCells = new ArrayList<>();
                for (String cell : cells) {
                    if (!cell.trim().isBlank()) {
                        cleanedCells.add(cell.trim());
                    }
                }
                line = String.join("  |  ", cleanedCells);
            }
            if (line.isBlank()) {
                if (!previousBlank) {
                    builder.append(System.lineSeparator());
                }
                previousBlank = true;
            } else {
                builder.append(line).append(System.lineSeparator());
                previousBlank = false;
            }
        }
        return builder.toString().trim();
    }

    private String buildExtendedAnalysisText(SummarySnapshot summary) {
        if (summary.devices().isEmpty()) {
            return buildAnalysisText(summary);
        }
        return buildAnalysisText(summary) + System.lineSeparator() + """
                未来预测：7 天后组合日均约 %s，30 天后约 %s，90 天后约 %s，365 天后约 %s。
                风险提醒：%s
                换新建议：%s
                """.formatted(
                FormatUtil.yuanPerDay(projectedPortfolioDaily(summary, 7)),
                FormatUtil.yuanPerDay(projectedPortfolioDaily(summary, 30)),
                FormatUtil.yuanPerDay(projectedPortfolioDaily(summary, 90)),
                FormatUtil.yuanPerDay(projectedPortfolioDaily(summary, 365)),
                riskAdvice(summary),
                replacementAdvice(summary)
        );
    }

    private BigDecimal totalThirtyDayDecrease(SummarySnapshot summary) {
        return summary.devices().stream()
                .map(device -> device.targetPlan().thirtyDayDecrease())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal projectedPortfolioDaily(SummarySnapshot summary, int extraDays) {
        return summary.devices().stream()
                .filter(device -> !device.replaced())
                .map(device -> projectedDeviceDaily(device, extraDays))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal activeProjectedPortfolioDaily(SummarySnapshot summary, int extraDays) {
        return projectedPortfolioDaily(summary, extraDays);
    }

    private String historicalChangeText(int daysAgo) {
        LocalDate date = LocalDate.now().minusDays(daysAgo);
        SummarySnapshot past = calculatorService.calculateSummary(appData, date);
        SummarySnapshot current = calculatorService.calculateSummary(appData, LocalDate.now());
        BigDecimal diff = current.totalDailyCost().subtract(past.totalDailyCost());
        String prefix = diff.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return prefix + FormatUtil.yuanPerDay(diff) + " / " + percent(diff.abs(), past.totalDailyCost());
    }

    private BigDecimal projectedDeviceDaily(DeviceCostSnapshot device, int extraDays) {
        if (device.replaced()) {
            return device.currentDailyCost();
        }
        BigDecimal total = divideByDays(device.basePrice(), device.baseUsedDays() + extraDays);
        for (AccessoryCostSnapshot accessory : device.accessories()) {
            total = total.add(divideByDays(accessory.price(), accessory.usedDays() + extraDays));
        }
        return total;
    }

    private BigDecimal divideByDays(BigDecimal amount, long days) {
        long safeDays = Math.max(1, days);
        return amount.divide(BigDecimal.valueOf(safeDays), 10, RoundingMode.HALF_UP);
    }

    private String accessoryInvestmentRatio(SummarySnapshot summary) {
        BigDecimal accessories = summary.devices().stream()
                .map(DeviceCostSnapshot::accessoriesInvestment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return percent(accessories, summary.totalInvestment());
    }

    private String riskAdvice(SummarySnapshot summary) {
        DeviceCostSnapshot highestDaily = summary.devices().stream()
                .max(Comparator.comparing(DeviceCostSnapshot::currentDailyCost))
                .orElseThrow();
        String highestDailyRatio = percent(highestDaily.currentDailyCost(), summary.totalDailyCost());
        long notAchieved = summary.devices().stream().filter(device -> !device.targetPlan().achieved()).count();
        if (highestDaily.currentDailyCost().multiply(new BigDecimal("100"))
                .compareTo(summary.totalDailyCost().multiply(new BigDecimal("35"))) >= 0) {
            return highestDaily.name() + " 当前日均占比 " + highestDailyRatio + "，组合成本集中度偏高；仍有 "
                    + notAchieved + " 台设备未达目标。";
        }
        return "组合成本集中度可控；仍有 " + notAchieved + " 台设备未达目标，建议继续观察高日均设备。";
    }

    private String replacementAdvice(SummarySnapshot summary) {
        return summary.devices().stream()
                .filter(device -> !device.targetPlan().achieved())
                .max(Comparator.comparing(device -> device.targetPlan().replacementIndex()))
                .map(device -> device.name() + " 的换新指数最高，为 " + device.targetPlan().replacementIndex()
                        + "/100，预计达标日期 " + device.targetPlan().estimatedDate() + "。")
                .orElse("所有设备已达到当前目标日均，可以按实际体验和预算决定是否换新。");
    }

    private String buildAnalysisText(SummarySnapshot summary) {
        if (summary.devices().isEmpty()) {
            return "暂无设备数据。";
        }
        DeviceCostSnapshot highestDaily = summary.devices().stream().max(Comparator.comparing(DeviceCostSnapshot::currentDailyCost)).orElseThrow();
        DeviceCostSnapshot highestInvestment = summary.devices().stream().max(Comparator.comparing(DeviceCostSnapshot::totalInvestment)).orElseThrow();
        String dailyPercent = percent(highestDaily.currentDailyCost(), summary.totalDailyCost());
        String investmentPercent = percent(highestInvestment.totalInvestment(), summary.totalInvestment());
        BigDecimal thirtyDayDecrease = summary.devices().stream()
                .map(device -> device.targetPlan().thirtyDayDecrease())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return """
                当前日均最高的是“%s”：%s，占全部设备日均的 %s。

                累计投入最高的是“%s”：%s，占总投入的 %s。

                如果未来 30 天不新增设备/配件，组合合计日均预计自然下降约 %s。

                当前组合的等效年化使用成本约 %s，可用于和“每年换新预算”做对比。
                """.formatted(
                highestDaily.name(), FormatUtil.yuanPerDay(highestDaily.currentDailyCost()), dailyPercent,
                highestInvestment.name(), FormatUtil.yuan(highestInvestment.totalInvestment()), investmentPercent,
                FormatUtil.yuanPerDay(thirtyDayDecrease), FormatUtil.yuan(summary.equivalentAnnualCost())
        );
    }

    private String percent(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return "0.00%";
        }
        return part.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP) + "%";
    }

    private String averageWeightedDays(SummarySnapshot summary) {
        if (summary.devices().isEmpty()) {
            return "0.00";
        }
        BigDecimal total = summary.devices().stream().map(DeviceCostSnapshot::weightedUsedDays).reduce(BigDecimal.ZERO, BigDecimal::add);
        return FormatUtil.money(total.divide(BigDecimal.valueOf(summary.devices().size()), 10, RoundingMode.HALF_UP));
    }

    private Node statCard(String title, String value) {
        VBox card = new VBox(5, label(title, "stat-title"), label(value, "stat-value"));
        card.getStyleClass().add("stat-card");
        card.setMinWidth(150);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Node plainStat(String title, String value) {
        VBox card = new VBox(5, label(title, "stat-title-plain"), label(value, "stat-value-plain"));
        card.getStyleClass().add("device-card");
        card.setMinWidth(220);
        return card;
    }

    private Label titleLabel(String text) {
        return label(text, "section-title");
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private Button sampleButton() {
        Button button = secondaryButton("导入示例数据");
        button.setOnAction(event -> showTextImportDialog(SAMPLE_CSV));
        return button;
    }

    private GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        return form;
    }

    private String validateDevice(String name, String price, String target, LocalDate date) {
        if (name == null || name.isBlank()) return "设备名称不能为空";
        if (!isPositiveDecimal(price)) return "设备价格必须大于 0";
        if (!isPositiveDecimal(target)) return "目标日均必须大于 0";
        if (date == null) return "购买日期不能为空";
        return null;
    }

    private String validateAccessory(Device owner, String name, String price, LocalDate date) {
        if (owner == null) return "所属设备不能为空";
        if (name == null || name.isBlank()) return "配件名称不能为空";
        if (!isPositiveDecimal(price)) return "配件价格必须大于 0";
        if (date == null) return "购买日期不能为空";
        return null;
    }

    private boolean isPositiveDecimal(String text) {
        try {
            return new BigDecimal(text.trim()).compareTo(BigDecimal.ZERO) > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void moveDevice(Device device, int delta) {
        List<Device> devices = appData.getDevices();
        int index = devices.indexOf(device);
        int targetIndex = index + delta;
        if (index < 0 || targetIndex < 0 || targetIndex >= devices.size()) return;
        Device other = devices.get(targetIndex);
        devices.set(targetIndex, device);
        devices.set(index, other);
        normalizeSortOrder();
        saveAndRefresh();
    }

    private void toggleReplacement(Device device) {
        if (device.isReplaced()) {
            if (!confirm("恢复使用", "确认将“" + device.getName() + "”恢复为使用中？")) {
                return;
            }
            device.setReplaced(false);
            device.setReplacementDate(null);
        } else {
            if (!confirm("标记已换新", "确认将“" + device.getName() + "”标记为已换新？标记后它会保留记录，但不再计入当前活跃日均。")) {
                return;
            }
            device.setReplaced(true);
            device.setReplacementDate(LocalDate.now());
        }
        device.setUpdatedAt(LocalDateTime.now());
        saveAndRefresh();
    }

    private void normalizeSortOrder() {
        for (int i = 0; i < appData.getDevices().size(); i++) {
            appData.getDevices().get(i).setSortOrder(i);
        }
    }

    private Device findDevice(String id) {
        return appData.getDevices().stream().filter(device -> device.getId().equals(id)).findFirst().orElse(null);
    }

    private Device cloneDevice(Device source) {
        Device device = new Device();
        copyDevice(source, device);
        return device;
    }

    private void copyDevice(Device source, Device target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setBasePrice(source.getBasePrice());
        target.setPurchaseDate(source.getPurchaseDate());
        target.setTargetDailyCost(source.getTargetDailyCost());
        target.setAccessories(source.getAccessories());
        target.setReplaced(source.isReplaced());
        target.setReplacementDate(source.getReplacementDate());
        target.setSortOrder(source.getSortOrder());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private Accessory cloneAccessory(Accessory source) {
        Accessory accessory = new Accessory();
        copyAccessory(source, accessory);
        return accessory;
    }

    private void copyAccessory(Accessory source, Accessory target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setPrice(source.getPrice());
        target.setPurchaseDate(source.getPurchaseDate());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private void saveAndRefresh() {
        try {
            dataStore.save(appData);
            refresh();
        } catch (StorageException e) {
            showError("保存失败", e.getMessage());
        }
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private Button dangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger-button");
        return button;
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface SnapshotValue {
        String get(DeviceCostSnapshot snapshot);
    }
}
