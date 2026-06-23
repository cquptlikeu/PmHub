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

    /**
     * 根据风险类型码返回中文名称；未知码原样返回，避免前端硬编码翻译表导致前后端契约漂移。
     */
    public static String descByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RiskType type : values()) {
            if (type.code.equals(code)) {
                return type.desc;
            }
        }
        return code;
    }
}
