package com.dailycost.service;

import com.dailycost.model.DeviceCostSnapshot;
import com.dailycost.model.SummarySnapshot;
import com.dailycost.util.FormatUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OllamaAnalysisService {
    public static final String DEFAULT_ENDPOINT = "http://localhost:11434";
    public static final String DEFAULT_MODEL = "qwen3:8b";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaAnalysisService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper());
    }

    OllamaAnalysisService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String analyze(SummarySnapshot summary) {
        return analyze(DEFAULT_ENDPOINT, DEFAULT_MODEL, summary);
    }

    public String analyze(String endpoint, String model, SummarySnapshot summary) {
        return analyze(endpoint, model, summary, AnalysisFocus.OVERALL, summary.devices());
    }

    public String analyze(String endpoint, String model, SummarySnapshot summary, AnalysisFocus focus, List<DeviceCostSnapshot> selectedDevices) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", model == null || model.isBlank() ? DEFAULT_MODEL : model);
            request.put("prompt", buildPrompt(summary, focus, selectedDevices));
            request.put("stream", false);
            request.put("system", """
                    你是严谨的个人设备成本分析助手。你只能基于用户给出的本地统计数据分析，不要编造不存在的交易、预算或折旧规则。
                    你必须正确理解：日均成本是历史投入按使用天数摊薄后的显示值，不是每天真实花掉的钱。
                    你必须正确理解：30天下降、90天下降是继续持有时自然摊薄带来的日均下降，不是换掉设备后立刻省下的钱。
                    你必须正确理解：换新指数=目标日均/当前日均*100，越高越接近目标；100表示已达标。低指数通常代表还没摊薄够，不等于应该立即替换。
                    输出中文纯文本，结论要保守、可执行，避免恐吓式措辞。
                    """);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeEndpoint(endpoint) + "/api/generate"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OllamaException("Ollama 返回状态码 " + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            JsonNode content = json.get("response");
            if (content == null || content.asText().isBlank()) {
                throw new OllamaException("Ollama 未返回分析内容");
            }
            return content.asText();
        } catch (IOException e) {
            throw new OllamaException("无法连接本地 Ollama。请确认已运行：ollama run qwen3:8b", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("AI 分析已中断", e);
        } catch (IllegalArgumentException e) {
            throw new OllamaException("Ollama 地址无效", e);
        }
    }

    public String buildPrompt(SummarySnapshot summary) {
        return buildPrompt(summary, AnalysisFocus.OVERALL, summary.devices());
    }

    public String buildPrompt(SummarySnapshot summary, AnalysisFocus focus, List<DeviceCostSnapshot> selectedDevices) {
        List<DeviceCostSnapshot> devices = selectedDevices == null || selectedDevices.isEmpty()
                ? summary.devices()
                : selectedDevices;

        StringBuilder builder = new StringBuilder();
        builder.append("分析场景：").append(focus.title()).append("\n\n");
        builder.append("核心口径，必须严格遵守：\n");
        builder.append("1. 当前日均=历史已支付投入/已使用天数，是摊销指标，不是每天现金支出。\n");
        builder.append("2. 30天自然下降=继续使用30天后，摊销日均自然下降的幅度；它不是卖掉或换掉设备能立刻省下的钱。\n");
        builder.append("3. 换新指数=目标日均/当前日均*100，并限制在1-100。指数越高越接近目标，100表示已达到目标。\n");
        builder.append("4. 低换新指数表示当前仍高于目标、还需要时间摊薄；不能因为指数低就直接建议立即换新。\n");
        builder.append("5. 还需天数表示继续持有并使用时，预计达到目标日均还需要的天数。\n");
        builder.append("6. 是否换新要结合：是否已达目标、剩余天数、当前体验、是否刚购买、是否有替代需求。缺少体验数据时只能建议观察或继续摊薄。\n\n");

        builder.append("总体数据：\n");
        builder.append("- 设备数：").append(summary.deviceCount()).append('\n');
        builder.append("- 配件数：").append(summary.accessoryCount()).append('\n');
        builder.append("- 累计投入：").append(FormatUtil.yuan(summary.totalInvestment())).append('\n');
        builder.append("- 当前合计日均：").append(FormatUtil.yuanPerDay(summary.totalDailyCost())).append('\n');
        builder.append("- 等效月摊销：").append(FormatUtil.yuan(summary.equivalentMonthlyCost())).append('\n');
        builder.append("- 等效年摊销：").append(FormatUtil.yuan(summary.equivalentAnnualCost())).append('\n');
        builder.append("- 达成目标：").append(summary.achievedTargetCount()).append('/').append(summary.deviceCount()).append("\n\n");

        builder.append("设备明细：\n");
        for (DeviceCostSnapshot device : devices) {
            builder.append("- ").append(device.name())
                    .append("；总投入=").append(FormatUtil.yuan(device.totalInvestment()))
                    .append("；当前日均=").append(FormatUtil.yuanPerDay(device.currentDailyCost()))
                    .append("；目标日均=").append(FormatUtil.yuanPerDay(device.targetDailyCost()))
                    .append("；已加权使用=").append(FormatUtil.money(device.weightedUsedDays())).append("天")
                    .append("；还需=").append(FormatUtil.money(device.targetPlan().remainingDays())).append("天")
                    .append("；预计达标=").append(FormatUtil.date(device.targetPlan().estimatedDate()))
                    .append("；30天自然下降=").append(FormatUtil.yuanPerDay(device.targetPlan().thirtyDayDecrease()))
                    .append("；边际每日下降=").append(FormatUtil.yuanPerDay(device.targetPlan().marginalDecreasePerDay()))
                    .append("；换新指数=").append(device.targetPlan().replacementIndex()).append("/100")
                    .append("；是否达标=").append(device.targetPlan().achieved() ? "是" : "否")
                    .append("；系统建议=").append(device.targetPlan().advice())
                    .append('\n');
        }

        builder.append("\n输出要求：\n");
        builder.append("- 使用纯文本，不要 Markdown 表格、加粗、分隔线或代码块。\n");
        builder.append("- 每个小节使用“1. 总体判断：”这种标题，每条建议单独换行。\n");
        builder.append("- 不要说“30天可节省X元/天”，只能说“继续持有30天后日均预计自然下降X元/天”。\n");
        builder.append("- 不要把低换新指数解释为必须立即替换；应解释为距离目标日均还远。\n");
        builder.append("- 必须把建议分成：继续持有摊薄、观察、考虑换新、已达标可自由决策。\n");
        builder.append("- 如果缺少设备体验、故障、出售价格、真实预算信息，不要假设这些信息。\n");
        builder.append("- 重点输出可执行建议，不要使用“成本失控”“严重预算风险”等夸张措辞，除非数据本身能证明真实现金流风险。\n\n");

        builder.append("请输出：\n");
        builder.append("1. 总体判断：说明这是摊销视角，不是真实每日消费。\n");
        builder.append("2. 需要关注的设备：说明为什么关注，是因为日均高、还需天数长，还是目标设置过严。\n");
        builder.append("3. 持有/换新建议：逐类给出继续摊薄、观察、考虑换新、已达标。\n");
        builder.append("4. 未来30/90/365天：只能描述自然摊薄趋势和复查节点。\n");
        builder.append("5. 预算建议：基于当前投入和目标日均，给出保守建议。\n");
        return builder.toString();
    }

    private String normalizeEndpoint(String endpoint) {
        String value = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public enum AnalysisFocus {
        OVERALL("整体成本分析"),
        REPLACEMENT("目标与换新分析"),
        BUDGET("预算风险分析"),
        CHART_SELECTION("图表所选设备分析"),
        SINGLE_DEVICE("单设备分析");

        private final String title;

        AnalysisFocus(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }
}
