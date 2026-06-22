package com.laigeoffer.pmhub.project.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectWeeklyReportDraft {

    private String content;

    private String structuredContent;
}
