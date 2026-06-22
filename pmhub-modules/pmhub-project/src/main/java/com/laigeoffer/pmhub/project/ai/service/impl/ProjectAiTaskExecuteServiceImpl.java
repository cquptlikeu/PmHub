package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.project.ai.service.ProjectAiTaskExecuteService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProjectAiTaskExecuteServiceImpl implements ProjectAiTaskExecuteService {

    private final ProjectAiTaskRunner projectAiTaskRunner;

    public ProjectAiTaskExecuteServiceImpl(ProjectAiTaskRunner projectAiTaskRunner) {
        this.projectAiTaskRunner = projectAiTaskRunner;
    }

    @Override
    @Async("threadPoolTaskExecutor")
    public void submitAnalysis(String analysisTaskId) {
        projectAiTaskRunner.executeAnalysis(analysisTaskId);
    }
}
