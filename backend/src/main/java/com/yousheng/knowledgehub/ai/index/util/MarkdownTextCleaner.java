package com.yousheng.knowledgehub.ai.index.util;

public final class MarkdownTextCleaner {
    private MarkdownTextCleaner() {
    }

    public static String clean(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String text = markdown.replace("\r\n", "\n")
                .replace("\r", "\n");

        // 保留代码内容，只移除 fenced code 的围栏行
        text = text.replaceAll("(?m)^```.*$", "");

        // 图片：保留 alt 文本
        text = text.replaceAll("!\\[([^\\]]*)]\\([^)]*\\)", "$1");

        // 链接：保留链接文本
        text = text.replaceAll("\\[([^\\]]+)]\\([^)]*\\)", "$1");

        // 只清理明确的 Markdown 行级结构，不全局删除技术字符
        text = text.replaceAll("(?m)^#{1,6}\\s+", "");
        text = text.replaceAll("(?m)^>\\s?", "");
        text = text.replaceAll("(?m)^\\s*[-*+]\\s+", "");
        text = text.replaceAll("(?m)^\\s*\\d+[.)]\\s+", "");

        // 行内代码只移除反引号，保留内部代码内容
        text = text.replace("`", "");

        // 压缩横向空白，但保留换行，方便后续按段落/行切分
        text = text.replaceAll("[ \\t\\x0B\\f]+", " ");

        // 过多空行压缩成两个换行
        text = text.replaceAll("\\n{3,}", "\n\n");

        return text.trim();
    }
}
