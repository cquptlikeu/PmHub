package com.laigeoffer.pmhub.project.ai.score;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthDeduction {

    private String reason;

    private int score;
}
