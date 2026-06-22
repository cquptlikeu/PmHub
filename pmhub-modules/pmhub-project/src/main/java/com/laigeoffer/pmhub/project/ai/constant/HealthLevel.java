package com.laigeoffer.pmhub.project.ai.constant;

public enum HealthLevel {
    HEALTHY("HEALTHY", "健康"),
    WARNING("WARNING", "关注"),
    DANGER("DANGER", "危险");

    private final String code;
    private final String desc;

    HealthLevel(String code, String desc) {
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
