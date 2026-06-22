package com.laigeoffer.pmhub.project.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectHealthSnapshotMapper extends BaseMapper<ProjectHealthSnapshot> {

    ProjectHealthSnapshot selectLatestByProjectId(@Param("projectId") String projectId);

    List<ProjectHealthSnapshot> selectLatestByProjectIds(@Param("projectIds") List<String> projectIds);
}
