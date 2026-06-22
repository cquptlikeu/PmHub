package com.laigeoffer.pmhub.project.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.utils.uuid.IdUtils;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.project.ai.client.WorkflowRiskClient;
import com.laigeoffer.pmhub.project.ai.constant.WeeklyReportStatus;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.domain.ProjectWeeklyReport;
import com.laigeoffer.pmhub.project.ai.dto.ProjectWeeklyReportDraft;
import com.laigeoffer.pmhub.project.ai.dto.WeeklyReportGenerateRequest;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectRiskRecordMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectWeeklyReportMapper;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiNarrativeService;
import com.laigeoffer.pmhub.project.ai.service.ProjectWeeklyReportService;
import com.laigeoffer.pmhub.project.ai.vo.ProjectWeeklyReportVO;
import com.laigeoffer.pmhub.project.domain.Project;
import com.laigeoffer.pmhub.project.mapper.ProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectWeeklyReportServiceImpl implements ProjectWeeklyReportService {

    @Resource
    private ProjectWeeklyReportMapper projectWeeklyReportMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectHealthSnapshotMapper projectHealthSnapshotMapper;

    @Resource
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Resource
    private WorkflowRiskClient workflowRiskClient;

    @Resource
    private ProjectAiNarrativeService projectAiNarrativeService;

    @Override
    public String generateWeeklyReport(WeeklyReportGenerateRequest request) {
        validateRequest(request);
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new ServiceException("项目不存在或已删除");
        }

        List<ProjectWeeklyReport> existingReports = findExistingReports(
                request.getProjectId(), request.getWeekStart(), request.getWeekEnd());
        if (!Boolean.TRUE.equals(request.getForceRegenerate()) && !existingReports.isEmpty()) {
            return existingReports.get(0).getId();
        }

        ProjectHealthSnapshot snapshot = projectHealthSnapshotMapper.selectLatestByProjectId(request.getProjectId());
        List<ProjectRiskRecord> risks = loadRisks(snapshot);
        String workflowSummary = workflowRiskClient.getProjectRiskSummary(request.getProjectId()).getSummary();
        ProjectWeeklyReportDraft draft = projectAiNarrativeService.buildWeeklyReport(
                request.getProjectId(), request.getWeekStart(), request.getWeekEnd(), snapshot, risks, workflowSummary);

        ProjectWeeklyReport report = buildReport(request, snapshot, draft, resolveNextVersion(existingReports));
        int rows = projectWeeklyReportMapper.insert(report);
        if (rows != 1) {
            throw new ServiceException("生成周报失败");
        }
        return report.getId();
    }

    @Override
    public List<ProjectWeeklyReportVO> queryWeeklyReports(String projectId) {
        if (isBlank(projectId)) {
            throw new ServiceException("项目ID不能为空");
        }
        List<ProjectWeeklyReport> reports = projectWeeklyReportMapper.selectByProjectId(projectId);
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyList();
        }
        return reports.stream().map(report -> {
            ProjectWeeklyReportVO vo = new ProjectWeeklyReportVO();
            BeanUtils.copyProperties(report, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private void validateRequest(WeeklyReportGenerateRequest request) {
        if (request == null || isBlank(request.getProjectId())) {
            throw new ServiceException("项目ID不能为空");
        }
        if (request.getWeekStart() == null || request.getWeekEnd() == null
                || request.getWeekEnd().before(request.getWeekStart())) {
            throw new ServiceException("周报时间范围不合法");
        }
    }

    private List<ProjectWeeklyReport> findExistingReports(String projectId, Date weekStart, Date weekEnd) {
        LambdaQueryWrapper<ProjectWeeklyReport> queryWrapper = new LambdaQueryWrapper<ProjectWeeklyReport>()
                .eq(ProjectWeeklyReport::getProjectId, projectId)
                .eq(ProjectWeeklyReport::getWeekStart, weekStart)
                .eq(ProjectWeeklyReport::getWeekEnd, weekEnd)
                .orderByDesc(ProjectWeeklyReport::getVersion)
                .orderByDesc(ProjectWeeklyReport::getCreatedTime);
        List<ProjectWeeklyReport> reports = projectWeeklyReportMapper.selectList(queryWrapper);
        return reports == null ? Collections.emptyList() : reports;
    }

    private List<ProjectRiskRecord> loadRisks(ProjectHealthSnapshot snapshot) {
        if (snapshot == null || snapshot.getAnalysisTaskId() == null) {
            return Collections.emptyList();
        }
        return projectRiskRecordMapper.selectByAnalysisTaskId(snapshot.getAnalysisTaskId());
    }

    private int resolveNextVersion(List<ProjectWeeklyReport> existingReports) {
        if (existingReports == null || existingReports.isEmpty()) {
            return 1;
        }
        Integer currentVersion = existingReports.get(0).getVersion();
        return currentVersion == null ? 1 : currentVersion + 1;
    }

    private ProjectWeeklyReport buildReport(WeeklyReportGenerateRequest request, ProjectHealthSnapshot snapshot,
                                            ProjectWeeklyReportDraft draft, int version) {
        Date now = new Date();
        String username = SecurityUtils.getUsername();
        ProjectWeeklyReport report = new ProjectWeeklyReport();
        report.setId(IdUtils.fastUUID());
        report.setProjectId(request.getProjectId());
        report.setAnalysisTaskId(snapshot == null ? null : snapshot.getAnalysisTaskId());
        report.setWeekStart(request.getWeekStart());
        report.setWeekEnd(request.getWeekEnd());
        report.setContent(draft.getContent());
        report.setStructuredContent(draft.getStructuredContent());
        report.setVersion(version);
        report.setStatus(WeeklyReportStatus.SUCCESS.getCode());
        report.setCreatedBy(username);
        report.setCreatedTime(now);
        report.setUpdatedBy(username);
        report.setUpdatedTime(now);
        return report;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
