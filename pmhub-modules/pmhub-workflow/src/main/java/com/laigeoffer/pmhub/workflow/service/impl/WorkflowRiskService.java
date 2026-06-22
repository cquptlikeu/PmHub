package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class WorkflowRiskService {

    private static final String APPROVAL_REQUIRED = "0";
    private static final String PROJECT_TYPE = "project";
    private static final String TASK_TYPE = "task";
    private static final String NO_BLOCKED_SUMMARY = "当前无审批卡点";

    private final WfTaskProcessMapper wfTaskProcessMapper;
    private final RuntimeService runtimeService;

    public WorkflowRiskService(WfTaskProcessMapper wfTaskProcessMapper, RuntimeService runtimeService) {
        this.wfTaskProcessMapper = wfTaskProcessMapper;
        this.runtimeService = runtimeService;
    }

    public ProjectWorkflowRiskSummaryVO getProjectRiskSummary(String projectId) {
        List<WfTaskProcess> candidates = wfTaskProcessMapper.selectProjectRiskCandidates(projectId);
        boolean projectApprovalBlocked = false;
        int taskApprovalBlockedCount = 0;
        if (candidates != null) {
            for (WfTaskProcess candidate : candidates) {
                if (!APPROVAL_REQUIRED.equals(candidate.getApproved()) || !isActiveProcess(candidate.getInstanceId())) {
                    continue;
                }
                if (PROJECT_TYPE.equals(candidate.getType())) {
                    projectApprovalBlocked = true;
                } else if (TASK_TYPE.equals(candidate.getType())) {
                    taskApprovalBlockedCount++;
                }
            }
        }
        ProjectWorkflowRiskSummaryVO summary = new ProjectWorkflowRiskSummaryVO();
        summary.setProjectId(projectId);
        summary.setProjectApprovalBlocked(projectApprovalBlocked);
        summary.setTaskApprovalBlockedCount(taskApprovalBlockedCount);
        summary.setTotalBlockedCount((projectApprovalBlocked ? 1 : 0) + taskApprovalBlockedCount);
        summary.setSummary(buildSummary(projectApprovalBlocked, taskApprovalBlockedCount));
        return summary;
    }

    private boolean isActiveProcess(String instanceId) {
        if (instanceId == null || instanceId.trim().isEmpty()) {
            return false;
        }
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId)
                .singleResult() != null;
    }

    private String buildSummary(boolean projectApprovalBlocked, int taskApprovalBlockedCount) {
        if (!projectApprovalBlocked && taskApprovalBlockedCount <= 0) {
            return NO_BLOCKED_SUMMARY;
        }
        if (projectApprovalBlocked && taskApprovalBlockedCount > 0) {
            return "项目发布审批进行中，另有" + taskApprovalBlockedCount + "个任务审批进行中";
        }
        if (projectApprovalBlocked) {
            return "项目发布审批进行中";
        }
        return "有" + taskApprovalBlockedCount + "个任务审批进行中";
    }
}
