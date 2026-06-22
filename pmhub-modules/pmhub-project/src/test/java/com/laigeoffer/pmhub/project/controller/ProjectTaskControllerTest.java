package com.laigeoffer.pmhub.project.controller;

import com.laigeoffer.pmhub.api.workflow.DeployFeignService;
import com.laigeoffer.pmhub.api.workflow.ProcessFeignService;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.AjaxResult;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.project.service.ProjectTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectTaskControllerTest {

    @Mock
    private ProjectTaskService projectTaskService;

    @Mock
    private ProcessFeignService processService;

    @Mock
    private DeployFeignService wfDeployService;

    @InjectMocks
    private ProjectTaskController controller;

    @Test
    void shouldReturnWorkflowErrorMessageWhenApprovalSetUpdateFails() {
        when(wfDeployService.updateApprovalSet2(any(ApprovalSetDTO.class), eq(SecurityConstants.INNER)))
            .thenAnswer(invocation -> R.fail("已通过的流程不允许修改审批设置"));

        AjaxResult result = controller.updateApprovalSet(new ApprovalSetDTO());

        assertEquals("已通过的流程不允许修改审批设置", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnSuccessWhenApprovalSetUpdateSucceeds() {
        when(wfDeployService.updateApprovalSet2(any(ApprovalSetDTO.class), eq(SecurityConstants.INNER)))
            .thenAnswer(invocation -> R.ok(Boolean.TRUE));

        AjaxResult result = controller.updateApprovalSet(new ApprovalSetDTO());

        assertEquals("操作成功", result.get(AjaxResult.MSG_TAG));
    }
}
