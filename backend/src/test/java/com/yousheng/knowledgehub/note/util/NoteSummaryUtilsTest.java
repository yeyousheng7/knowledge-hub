package com.yousheng.knowledgehub.note.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteSummaryUtilsTest {

    @Test
    void resolveSummary_manualSummary_returnsTrimmedSummary() {
        String result = NoteSummaryUtils.resolveSummary("  Hello World  ", "# Some Content");

        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void resolveSummary_blankSummary_generatesFromContent() {
        String result = NoteSummaryUtils.resolveSummary("   ", "# Hello World\nSome paragraph.");

        assertThat(result).isEqualTo("Hello World Some paragraph.");
    }

    @Test
    void resolveSummary_nullContent_returnsNull() {
        String result = NoteSummaryUtils.resolveSummary(null, null);

        assertThat(result).isNull();
    }

    @Test
    void resolveSummary_generatedSummaryMaxLength200() {
        String longContent = "a".repeat(300);

        String result = NoteSummaryUtils.resolveSummary(null, longContent);

        assertThat(result).hasSize(200);
        assertThat(result).isEqualTo("a".repeat(200));
    }

    @Test
    void resolveSummary_cleansMarkdownSyntax() {
        String markdown = """
                # Title
                Some **bold** text with `code` and [link](url).
                """;

        String result = NoteSummaryUtils.resolveSummary(null, markdown);

        assertThat(result).isEqualTo("Title Some bold text with code and link.");
    }
}
