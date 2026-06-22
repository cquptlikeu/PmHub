package com.laigeoffer.pmhub.project.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectWeeklyReportDraft {

    private String content;

    private String structuredContent;

    /** 内容是否由大模型生成；false 表示降级为本地模板生成。 */
    private boolean modelGenerated;
}
