package com.laigeoffer.pmhub.workflow.service.impl;

import com.laigeoffer.pmhub.base.core.annotation.DataSource;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysDept;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysRole;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.enums.DataSourceType;
import com.laigeoffer.pmhub.workflow.mapper.WfCopyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowSystemService {
    @Autowired
    private WfCopyMapper wfCopyMapper;

    @DataSource(DataSourceType.SYSTEM)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<Long> selectUserIdsByRoleId(Long roleId) {
        return wfCopyMapper.selectUserIdsByRoleId(roleId);
    }

    @DataSource(DataSourceType.SYSTEM)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public List<Long> selectUserIds(List<String> deptIds) {
        return wfCopyMapper.selectUserIds(deptIds);
    }

    @DataSource(DataSourceType.SYSTEM)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public SysUser selectUserById(Long userId) {
        return wfCopyMapper.selectUserById(userId);
    }

    @DataSource(DataSourceType.SYSTEM)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public SysRole selectRoleById(Long roleId) {
        return wfCopyMapper.selectRoleById(roleId);
    }

    @DataSource(DataSourceType.SYSTEM)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public SysDept selectDeptById(Long deptId) {
        return wfCopyMapper.selectDeptById(deptId);
    }
}
