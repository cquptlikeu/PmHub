package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.base.core.annotation.DataSource;
import com.laigeoffer.pmhub.base.core.enums.DataSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowProjectServiceTest {

    @Test
    void projectReadShouldSuspendOuterWorkflowTransaction() throws Exception {
        Method method = WorkflowProjectService.class.getMethod("selectTaskExecuteStatus", String.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.PROJECT, dataSource.value());
        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
        assertEquals(true, transactional.readOnly());
    }

    @Test
    void projectWriteShouldUseIndependentProjectTransaction() throws Exception {
        Method method = WorkflowProjectService.class.getMethod("markTaskInProgress", String.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.PROJECT, dataSource.value());
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    @Test
    void taskProcessReadShouldSuspendOuterWorkflowTransaction() throws Exception {
        Method method = WorkflowProjectService.class.getMethod("selectTaskProcess", String.class, String.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.PROJECT, dataSource.value());
        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
        assertEquals(true, transactional.readOnly());
    }

    @Test
    void taskProcessWriteShouldUseIndependentProjectTransaction() throws Exception {
        Method method = WorkflowProjectService.class.getMethod("updateTaskProcessInstanceInfo",
                com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.PROJECT, dataSource.value());
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
