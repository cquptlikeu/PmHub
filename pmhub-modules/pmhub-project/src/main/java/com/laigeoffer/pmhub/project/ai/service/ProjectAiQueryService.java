package com.laigeoffer.pmhub.project.ai.service;

import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.vo.ProjectAiSummaryVO;
import com.laigeoffer.pmhub.project.ai.vo.ProjectHealthSnapshotVO;
import com.laigeoffer.pmhub.project.ai.vo.ProjectRiskRecordVO;

import java.util.List;

public interface ProjectAiQueryService {

    ProjectAiSummaryVO querySummary(String projectId);

    ProjectHealthSnapshotVO queryLatestSnapshot(String projectId);

    List<ProjectRiskRecordVO> queryRiskRecords(String projectId);

    ProjectAiAnalysisTask queryTask(String analysisTaskId);
}
