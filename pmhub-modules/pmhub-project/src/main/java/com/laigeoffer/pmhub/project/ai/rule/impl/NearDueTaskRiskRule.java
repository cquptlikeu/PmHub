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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class NearDueTaskRiskRule implements ProjectRiskRule {

    private static final String TASK_SOURCE_TYPE = "TASK";
    private static final long THREE_DAYS_MILLIS = TimeUnit.DAYS.toMillis(3);

    @Override
    public List<ProjectRiskRecord> evaluate(ProjectRiskAnalysisContext context) {
        if (context.getProjectTasks() == null || context.getProjectTasks().isEmpty()) {
            return Collections.emptyList();
        }
        Date analyzeTime = context.getAnalyzeTime();
        long deadline = analyzeTime.getTime() + THREE_DAYS_MILLIS;
        return context.getProjectTasks().stream()
                .filter(task -> isNearDueTask(task, analyzeTime.getTime(), deadline))
                .map(task -> buildRisk(context, task))
                .collect(Collectors.toList());
    }

    private boolean isNearDueTask(ProjectTask task, long now, long deadline) {
        return task.getCloseTime() != null
                && task.getCloseTime().getTime() >= now
                && task.getCloseTime().getTime() <= deadline
                && !Objects.equals(ProjectTaskStatusEnum.FINISHED.getStatus(), task.getExecuteStatus());
    }

    private ProjectRiskRecord buildRisk(ProjectRiskAnalysisContext context, ProjectTask task) {
        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setId(IdUtils.fastUUID());
        record.setProjectId(context.getProjectId());
        record.setAnalysisTaskId(context.getAnalysisTaskId());
        record.setRiskType(RiskType.NEAR_DUE.getCode());
        record.setRiskLevel(RiskLevel.MEDIUM.getCode());
        record.setSourceType(TASK_SOURCE_TYPE);
        record.setSourceId(task.getId());
        record.setTitle("任务临近截止: " + task.getTaskName());
        record.setReason("任务将在 3 天内到达截止时间，且任务未完成");
        record.setSuggestion("尽快确认进度和资源，必要时提前预警");
        record.setStatus(RiskStatus.OPEN.getCode());
        record.setCreatedBy(context.getOperator());
        record.setCreatedTime(context.getAnalyzeTime());
        record.setUpdatedBy(context.getOperator());
        record.setUpdatedTime(context.getAnalyzeTime());
        return record;
    }
}
