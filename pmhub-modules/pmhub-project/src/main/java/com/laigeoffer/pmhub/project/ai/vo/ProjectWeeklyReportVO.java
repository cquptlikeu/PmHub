package com.laigeoffer.pmhub.project.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProjectWeeklyReportVO {

    private String id;

    private String projectId;

    private String analysisTaskId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date weekStart;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date weekEnd;

    private String content;

    private String structuredContent;

    private Integer version;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;
}
