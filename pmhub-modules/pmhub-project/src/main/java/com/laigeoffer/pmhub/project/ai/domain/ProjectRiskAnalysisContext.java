package com.laigeoffer.pmhub.project.ai.domain;

import com.laigeoffer.pmhub.project.domain.ProjectTask;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class ProjectRiskAnalysisContext {

    private String analysisTaskId;

    private String projectId;

    private String operator;

    private Date analyzeTime;

    private List<ProjectTask> projectTasks;
}
