package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.context.SecurityContextHolder;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.project.ai.constant.AnalysisTaskStatus;
import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.dto.ProjectAnalyzeRequest;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectAiAnalysisTaskMapper;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiTaskExecuteService;
import com.laigeoffer.pmhub.project.domain.Project;
import com.laigeoffer.pmhub.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAiAnalysisServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper;

    @Mock
    private ProjectAiTaskExecuteService projectAiTaskExecuteService;

    @InjectMocks
    private ProjectAiAnalysisServiceImpl projectAiAnalysisService;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        loginUser.setUser(sysUser);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void shouldRejectBlankProjectId() {
        ProjectAnalyzeRequest request = new ProjectAnalyzeRequest();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectAiAnalysisService.createAnalysisTask(request));

        assertEquals("项目ID不能为空", exception.getMessage());
        verifyNoInteractions(projectMapper, projectAiAnalysisTaskMapper);
    }

    @Test
    void shouldRejectMissingProject() {
        ProjectAnalyzeRequest request = new ProjectAnalyzeRequest();
        request.setProjectId("missing-project");
        when(projectMapper.selectById("missing-project")).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectAiAnalysisService.createAnalysisTask(request));

        assertEquals("项目不存在或已删除", exception.getMessage());
        verify(projectMapper).selectById("missing-project");
        verify(projectAiAnalysisTaskMapper, never()).insert(any(ProjectAiAnalysisTask.class));
    }

    @Test
    void shouldReturnExistingRunningTaskWhenProcessingTaskExists() {
        ProjectAnalyzeRequest request = new ProjectAnalyzeRequest();
        request.setProjectId("project-1");
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));
        ProjectAiAnalysisTask existingTask = new ProjectAiAnalysisTask();
        existingTask.setId("task-123");
        existingTask.setStatus(AnalysisTaskStatus.RUNNING.getCode());
        when(projectAiAnalysisTaskMapper.selectOne(any())).thenReturn(existingTask);

        String taskId = projectAiAnalysisService.createAnalysisTask(request);

        assertEquals("task-123", taskId);
        verifyNoInteractions(projectAiTaskExecuteService);
        verify(projectAiAnalysisTaskMapper, never()).insert(any(ProjectAiAnalysisTask.class));
    }

    @Test
    void shouldResubmitExistingPendingTaskWhenItHasNotStarted() {
        ProjectAnalyzeRequest request = new ProjectAnalyzeRequest();
        request.setProjectId("project-1");
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));
        ProjectAiAnalysisTask existingTask = new ProjectAiAnalysisTask();
        existingTask.setId("task-456");
        existingTask.setStatus(AnalysisTaskStatus.PENDING.getCode());
        when(projectAiAnalysisTaskMapper.selectOne(any())).thenReturn(existingTask);

        String taskId = projectAiAnalysisService.createAnalysisTask(request);

        assertEquals("task-456", taskId);
        verify(projectAiTaskExecuteService).submitAnalysis("task-456");
        verify(projectAiAnalysisTaskMapper, never()).insert(any(ProjectAiAnalysisTask.class));
    }

    @Test
    void shouldCreateTaskWhenForceReanalyzeEnabled() {
        ProjectAnalyzeRequest request = new ProjectAnalyzeRequest();
        request.setProjectId("project-1");
        request.setForceReanalyze(Boolean.TRUE);
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));
        when(projectAiAnalysisTaskMapper.insert(any(ProjectAiAnalysisTask.class))).thenReturn(1);

        String taskId = projectAiAnalysisService.createAnalysisTask(request);

        ArgumentCaptor<ProjectAiAnalysisTask> captor = ArgumentCaptor.forClass(ProjectAiAnalysisTask.class);
        verify(projectAiAnalysisTaskMapper).insert(captor.capture());
        verify(projectAiTaskExecuteService).submitAnalysis(taskId);
        ProjectAiAnalysisTask savedTask = captor.getValue();
        assertEquals(taskId, savedTask.getId());
        assertEquals("project-1", savedTask.getProjectId());
        assertEquals("MANUAL", savedTask.getTriggerType());
        assertEquals(AnalysisTaskStatus.PENDING.getCode(), savedTask.getStatus());
        assertEquals("tester", savedTask.getCreatedBy());
        assertEquals("tester", savedTask.getUpdatedBy());
        assertNotNull(savedTask.getCreatedTime());
        assertNotNull(savedTask.getUpdatedTime());
        assertTrue(savedTask.getId().length() > 10);
    }

    @Test
    void shouldRetryFailedTask() {
        ProjectAiAnalysisTask failedTask = new ProjectAiAnalysisTask();
        failedTask.setId("task-failed");
        failedTask.setProjectId("project-1");
        failedTask.setStatus(AnalysisTaskStatus.FAILED.getCode());
        when(projectAiAnalysisTaskMapper.selectById("task-failed")).thenReturn(failedTask);
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));
        when(projectAiAnalysisTaskMapper.update(any(), any())).thenReturn(1);

        String taskId = projectAiAnalysisService.retryAnalysisTask("task-failed");

        assertEquals("task-failed", taskId);
        verify(projectAiAnalysisTaskMapper).update(any(), any());
        verify(projectAiTaskExecuteService).submitAnalysis("task-failed");
    }

    @Test
    void shouldRejectRetryWhenTaskIsNotFailed() {
        ProjectAiAnalysisTask runningTask = new ProjectAiAnalysisTask();
        runningTask.setId("task-running");
        runningTask.setStatus(AnalysisTaskStatus.RUNNING.getCode());
        when(projectAiAnalysisTaskMapper.selectById("task-running")).thenReturn(runningTask);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectAiAnalysisService.retryAnalysisTask("task-running"));

        assertEquals("仅失败的分析任务可重试", exception.getMessage());
        verify(projectAiAnalysisTaskMapper, never()).update(any(), any());
        verifyNoInteractions(projectAiTaskExecuteService);
    }

    private Project buildProject(Integer deleted) {
        Project project = new Project();
        project.setId("project-1");
        project.setDeleted(deleted);
        return project;
    }
}
