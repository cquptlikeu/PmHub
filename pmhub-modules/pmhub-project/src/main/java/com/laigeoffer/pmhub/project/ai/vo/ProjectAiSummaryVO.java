package com.laigeoffer.pmhub.project.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProjectAiSummaryVO {

    private String projectId;

    private String analysisTaskId;

    private String analysisStatus;

    private String errorMessage;

    private Integer healthScore;

    private String healthLevel;

    private Integer riskCount;

    private Integer highRiskCount;

    private String deductionDetail;

    private String aiSummary;

    private String workflowSummary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date latestAnalyzeTime;
}
