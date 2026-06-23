package com.laigeoffer.pmhub.project.ai.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RiskTypeTest {

    @Test
    void shouldReturnChineseNameForKnownCode() {
        assertEquals("延期任务", RiskType.descByCode("DELAYED_TASK"));
        assertEquals("临期任务", RiskType.descByCode("NEAR_DUE"));
        assertEquals("阻塞任务", RiskType.descByCode("BLOCKED_TASK"));
        assertEquals("成员负载过高", RiskType.descByCode("MEMBER_OVERLOAD"));
        assertEquals("流程卡点", RiskType.descByCode("WORKFLOW_BLOCKED"));
    }

    @Test
    void shouldReturnRawCodeForUnknownCode() {
        assertEquals("UNKNOWN_CODE", RiskType.descByCode("UNKNOWN_CODE"));
    }

    @Test
    void shouldReturnNullWhenCodeIsNull() {
        assertNull(RiskType.descByCode(null));
    }
}
