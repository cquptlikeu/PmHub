package com.laigeoffer.pmhub.project.ai.controller;

import com.laigeoffer.pmhub.base.core.core.domain.AjaxResult;
import com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions;
import com.laigeoffer.pmhub.project.ai.dto.ProjectAnalyzeRequest;
import com.laigeoffer.pmhub.project.ai.dto.WeeklyReportGenerateRequest;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiAnalysisService;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiQueryService;
import com.laigeoffer.pmhub.project.ai.service.ProjectWeeklyReportService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/project/ai")
public class ProjectAiController {

    @Resource
    private ProjectAiAnalysisService projectAiAnalysisService;

    @Resource
    private ProjectAiQueryService projectAiQueryService;

    @Resource
    private ProjectWeeklyReportService projectWeeklyReportService;

    @RequiresPermissions("project:ai:analyze")
    @PostMapping("/analyze")
    public AjaxResult analyze(@RequestBody ProjectAnalyzeRequest request) {
        return AjaxResult.success(projectAiAnalysisService.createAnalysisTask(request));
    }

    @RequiresPermissions("project:ai:task")
    @GetMapping("/tasks/{analysisTaskId}")
    public AjaxResult queryTask(@PathVariable String analysisTaskId) {
        return AjaxResult.success(projectAiQueryService.queryTask(analysisTaskId));
    }

    @RequiresPermissions("project:ai:analyze")
    @PostMapping("/tasks/{analysisTaskId}/retry")
    public AjaxResult retryTask(@PathVariable String analysisTaskId) {
        return AjaxResult.success(projectAiAnalysisService.retryAnalysisTask(analysisTaskId));
    }

    @RequiresPermissions("project:ai:summary")
    @GetMapping("/summary/{projectId}")
    public AjaxResult querySummary(@PathVariable String projectId) {
        return AjaxResult.success(projectAiQueryService.querySummary(projectId));
    }

    @RequiresPermissions("project:ai:risks")
    @GetMapping("/risks/{projectId}")
    public AjaxResult queryRiskRecords(@PathVariable String projectId) {
        return AjaxResult.success(projectAiQueryService.queryRiskRecords(projectId));
    }

    @RequiresPermissions("project:ai:weeklyReportGenerate")
    @PostMapping("/weekly-report/generate")
    public AjaxResult generateWeeklyReport(@RequestBody WeeklyReportGenerateRequest request) {
        return AjaxResult.success(projectWeeklyReportService.generateWeeklyReport(request));
    }

    @RequiresPermissions("project:ai:weeklyReportList")
    @GetMapping("/weekly-report/{projectId}")
    public AjaxResult queryWeeklyReports(@PathVariable String projectId) {
        return AjaxResult.success(projectWeeklyReportService.queryWeeklyReports(projectId));
    }
}
