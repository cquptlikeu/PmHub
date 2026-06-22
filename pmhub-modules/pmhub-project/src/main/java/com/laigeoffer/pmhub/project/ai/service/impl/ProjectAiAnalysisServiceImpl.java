package com.laigeoffer.pmhub.project.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.utils.uuid.IdUtils;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.project.ai.constant.AnalysisTaskStatus;
import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.dto.ProjectAnalyzeRequest;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectAiAnalysisTaskMapper;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiAnalysisService;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiTaskExecuteService;
import com.laigeoffer.pmhub.project.domain.Project;
import com.laigeoffer.pmhub.project.mapper.ProjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class ProjectAiAnalysisServiceImpl implements ProjectAiAnalysisService {

    private static final String MANUAL_TRIGGER_TYPE = "MANUAL";

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper;

    @Resource
    private ProjectAiTaskExecuteService projectAiTaskExecuteService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAnalysisTask(ProjectAnalyzeRequest request) {
        validateRequest(request);
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new ServiceException("项目不存在或已删除");
        }
        if (!Boolean.TRUE.equals(request.getForceReanalyze())) {
            ProjectAiAnalysisTask existingTask = findLatestProcessingTask(request.getProjectId());
            if (existingTask != null) {
                resubmitIfPendingAndNotStarted(existingTask);
                return existingTask.getId();
            }
        }
        ProjectAiAnalysisTask task = buildPendingTask(request.getProjectId());
        int inserted = projectAiAnalysisTaskMapper.insert(task);
        if (inserted != 1) {
            throw new ServiceException("创建分析任务失败");
        }
        submitAnalysisAfterCommit(task.getId());
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryAnalysisTask(String analysisTaskId) {
        if (StringUtils.isBlank(analysisTaskId)) {
            throw new ServiceException("分析任务ID不能为空");
        }
        ProjectAiAnalysisTask task = projectAiAnalysisTaskMapper.selectById(analysisTaskId);
        if (task == null) {
            throw new ServiceException("分析任务不存在");
        }
        if (!AnalysisTaskStatus.FAILED.getCode().equals(task.getStatus())) {
            throw new ServiceException("仅失败的分析任务可重试");
        }
        Project project = projectMapper.selectById(task.getProjectId());
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new ServiceException("项目不存在或已删除");
        }

        UpdateWrapper<ProjectAiAnalysisTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", analysisTaskId)
                .eq("status", AnalysisTaskStatus.FAILED.getCode())
                .set("status", AnalysisTaskStatus.PENDING.getCode())
                .set("error_message", null)
                .set("started_time", null)
                .set("finished_time", null)
                .set("updated_by", SecurityUtils.getUsername())
                .set("updated_time", new Date());
        int updated = projectAiAnalysisTaskMapper.update(null, updateWrapper);
        if (updated != 1) {
            throw new ServiceException("分析任务状态已变化，请刷新后重试");
        }
        submitAnalysisAfterCommit(analysisTaskId);
        return analysisTaskId;
    }

    private void submitAnalysisAfterCommit(String analysisTaskId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            projectAiTaskExecuteService.submitAnalysis(analysisTaskId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                projectAiTaskExecuteService.submitAnalysis(analysisTaskId);
            }
        });
    }

    private void validateRequest(ProjectAnalyzeRequest request) {
        if (request == null || StringUtils.isBlank(request.getProjectId())) {
            throw new ServiceException("项目ID不能为空");
        }
    }

    private void resubmitIfPendingAndNotStarted(ProjectAiAnalysisTask existingTask) {
        if (!AnalysisTaskStatus.PENDING.getCode().equals(existingTask.getStatus())) {
            return;
        }
        if (existingTask.getStartedTime() != null) {
            return;
        }
        projectAiTaskExecuteService.submitAnalysis(existingTask.getId());
    }

    private ProjectAiAnalysisTask findLatestProcessingTask(String projectId) {
        LambdaQueryWrapper<ProjectAiAnalysisTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectAiAnalysisTask::getProjectId, projectId)
                .in(ProjectAiAnalysisTask::getStatus, AnalysisTaskStatus.PENDING.getCode(), AnalysisTaskStatus.RUNNING.getCode())
                .orderByDesc(ProjectAiAnalysisTask::getCreatedTime)
                .last("limit 1");
        return projectAiAnalysisTaskMapper.selectOne(queryWrapper);
    }

    private ProjectAiAnalysisTask buildPendingTask(String projectId) {
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        ProjectAiAnalysisTask task = new ProjectAiAnalysisTask();
        task.setId(IdUtils.fastUUID());
        task.setProjectId(projectId);
        task.setTriggerType(MANUAL_TRIGGER_TYPE);
        task.setStatus(AnalysisTaskStatus.PENDING.getCode());
        task.setCreatedBy(username);
        task.setCreatedTime(now);
        task.setUpdatedBy(username);
        task.setUpdatedTime(now);
        return task;
    }
}
