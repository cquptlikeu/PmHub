package com.laigeoffer.pmhub.project.ai.util;

import com.laigeoffer.pmhub.base.core.utils.html.EscapeUtil;

/**
 * AI 内容安全处理工具。
 *
 * <p>在大模型的「输入」与「输出」两个边界做防护，避免不可信文本直接落库或拼入提示词：
 * <ul>
 *     <li>{@link #sanitizeOutput(String)}：清洗模型/本地生成的展示文本，剥离脚本等危险 HTML，
 *     防止存储型 XSS，并限制长度。</li>
 *     <li>{@link #sanitizePromptText(String)}：净化拼入提示词的用户可控字段（如任务名转化的风险标题），
 *     去除标签、控制字符与代码/模板标记并截断，降低提示词注入面。</li>
 * </ul>
 *
 * @author canghe
 */
public final class AiContentSanitizer {

    /** 展示文本最大长度，避免超长模型输出撑爆存储与页面。 */
    private static final int MAX_OUTPUT_LENGTH = 8000;

    /** 拼入提示词的单个字段最大长度。 */
    private static final int MAX_PROMPT_FIELD_LENGTH = 200;

    private AiContentSanitizer() {
    }

    /**
     * 清洗对外展示 / 落库的文本：做 HTML 实体转义并限制长度。
     *
     * <p>对不可信的大模型输出做实体转义（{@code < > & " '}），使其在页面中只能作为纯文本呈现，
     * 即便前端使用 {@code v-html} 渲染也无法执行注入脚本（纵深防御）。
     *
     * @param content 原始文本（可能来自大模型）
     * @return 转义后的安全文本；入参为空时原样返回
     */
    public static String sanitizeOutput(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_OUTPUT_LENGTH) {
            trimmed = trimmed.substring(0, MAX_OUTPUT_LENGTH);
        }
        return htmlEscape(trimmed);
    }

    /**
     * 净化拼入提示词的用户可控文本：剥离 HTML、去除控制字符与代码/模板标记，折叠空白并截断。
     *
     * @param text 用户可控文本（如风险标题、原因、建议）
     * @return 净化后的文本；入参为空时原样返回
     */
    public static String sanitizePromptText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String cleaned = EscapeUtil.clean(text);
        cleaned = cleaned.replaceAll("[\\p{Cntrl}]", " ");
        cleaned = cleaned.replaceAll("[`{}]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > MAX_PROMPT_FIELD_LENGTH) {
            cleaned = cleaned.substring(0, MAX_PROMPT_FIELD_LENGTH) + "...";
        }
        return cleaned;
    }

    private static String htmlEscape(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\'':
                    builder.append("&#39;");
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.toString();
    }
}
