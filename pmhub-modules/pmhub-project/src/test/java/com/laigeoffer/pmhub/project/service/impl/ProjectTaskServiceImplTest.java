package com.laigeoffer.pmhub.project.service.impl;

import com.laigeoffer.pmhub.api.system.UserFeignService;
import com.laigeoffer.pmhub.api.workflow.DeployFeignService;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.context.SecurityContextHolder;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.project.domain.ProjectMember;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import com.laigeoffer.pmhub.project.domain.vo.project.task.TaskReqVO;
import com.laigeoffer.pmhub.project.mapper.ProjectFileMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectMemberMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectStageMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskProcessMapper;
import com.laigeoffer.pmhub.project.service.ProjectLogService;
import com.laigeoffer.pmhub.project.service.task.QueryTaskLogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectTaskServiceImplTest {

    @Mock
    private ProjectTaskMapper projectTaskMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectLogService projectLogService;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectStageMapper projectStageMapper;

    @Mock
    private QueryTaskLogFactory queryTaskLogFactory;

    @Mock
    private ProjectFileMapper projectFileMapper;

    @Mock
    private ProjectTaskProcessMapper projectTaskProcessMapper;

    @Mock
    private DeployFeignService wfDeployService;

    @Mock
    private UserFeignService userFeignService;

    @InjectMocks
    private ProjectTaskServiceImpl projectTaskService;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("admin");
        loginUser.setUser(sysUser);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void shouldCreateApprovalTaskWithThirtyTwoCharWorkflowExtraId() {
        TaskReqVO request = new TaskReqVO();
        request.setProjectId("project-1");
        request.setTaskName("approval task");
        request.setUserId(1L);
        request.setApproved("0");
        request.setDefinitionId("definition-1");
        request.setDeploymentId("deployment-1");
        when(wfDeployService.insertOrUpdateApprovalSet(any(ApprovalSetDTO.class), eq(SecurityConstants.INNER)))
                .thenReturn(R.ok(Boolean.TRUE));

        String taskId = projectTaskService.add(request);

        ArgumentCaptor<ProjectTask> taskCaptor = ArgumentCaptor.forClass(ProjectTask.class);
        verify(projectTaskMapper).insert(taskCaptor.capture());
        assertEquals(32, taskCaptor.getValue().getId().length());
        assertFalse(taskCaptor.getValue().getId().contains("-"));

        ArgumentCaptor<ApprovalSetDTO> approvalCaptor = ArgumentCaptor.forClass(ApprovalSetDTO.class);
        verify(wfDeployService).insertOrUpdateApprovalSet(approvalCaptor.capture(), eq(SecurityConstants.INNER));
        ApprovalSetDTO approval = approvalCaptor.getValue();
        assertEquals(taskId, approval.getExtraId());
        assertEquals(32, approval.getExtraId().length());
        assertFalse(approval.getExtraId().contains("-"));
        verify(projectMemberMapper).insert(any(ProjectMember.class));
    }
}
