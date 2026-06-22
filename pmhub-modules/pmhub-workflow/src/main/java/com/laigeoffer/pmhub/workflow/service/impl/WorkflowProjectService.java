package com.laigeoffer.pmhub.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.laigeoffer.pmhub.base.core.annotation.DataSource;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.core.enums.DataSourceType;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalSet;
import com.laigeoffer.pmhub.workflow.domain.WfMaterialsScrappedProcess;
import com.laigeoffer.pmhub.workflow.mapper.WfApprovalSetMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class WorkflowProjectService {

    @Autowired
    private WfTaskProcessMapper wfTaskProcessMapper;

    @Autowired
    private WfApprovalSetMapper wfApprovalSetMapper;

    @Autowired
    private WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public WfTaskProcess selectByInstanceId(String instanceId) {
        LambdaQueryWrapper<WfTaskProcess> qw = new LambdaQueryWrapper<>();
        qw.eq(WfTaskProcess::getInstanceId, instanceId);
        return wfTaskProcessMapper.selectOne(qw);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public WfTaskProcess selectByInstanceIdAndType(String instanceId, String type) {
        LambdaQueryWrapper<WfTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfTaskProcess::getInstanceId, instanceId)
                .eq(WfTaskProcess::getType, type);
        return wfTaskProcessMapper.selectOne(queryWrapper);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateDeployInfo(String oldDefinitionKey,
                                 String newDefinitionId,
                                 String deploymentId) {

        LambdaUpdateChainWrapper<WfTaskProcess> wfTaskProcess =
                new LambdaUpdateChainWrapper<>(wfTaskProcessMapper);
        wfTaskProcess.likeRight(WfTaskProcess::getDefinitionId, oldDefinitionKey)
                .eq(WfTaskProcess::getApproved, 0)
                .isNull(WfTaskProcess::getInstanceId)
                .set(WfTaskProcess::getDefinitionId, newDefinitionId)
                .set(WfTaskProcess::getDeploymentId, deploymentId);
        wfTaskProcess.update();
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Integer selectTaskExecuteStatus(String taskId) {
        return wfTaskProcessMapper.selectStatusByTaskId(taskId);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Integer selectTaskStatus(String taskId) {
        return wfTaskProcessMapper.selectStatusByTaskId2(taskId);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public WfTaskProcess selectTaskProcess(String extraId, String type) {
        LambdaQueryWrapper<WfTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfTaskProcess::getExtraId, extraId).eq(WfTaskProcess::getType, type);
        return wfTaskProcessMapper.selectOne(queryWrapper);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public WfTaskProcess upsertTaskProcess(String extraId, String type, String approved, String definitionId, String deploymentId) {
        String operator = currentOperator();
        WfTaskProcess wfTaskProcess = selectTaskProcess(extraId, type);
        if (wfTaskProcess == null) {
            wfTaskProcess = new WfTaskProcess();
            wfTaskProcess.setExtraId(extraId);
            wfTaskProcess.setType(type);
            wfTaskProcess.setCreatedBy(operator);
            wfTaskProcess.setCreatedTime(new Date());
        }
        wfTaskProcess.setApproved(approved);
        wfTaskProcess.setDefinitionId(definitionId);
        wfTaskProcess.setDeploymentId(deploymentId);
        wfTaskProcess.setUpdatedBy(operator);
        wfTaskProcess.setUpdatedTime(new Date());
        if (StringUtils.isBlank(wfTaskProcess.getId())) {
            wfTaskProcessMapper.insert(wfTaskProcess);
        } else {
            wfTaskProcessMapper.updateById(wfTaskProcess);
        }
        return wfTaskProcess;
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateTaskProcessInstanceInfo(WfTaskProcess wfTaskProcess) {
        wfTaskProcess.setUpdatedBy(currentOperator());
        wfTaskProcess.setUpdatedTime(new Date());
        wfTaskProcessMapper.updateById(wfTaskProcess);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateTaskApprovedStatus(String taskId) {
        wfTaskProcessMapper.updateTaskStatus(taskId);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void resetTaskStatus(String taskId) {
        wfTaskProcessMapper.updateTaskStatus2(taskId);
    }

    @DataSource(DataSourceType.PROJECT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markTaskInProgress(String taskId) {
        wfTaskProcessMapper.updateTaskStatus3(taskId);
    }

    private String currentOperator() {
        try {
            String username = SecurityUtils.getUsername();
            if (StringUtils.isNotBlank(username)) {
                return username;
            }
        } catch (ServiceException ignored) {
            // fall back to request header/user context below
        }
        return "admin";
    }
}
