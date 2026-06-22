package com.laigeoffer.pmhub.project.service.impl;

import com.github.pagehelper.PageInfo;
import com.laigeoffer.pmhub.api.system.UserFeignService;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.context.SecurityContextHolder;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.core.core.domain.vo.SysUserVO;
import com.laigeoffer.pmhub.project.ai.constant.HealthLevel;
import com.laigeoffer.pmhub.project.ai.domain.ProjectHealthSnapshot;
import com.laigeoffer.pmhub.project.ai.mapper.ProjectHealthSnapshotMapper;
import com.laigeoffer.pmhub.project.domain.vo.project.ProjectReqVO;
import com.laigeoffer.pmhub.project.domain.vo.project.ProjectResVO;
import com.laigeoffer.pmhub.project.mapper.ProjectCollectionMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectMemberMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectStageMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskMapper;
import com.laigeoffer.pmhub.project.service.ProjectLogService;
import com.laigeoffer.pmhub.project.service.project.QueryProjectFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectTaskMapper projectTaskMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectStageMapper projectStageMapper;

    @Mock
    private ProjectLogService projectLogService;

    @Mock
    private ProjectCollectionMapper projectCollectionMapper;

    @Mock
    private QueryProjectFactory queryProjectFactory;

    @Mock
    private UserFeignService userFeignService;

    @Mock
    private ProjectHealthSnapshotMapper projectHealthSnapshotMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("admin");
        loginUser.setUser(sysUser);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    @Test
    void shouldAttachLatestAiHealthSnapshotToProjectList() {
        ProjectReqVO request = new ProjectReqVO();
        request.setPageNum(1);
        request.setPageSize(10);

        ProjectResVO project = new ProjectResVO();
        project.setProjectId("project-1");
        project.setProjectCode("P001");
        project.setProjectName("risk project");
        project.setUserId(1L);
        project.setStatus(1);
        project.setProjectType(0);
        project.setPublished(1);
        when(queryProjectFactory.execute(request)).thenReturn(Collections.singletonList(project));

        SysUserVO user = new SysUserVO();
        user.setUserId(1L);
        user.setNickName("admin");
        when(userFeignService.listOfInner(any(), eq(SecurityConstants.INNER)))
                .thenReturn(R.ok(Collections.singletonList(user)));

        Date snapshotTime = new Date();
        ProjectHealthSnapshot snapshot = new ProjectHealthSnapshot();
        snapshot.setProjectId("project-1");
        snapshot.setHealthScore(72);
        snapshot.setHealthLevel(HealthLevel.WARNING.getCode());
        snapshot.setRiskCount(2);
        snapshot.setHighRiskCount(1);
        snapshot.setSnapshotTime(snapshotTime);
        when(projectHealthSnapshotMapper.selectLatestByProjectIds(Collections.singletonList("project-1")))
                .thenReturn(Collections.singletonList(snapshot));

        PageInfo<ProjectResVO> result = projectService.list(request);

        ProjectResVO row = result.getList().get(0);
        assertEquals(72, row.getHealthScore());
        assertEquals(HealthLevel.WARNING.getCode(), row.getHealthLevel());
        assertEquals(2, row.getRiskCount());
        assertEquals(1, row.getHighRiskCount());
        assertEquals(snapshotTime, row.getLatestAnalyzeTime());
    }
}
