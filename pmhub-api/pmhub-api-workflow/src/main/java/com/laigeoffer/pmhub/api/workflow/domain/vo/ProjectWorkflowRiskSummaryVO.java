package com.laigeoffer.pmhub.api.workflow.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProjectWorkflowRiskSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String projectId;

    private Boolean projectApprovalBlocked;

    private Integer taskApprovalBlockedCount;

    private Integer totalBlockedCount;

    private String summary;
}
