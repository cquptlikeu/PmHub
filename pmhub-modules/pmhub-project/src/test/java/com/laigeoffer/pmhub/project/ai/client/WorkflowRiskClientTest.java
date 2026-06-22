package com.laigeoffer.pmhub.project.ai.client;

import com.laigeoffer.pmhub.api.workflow.WorkflowRiskFeignService;
import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowRiskClientTest {

    @Mock
    private WorkflowRiskFeignService workflowRiskFeignService;

    @InjectMocks
    private WorkflowRiskClient workflowRiskClient;

    @Test
    void shouldReturnRemoteSummaryWhenFeignSuccess() {
        ProjectWorkflowRiskSummaryVO summary = new ProjectWorkflowRiskSummaryVO();
        summary.setProjectId("project-1");
        summary.setSummary("项目发布审批进行中");
        when(workflowRiskFeignService.getProjectRiskSummary("project-1", SecurityConstants.INNER))
                .thenReturn(R.ok(summary));

        ProjectWorkflowRiskSummaryVO result = workflowRiskClient.getProjectRiskSummary("project-1");

        assertEquals("项目发布审批进行中", result.getSummary());
        assertEquals("project-1", result.getProjectId());
    }

    @Test
    void shouldReturnDefaultSummaryWhenFeignFails() {
        when(workflowRiskFeignService.getProjectRiskSummary("project-1", SecurityConstants.INNER))
                .thenReturn(R.fail("remote error"));

        ProjectWorkflowRiskSummaryVO result = workflowRiskClient.getProjectRiskSummary("project-1");

        assertEquals("project-1", result.getProjectId());
        assertEquals("当前无审批卡点", result.getSummary());
        assertFalse(Boolean.TRUE.equals(result.getProjectApprovalBlocked()));
        assertEquals(Integer.valueOf(0), result.getTaskApprovalBlockedCount());
    }
}
