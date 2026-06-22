package com.laigeoffer.pmhub.project.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "pmhub.ai.model")
public class AiModelProperties {

    /**
     * 默认关闭模型调用，避免本地和测试环境缺少密钥时影响 AI 分析主链路。
     */
    private boolean enabled = false;

    /**
     * OpenAI 兼容接口地址，例如 https://api.openai.com。
     */
    private String baseUrl;

    private String chatCompletionsPath = "/v1/chat/completions";

    private String apiKey;

    private String model = "gpt-4o-mini";

    private Double temperature = 0.2D;

    private Integer maxTokens = 800;

    private int connectTimeoutMillis = 5000;

    private int readTimeoutMillis = 15000;
}
