package com.laigeoffer.pmhub.project.ai.constant;

public enum RiskType {
    DELAYED_TASK("DELAYED_TASK", "延期任务"),
    NEAR_DUE("NEAR_DUE", "临期任务"),
    BLOCKED_TASK("BLOCKED_TASK", "阻塞任务"),
    MEMBER_OVERLOAD("MEMBER_OVERLOAD", "成员负载过高"),
    WORKFLOW_BLOCKED("WORKFLOW_BLOCKED", "流程卡点");

    private final String code;
    private final String desc;

    RiskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
