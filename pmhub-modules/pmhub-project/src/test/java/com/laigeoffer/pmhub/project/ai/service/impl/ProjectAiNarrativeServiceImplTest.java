package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.project.ai.client.AiModelClient;
import com.laigeoffer.pmhub.project.ai.constant.HealthLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskStatus;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.dto.ProjectWeeklyReportDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAiNarrativeServiceImplTest {

    @Mock
    private AiModelClient aiModelClient;

    private ProjectAiNarrativeServiceImpl projectAiNarrativeService;

    @BeforeEach
    void setUp() {
        projectAiNarrativeService = new ProjectAiNarrativeServiceImpl(aiModelClient);
    }

    @Test
    void shouldGenerateSummaryByModelWithStructuredRisks() {
        when(aiModelClient.generateSummary(any())).thenReturn(Optional.of("模型摘要"));

        String summary = projectAiNarrativeService.buildSummary(
                buildSnapshot(), Collections.singletonList(buildRisk()), "当前无审批卡点");

        assertEquals("模型摘要", summary);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(aiModelClient).generateSummary(captor.capture());
        Map structuredInput = captor.getValue();
        assertEquals("SYSTEM_RULES", structuredInput.get("riskJudgementSource"));
        assertEquals("project-1", structuredInput.get("projectId"));
        assertEquals(1, structuredInput.get("riskCount"));
        List riskRecords = (List) structuredInput.get("riskRecords");
        assertFalse(riskRecords.isEmpty());
        Map riskRecord = (Map) riskRecords.get(0);
        assertEquals(RiskType.DELAYED_TASK.getCode(), riskRecord.get("riskType"));
        assertEquals("任务已逾期", riskRecord.get("title"));
    }

    @Test
    void shouldFallbackToLocalSummaryWhenModelReturnsEmpty() {
        when(aiModelClient.generateSummary(any())).thenReturn(Optional.empty());

        String summary = projectAiNarrativeService.buildSummary(
                buildSnapshot(), Collections.singletonList(buildRisk()), "当前无审批卡点");

        assertTrue(summary.contains("当前项目健康度"));
        assertTrue(summary.contains("任务已逾期"));
    }

    @Test
    void shouldPersistStructuredRiskInputInWeeklyReport() {
        when(aiModelClient.generateWeeklyReport(any())).thenReturn(Optional.empty());

        ProjectWeeklyReportDraft draft = projectAiNarrativeService.buildWeeklyReport(
                "project-1",
                new Date(1714492800000L),
                new Date(1715097600000L),
                buildSnapshot(),
                Collections.singletonList(buildRisk()),
                "当前无审批卡点");

        assertTrue(draft.getStructuredContent().contains("\"riskJudgementSource\":\"SYSTEM_RULES\""));
        assertTrue(draft.getStructuredContent().contains("\"riskRecords\""));
        assertTrue(draft.getStructuredContent().contains(RiskType.DELAYED_TASK.getCode()));
        assertTrue(draft.getContent().contains("任务已逾期"));
    }

    private ProjectHealthSnapshot buildSnapshot() {
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setAnalysisTaskId("analysis-1");
        snapshot.setHealthScore(80);
        snapshot.setHealthLevel(HealthLevel.WARNING.getCode());
        snapshot.setRiskCount(1);
        snapshot.setHighRiskCount(1);
        snapshot.setDeductionDetail("任务已逾期 -20");
        return snapshot;
    }

    private ProjectRiskRecord buildRisk() {
        ProjectRiskRecord risk = new ProjectRiskRecord();
        risk.setRiskType(RiskType.DELAYED_TASK.getCode());
        risk.setRiskLevel(RiskLevel.HIGH.getCode());
        risk.setSourceType("TASK");
        risk.setSourceId("task-1");
        risk.setTitle("任务已逾期");
        risk.setReason("任务截止时间已超过当前时间，且任务未完成");
        risk.setSuggestion("优先处理逾期任务，重新评估负责人和排期");
        risk.setStatus(RiskStatus.OPEN.getCode());
        return risk;
    }
}
