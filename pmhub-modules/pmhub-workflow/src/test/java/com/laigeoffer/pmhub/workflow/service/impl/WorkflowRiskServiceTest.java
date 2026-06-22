package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowRiskServiceTest {

    @Mock
    private WfTaskProcessMapper wfTaskProcessMapper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;

    private WorkflowRiskService workflowRiskService;

    @BeforeEach
    void setUp() {
        workflowRiskService = new WorkflowRiskService(wfTaskProcessMapper, runtimeService);
    }

    @Test
    void shouldBuildBlockedSummaryForProjectAndTaskApprovals() {
        List<WfTaskProcess> candidates = Arrays.asList(
                buildCandidate("project-1", "project", "0", "proc-project"),
                buildCandidate("task-1", "task", "0", "proc-task-1"),
                buildCandidate("task-2", "task", "0", null),
                buildCandidate("task-3", "task", "1", "proc-task-3")
        );
        when(wfTaskProcessMapper.selectProjectRiskCandidates("project-1")).thenReturn(candidates);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("proc-project")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("proc-task-1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult())
                .thenReturn(mock(ProcessInstance.class))
                .thenReturn(mock(ProcessInstance.class));

        ProjectWorkflowRiskSummaryVO summary = workflowRiskService.getProjectRiskSummary("project-1");

        assertTrue(summary.getProjectApprovalBlocked());
        assertEquals(Integer.valueOf(1), summary.getTaskApprovalBlockedCount());
        assertEquals(Integer.valueOf(2), summary.getTotalBlockedCount());
        assertTrue(summary.getSummary().contains("项目发布审批进行中"));
    }

    @Test
    void shouldReturnNoBlockedSummaryWhenNoActiveApprovalExists() {
        when(wfTaskProcessMapper.selectProjectRiskCandidates("project-1")).thenReturn(Collections.emptyList());

        ProjectWorkflowRiskSummaryVO summary = workflowRiskService.getProjectRiskSummary("project-1");

        assertFalse(summary.getProjectApprovalBlocked());
        assertEquals(Integer.valueOf(0), summary.getTaskApprovalBlockedCount());
        assertEquals(Integer.valueOf(0), summary.getTotalBlockedCount());
        assertEquals("当前无审批卡点", summary.getSummary());
    }

    private WfTaskProcess buildCandidate(String extraId, String type, String approved, String instanceId) {
        WfTaskProcess process = new WfTaskProcess();
        process.setExtraId(extraId);
        process.setType(type);
        process.setApproved(approved);
        process.setInstanceId(instanceId);
        return process;
    }
}
