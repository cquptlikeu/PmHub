package com.laigeoffer.pmhub.workflow.listener;

import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.workflow.domain.WfTaskMessageDeal;
import com.laigeoffer.pmhub.workflow.domain.vo.TaskCompletedStateVO;
import com.laigeoffer.pmhub.workflow.mapper.ListenerMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfCopyMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskMessageDealMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import com.laigeoffer.pmhub.workflow.service.impl.WorkflowProjectService;
import com.laigeoffer.pmhub.workflow.service.impl.WorkflowSystemService;
import com.laigeoffer.pmhub.workflow.utils.ProcessUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalProcessEndListenerTest {

    @Mock
    private ListenerMapper listenerMapper;

    @Mock
    private WfCopyMapper wfCopyMapper;

    @Mock
    private WfTaskProcessMapper wfTaskProcessMapper;

    @Mock
    private WfTaskMessageDealMapper wfTaskMessageDealMapper;

    @Mock
    private WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;

    @Mock
    private WorkflowProjectService workflowProjectService;

    @Mock
    private WorkflowSystemService workflowSystemService;

    @Mock
    private FlowableEntityEventImpl event;

    @Mock
    private ExecutionEntityImpl processInstance;

    @Test
    void shouldReadTaskProcessFromProjectServiceWhenTaskApprovalPasses() {
        GlobalProcessEndListener listener = newListener();
        WfTaskProcess taskProcess = new WfTaskProcess();
        taskProcess.setExtraId("task-1");

        mockCommonContext("1");
        when(workflowProjectService.selectByInstanceIdAndType("proc-1", ProcessUtils.TASK_APPROVAL_TYPE))
                .thenReturn(taskProcess);
        when(wfTaskMessageDealMapper.selectList(any())).thenReturn(Collections.<WfTaskMessageDeal>emptyList());

        ReflectionTestUtils.invokeMethod(listener, "processCompleted", event);

        verify(workflowProjectService).selectByInstanceIdAndType("proc-1", ProcessUtils.TASK_APPROVAL_TYPE);
        verify(workflowProjectService).updateTaskApprovedStatus("task-1");
        verify(wfTaskProcessMapper, never()).selectOne(any());
    }

    @Test
    void shouldReadTaskProcessFromProjectServiceWhenTaskApprovalRejects() {
        GlobalProcessEndListener listener = newListener();
        WfTaskProcess taskProcess = new WfTaskProcess();
        taskProcess.setExtraId("task-1");

        mockCommonContext("3");
        when(workflowProjectService.selectByInstanceIdAndType("proc-1", ProcessUtils.TASK_APPROVAL_TYPE))
                .thenReturn(taskProcess);
        when(wfMaterialsScrappedProcessMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(wfTaskMessageDealMapper.selectList(any())).thenReturn(Collections.<WfTaskMessageDeal>emptyList());

        ReflectionTestUtils.invokeMethod(listener, "processCompleted", event);

        verify(workflowProjectService).selectByInstanceIdAndType("proc-1", ProcessUtils.TASK_APPROVAL_TYPE);
        verify(workflowProjectService).resetTaskStatus("task-1");
        verify(wfTaskProcessMapper, never()).selectOne(any());
    }

    private void mockCommonContext(String taskType) {
        TaskCompletedStateVO taskCompletedStateVO = new TaskCompletedStateVO();
        taskCompletedStateVO.setTaskType(taskType);

        SysUser sysUser = new SysUser();
        sysUser.setUserWxName("");

        when(event.getType()).thenReturn(FlowableEngineEventType.PROCESS_COMPLETED);
        when(event.getEntity()).thenReturn(processInstance);
        when(event.getProcessInstanceId()).thenReturn("proc-1");
        when(processInstance.getVariable(ProcessUtils.APPROVAL_TYPE)).thenReturn(ProcessUtils.TASK_APPROVAL_TYPE);
        when(processInstance.getVariable(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR)).thenReturn("1");
        when(processInstance.getProcessDefinitionKey()).thenReturn("Process_1");
        when(processInstance.getId()).thenReturn("proc-1");
        when(listenerMapper.getLastTaskCompletedState("proc-1")).thenReturn(taskCompletedStateVO);
        when(workflowSystemService.selectUserById(1L)).thenReturn(sysUser);
    }

    private GlobalProcessEndListener newListener() {
        GlobalProcessEndListener listener = new GlobalProcessEndListener();
        ReflectionTestUtils.setField(listener, "listenerMapper", listenerMapper);
        ReflectionTestUtils.setField(listener, "wfCopyMapper", wfCopyMapper);
        ReflectionTestUtils.setField(listener, "wfTaskProcessMapper", wfTaskProcessMapper);
        ReflectionTestUtils.setField(listener, "wfTaskMessageDealMapper", wfTaskMessageDealMapper);
        ReflectionTestUtils.setField(listener, "wfMaterialsScrappedProcessMapper", wfMaterialsScrappedProcessMapper);
        ReflectionTestUtils.setField(listener, "workflowProjectService", workflowProjectService);
        ReflectionTestUtils.setField(listener, "workflowSystemService", workflowSystemService);
        return listener;
    }
}
