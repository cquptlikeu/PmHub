package com.laigeoffer.pmhub.project.ai.score;

import com.laigeoffer.pmhub.project.ai.constant.HealthLevel;
import com.laigeoffer.pmhub.project.ai.constant.RiskLevel;
import com.laigeoffer.pmhub.project.ai.domain.ProjectRiskRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Service
public class ProjectHealthScoreService {

    private static final int BASE_SCORE = 100;

    public HealthScoreResult calculate(List<ProjectRiskRecord> risks) {
        List<HealthDeduction> deductions = new ArrayList<>();
        int highRiskCount = 0;
        for (ProjectRiskRecord risk : risks) {
            if (RiskLevel.HIGH.getCode().equals(risk.getRiskLevel())) {
                deductions.add(HealthDeduction.builder().reason(risk.getTitle()).score(20).build());
                highRiskCount++;
                continue;
            }
            if (RiskLevel.MEDIUM.getCode().equals(risk.getRiskLevel())) {
                deductions.add(HealthDeduction.builder().reason(risk.getTitle()).score(10).build());
                continue;
            }
            deductions.add(HealthDeduction.builder().reason(risk.getTitle()).score(5).build());
        }
        int totalDeduction = deductions.stream().mapToInt(HealthDeduction::getScore).sum();
        int finalScore = Math.max(0, BASE_SCORE - totalDeduction);
        return HealthScoreResult.builder()
                .healthScore(finalScore)
                .healthLevel(resolveHealthLevel(finalScore))
                .deductionDetail(buildDeductionDetail(deductions))
                .highRiskCount(highRiskCount)
                .build();
    }

    private String resolveHealthLevel(int healthScore) {
        if (healthScore >= 80) {
            return HealthLevel.HEALTHY.getCode();
        }
        if (healthScore >= 60) {
            return HealthLevel.WARNING.getCode();
        }
        return HealthLevel.DANGER.getCode();
    }

    private String buildDeductionDetail(List<HealthDeduction> deductions) {
        if (deductions.isEmpty()) {
            return "未识别到风险，健康分无扣减";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (HealthDeduction deduction : deductions) {
            joiner.add(deduction.getReason() + " -" + deduction.getScore());
        }
        return joiner.toString();
    }
}
