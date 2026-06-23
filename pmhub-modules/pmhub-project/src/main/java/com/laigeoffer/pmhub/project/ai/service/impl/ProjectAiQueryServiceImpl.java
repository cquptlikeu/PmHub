package com.laigeoffer.pmhub.project.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laigeoffer.pmhub.project.ai.client.WorkflowRiskClient;
import com.laigeoffer.pmhub.project.ai.constant.RiskType;
import com.laigeoffer.pmhub.project.ai.domain.ProjectAiAnalysisTask;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectAiAnalysisTaskMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectRiskRecordMapper;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiNarrativeService;
import com.laigeoffer.pmhub.project.ai.service.ProjectAiQueryService;
import com.laigeoffer.pmhub.project.ai.vo.ProjectAiSummaryVO;
import com.laigeoffer.pmhub.project.ai.vo.ProjectHealthSnapshotVO;
import com.laigeoffer.pmhub.project.ai.vo.ProjectRiskRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectAiQueryServiceImpl implements ProjectAiQueryService {

    @Resource
    private ProjectAiAnalysisTaskMapper projectAiAnalysisTaskMapper;

    @Resource
    private ProjectHealthSnapshotMapper projectHealthSnapshotMapper;

    @Resource
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Resource
    private WorkflowRiskClient workflowRiskClient;

    @Resource
    private ProjectAiNarrativeService projectAiNarrativeService;

    @Override
    public ProjectAiSummaryVO querySummary(String projectId) {
        ProjectHealthSnapshot snapshot = projectHealthSnapshotMapper.selectLatestByProjectId(projectId);
        ProjectAiAnalysisTask latestTask = loadLatestTask(projectId);
        String workflowSummary = workflowRiskClient.getProjectRiskSummary(projectId).getSummary();
        ProjectAiSummaryVO summaryVO = new ProjectAiSummaryVO();
        summaryVO.setProjectId(projectId);
        summaryVO.setWorkflowSummary(workflowSummary);
        fillLatestTaskStatus(summaryVO, latestTask);
        if (snapshot == null) {
            summaryVO.setAiSummary(projectAiNarrativeService.buildSummary(null, Collections.emptyList(), workflowSummary));
            return summaryVO;
        }

        summaryVO.setHealthScore(snapshot.getHealthScore());
        summaryVO.setHealthLevel(snapshot.getHealthLevel());
        summaryVO.setRiskCount(snapshot.getRiskCount());
        summaryVO.setHighRiskCount(snapshot.getHighRiskCount());
        summaryVO.setDeductionDetail(snapshot.getDeductionDetail());
        summaryVO.setLatestAnalyzeTime(snapshot.getSnapshotTime());

        if (latestTask == null || Objects.equals(snapshot.getAnalysisTaskId(), latestTask.getId())) {
            ProjectAiAnalysisTask snapshotTask = loadTask(snapshot.getAnalysisTaskId());
            fillLatestTaskStatus(summaryVO, snapshotTask);
            if (snapshotTask == null) {
                summaryVO.setAnalysisTaskId(snapshot.getAnalysisTaskId());
            }
        }

        List<ProjectRiskRecord> risks = loadRisks(snapshot.getAnalysisTaskId());
        summaryVO.setAiSummary(projectAiNarrativeService.buildSummary(snapshot, risks, workflowSummary));
        return summaryVO;
    }

    @Override
    public ProjectHealthSnapshotVO queryLatestSnapshot(String projectId) {
        ProjectHealthSnapshot snapshot = projectHealthSnapshotMapper.selectLatestByProjectId(projectId);
        if (snapshot == null) {
            return null;
        }
        ProjectHealthSnapshotVO snapshotVO = new ProjectHealthSnapshotVO();
        BeanUtils.copyProperties(snapshot, snapshotVO);
        return snapshotVO;
    }

    @Override
    public List<ProjectRiskRecordVO> queryRiskRecords(String projectId) {
        ProjectHealthSnapshot snapshot = projectHealthSnapshotMapper.selectLatestByProjectId(projectId);
        if (snapshot == null || snapshot.getAnalysisTaskId() == null) {
            return Collections.emptyList();
        }
        List<ProjectRiskRecord> records = loadRisks(snapshot.getAnalysisTaskId());
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(record -> {
            ProjectRiskRecordVO vo = new ProjectRiskRecordVO();
            BeanUtils.copyProperties(record, vo);
            vo.setRiskTypeName(RiskType.descByCode(record.getRiskType()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ProjectAiAnalysisTask queryTask(String analysisTaskId) {
        return projectAiAnalysisTaskMapper.selectById(analysisTaskId);
    }

    private void fillLatestTaskStatus(ProjectAiSummaryVO summaryVO, ProjectAiAnalysisTask task) {
        if (task == null) {
            return;
        }
        summaryVO.setAnalysisTaskId(task.getId());
        summaryVO.setAnalysisStatus(task.getStatus());
        summaryVO.setErrorMessage(task.getErrorMessage());
    }

    private ProjectAiAnalysisTask loadLatestTask(String projectId) {
        LambdaQueryWrapper<ProjectAiAnalysisTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectAiAnalysisTask::getProjectId, projectId)
                .orderByDesc(ProjectAiAnalysisTask::getCreatedTime)
                .last("limit 1");
        return projectAiAnalysisTaskMapper.selectOne(queryWrapper);
    }

    private ProjectAiAnalysisTask loadTask(String analysisTaskId) {
        if (analysisTaskId == null) {
            return null;
        }
        return projectAiAnalysisTaskMapper.selectById(analysisTaskId);
    }

    private List<ProjectRiskRecord> loadRisks(String analysisTaskId) {
        if (analysisTaskId == null) {
            return Collections.emptyList();
        }
        return projectRiskRecordMapper.selectByAnalysisTaskId(analysisTaskId);
    }
}
