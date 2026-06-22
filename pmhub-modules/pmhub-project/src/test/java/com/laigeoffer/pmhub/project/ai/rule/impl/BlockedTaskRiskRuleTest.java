package com.laigeoffer.pmhub.project.ai.rule.impl;

import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockedTaskRiskRuleTest {

    private final BlockedTaskRiskRule blockedTaskRiskRule = new BlockedTaskRiskRule();

    @Test
    void shouldIdentifyUnclaimedTaskAsBlockedRisk() {
        ProjectTask blockedTask = new ProjectTask();
        blockedTask.setId("task-1");
        blockedTask.setTaskName("Blocked task");
        blockedTask.setUserId(null);
        blockedTask.setStatus(ProjectTaskStatusEnum.NO_CLAIMED.getStatus());
        blockedTask.setExecuteStatus(ProjectTaskStatusEnum.NO_CLAIMED.getStatus());

        ProjectTask normalTask = new ProjectTask();
        normalTask.setId("task-2");
        normalTask.setTaskName("Normal task");
        normalTask.setUserId(100L);
        normalTask.setStatus(ProjectTaskStatusEnum.DOING.getStatus());
        normalTask.setExecuteStatus(ProjectTaskStatusEnum.DOING.getStatus());

        List<ProjectRiskRecord> risks = blockedTaskRiskRule.evaluate(ProjectRiskAnalysisContext.builder()
                .analysisTaskId("analysis-1")
                .projectId("project-1")
                .operator("tester")
                .analyzeTime(new Date())
                .projectTasks(Arrays.asList(blockedTask, normalTask))
                .build());

        assertEquals(1, risks.size());
        assertEquals(RiskType.BLOCKED_TASK.getCode(), risks.get(0).getRiskType());
        assertEquals(RiskLevel.MEDIUM.getCode(), risks.get(0).getRiskLevel());
        assertEquals("task-1", risks.get(0).getSourceId());
    }
}
