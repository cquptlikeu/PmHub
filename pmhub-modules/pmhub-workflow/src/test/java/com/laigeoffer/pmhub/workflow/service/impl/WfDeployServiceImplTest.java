package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.base.core.context.SecurityContextHolder;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalSet;
import com.laigeoffer.pmhub.workflow.mapper.WfApprovalSetMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfDeployFormMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WfDeployServiceImplTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private WfDeployFormMapper deployFormMapper;

    @Mock
    private WfApprovalSetMapper wfApprovalSetMapper;

    @Mock
    private WfTaskProcessMapper wfTaskProcessMapper;

    @Mock
    private WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;

    @Mock
    private WorkflowProjectService workflowProjectService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void shouldUseHeaderUsernameForInnerInsertWhenLoginUserMissing() {
        SecurityContextHolder.setUserName("admin");
        when(wfApprovalSetMapper.selectOne(any())).thenReturn(null);

        WfDeployServiceImpl service = new WfDeployServiceImpl(
                repositoryService,
                deployFormMapper,
                wfApprovalSetMapper,
                wfTaskProcessMapper,
                wfMaterialsScrappedProcessMapper,
                workflowProjectService
        );

        boolean result = service.insertOrUpdateApprovalSet("task-1", "task", "0", "def-1", "dep-1");

        ArgumentCaptor<WfApprovalSet> captor = ArgumentCaptor.forClass(WfApprovalSet.class);
        verify(wfApprovalSetMapper).insert(captor.capture());
        WfApprovalSet saved = captor.getValue();
        assertTrue(result);
        assertEquals("task-1", saved.getExtraId());
        assertEquals("task", saved.getType());
        assertEquals("0", saved.getApproved());
        assertEquals("admin", saved.getCreatedBy());
        assertEquals("admin", saved.getUpdatedBy());
        assertNotNull(saved.getCreatedTime());
        assertNotNull(saved.getUpdatedTime());
    }

    @Test
    void shouldCreateTaskApprovalSetWhenMissing() {
        when(wfApprovalSetMapper.selectOne(any())).thenReturn(null);
        WfDeployServiceImpl service = newService();
        ApprovalSetDTO request = new ApprovalSetDTO();
        request.setTaskId("task-1");
        request.setApproved("0");
        request.setDefinitionId("def-1");
        request.setDeploymentId("dep-1");

        boolean result = service.updateApprovalSet2(request, ProjectStatusEnum.PROJECT.getStatusName());

        ArgumentCaptor<WfApprovalSet> captor = ArgumentCaptor.forClass(WfApprovalSet.class);
        verify(wfApprovalSetMapper).insert(captor.capture());
        verify(wfApprovalSetMapper, never()).updateById(any());
        WfApprovalSet saved = captor.getValue();
        assertTrue(result);
        assertEquals("task-1", saved.getExtraId());
        assertEquals(ProjectStatusEnum.TASK.getStatusName(), saved.getType());
        assertEquals("0", saved.getApproved());
        assertEquals("def-1", saved.getDefinitionId());
        assertEquals("dep-1", saved.getDeploymentId());
        assertNotNull(saved.getCreatedTime());
        assertNotNull(saved.getUpdatedTime());
    }

    @Test
    void shouldReadTaskProcessFromProjectDataSourceWhenApprovalSetExists() {
        WfApprovalSet approvalSet = new WfApprovalSet();
        approvalSet.setId("set-1");
        approvalSet.setApproved("0");
        approvalSet.setExtraId("task-1");
        approvalSet.setType(ProjectStatusEnum.TASK.getStatusName());
        when(wfApprovalSetMapper.selectOne(any())).thenReturn(approvalSet);
        when(workflowProjectService.selectTaskProcess("task-1", ProjectStatusEnum.TASK.getStatusName()))
                .thenReturn(null);
        WfDeployServiceImpl service = newService();
        ApprovalSetDTO request = new ApprovalSetDTO();
        request.setTaskId("task-1");
        request.setApproved("0");
        request.setDefinitionId("def-2");
        request.setDeploymentId("dep-2");

        boolean result = service.updateApprovalSet2(request, ProjectStatusEnum.PROJECT.getStatusName());

        assertTrue(result);
        verify(workflowProjectService).selectTaskProcess("task-1", ProjectStatusEnum.TASK.getStatusName());
        verify(wfTaskProcessMapper, never()).selectOne(any());
        verify(wfApprovalSetMapper).updateById(approvalSet);
        assertEquals("def-2", approvalSet.getDefinitionId());
        assertEquals("dep-2", approvalSet.getDeploymentId());
    }

    @Test
    void shouldAllowCompletedTaskToBindApprovalWhenPreviouslyNoApproval() {
        WfApprovalSet approvalSet = new WfApprovalSet();
        approvalSet.setId("set-1");
        approvalSet.setApproved("1");
        approvalSet.setExtraId("task-1");
        approvalSet.setType(ProjectStatusEnum.TASK.getStatusName());
        when(wfApprovalSetMapper.selectOne(any())).thenReturn(approvalSet);
        when(workflowProjectService.selectTaskStatus("task-1")).thenReturn(ProjectStatusEnum.ARCHIVED.getStatus());
        WfDeployServiceImpl service = newService();
        ApprovalSetDTO request = new ApprovalSetDTO();
        request.setTaskId("task-1");
        request.setApproved("0");
        request.setDefinitionId("def-3");
        request.setDeploymentId("dep-3");

        boolean result = service.updateApprovalSet2(request, ProjectStatusEnum.PROJECT.getStatusName());

        assertTrue(result);
        verify(wfApprovalSetMapper).updateById(approvalSet);
        assertEquals("0", approvalSet.getApproved());
        assertEquals("def-3", approvalSet.getDefinitionId());
        assertEquals("dep-3", approvalSet.getDeploymentId());
    }

    private WfDeployServiceImpl newService() {
        return new WfDeployServiceImpl(
                repositoryService,
                deployFormMapper,
                wfApprovalSetMapper,
                wfTaskProcessMapper,
                wfMaterialsScrappedProcessMapper,
                workflowProjectService
        );
    }
}
