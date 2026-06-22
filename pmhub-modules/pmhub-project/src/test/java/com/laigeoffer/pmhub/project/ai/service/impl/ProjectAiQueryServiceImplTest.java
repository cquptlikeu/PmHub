package com.laigeoffer.pmhub.project.ai.service.impl;

import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.project.ai.client.WorkflowRiskClient;
import com.laigeoffer.pmhub.project.ai.constant.AnalysisTaskStatus;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectAiAnalysisTaskMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectRiskRecordMapper;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiNarrativeService;
import com.laigeoffer.pmhub.project.ai.vo.ProjectAiSummaryVO;
import com.laigeoffer.pmhub.project.ai.vo.ProjectRiskRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAiQueryServiceImplTest {

    @Mock
    private ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper;

    @Mock
    private ProjectHealthSnapshotMapper projectHealthSnapshotMapper;

    @Mock
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Mock
    private WorkflowRiskClient workflowRiskClient;

    @Spy
    private ProjectAiNarrativeService projectAiNarrativeService = new ProjectAiNarrativeServiceImpl();

    @InjectMocks
    private ProjectAiQueryServiceImpl projectAiQueryService;

    @Test
    void shouldQueryRiskRecordsByLatestSnapshotTaskId() {
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setAnalysisTaskId("analysis-1");
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(snapshot);

        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setProjectId("project-1");
        record.setAnalysisTaskId("analysis-1");
        record.setTitle("workflow blocked");
        when(projectRiskRecordMapper.selectByAnalysisTaskId("analysis-1"))
                .thenReturn(Collections.singletonList(record));

        List<ProjectRiskRecordVO> records = projectAiQueryService.queryRiskRecords("project-1");

        assertEquals(1, records.size());
        assertEquals("analysis-1", records.get(0).getAnalysisTaskId());
        verify(projectRiskRecordMapper).selectByAnalysisTaskId("analysis-1");
        verify(projectRiskRecordMapper, never()).selectByProjectId("project-1");
    }

    @Test
    void shouldReturnEmptyRiskListWhenLatestSnapshotMissing() {
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(null);

        List<ProjectRiskRecordVO> records = projectAiQueryService.queryRiskRecords("project-1");

        assertTrue(records.isEmpty());
        verify(projectRiskRecordMapper, never()).selectByAnalysisTaskId("project-1");
    }

    @Test
    void shouldFillWorkflowSummaryWhenQuerySummary() {
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setAnalysisTaskId("analysis-1");
        snapshot.setHealthScore(80);
        snapshot.setHealthLevel("HEALTHY");
        snapshot.setRiskCount(1);
        snapshot.setHighRiskCount(0);
        snapshot.setDeductionDetail("none");
        snapshot.setSnapshotTime(new Date());
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(snapshot);

        ProjectWorkflowRiskSummaryVO workflowSummary = new ProjectWorkflowRiskSummaryVO();
        workflowSummary.setProjectId("project-1");
        workflowSummary.setSummary("项目发布审批进行中");
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(workflowSummary);

        ProjectRiskRecord record = new ProjectRiskRecord();
        record.setProjectId("project-1");
        record.setAnalysisTaskId("analysis-1");
        record.setRiskLevel(RiskLevel.HIGH.getCode());
        record.setTitle("流程审批卡点");
        when(projectRiskRecordMapper.selectByAnalysisTaskId("analysis-1"))
                .thenReturn(Collections.singletonList(record));

        ProjectAiSummaryVO summaryVO = projectAiQueryService.querySummary("project-1");

        assertNotNull(summaryVO);
        assertEquals("project-1", summaryVO.getProjectId());
        assertEquals("项目发布审批进行中", summaryVO.getWorkflowSummary());
        assertNotNull(summaryVO.getAiSummary());
        assertTrue(summaryVO.getAiSummary().contains("80"));
        assertTrue(summaryVO.getAiSummary().contains("流程审批卡点"));
        assertTrue(summaryVO.getAiSummary().contains("项目发布审批进行中"));
    }

    @Test
    void shouldExposeLatestFailedTaskWhenSnapshotIsOlder() {
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setAnalysisTaskId("analysis-success");
        snapshot.setHealthScore(100);
        snapshot.setHealthLevel("HEALTHY");
        snapshot.setRiskCount(0);
        snapshot.setHighRiskCount(0);
        snapshot.setSnapshotTime(new Date());
        when(projectHealthSnapshotMapper.selectLatestByProjectId("project-1")).thenReturn(snapshot);

        ProjectAiAnalysisTask failedTask = new ProjectAiAnalysisTask();
        failedTask.setId("analysis-failed");
        failedTask.setProjectId("project-1");
        failedTask.setStatus(AnalysisTaskStatus.FAILED.getCode());
        failedTask.setErrorMessage("workflow service unavailable");
        when(projectAiAnalysisTaskMapper.selectOne(any())).thenReturn(failedTask);

        ProjectWorkflowRiskSummaryVO workflowSummary = new ProjectWorkflowRiskSummaryVO();
        workflowSummary.setSummary("当前无审批卡点");
        when(workflowRiskClient.getProjectRiskSummary("project-1")).thenReturn(workflowSummary);

        ProjectAiSummaryVO summaryVO = projectAiQueryService.querySummary("project-1");

        assertEquals("analysis-failed", summaryVO.getAnalysisTaskId());
        assertEquals(AnalysisTaskStatus.FAILED.getCode(), summaryVO.getAnalysisStatus());
        assertEquals("workflow service unavailable", summaryVO.getErrorMessage());
        assertEquals(Integer.valueOf(100), summaryVO.getHealthScore());
    }
}
