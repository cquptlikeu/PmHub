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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class MemberOverloadRiskRule implements ProjectRiskRule {

    private static final String USER_SOURCE_TYPE = "USER";
    private static final long OVERLOAD_THRESHOLD = 5L;

    @Override
    public List<ProjectRiskRecord> evaluate(ProjectRiskAnalysisContext context) {
        if (context.getProjectTasks() == null || context.getProjectTasks().isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> taskCountByUser = context.getProjectTasks().stream()
                .filter(task -> task.getUserId() != null)
                .filter(task -> !Objects.equals(ProjectTaskStatusEnum.FINISHED.getStatus(), task.getExecuteStatus()))
                .collect(Collectors.groupingBy(ProjectTask::getUserId, Collectors.counting()));
        return taskCountByUser.entrySet().stream()
                .filter(entry -> entry.getValue() > OVERLOAD_THRESHOLD)
                .map(entry -> buildRisk(context, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private ProjectRiskRecord buildRisk(ProjectRiskAnalysisContext context, Long userId, Long taskCount) {
        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setId(IdUtils.fastUUID());
        record.setProjectId(context.getProjectId());
        record.setAnalysisTaskId(context.getAnalysisTaskId());
        record.setRiskType(RiskType.MEMBER_OVERLOAD.getCode());
        record.setRiskLevel(RiskLevel.MEDIUM.getCode());
        record.setSourceType(USER_SOURCE_TYPE);
        record.setSourceId(String.valueOf(userId));
        record.setTitle("成员负载过高: " + userId);
        record.setReason("成员当前未完成任务数为 " + taskCount + "，超过阈值 " + OVERLOAD_THRESHOLD);
        record.setSuggestion("重新分配任务或调整优先级，避免关键成员过载");
        record.setStatus(RiskStatus.OPEN.getCode());
        record.setCreatedBy(context.getOperator());
        record.setCreatedTime(context.getAnalyzeTime());
        record.setUpdatedBy(context.getOperator());
        record.setUpdatedTime(context.getAnalyzeTime());
        return record;
    }
}
