package com.laigeoffer.pmhub.project.ai.rule.impl;

import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.base.core.utils.uuid.IdUtils;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskStatus;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.rule.ProjectRiskRule;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class BlockedTaskRiskRule implements ProjectRiskRule {

    private static final String TASK_SOURCE_TYPE = "TASK";

    @Override
    public List<ProjectRiskRecord> evaluate(ProjectRiskAnalysisContext context) {
        if (context.getProjectTasks() == null || context.getProjectTasks().isEmpty()) {
            return Collections.emptyList();
        }
        return context.getProjectTasks().stream()
                .filter(this::isBlockedTask)
                .map(task -> buildRisk(context, task))
                .collect(Collectors.toList());
    }

    private boolean isBlockedTask(ProjectTask task) {
        return task.getUserId() == null
                || Objects.equals(ProjectTaskStatusEnum.NO_CLAIMED.getStatus(), task.getStatus())
                || Objects.equals(ProjectTaskStatusEnum.NO_CLAIMED.getStatus(), task.getExecuteStatus());
    }

    private ProjectRiskRecord buildRisk(ProjectRiskAnalysisContext context, ProjectTask task) {
        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setId(IdUtils.fastUUID());
        record.setProjectId(context.getProjectId());
        record.setAnalysisTaskId(context.getAnalysisTaskId());
        record.setRiskType(RiskType.BLOCKED_TASK.getCode());
        record.setRiskLevel(RiskLevel.MEDIUM.getCode());
        record.setSourceType(TASK_SOURCE_TYPE);
        record.setSourceId(task.getId());
        record.setTitle("任务阻塞: " + task.getTaskName());
        record.setReason("任务当前无人认领或处于待认领状态");
        record.setSuggestion("尽快明确负责人并恢复任务流转");
        record.setStatus(RiskStatus.OPEN.getCode());
        record.setCreatedBy(context.getOperator());
        record.setCreatedTime(context.getAnalyzeTime());
        record.setUpdatedBy(context.getOperator());
        record.setUpdatedTime(context.getAnalyzeTime());
        return record;
    }
}
