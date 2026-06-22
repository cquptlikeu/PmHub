package com.laigeoffer.pmhub.project.ai.rule.impl;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.project.ai.client.WorkflowRiskClient;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowBlockedRiskRuleTest {

    @Mock
    private WorkflowRiskClient workflowRiskClient;

    @InjectMocks
    private WorkflowBlockedRiskRule workflowBlockedRiskRule;

    @Test
    void shouldCreateHighRiskWhenProjectWorkflowBlocked() {
        ProjectWorkflowRiskSummaryVO summary = new ProjectWorkflowRiskSummaryVO();
        summary.setProjectId("project-1");
        summary.setProjectApprovalBlocked(Boolean.TRUE);
        summary.setTaskApprovalBlockedCount(2);
        summary.setTotalBlockedCount(3);
        summary.setSummary("项目发布审批进行中，另有2个任务审批进行中");
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(summary);

        ProjectRiskAnalysisContext context = ProjectRiskAnalysisContext.builder()
                .projectId("project-1")
                .analysisTaskId("analysis-1")
                .operator("tester")
                .analyzeTime(new Date())
                .build();

        List<ProjectRiskRecord> records = workflowBlockedRiskRule.evaluate(context);

        assertEquals(1, records.size());
        ProjectRiskRecord record = records.get(0);
        assertEquals(RiskType.WORKFLOW_BLOCKED.getCode(), record.getRiskType());
        assertEquals(RiskLevel.HIGH.getCode(), record.getRiskLevel());
        assertEquals("PROJECT", record.getSourceType());
        assertEquals("project-1", record.getSourceId());
        assertTrue(record.getReason().contains("审批进行中"));
    }

    @Test
    void shouldReturnEmptyListWhenWorkflowIsNotBlocked() {
        ProjectWorkflowRiskSummaryVO summary = new ProjectWorkflowRiskSummaryVO();
        summary.setProjectId("project-1");
        summary.setProjectApprovalBlocked(Boolean.FALSE);
        summary.setTaskApprovalBlockedCount(0);
        summary.setTotalBlockedCount(0);
        summary.setSummary("当前无审批卡点");
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(summary);

        ProjectRiskAnalysisContext context = ProjectRiskAnalysisContext.builder()
                .projectId("project-1")
                .analysisTaskId("analysis-1")
                .operator("tester")
                .analyzeTime(new Date())
                .build();

        List<ProjectRiskRecord> records = workflowBlockedRiskRule.evaluate(context);

        assertTrue(records.isEmpty());
    }
}
