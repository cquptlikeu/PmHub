package com.laigeoffer.pmhub.project.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectRiskRecordMapper extends BaseMapper<ProjectRiskRecord> {

    List<ProjectRiskRecord> selectByProjectId(@Param("projectId") String projectId);

    List<ProjectRiskRecord> selectByAnalysisTaskId(@Param("analysisTaskId") String analysisTaskId);
}
