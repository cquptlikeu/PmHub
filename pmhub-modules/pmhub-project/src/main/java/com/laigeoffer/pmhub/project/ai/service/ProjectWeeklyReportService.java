package com.laigeoffer.pmhub.project.ai.service;

import com.laigeoffer.pmhub.project.ai.dto.WeeklyReportGenerateRequest;
import com.laigeoffer.pmhub.project.ai.vo.ProjectWeeklyReportVO;

import java.util.List;

public interface ProjectWeeklyReportService {

    String generateWeeklyReport(WeeklyReportGenerateRequest request);

    List<ProjectWeeklyReportVO> queryWeeklyReports(String projectId);
}
