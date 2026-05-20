package com.laigeoffer.pmhub.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.laigeoffer.pmhub.base.core.annotation.DataSource;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.core.enums.DataSourceType;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalSet;
import com.laigeoffer.pmhub.workflow.domain.WfMaterialsScrappedProcess;
import com.laigeoffer.pmhub.workflow.mapper.WfApprovalSetMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfTaskProcessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowProjectService {

    @Autowired
    private WfTaskProcessMapper wfTaskProcessMapper;

    @Autowired
    private WfApprovalSetMapper wfApprovalSetMapper;

    @Autowired
    private WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;

    @DataSource(DataSourceType.PROJECT)
    //@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public WfTaskProcess selectByInstanceId(String instanceId) {
        LambdaQueryWrapper<WfTaskProcess> qw = new LambdaQueryWrapper<>();
        qw.eq(WfTaskProcess::getInstanceId, instanceId);
        return wfTaskProcessMapper.selectOne(qw);
    }

    @DataSource(DataSourceType.PROJECT)
    //@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
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
}
