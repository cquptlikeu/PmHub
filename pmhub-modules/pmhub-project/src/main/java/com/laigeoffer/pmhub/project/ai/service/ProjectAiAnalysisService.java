package com.laigeoffer.pmhub.project.ai.service;

import com.laigeoffer.pmhub.project.ai.dto.ProjectAnalyzeRequest;

public interface ProjectAiAnalysisService {

    String createAnalysisTask(ProjectAnalyzeRequest request);

    String retryAnalysisTask(String analysisTaskId);
}
