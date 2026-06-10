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
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", model == null || model.isBlank() ? DEFAULT_MODEL : model);
            request.put("prompt", buildPrompt(summary));
            request.put("stream", false);
            request.put("system", "你是一个严谨的个人设备成本分析助手。只基于用户给出的本地统计摘要分析，不要假设不存在的数据。输出中文，结构清晰，建议务实。");

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
        StringBuilder builder = new StringBuilder();
        builder.append("请分析以下个人设备日均成本数据，给出成本结构、换新优先级、预算风险、未来摊薄建议和需要重点关注的设备。\n\n");
        builder.append("总体：\n");
        builder.append("- 设备数：").append(summary.deviceCount()).append('\n');
        builder.append("- 配件数：").append(summary.accessoryCount()).append('\n');
        builder.append("- 累计投入：").append(FormatUtil.yuan(summary.totalInvestment())).append('\n');
        builder.append("- 合计日均：").append(FormatUtil.yuanPerDay(summary.totalDailyCost())).append('\n');
        builder.append("- 等效月租：").append(FormatUtil.yuan(summary.equivalentMonthlyCost())).append('\n');
        builder.append("- 等效年化：").append(FormatUtil.yuan(summary.equivalentAnnualCost())).append('\n');
        builder.append("- 达成目标：").append(summary.achievedTargetCount()).append('/').append(summary.deviceCount()).append("\n\n");
        builder.append("设备明细：\n");
        for (DeviceCostSnapshot device : summary.devices()) {
            builder.append("- ").append(device.name())
                    .append("，总投入 ").append(FormatUtil.yuan(device.totalInvestment()))
                    .append("，当前日均 ").append(FormatUtil.yuanPerDay(device.currentDailyCost()))
                    .append("，目标 ").append(FormatUtil.yuanPerDay(device.targetDailyCost()))
                    .append("，还需 ").append(FormatUtil.money(device.targetPlan().remainingDays())).append(" 天")
                    .append("，30天下降 ").append(FormatUtil.yuanPerDay(device.targetPlan().thirtyDayDecrease()))
                    .append("，替换指数 ").append(device.targetPlan().replacementIndex()).append("/100")
                    .append("，建议：").append(device.targetPlan().advice())
                    .append('\n');
        }
        builder.append("\n请输出：1. 总体判断；2. 风险设备；3. 换新优先级；4. 未来 30/90/365 天建议；5. 预算建议。");
        builder.append("\n输出格式要求：使用纯文本，不要使用 Markdown 标题、表格、加粗、分隔线或代码块。每个小节用“1. 总体判断：”这种标题，每条建议单独换行。");
        return builder.toString();
    }

    private String normalizeEndpoint(String endpoint) {
        String value = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
