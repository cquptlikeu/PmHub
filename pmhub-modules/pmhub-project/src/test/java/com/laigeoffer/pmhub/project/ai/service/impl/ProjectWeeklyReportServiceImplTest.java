package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.context.SecurityContextHolder;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.project.ai.client.WorkflowRiskClient;
import com.laigeoffer.pmhub.project.ai.constant.HealthLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.domain.ProjectWeeklyReport;
import com.laigeoffer.pmhub.project.ai.dto.ProjectWeeklyReportDraft;
import com.laigeoffer.pmhub.project.ai.dto.WeeklyReportGenerateRequest;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectRiskRecordMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectWeeklyReportMapper;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiNarrativeService;
import com.laigeoffer.pmhub.project.domain.Project;
import com.laigeoffer.pmhub.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectWeeklyReportServiceImplTest {

    @Mock
    private ProjectWeeklyReportMapper projectWeeklyReportMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectHealthSnapshotMapper projectHealthSnapshotMapper;

    @Mock
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Mock
    private WorkflowRiskClient workflowRiskClient;

    @Spy
    private ProjectAiNarrativeService projectAiNarrativeService = new ProjectAiNarrativeServiceImpl();

    @InjectMocks
    private ProjectWeeklyReportServiceImpl projectWeeklyReportService;

    @Captor
    private ArgumentCaptor<ProjectWeeklyReport> reportCaptor;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        loginUser.setUser(sysUser);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void shouldGenerateWeeklyReportAndPersistContent() {
        WeeklyReportGenerateRequest request = new WeeklyReportGenerateRequest();
        request.setProjectId("project-1");
        request.setWeekStart(new Date(1714492800000L));
        request.setWeekEnd(new Date(1715097600000L));
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));
        when(projectWeeklyReportMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(projectWeeklyReportMapper.insert(any(ProjectWeeklyReport.class))).thenReturn(1);

        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setAnalysisTaskId("analysis-1");
        snapshot.setHealthScore(75);
        snapshot.setHealthLevel(HealthLevel.WARNING.getCode());
        snapshot.setRiskCount(2);
        snapshot.setHighRiskCount(1);
        snapshot.setDeductionDetail("延期任务扣10分");
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(snapshot);

        ProjectRiskRecord highRisk = new ProjectRiskRecord();
        highRisk.setAnalysisTaskId("analysis-1");
        highRisk.setRiskLevel(RiskLevel.HIGH.getCode());
        highRisk.setTitle("延期任务");
        ProjectRiskRecord mediumRisk = new ProjectRiskRecord();
        mediumRisk.setAnalysisTaskId("analysis-1");
        mediumRisk.setRiskLevel(RiskLevel.MEDIUM.getCode());
        mediumRisk.setTitle("流程审批卡点");
        when(projectRiskRecordMapper.selectByAnalysisTaskId("analysis-1"))
                .thenReturn(Arrays.asList(highRisk, mediumRisk));
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(buildWorkflowSummary());

        String reportId = projectWeeklyReportService.generateWeeklyReport(request);

        verify(projectWeeklyReportMapper).insert(reportCaptor.capture());
        ProjectWeeklyReport report = reportCaptor.getValue();
        assertEquals(reportId, report.getId());
        assertEquals("project-1", report.getProjectId());
        assertEquals("analysis-1", report.getAnalysisTaskId());
        // 无模型客户端 -> 走本地模板降级，状态应为 FALLBACK 并留痕，下游可区分真 AI 与兜底
        assertEquals("FALLBACK", report.getStatus());
        assertNotNull(report.getErrorMessage());
        assertTrue(report.getErrorMessage().contains("降级"));
        assertEquals(Integer.valueOf(1), report.getVersion());
        assertTrue(report.getContent().contains("75"));
        assertTrue(report.getContent().contains("延期任务"));
        assertTrue(report.getContent().contains("项目发布审批进行中"));
        assertTrue(report.getStructuredContent().contains("\"projectId\":\"project-1\""));
        assertEquals("tester", report.getCreatedBy());
        assertNotNull(report.getCreatedTime());
    }

    @Test
    void shouldMarkReportAsSuccessWhenModelGenerates() {
        WeeklyReportGenerateRequest request = new WeeklyReportGenerateRequest();
        request.setProjectId("project-1");
        request.setWeekStart(new Date(1714492800000L));
        request.setWeekEnd(new Date(1715097600000L));
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));
        when(projectWeeklyReportMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(projectWeeklyReportMapper.insert(any(ProjectWeeklyReport.class))).thenReturn(1);
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(buildSnapshot());
        when(projectRiskRecordMapper.selectByAnalysisTaskId("analysis-1")).thenReturn(Collections.emptyList());
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(buildWorkflowSummary());
        doReturn(ProjectWeeklyReportDraft.builder()
                .content("模型生成的周报内容")
                .structuredContent("{}")
                .modelGenerated(true)
                .build())
                .when(projectAiNarrativeService).buildWeeklyReport(any(), any(), any(), any(), any(), any());

        projectWeeklyReportService.generateWeeklyReport(request);

        verify(projectWeeklyReportMapper).insert(reportCaptor.capture());
        ProjectWeeklyReport report = reportCaptor.getValue();
        assertEquals("SUCCESS", report.getStatus());
        assertNull(report.getErrorMessage());
        assertEquals("模型生成的周报内容", report.getContent());
    }

    @Test
    void shouldReuseExistingWeeklyReportWhenNotForced() {
        WeeklyReportGenerateRequest request = new WeeklyReportGenerateRequest();
        request.setProjectId("project-1");
        request.setWeekStart(new Date(1714492800000L));
        request.setWeekEnd(new Date(1715097600000L));
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));

        ProjectWeeklyReport existing = new ProjectWeeklyReport();
        existing.setId("report-1");
        existing.setWeekStart(request.getWeekStart());
        existing.setWeekEnd(request.getWeekEnd());
        existing.setVersion(2);
        when(projectWeeklyReportMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        String reportId = projectWeeklyReportService.generateWeeklyReport(request);

        assertEquals("report-1", reportId);
        verify(projectWeeklyReportMapper, never()).insert(any(ProjectWeeklyReport.class));
    }

    @Test
    void shouldCreateNextVersionWhenForceRegenerate() {
        WeeklyReportGenerateRequest request = new WeeklyReportGenerateRequest();
        request.setProjectId("project-1");
        request.setWeekStart(new Date(1714492800000L));
        request.setWeekEnd(new Date(1715097600000L));
        request.setForceRegenerate(Boolean.TRUE);
        when(projectMapper.selectById("project-1")).thenReturn(buildProject(0));

        ProjectWeeklyReport existing = new ProjectWeeklyReport();
        existing.setId("report-2");
        existing.setWeekStart(request.getWeekStart());
        existing.setWeekEnd(request.getWeekEnd());
        existing.setVersion(2);
        when(projectWeeklyReportMapper.selectList(any())).thenReturn(Collections.singletonList(existing));
        when(projectWeeklyReportMapper.insert(any(ProjectWeeklyReport.class))).thenReturn(1);
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(buildSnapshot());
        when(projectRiskRecordMapper.selectByAnalysisTaskId("analysis-1")).thenReturn(Collections.emptyList());
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(buildWorkflowSummary());

        String reportId = projectWeeklyReportService.generateWeeklyReport(request);

        verify(projectWeeklyReportMapper).insert(reportCaptor.capture());
        ProjectWeeklyReport report = reportCaptor.getValue();
        assertEquals(reportId, report.getId());
        assertEquals(Integer.valueOf(3), report.getVersion());
    }

    @Test
    void shouldQueryWeeklyReportHistory() {
        ProjectWeeklyReport latest = new ProjectWeeklyReport();
        latest.setId("report-latest");
        latest.setProjectId("project-1");
        latest.setVersion(2);
        latest.setContent("新版周报");
        latest.setCreatedTime(new Date(1715184000000L));
        ProjectWeeklyReport previous = new ProjectWeeklyReport();
        previous.setId("report-previous");
        previous.setProjectId("project-1");
        previous.setVersion(1);
        previous.setContent("旧版周报");
        previous.setCreatedTime(new Date(1715097600000L));
        when(projectWeeklyReportMapper.selectByProjectId("project-1")).thenReturn(Arrays.asList(latest, previous));

        List<com.laigeoffer.pmhub.project.ai.vo.ProjectWeeklyReportVO> reports =
                projectWeeklyReportService.queryWeeklyReports("project-1");

        assertEquals(2, reports.size());
        assertEquals("report-latest", reports.get(0).getId());
        assertEquals(Integer.valueOf(2), reports.get(0).getVersion());
        assertEquals("新版周报", reports.get(0).getContent());
        assertEquals("report-previous", reports.get(1).getId());
    }

    @Test
    void shouldRejectBlankProjectIdWhenQueryWeeklyReports() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectWeeklyReportService.queryWeeklyReports(" "));

        assertTrue(exception.getMessage().contains("项目ID"));
    }

    @Test
    void shouldRejectInvalidWeekRange() {
        WeeklyReportGenerateRequest request = new WeeklyReportGenerateRequest();
        request.setProjectId("project-1");
        request.setWeekStart(new Date(1715097600000L));
        request.setWeekEnd(new Date(1714492800000L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectWeeklyReportService.generateWeeklyReport(request));

        assertTrue(exception.getMessage().contains("周报时间范围"));
    }

    private Project buildProject(Integer deleted) {
        Project project = new Project();
        project.setId("project-1");
        project.setDeleted(deleted);
        return project;
    }

    private ProjectHealthSnapshot buildSnapshot() {
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setAnalysisTaskId("analysis-1");
        snapshot.setHealthScore(85);
        snapshot.setHealthLevel(HealthLevel.HEALTHY.getCode());
        snapshot.setRiskCount(0);
        snapshot.setHighRiskCount(0);
        snapshot.setDeductionDetail("未识别到风险");
        return snapshot;
    }

    private com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO buildWorkflowSummary() {
        com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO summary =
                new com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO();
        summary.setProjectId("project-1");
        summary.setProjectApprovalBlocked(Boolean.TRUE);
        summary.setTaskApprovalBlockedCount(1);
        summary.setTotalBlockedCount(2);
        summary.setSummary("项目发布审批进行中，另有1个任务审批进行中");
        return summary;
    }
}
