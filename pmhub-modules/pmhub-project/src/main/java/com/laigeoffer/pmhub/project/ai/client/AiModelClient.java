package com.laigeoffer.pmhub.project.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.project.ai.config.AiModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AiModelClient {

    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是项目管理助手。你只能基于用户提供的 JSON 结构化分析结果生成中文总结。" +
                    "风险已经由系统规则识别完成，你不得新增、删除、推断或重新判断风险；" +
                    "不得输出 JSON；如果 riskCount 为 0，只能说明未识别到风险。";

    private static final String WEEKLY_SYSTEM_PROMPT =
            "你是项目周报助手。你只能基于用户提供的 JSON 结构化分析结果生成中文周报。" +
                    "风险已经由系统规则识别完成，你不得新增、删除、推断或重新判断风险；" +
                    "按健康概况、重点风险、流程状态、建议动作四段输出。";

    private final RestTemplate restTemplate;
    private final AiModelProperties properties;
    private final ObjectMapper objectMapper;

    public AiModelClient(@Qualifier("aiModelRestTemplate") RestTemplate restTemplate,
                         AiModelProperties properties,
                         ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<String> generateSummary(Map<String, Object> structuredInput) {
        return complete(SUMMARY_SYSTEM_PROMPT,
                "请基于下面 JSON 生成一段项目 AI 摘要，禁止判断输入之外的风险：\n" + toJson(structuredInput));
    }

    public Optional<String> generateWeeklyReport(Map<String, Object> structuredInput) {
        return complete(WEEKLY_SYSTEM_PROMPT,
                "请基于下面 JSON 生成项目 AI 周报，禁止判断输入之外的风险：\n" + toJson(structuredInput));
    }

    private Optional<String> complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey().trim());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    resolveChatCompletionsUrl(), new HttpEntity<>(requestBody, headers), Map.class);
            return extractContent(response.getBody());
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("AI model completion failed, fallback to local narrative. {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean isAvailable() {
        return properties != null
                && properties.isEnabled()
                && StringUtils.isNotBlank(properties.getBaseUrl())
                && StringUtils.isNotBlank(properties.getApiKey())
                && StringUtils.isNotBlank(properties.getModel());
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("temperature", properties.getTemperature());
        requestBody.put("max_tokens", properties.getMaxTokens());
        requestBody.put("messages", Arrays.asList(
                buildMessage("system", systemPrompt),
                buildMessage("user", userPrompt)
        ));
        return requestBody;
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Optional<String> extractContent(Map body) {
        if (body == null) {
            return Optional.empty();
        }
        Object choicesObject = body.get("choices");
        if (!(choicesObject instanceof List) || ((List<?>) choicesObject).isEmpty()) {
            return Optional.empty();
        }
        Object firstChoiceObject = ((List<?>) choicesObject).get(0);
        if (!(firstChoiceObject instanceof Map)) {
            return Optional.empty();
        }
        Object messageObject = ((Map<?, ?>) firstChoiceObject).get("message");
        if (!(messageObject instanceof Map)) {
            return Optional.empty();
        }
        Object contentObject = ((Map<?, ?>) messageObject).get("content");
        if (!(contentObject instanceof String) || StringUtils.isBlank((String) contentObject)) {
            return Optional.empty();
        }
        return Optional.of(((String) contentObject).trim());
    }

    private String resolveChatCompletionsUrl() {
        String baseUrl = trimRight(properties.getBaseUrl().trim(), "/");
        String path = StringUtils.isBlank(properties.getChatCompletionsPath())
                ? "/v1/chat/completions" : properties.getChatCompletionsPath().trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }

    private String trimRight(String value, String suffix) {
        while (value.endsWith(suffix)) {
            value = value.substring(0, value.length() - suffix.length());
        }
        return value;
    }

    private String toJson(Map<String, Object> structuredInput) {
        try {
            return objectMapper.writeValueAsString(structuredInput);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("AI 结构化输入序列化失败", exception);
        }
    }
}
