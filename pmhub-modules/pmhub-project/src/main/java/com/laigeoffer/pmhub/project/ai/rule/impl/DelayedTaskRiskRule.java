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
public class DelayedTaskRiskRule implements ProjectRiskRule {

    private static final String TASK_SOURCE_TYPE = "TASK";

    @Override
    public List<ProjectRiskRecord> evaluate(ProjectRiskAnalysisContext context) {
        if (context.getProjectTasks() == null || context.getProjectTasks().isEmpty()) {
            return Collections.emptyList();
        }
        long now = context.getAnalyzeTime().getTime();
        return context.getProjectTasks().stream()
                .filter(task -> isDelayedTask(task, now))
                .map(task -> buildRisk(context, task))
                .collect(Collectors.toList());
    }

    private boolean isDelayedTask(ProjectTask task, long now) {
        return task.getCloseTime() != null
                && task.getCloseTime().getTime() < now
                && !Objects.equals(ProjectTaskStatusEnum.FINISHED.getStatus(), task.getExecuteStatus());
    }

    private ProjectRiskRecord buildRisk(ProjectRiskAnalysisContext context, ProjectTask task) {
        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setId(IdUtils.fastUUID());
        record.setProjectId(context.getProjectId());
        record.setAnalysisTaskId(context.getAnalysisTaskId());
        record.setRiskType(RiskType.DELAYED_TASK.getCode());
        record.setRiskLevel(RiskLevel.HIGH.getCode());
        record.setSourceType(TASK_SOURCE_TYPE);
        record.setSourceId(task.getId());
        record.setTitle("任务已逾期: " + task.getTaskName());
        record.setReason("任务截止时间已超过当前时间，且任务未完成");
        record.setSuggestion("优先处理逾期任务，重新评估负责人和排期");
        record.setStatus(RiskStatus.OPEN.getCode());
        record.setCreatedBy(context.getOperator());
        record.setCreatedTime(context.getAnalyzeTime());
        record.setUpdatedBy(context.getOperator());
        record.setUpdatedTime(context.getAnalyzeTime());
        return record;
    }
}
