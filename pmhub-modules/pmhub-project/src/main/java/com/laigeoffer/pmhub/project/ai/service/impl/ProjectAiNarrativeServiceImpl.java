package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.base.core.utils.DateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laigeoffer.pmhub.project.ai.client.AiModelClient;
import com.laigeoffer.pmhub.project.ai.constant.HealthLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.dto.ProjectWeeklyReportDraft;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiNarrativeService;
import com.laigeoffer.pmhub.project.ai.util.AiContentSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectAiNarrativeServiceImpl implements ProjectAiNarrativeService {

    private static final String DEFAULT_WORKFLOW_SUMMARY = "当前无审批卡点";
    private static final String NO_ANALYSIS_SUMMARY = "暂无分析结果，建议先执行项目分析。";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiModelClient aiModelClient;

    public ProjectAiNarrativeServiceImpl() {
    }

    ProjectAiNarrativeServiceImpl(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    @Autowired(required = false)
    public void setAiModelClient(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    @Override
    public String buildSummary(ProjectHealthSnapshot snapshot, List<ProjectRiskRecord> risks, String workflowSummary) {
        String normalizedWorkflowSummary = normalizeWorkflowSummary(workflowSummary);
        if (snapshot == null) {
            return AiContentSanitizer.sanitizeOutput(NO_ANALYSIS_SUMMARY + "流程情况：" + normalizedWorkflowSummary);
        }

        String localSummary = buildLocalSummary(snapshot, risks, normalizedWorkflowSummary);
        Map<String, Object> structuredInput = buildStructuredContent(
                snapshot.getProjectId(), null, null, snapshot, risks, normalizedWorkflowSummary,
                extractTopRiskTitles(risks, 3), localSummary);
        return AiContentSanitizer.sanitizeOutput(generateModelSummary(structuredInput).orElse(localSummary));
    }

    @Override
    public ProjectWeeklyReportDraft buildWeeklyReport(String projectId, Date weekStart, Date weekEnd,
                                                      ProjectHealthSnapshot snapshot, List<ProjectRiskRecord> risks,
                                                      String workflowSummary) {
        String normalizedWorkflowSummary = normalizeWorkflowSummary(workflowSummary);
        List<String> titles = extractTopRiskTitles(risks, 5);
        String localContent = buildWeeklyContent(weekStart, weekEnd, snapshot, titles, normalizedWorkflowSummary);
        String localSummary = snapshot == null
                ? NO_ANALYSIS_SUMMARY + "流程情况：" + normalizedWorkflowSummary
                : buildLocalSummary(snapshot, risks, normalizedWorkflowSummary);
        Map<String, Object> structured = buildStructuredContent(
                projectId, weekStart, weekEnd, snapshot, risks, normalizedWorkflowSummary, titles, localSummary);
        Optional<String> modelContent = generateModelWeeklyReport(structured);
        boolean modelGenerated = modelContent.isPresent();
        String content = AiContentSanitizer.sanitizeOutput(modelContent.orElse(localContent));
        structured.put("generatedContent", content);
        return ProjectWeeklyReportDraft.builder()
                .content(content)
                .structuredContent(toJson(structured))
                .modelGenerated(modelGenerated)
                .build();
    }

    private String buildLocalSummary(ProjectHealthSnapshot snapshot, List<ProjectRiskRecord> risks,
                                     String normalizedWorkflowSummary) {
        List<String> titles = extractTopRiskTitles(risks, 3);
        StringBuilder builder = new StringBuilder();
        builder.append("当前项目健康度").append(resolveHealthLevelDesc(snapshot.getHealthLevel()))
                .append("（").append(snapshot.getHealthScore()).append("分），共识别")
                .append(nullSafe(snapshot.getRiskCount())).append("项风险，其中高风险")
                .append(nullSafe(snapshot.getHighRiskCount())).append("项。");
        if (!titles.isEmpty()) {
            builder.append("主要风险：").append(String.join("、", titles)).append("。");
        }
        builder.append("流程情况：").append(normalizedWorkflowSummary).append("。");
        builder.append("建议优先处理").append(resolvePriorityAction(titles, normalizedWorkflowSummary)).append("。");
        return builder.toString();
    }

    private String buildWeeklyContent(Date weekStart, Date weekEnd, ProjectHealthSnapshot snapshot,
                                      List<String> titles, String workflowSummary) {
        StringBuilder builder = new StringBuilder();
        builder.append("本周项目AI周报（")
                .append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, weekStart))
                .append(" 至 ")
                .append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, weekEnd))
                .append("）\n");
        if (snapshot == null) {
            builder.append("1. 健康概况：暂无分析结果，建议先执行项目分析。\n");
            builder.append("2. 流程状态：").append(workflowSummary).append("\n");
            builder.append("3. 建议动作：先触发项目分析任务，再根据结果安排整改。");
            return builder.toString();
        }

        builder.append("1. 健康概况：健康度").append(resolveHealthLevelDesc(snapshot.getHealthLevel()))
                .append("，得分").append(snapshot.getHealthScore()).append("，共")
                .append(nullSafe(snapshot.getRiskCount())).append("项风险，高风险")
                .append(nullSafe(snapshot.getHighRiskCount())).append("项。\n");
        builder.append("2. 重点风险：")
                .append(titles.isEmpty() ? "本周未识别新增重点风险。" : String.join("、", titles))
                .append("\n");
        builder.append("3. 流程状态：").append(workflowSummary).append("\n");
        builder.append("4. 建议动作：").append(resolvePriorityAction(titles, workflowSummary)).append("。");
        return builder.toString();
    }

    private Map<String, Object> buildStructuredContent(String projectId, Date weekStart, Date weekEnd,
                                                       ProjectHealthSnapshot snapshot, List<ProjectRiskRecord> risks,
                                                       String workflowSummary, List<String> titles,
                                                       String generatedSummary) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("projectId", projectId);
        structured.put("weekStart", weekStart == null ? null : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, weekStart));
        structured.put("weekEnd", weekEnd == null ? null : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, weekEnd));
        structured.put("analysisTaskId", snapshot == null ? null : snapshot.getAnalysisTaskId());
        structured.put("healthScore", snapshot == null ? null : snapshot.getHealthScore());
        structured.put("healthLevel", snapshot == null ? null : snapshot.getHealthLevel());
        structured.put("riskCount", snapshot == null ? 0 : snapshot.getRiskCount());
        structured.put("highRiskCount", snapshot == null ? 0 : snapshot.getHighRiskCount());
        structured.put("deductionDetail", snapshot == null ? null : snapshot.getDeductionDetail());
        structured.put("workflowSummary", workflowSummary);
        structured.put("topRiskTitles", titles);
        structured.put("riskTitles", extractTopRiskTitles(risks, Integer.MAX_VALUE));
        structured.put("riskRecords", buildStructuredRiskRecords(risks));
        structured.put("riskJudgementSource", "SYSTEM_RULES");
        structured.put("modelConstraint", "模型只能基于 riskRecords 和健康快照生成文案，不允许新增、删除、推断或重新判断风险。");
        structured.put("generatedSummary", generatedSummary);
        return structured;
    }

    private List<Map<String, Object>> buildStructuredRiskRecords(List<ProjectRiskRecord> risks) {
        if (risks == null || risks.isEmpty()) {
            return Collections.emptyList();
        }
        return risks.stream().map(risk -> {
            Map<String, Object> riskItem = new LinkedHashMap<>();
            riskItem.put("riskType", risk.getRiskType());
            riskItem.put("riskLevel", risk.getRiskLevel());
            riskItem.put("sourceType", risk.getSourceType());
            riskItem.put("sourceId", risk.getSourceId());
            riskItem.put("title", AiContentSanitizer.sanitizePromptText(risk.getTitle()));
            riskItem.put("reason", AiContentSanitizer.sanitizePromptText(risk.getReason()));
            riskItem.put("suggestion", AiContentSanitizer.sanitizePromptText(risk.getSuggestion()));
            riskItem.put("status", risk.getStatus());
            return riskItem;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private Optional<String> generateModelSummary(Map<String, Object> structuredInput) {
        if (aiModelClient == null) {
            return Optional.empty();
        }
        try {
            Optional<String> result = aiModelClient.generateSummary(structuredInput);
            return result == null ? Optional.empty() : result;
        } catch (Exception exception) {
            log.warn("AI summary generation failed, fallback to local narrative. {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> generateModelWeeklyReport(Map<String, Object> structuredInput) {
        if (aiModelClient == null) {
            return Optional.empty();
        }
        try {
            Optional<String> result = aiModelClient.generateWeeklyReport(structuredInput);
            return result == null ? Optional.empty() : result;
        } catch (Exception exception) {
            log.warn("AI weekly report generation failed, fallback to local narrative. {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private List<String> extractTopRiskTitles(List<ProjectRiskRecord> risks, int limit) {
        if (risks == null || risks.isEmpty()) {
            return Collections.emptyList();
        }
        return risks.stream()
                .sorted((left, right) -> Integer.compare(
                        resolveRiskLevelRank(right.getRiskLevel()), resolveRiskLevelRank(left.getRiskLevel())))
                .map(ProjectRiskRecord::getTitle)
                .filter(title -> title != null && !title.trim().isEmpty())
                .limit(limit)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int resolveRiskLevelRank(String riskLevel) {
        if (RiskLevel.HIGH.getCode().equals(riskLevel)) {
            return 3;
        }
        if (RiskLevel.MEDIUM.getCode().equals(riskLevel)) {
            return 2;
        }
        return 1;
    }

    private String resolveHealthLevelDesc(String healthLevel) {
        if (HealthLevel.HEALTHY.getCode().equals(healthLevel)) {
            return HealthLevel.HEALTHY.getDesc();
        }
        if (HealthLevel.WARNING.getCode().equals(healthLevel)) {
            return HealthLevel.WARNING.getDesc();
        }
        if (HealthLevel.DANGER.getCode().equals(healthLevel)) {
            return HealthLevel.DANGER.getDesc();
        }
        return "未知";
    }

    private String resolvePriorityAction(List<String> titles, String workflowSummary) {
        if (!titles.isEmpty()) {
            return titles.get(0);
        }
        if (!DEFAULT_WORKFLOW_SUMMARY.equals(workflowSummary)) {
            return "当前审批卡点";
        }
        return "健康趋势复核";
    }

    private String normalizeWorkflowSummary(String workflowSummary) {
        return workflowSummary == null || workflowSummary.trim().isEmpty()
                ? DEFAULT_WORKFLOW_SUMMARY : workflowSummary;
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private String toJson(Map<String, Object> structured) {
        try {
            return OBJECT_MAPPER.writeValueAsString(structured);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("周报结构化内容序列化失败", exception);
        }
    }
}
