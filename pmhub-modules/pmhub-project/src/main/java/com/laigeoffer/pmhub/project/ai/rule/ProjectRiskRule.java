package com.laigeoffer.pmhub.project.ai.rule;

import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskAnalysisContext;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;

import java.util.List;

public interface ProjectRiskRule {

    List<ProjectRiskRecord> evaluate(ProjectRiskAnalysisContext context);
}
