package com.laigeoffer.pmhub.project.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProjectRiskRecordVO {

    private String id;

    private String projectId;

    private String analysisTaskId;

    private String riskType;

    /**
     * 风险类型中文名称，由后端统一翻译，前端直接展示，避免前后端各维护一份字典。
     */
    private String riskTypeName;

    private String riskLevel;

    private String sourceType;

    private String sourceId;

    private String title;

    private String reason;

    private String suggestion;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;
}
