package com.laigeoffer.pmhub.api.workflow.factory;

import com.laigeoffer.pmhub.api.workflow.DeployFeignService;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Workflow deployment service fallback.
 *
 * @author canghe
 */
@Component
public class DeployFeignFallbackFactory implements FallbackFactory<DeployFeignService> {

    private static final Logger log = LoggerFactory.getLogger(DeployFeignFallbackFactory.class);

    @Override
    public DeployFeignService create(Throwable throwable) {
        log.error("workflow deploy service call failed: {}", throwable.getMessage(), throwable);
        return new DeployFeignService() {
            @Override
            public R<?> updateApprovalSet(ApprovalSetDTO approvalSetDTO, String source) {
                return R.fail("远程调用审批服务失败: " + throwable.getMessage());
            }

            @Override
            public R<?> updateApprovalSet2(ApprovalSetDTO approvalSetDTO, String source) {
                return R.fail("远程调用审批服务失败: " + throwable.getMessage());
            }

            @Override
            public R<?> selectList(List<String> taskId, String source) {
                return R.fail("远程调用审批服务失败: " + throwable.getMessage());
            }

            @Override
            public R<Boolean> insertOrUpdateApprovalSet(ApprovalSetDTO approvalSetDTO, String source) {
                return R.fail(Boolean.FALSE, "远程调用审批服务失败: " + throwable.getMessage());
            }

            @Override
            public R<?> insertApprovalSet(String source) {
                return R.fail("远程调用审批服务失败: " + throwable.getMessage());
            }
        };
    }
}
