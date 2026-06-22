package com.laigeoffer.pmhub.project.ai.client;

import com.laigeoffer.pmhub.api.workflow.WorkflowRiskFeignService;
import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRiskClient {

    private final WorkflowRiskFeignService workflowRiskFeignService;

    public WorkflowRiskClient(WorkflowRiskFeignService workflowRiskFeignService) {
        this.workflowRiskFeignService = workflowRiskFeignService;
    }

    public ProjectWorkflowRiskSummaryVO getProjectRiskSummary(String projectId) {
        R<ProjectWorkflowRiskSummaryVO> response =
                workflowRiskFeignService.getProjectRiskSummary(projectId, SecurityConstants.INNER);
        if (response != null && R.isSuccess(response) && response.getData() != null) {
            return response.getData();
        }
        return buildDefaultSummary(projectId);
    }

    private ProjectWorkflowRiskSummaryVO buildDefaultSummary(String projectId) {
        ProjectWorkflowRiskSummaryVO summary = new ProjectWorkflowRiskSummaryVO();
        summary.setProjectId(projectId);
        summary.setProjectApprovalBlocked(Boolean.FALSE);
        summary.setTaskApprovalBlockedCount(0);
        summary.setTotalBlockedCount(0);
        summary.setSummary("当前无审批卡点");
        return summary;
    }
}
