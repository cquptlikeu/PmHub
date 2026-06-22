package com.laigeoffer.pmhub.api.workflow.factory;

import com.laigeoffer.pmhub.api.workflow.WorkflowRiskFeignService;
import com.laigeoffer.pmhub.api.workflow.domain.vo.ProjectWorkflowRiskSummaryVO;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRiskFeignFallbackFactory implements FallbackFactory<WorkflowRiskFeignService> {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRiskFeignFallbackFactory.class);

    @Override
    public WorkflowRiskFeignService create(Throwable throwable) {
        log.error("workflow risk service call failed: {}", throwable.getMessage());
        return new WorkflowRiskFeignService() {
            @Override
            public R<ProjectWorkflowRiskSummaryVO> getProjectRiskSummary(String projectId, String source) {
                return R.fail("查询项目流程风险摘要失败:" + throwable.getMessage());
            }
        };
    }
}
