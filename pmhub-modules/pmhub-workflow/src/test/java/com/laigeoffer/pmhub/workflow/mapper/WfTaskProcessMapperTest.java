package com.laigeoffer.pmhub.workflow.mapper;

import com.laigeoffer.pmhub.base.core.annotation.DataSource;
import com.laigeoffer.pmhub.base.core.enums.DataSourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WfTaskProcessMapperTest {

    @Test
    void shouldUseProjectDataSource() {
        DataSource dataSource = WfTaskProcessMapper.class.getAnnotation(DataSource.class);

        assertNotNull(dataSource);
        assertEquals(DataSourceType.PROJECT, dataSource.value());
    }
}
