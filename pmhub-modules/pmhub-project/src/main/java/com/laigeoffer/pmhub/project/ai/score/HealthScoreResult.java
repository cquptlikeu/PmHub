package com.laigeoffer.pmhub.project.ai.score;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthScoreResult {

    private Integer healthScore;

    private String healthLevel;

    private String deductionDetail;

    private Integer highRiskCount;
}
