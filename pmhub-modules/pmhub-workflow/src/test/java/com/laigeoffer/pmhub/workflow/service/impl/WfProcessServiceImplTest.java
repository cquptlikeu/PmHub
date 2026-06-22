package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.workflow.domain.WfForm;
import com.laigeoffer.pmhub.workflow.mapper.WfCopyMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfDeployFormMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfFormMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import com.laigeoffer.pmhub.workflow.service.IWfDeployService;
import com.laigeoffer.pmhub.workflow.service.IWfTaskService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WfProcessServiceImplTest {

    @Mock
    private IWfTaskService wfTaskService;

    @Mock
    private WfCopyMapper wfCopyMapper;

    @Mock
    private WfDeployFormMapper deployFormMapper;

    @Mock
    private WfTaskProcessMapper wfTaskProcessMapper;

    @Mock
    private IWfDeployService deployService;

    @Mock
    private WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;

    @Mock
    private WfFormMapper wfFormMapper;

    @Mock
    private WorkflowProjectService workflowProjectService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;

    @Mock
    private ProcessDefinition processDefinition;

    @Test
    void shouldFallbackToWorkflowFormWhenDeployFormMappingMissing() {
        WfProcessServiceImpl service = buildService();
        when(repositoryService.getBpmnModel("def-1")).thenReturn(buildBpmnModel("start_1", "key_12"));
        when(deployFormMapper.selectVoOne(any())).thenReturn(null);

        WfForm wfForm = new WfForm();
        wfForm.setFormId(12L);
        wfForm.setContent("{\"fields\":[]}");
        when(wfFormMapper.selectById(12L)).thenReturn(wfForm);

        String content = service.selectFormContent("def-1", "dep-1");

        assertEquals("{\"fields\":[]}", content);
    }

    @Test
    void shouldThrowWhenStartFormConfigMissing() {
        WfProcessServiceImpl service = buildService();
        when(repositoryService.getBpmnModel("def-1")).thenReturn(buildBpmnModel("start_1", "key_12"));
        when(deployFormMapper.selectVoOne(any())).thenReturn(null);
        when(wfFormMapper.selectById(12L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.selectFormContent("def-1", "dep-1"));

        assertEquals("Start form content not found. Please redeploy or rebind the approval flow.", exception.getMessage());
    }

    @Test
    void shouldReturnEmptyFormWhenStartFormNotConfigured() {
        WfProcessServiceImpl service = buildService();
        when(repositoryService.getBpmnModel("def-1")).thenReturn(buildBpmnModel("start_1", null));

        String content = service.selectFormContent("def-1", "dep-1");

        assertEquals("{\"formRef\":\"elForm\",\"formModel\":\"formData\",\"size\":\"medium\",\"labelPosition\":\"right\",\"labelWidth\":100,\"formRules\":\"rules\",\"gutter\":15,\"disabled\":false,\"span\":24,\"formBtns\":true,\"fields\":[]}", content);
    }

    @Test
    void shouldReadTaskExecuteStatusFromProjectDataSourceService() {
        WfProcessServiceImpl service = buildService();
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("def-1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(processDefinition);
        when(workflowProjectService.selectTaskExecuteStatus("task-1"))
                .thenReturn(ProjectTaskStatusEnum.DOING.getStatus());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.startTaskProcessByDefId("task-1", "def-1", "/detail", new java.util.HashMap<>()));

        assertEquals("执行状态为已完成才能发起审批", exception.getMessage());
    }

    private WfProcessServiceImpl buildService() {
        WfProcessServiceImpl service = new WfProcessServiceImpl(
                wfTaskService,
                wfCopyMapper,
                deployFormMapper,
                wfTaskProcessMapper,
                deployService,
                wfMaterialsScrappedProcessMapper,
                wfFormMapper,
                workflowProjectService
        );
        ReflectionTestUtils.setField(service, "repositoryService", repositoryService);
        return service;
    }

    private BpmnModel buildBpmnModel(String startEventId, String formKey) {
        StartEvent startEvent = new StartEvent();
        startEvent.setId(startEventId);
        startEvent.setFormKey(formKey);

        Process process = new Process();
        process.addFlowElement(startEvent);

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        return bpmnModel;
    }
}
