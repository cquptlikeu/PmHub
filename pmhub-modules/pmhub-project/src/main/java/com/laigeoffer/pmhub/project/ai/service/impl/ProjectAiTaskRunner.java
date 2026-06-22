package com.laigeoffer.pmhub.project.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.base.core.utils.uuid.IdUtils;
import com.laigeoffer.pmhub.project.ai.constant.AnalysisTaskStatus;
import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectAiAnalysisTaskMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectRiskRecordMapper;
import com.laigeoffer.pmhub.project.ai.rule.ProjectRiskRule;
import com.laigeoffer.pmhub.project.ai.score.HealthScoreResult;
import com.laigeoffer.pmhub.project.ai.score.ProjectHealthScoreService;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ProjectAiTaskRunner {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

    private final ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectRiskRecordMapper projectRiskRecordMapper;
    private final ProjectHealthSnapshotMapper projectHealthSnapshotMapper;
    private final List<ProjectRiskRule> projectRiskRules;
    private final ProjectHealthScoreService projectHealthScoreService;
    private final TransactionTemplate transactionTemplate;

    public ProjectAiTaskRunner(ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper,
                               ProjectTaskMapper projectTaskMapper,
                               ProjectRiskRecordMapper projectRiskRecordMapper,
                               ProjectHealthSnapshotMapper projectHealthSnapshotMapper,
                               List<ProjectRiskRule> projectRiskRules,
                               ProjectHealthScoreService projectHealthScoreService,
                               TransactionTemplate transactionTemplate) {
        this.projectAiAnalysisTaskMapper = projectAiAnalysisTaskMapper;
        this.projectTaskMapper = projectTaskMapper;
        this.projectRiskRecordMapper = projectRiskRecordMapper;
        this.projectHealthSnapshotMapper = projectHealthSnapshotMapper;
        this.projectRiskRules = projectRiskRules;
        this.projectHealthScoreService = projectHealthScoreService;
        this.transactionTemplate = transactionTemplate;
    }

    public void executeAnalysis(String analysisTaskId) {
        ProjectAiAnalysisTask task = projectAiAnalysisTaskMapper.selectById(analysisTaskId);
        if (task == null) {
            return;
        }
        Date startedTime = new Date();
        updateTaskStatus(task, AnalysisTaskStatus.RUNNING.getCode(), startedTime, null);
        try {
            transactionTemplate.executeWithoutResult(status -> runAnalysis(task, startedTime));
            updateTaskStatus(task, AnalysisTaskStatus.SUCCESS.getCode(), startedTime, new Date());
        } catch (Exception ex) {
            updateFailedTask(task, startedTime, ex);
        }
    }

    private void runAnalysis(ProjectAiAnalysisTask task, Date analyzeTime) {
        List<ProjectTask> projectTasks = loadProjectTasks(task.getProjectId());
        ProjectRiskAnalysisContext context = ProjectRiskAnalysisContext.builder()
                .analysisTaskId(task.getId())
                .projectId(task.getProjectId())
                .operator(task.getCreatedBy())
                .analyzeTime(analyzeTime)
                .projectTasks(projectTasks)
                .build();
        List<ProjectRiskRecord> risks = collectRisks(context);
        saveRiskRecords(context, risks);
        saveHealthSnapshot(task, analyzeTime, risks);
    }

    private List<ProjectTask> loadProjectTasks(String projectId) {
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getProjectId, projectId)
                .eq(ProjectTask::getDeleted, 0);
        return projectTaskMapper.selectList(queryWrapper);
    }

    private List<ProjectRiskRecord> collectRisks(ProjectRiskAnalysisContext context) {
        List<ProjectRiskRecord> risks = new ArrayList<>();
        for (ProjectRiskRule projectRiskRule : projectRiskRules) {
            List<ProjectRiskRecord> currentRisks = projectRiskRule.evaluate(context);
            if (currentRisks != null && !currentRisks.isEmpty()) {
                risks.addAll(currentRisks);
            }
        }
        return risks;
    }

    private void saveRiskRecords(ProjectRiskAnalysisContext context, List<ProjectRiskRecord> risks) {
        for (ProjectRiskRecord risk : risks) {
            fillRiskDefaults(context, risk);
            projectRiskRecordMapper.insert(risk);
        }
    }

    private void fillRiskDefaults(ProjectRiskAnalysisContext context, ProjectRiskRecord risk) {
        if (StringUtils.isBlank(risk.getId())) {
            risk.setId(IdUtils.fastUUID());
        }
        if (StringUtils.isBlank(risk.getProjectId())) {
            risk.setProjectId(context.getProjectId());
        }
        if (StringUtils.isBlank(risk.getAnalysisTaskId())) {
            risk.setAnalysisTaskId(context.getAnalysisTaskId());
        }
        if (StringUtils.isBlank(risk.getCreatedBy())) {
            risk.setCreatedBy(context.getOperator());
        }
        if (risk.getCreatedTime() == null) {
            risk.setCreatedTime(context.getAnalyzeTime());
        }
        if (StringUtils.isBlank(risk.getUpdatedBy())) {
            risk.setUpdatedBy(context.getOperator());
        }
        if (risk.getUpdatedTime() == null) {
            risk.setUpdatedTime(context.getAnalyzeTime());
        }
    }

    private void saveHealthSnapshot(ProjectAiAnalysisTask task, Date analyzeTime, List<ProjectRiskRecord> risks) {
        HealthScoreResult scoreResult = projectHealthScoreService.calculate(risks);
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setId(IdUtils.fastUUID());
        snapshot.setProjectId(task.getProjectId());
        snapshot.setAnalysisTaskId(task.getId());
        snapshot.setHealthScore(scoreResult.getHealthScore());
        snapshot.setHealthLevel(scoreResult.getHealthLevel());
        snapshot.setDeductionDetail(scoreResult.getDeductionDetail());
        snapshot.setRiskCount(risks.size());
        snapshot.setHighRiskCount(scoreResult.getHighRiskCount());
        snapshot.setSnapshotTime(analyzeTime);
        snapshot.setCreatedBy(task.getCreatedBy());
        snapshot.setCreatedTime(analyzeTime);
        snapshot.setUpdatedBy(task.getCreatedBy());
        snapshot.setUpdatedTime(analyzeTime);
        projectHealthSnapshotMapper.insert(snapshot);
    }

    private void updateTaskStatus(ProjectAiAnalysisTask source, String status, Date startedTime, Date finishedTime) {
        ProjectAiAnalysisTask task = new ProjectAiAnalysisTask();
        task.setId(source.getId());
        task.setStatus(status);
        task.setStartedTime(startedTime);
        task.setFinishedTime(finishedTime);
        task.setUpdatedBy(source.getCreatedBy());
        task.setUpdatedTime(new Date());
        projectAiAnalysisTaskMapper.updateById(task);
    }

    private void updateFailedTask(ProjectAiAnalysisTask task, Date startedTime, Exception ex) {
        ProjectAiAnalysisTask failedTask = new ProjectAiAnalysisTask();
        failedTask.setId(task.getId());
        failedTask.setStatus(AnalysisTaskStatus.FAILED.getCode());
        failedTask.setStartedTime(startedTime);
        failedTask.setFinishedTime(new Date());
        failedTask.setErrorMessage(resolveErrorMessage(ex));
        failedTask.setUpdatedBy(task.getCreatedBy());
        failedTask.setUpdatedTime(new Date());
        projectAiAnalysisTaskMapper.updateById(failedTask);
    }

    private String resolveErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (StringUtils.isBlank(message)) {
            return "执行分析任务失败";
        }
        return message.length() > ERROR_MESSAGE_MAX_LENGTH ? message.substring(0, ERROR_MESSAGE_MAX_LENGTH) : message;
    }
}
