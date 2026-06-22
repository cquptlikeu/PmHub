package com.laigeoffer.pmhub.project.ai.rule.impl;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.utils.uuid.IdUtils;
import com.laigeoffer.pmhub.project.ai.client.WorkflowRiskClient;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskStatus;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.rule.ProjectRiskRule;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class WorkflowBlockedRiskRule implements ProjectRiskRule {

    private static final String PROJECT_SOURCE_TYPE = "PROJECT";

    private final WorkflowRiskClient workflowRiskClient;

    public WorkflowBlockedRiskRule(WorkflowRiskClient workflowRiskClient) {
        this.workflowRiskClient = workflowRiskClient;
    }

    @Override
    public List<ProjectRiskRecord> evaluate(ProjectRiskAnalysisContext context) {
        ProjectWorkflowRiskSummaryVO summary = workflowRiskClient.getProjectRiskSummary(context.getProjectId());
        if (summary == null || summary.getTotalBlockedCount() == null || summary.getTotalBlockedCount() <= 0) {
            return Collections.emptyList();
        }
        return Collections.singletonList(buildRisk(context, summary));
    }

    private ProjectRiskRecord buildRisk(ProjectRiskAnalysisContext context, ProjectWorkflowRiskSummaryVO summary) {
        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setId(IdUtils.fastUUID());
        record.setProjectId(context.getProjectId());
        record.setAnalysisTaskId(context.getAnalysisTaskId());
        record.setRiskType(RiskType.WORKFLOW_BLOCKED.getCode());
        record.setRiskLevel(Boolean.TRUE.equals(summary.getProjectApprovalBlocked())
                ? RiskLevel.HIGH.getCode() : RiskLevel.MEDIUM.getCode());
        record.setSourceType(PROJECT_SOURCE_TYPE);
        record.setSourceId(context.getProjectId());
        record.setTitle("流程审批卡点");
        record.setReason(summary.getSummary());
        record.setSuggestion("优先推进未完成审批，必要时协调审批人处理积压");
        record.setStatus(RiskStatus.OPEN.getCode());
        record.setCreatedBy(context.getOperator());
        record.setCreatedTime(context.getAnalyzeTime());
        record.setUpdatedBy(context.getOperator());
        record.setUpdatedTime(context.getAnalyzeTime());
        return record;
    }
}
