package com.laigeoffer.pmhub.project.ai.service;

import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.dto.ProjectWeeklyReportDraft;

import java.util.Date;
import java.util.List;

public interface ProjectAiNarrativeService {

    String buildSummary(ProjectHealthSnapshot snapshot, List<ProjectRiskRecord> risks, String workflowSummary);

    ProjectWeeklyReportDraft buildWeeklyReport(String projectId, Date weekStart, Date weekEnd,
                                               ProjectHealthSnapshot snapshot, List<ProjectRiskRecord> risks,
                                               String workflowSummary);
}
