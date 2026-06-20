package com.yousheng.knowledgehub.ai.index.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownTextCleanerTest {

    @Test
    void null_returnsEmpty() {
        assertEquals("", MarkdownTextCleaner.clean(null));
    }

    @Test
    void empty_returnsEmpty() {
        assertEquals("", MarkdownTextCleaner.clean(""));
    }

    @Test
    void blank_returnsEmpty() {
        assertEquals("", MarkdownTextCleaner.clean("   \n  "));
    }

    @Test
    void headings_strippedButTextPreserved() {
        String result = MarkdownTextCleaner.clean("# Heading 1\n## Heading 2\n### Heading 3\nPlain text");

        assertThat(result).doesNotContain("#");
        assertThat(result).contains("Heading 1");
        assertThat(result).contains("Heading 2");
        assertThat(result).contains("Heading 3");
        assertThat(result).contains("Plain text");
    }

    @Test
    void headingWithCjkText_strippedButTextPreserved() {
        String result = MarkdownTextCleaner.clean("# 标题");

        assertThat(result).isEqualTo("标题");
    }

    @Test
    void listMarkers_strippedButTextPreserved() {
        String result = MarkdownTextCleaner.clean("- bullet\n* star\n+ plus\n1. ordered\n2) paren");

        assertThat(result).doesNotContain("- ");
        assertThat(result).doesNotContain("* ");
        assertThat(result).doesNotContain("+ ");
        assertThat(result).doesNotContain("1.");
        assertThat(result).doesNotContain("2)");
        assertThat(result).contains("bullet");
        assertThat(result).contains("star");
        assertThat(result).contains("plus");
        assertThat(result).contains("ordered");
        assertThat(result).contains("paren");
    }

    @Test
    void blockquote_strippedButTextPreserved() {
        String result = MarkdownTextCleaner.clean("> quoted text\n> more quote");

        assertThat(result).doesNotContain(">");
        assertThat(result).contains("quoted text");
        assertThat(result).contains("more quote");
    }

    @Test
    void link_keepsTextAndRemovesSyntax() {
        String result = MarkdownTextCleaner.clean("[Click here](https://example.com)");

        assertThat(result).contains("Click here");
        assertThat(result).doesNotContain("https://example.com");
        assertThat(result).doesNotContain("](");
    }

    @Test
    void image_keepsAltTextAndRemovesSyntax() {
        String result = MarkdownTextCleaner.clean("![Logo image](./logo.png)");

        assertThat(result).contains("Logo image");
        assertThat(result).doesNotContain("./logo.png");
        assertThat(result).doesNotContain("![");
    }

    @Test
    void codeBlock_fencesRemoved_contentPreserved() {
        String result = MarkdownTextCleaner.clean("```java\npublic class Foo {\n    int x = 1;\n}\n```");

        assertThat(result).doesNotContain("```");
        assertThat(result).contains("public class Foo");
        assertThat(result).contains("int x = 1");
    }

    @Test
    void inlineCode_backticksRemoved_contentPreserved() {
        String result = MarkdownTextCleaner.clean("call `foo()` and `bar()`");

        assertThat(result).doesNotContain("`");
        assertThat(result).contains("foo()");
        assertThat(result).contains("bar()");
    }

    // -- regression: technical identifiers and symbols must survive cleaning ----------

    @Test
    void shouldPreserve_user_id() {
        String result = MarkdownTextCleaner.clean("user_id");
        assertThat(result).contains("user_id");
    }

    @Test
    void shouldPreserve_snakeCase() {
        String result = MarkdownTextCleaner.clean("snake_case");
        assertThat(result).contains("snake_case");
    }

    @Test
    void shouldPreserve_sqlStar() {
        String result = MarkdownTextCleaner.clean("SELECT * FROM note");
        assertThat(result).contains("SELECT * FROM note");
    }

    @Test
    void shouldPreserve_genericType() {
        String result = MarkdownTextCleaner.clean("List<String>");
        assertThat(result).contains("List<String>");
    }

    @Test
    void shouldPreserve_unixPath() {
        String result = MarkdownTextCleaner.clean("~/.ssh/config");
        assertThat(result).contains("~/.ssh/config");
    }

    @Test
    void shouldPreserve_cPreprocessorDefine() {
        String result = MarkdownTextCleaner.clean("#define MAX 10");
        assertThat(result).contains("#define MAX 10");
    }

    @Test
    void shouldPreserve_cPreprocessorInclude() {
        String result = MarkdownTextCleaner.clean("#include <stdio.h>");
        assertThat(result).contains("#include <stdio.h>");
    }
}
