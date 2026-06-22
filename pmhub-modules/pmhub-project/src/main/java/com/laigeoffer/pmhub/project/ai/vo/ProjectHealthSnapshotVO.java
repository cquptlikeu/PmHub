package com.laigeoffer.pmhub.project.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProjectHealthSnapshotVO {

    private String id;

    private String projectId;

    private String analysisTaskId;

    private Integer healthScore;

    private String healthLevel;

    private String deductionDetail;

    private Integer riskCount;

    private Integer highRiskCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date snapshotTime;
}
