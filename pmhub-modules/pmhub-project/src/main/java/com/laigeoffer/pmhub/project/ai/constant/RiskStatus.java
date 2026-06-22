package com.laigeoffer.pmhub.project.ai.constant;

public enum RiskStatus {
    OPEN("OPEN", "待处理"),
    CONFIRMED("CONFIRMED", "已确认"),
    IGNORED("IGNORED", "已忽略"),
    RESOLVED("RESOLVED", "已解决");

    private final String code;
    private final String desc;

    RiskStatus(String code, String desc) {
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
