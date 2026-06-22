package com.laigeoffer.pmhub.project.ai.constant;

public enum WeeklyReportStatus {
    SUCCESS("SUCCESS", "成功"),
    FALLBACK("FALLBACK", "降级生成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    WeeklyReportStatus(String code, String desc) {
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
