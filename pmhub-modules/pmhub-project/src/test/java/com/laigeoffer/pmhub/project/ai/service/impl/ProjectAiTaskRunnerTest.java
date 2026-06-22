package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.project.ai.constant.AnalysisTaskStatus;
import com.laigeoffer.pmhub.project.ai.constant.HealthLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectAiAnalysisTaskMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectRiskRecordMapper;
import com.laigeoffer.pmhub.project.ai.rule.ProjectRiskRule;
import com.laigeoffer.pmhub.project.ai.score.HealthScoreResult;
import com.laigeoffer.pmhub.project.ai.score.ProjectHealthScoreService;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAiTaskRunnerTest {

    @Mock
    private ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper;

    @Mock
    private ProjectTaskMapper projectTaskMapper;

    @Mock
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Mock
    private ProjectHealthSnapshotMapper projectHealthSnapshotMapper;

    @Mock
    private ProjectRiskRule delayedTaskRiskRule;

    @Mock
    private ProjectRiskRule nearDueTaskRiskRule;

    @Mock
    private ProjectHealthScoreService projectHealthScoreService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ProjectAiTaskRunner projectAiTaskRunner;

    @Captor
    private ArgumentCaptor<ProjectAiAnalysisTask> taskCaptor;

    @Captor
    private ArgumentCaptor<ProjectHealthSnapshot> snapshotCaptor;

    @Captor
    private ArgumentCaptor<ProjectRiskRecord> riskCaptor;

    @BeforeEach
    void setUp() {
        projectAiTaskRunner = new ProjectAiTaskRunner(
                projectAiAnalysisTaskMapper,
                projectTaskMapper,
                projectRiskRecordMapper,
                projectHealthSnapshotMapper,
                Arrays.asList(delayedTaskRiskRule, nearDueTaskRiskRule),
                projectHealthScoreService,
                transactionTemplate
        );
        doAnswer(invocation -> {
            Consumer<Object> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void shouldExecuteTaskAndGenerateSnapshot() {
        ProjectAiAnalysisTask task = new ProjectAiAnalysisTask();
        task.setId("analysis-1");
        task.setProjectId("project-1");
        task.setCreatedBy("tester");
        when(projectAiAnalysisTaskMapper.selectById("analysis-1")).thenReturn(task);

        ProjectTask overdueTask = buildTask("task-1", -2, ProjectTaskStatusEnum.DOING.getStatus());
        ProjectTask nearDueTask = buildTask("task-2", 1, ProjectTaskStatusEnum.DOING.getStatus());
        when(projectTaskMapper.selectList(any())).thenReturn(Arrays.asList(overdueTask, nearDueTask));

        ProjectRiskRecord delayedRisk = new ProjectRiskRecord();
        delayedRisk.setRiskType(RiskType.DELAYED_TASK.getCode());
        delayedRisk.setRiskLevel(RiskLevel.HIGH.getCode());
        delayedRisk.setTitle("Delayed task");

        ProjectRiskRecord nearDueRisk = new ProjectRiskRecord();
        nearDueRisk.setRiskType(RiskType.NEAR_DUE.getCode());
        nearDueRisk.setRiskLevel(RiskLevel.MEDIUM.getCode());
        nearDueRisk.setTitle("Near due task");

        when(delayedTaskRiskRule.evaluate(any())).thenReturn(Collections.singletonList(delayedRisk));
        when(nearDueTaskRiskRule.evaluate(any())).thenReturn(Collections.singletonList(nearDueRisk));
        when(projectHealthScoreService.calculate(any())).thenReturn(
                HealthScoreResult.builder()
                        .healthScore(70)
                        .healthLevel(HealthLevel.WARNING.getCode())
                        .deductionDetail("Delayed task -20; Near due task -10")
                        .highRiskCount(1)
                        .build()
        );

        projectAiTaskRunner.executeAnalysis("analysis-1");

        verify(projectAiAnalysisTaskMapper, times(2)).updateById(taskCaptor.capture());
        List<ProjectAiAnalysisTask> updatedTasks = taskCaptor.getAllValues();
        assertEquals(AnalysisTaskStatus.RUNNING.getCode(), updatedTasks.get(0).getStatus());
        assertEquals(AnalysisTaskStatus.SUCCESS.getCode(), updatedTasks.get(1).getStatus());
        assertNotNull(updatedTasks.get(0).getStartedTime());
        assertNotNull(updatedTasks.get(1).getFinishedTime());

        verify(projectRiskRecordMapper, times(2)).insert(riskCaptor.capture());
        List<ProjectRiskRecord> savedRisks = riskCaptor.getAllValues();
        assertEquals("analysis-1", savedRisks.get(0).getAnalysisTaskId());
        assertEquals("project-1", savedRisks.get(0).getProjectId());

        verify(projectHealthSnapshotMapper).insert(snapshotCaptor.capture());
        ProjectHealthSnapshot snapshot = snapshotCaptor.getValue();
        assertEquals("analysis-1", snapshot.getAnalysisTaskId());
        assertEquals("project-1", snapshot.getProjectId());
        assertEquals(Integer.valueOf(70), snapshot.getHealthScore());
        assertEquals(HealthLevel.WARNING.getCode(), snapshot.getHealthLevel());
        assertEquals(Integer.valueOf(2), snapshot.getRiskCount());
        assertEquals(Integer.valueOf(1), snapshot.getHighRiskCount());
        assertTrue(snapshot.getDeductionDetail().contains("Delayed task"));
    }

    @Test
    void shouldMarkTaskFailedWhenExecutionThrowsException() {
        ProjectAiAnalysisTask task = new ProjectAiAnalysisTask();
        task.setId("analysis-2");
        task.setProjectId("project-2");
        task.setCreatedBy("tester");
        when(projectAiAnalysisTaskMapper.selectById("analysis-2")).thenReturn(task);
        when(projectTaskMapper.selectList(any())).thenThrow(new RuntimeException("load tasks failed"));

        projectAiTaskRunner.executeAnalysis("analysis-2");

        verify(projectAiAnalysisTaskMapper, times(2)).updateById(taskCaptor.capture());
        List<ProjectAiAnalysisTask> updatedTasks = taskCaptor.getAllValues();
        assertEquals(AnalysisTaskStatus.RUNNING.getCode(), updatedTasks.get(0).getStatus());
        assertEquals(AnalysisTaskStatus.FAILED.getCode(), updatedTasks.get(1).getStatus());
        assertTrue(updatedTasks.get(1).getErrorMessage().contains("load tasks failed"));
        verify(projectRiskRecordMapper, never()).insert(any(ProjectRiskRecord.class));
        verify(projectHealthSnapshotMapper, never()).insert(any(ProjectHealthSnapshot.class));
    }

    private ProjectTask buildTask(String id, int closeTimeOffsetDays, Integer status) {
        ProjectTask task = new ProjectTask();
        task.setId(id);
        task.setProjectId("project-1");
        task.setTaskName(id);
        task.setStatus(status);
        task.setExecuteStatus(status);
        task.setCloseTime(new Date(System.currentTimeMillis() + closeTimeOffsetDays * 24L * 60L * 60L * 1000L));
        return task;
    }
}
