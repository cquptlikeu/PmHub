package com.laigeoffer.pmhub.project.ai.rule.impl;

import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberOverloadRiskRuleTest {

    private final MemberOverloadRiskRule memberOverloadRiskRule = new MemberOverloadRiskRule();

    @Test
    void shouldIdentifyMemberOverloadRisk() {
        List<ProjectTask> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ProjectTask task = new ProjectTask();
            task.setId("task-" + i);
            task.setTaskName("Task-" + i);
            task.setUserId(200L);
            task.setStatus(ProjectTaskStatusEnum.DOING.getStatus());
            task.setExecuteStatus(ProjectTaskStatusEnum.DOING.getStatus());
            tasks.add(task);
        }
        ProjectTask finishedTask = new ProjectTask();
        finishedTask.setId("task-finished");
        finishedTask.setTaskName("Finished");
        finishedTask.setUserId(200L);
        finishedTask.setStatus(ProjectTaskStatusEnum.FINISHED.getStatus());
        finishedTask.setExecuteStatus(ProjectTaskStatusEnum.FINISHED.getStatus());
        tasks.add(finishedTask);

        List<ProjectRiskRecord> risks = memberOverloadRiskRule.evaluate(ProjectRiskAnalysisContext.builder()
                .analysisTaskId("analysis-1")
                .projectId("project-1")
                .operator("tester")
                .analyzeTime(new Date())
                .projectTasks(tasks)
                .build());

        assertEquals(1, risks.size());
        assertEquals(RiskType.MEMBER_OVERLOAD.getCode(), risks.get(0).getRiskType());
        assertEquals(RiskLevel.MEDIUM.getCode(), risks.get(0).getRiskLevel());
        assertEquals("200", risks.get(0).getSourceId());
    }
}
