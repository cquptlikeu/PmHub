package com.laigeoffer.pmhub.project.ai.dto;

import lombok.Data;

@Data
public class ProjectAnalyzeRequest {

    private String projectId;

    private Boolean forceReanalyze;
}
