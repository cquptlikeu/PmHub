package com.laigeoffer.pmhub.workflow.controller;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.security.annotation.InnerAuth;
import com.laigeoffer.pmhub.workflow.service.impl.WorkflowRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/risk")
public class WorkflowRiskController {

    private final WorkflowRiskService workflowRiskService;

    @InnerAuth
    @GetMapping("/projectSummary/{projectId}")
    public R<ProjectWorkflowRiskSummaryVO> getProjectRiskSummary(@PathVariable("projectId") String projectId) {
        return R.ok(workflowRiskService.getProjectRiskSummary(projectId));
    }
}
