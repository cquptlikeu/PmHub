package com.laigeoffer.pmhub.api.workflow;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.api.workflow.factory.WorkflowRiskFeignFallbackFactory;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.constant.ServiceNameConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "workflowRiskFeignService", value = ServiceNameConstants.WORKFLOW_SERVICE,
        fallbackFactory = WorkflowRiskFeignFallbackFactory.class)
public interface WorkflowRiskFeignService {

    @GetMapping("/workflow/risk/projectSummary/{projectId}")
    R<ProjectWorkflowRiskSummaryVO> getProjectRiskSummary(@PathVariable("projectId") String projectId,
                                                          @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
