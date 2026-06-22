package com.laigeoffer.pmhub.project.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laigeoffer.pmhub.project.ai.domain.ProjectWeeklyReport;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectWeeklyReportMapper extends BaseMapper<ProjectWeeklyReport> {

    List<ProjectWeeklyReport> selectByProjectId(@Param("projectId") String projectId);
}
