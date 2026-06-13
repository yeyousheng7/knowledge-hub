package com.yousheng.knowledgehub.note.util;

public final class NoteSummaryUtils {
    private static final int MAX_SUMMARY_LENGTH = 200;

    public static String resolveSummary(String summary, String contentMd) {
        if (summary == null || summary.trim().isEmpty()) {
            return generateSummary(contentMd);
        }
        return summary.trim();
    }

    private static String generateSummary(String contentMd) {
        if (contentMd == null || contentMd.trim().isEmpty()) {
            return null;
        }

        String text = contentMd;

        // fenced code block：去掉 ```java / ``` 这些围栏，保留内部文字
        text = text.replaceAll("(?m)^```[a-zA-Z0-9_-]*\\s*$", "");
        text = text.replaceAll("(?m)^```\\s*$", "");

        // inline code：`code` -> code
        text = text.replaceAll("`([^`]*)`", "$1");

        // 图片：![alt](url) -> alt
        text = text.replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1");

        // 链接：[text](url) -> text
        text = text.replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1");

        // 标题：### title -> title
        text = text.replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");

        // 引用：> quote -> quote
        text = text.replaceAll("(?m)^\\s{0,3}>\\s?", "");

        // 无序列表：- item / * item / + item -> item
        text = text.replaceAll("(?m)^\\s*[-*+]\\s+", "");

        // 有序列表：1. item -> item
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");

        // 粗体 / 斜体 / 删除线符号，简单去掉标记
        text = text.replace("*", "")
                .replace("_", "")
                .replace("~", "");

        // Markdown 分隔线
        text = text.replaceAll("(?m)^\\s*-{3,}\\s*$", " ");

        // 合并空白
        text = text.replaceAll("\\s+", " ").trim();

        if (text.isEmpty()) {
            return null;
        }

        if (text.length() <= MAX_SUMMARY_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_SUMMARY_LENGTH);
    }

    private NoteSummaryUtils() {
    }
}
