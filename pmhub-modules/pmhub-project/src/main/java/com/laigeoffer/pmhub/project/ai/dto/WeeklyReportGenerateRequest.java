package com.laigeoffer.pmhub.project.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class WeeklyReportGenerateRequest {

    private String projectId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date weekStart;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date weekEnd;

    private Boolean forceRegenerate;
}
