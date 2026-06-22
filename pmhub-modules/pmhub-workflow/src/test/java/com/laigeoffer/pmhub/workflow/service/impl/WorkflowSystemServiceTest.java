package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.base.core.annotation.DataSource;
import com.laigeoffer.pmhub.base.core.enums.DataSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowSystemServiceTest {

    @Test
    void systemUserReadShouldSuspendOuterWorkflowTransaction() throws Exception {
        Method method = WorkflowSystemService.class.getMethod("selectUserById", Long.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.SYSTEM, dataSource.value());
        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
        assertEquals(true, transactional.readOnly());
    }

    @Test
    void systemRoleUserReadShouldSuspendOuterWorkflowTransaction() throws Exception {
        Method method = WorkflowSystemService.class.getMethod("selectUserIdsByRoleId", Long.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.SYSTEM, dataSource.value());
        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
        assertEquals(true, transactional.readOnly());
    }

    @Test
    void systemDeptReadShouldSuspendOuterWorkflowTransaction() throws Exception {
        Method method = WorkflowSystemService.class.getMethod("selectDeptById", Long.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.SYSTEM, dataSource.value());
        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
        assertEquals(true, transactional.readOnly());
    }

    @Test
    void systemDeptUserReadShouldSuspendOuterWorkflowTransaction() throws Exception {
        Method method = WorkflowSystemService.class.getMethod("selectUserIds", java.util.List.class);

        DataSource dataSource = method.getAnnotation(DataSource.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(DataSourceType.SYSTEM, dataSource.value());
        assertEquals(Propagation.NOT_SUPPORTED, transactional.propagation());
        assertEquals(true, transactional.readOnly());
    }
}
