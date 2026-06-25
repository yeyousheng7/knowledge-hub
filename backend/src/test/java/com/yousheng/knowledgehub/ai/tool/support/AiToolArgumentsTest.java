package com.yousheng.knowledgehub.ai.tool.support;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class AiToolArgumentsTest {

    @Test
    void normalizePage_null_returns1() {
        AiToolResult<Integer> result = AiToolArguments.normalizePage(null);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(1);
    }

    @Test
    void normalizePage_zero_returnsBadRequest() {
        AiToolResult<Integer> result = AiToolArguments.normalizePage(0);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).contains("page");
    }

    @Test
    void normalizePage_negative_returnsBadRequest() {
        AiToolResult<Integer> result = AiToolArguments.normalizePage(-1);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).contains("page");
    }

    @Test
    void normalizePage_positive_returnsSameValue() {
        AiToolResult<Integer> result = AiToolArguments.normalizePage(3);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(3);
    }

    @Test
    void normalizeSize_null_returns5() {
        AiToolResult<Integer> result = AiToolArguments.normalizeSize(null);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(5);
    }

    @Test
    void normalizeSize_zero_returnsBadRequest() {
        AiToolResult<Integer> result = AiToolArguments.normalizeSize(0);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).contains("size");
    }

    @Test
    void normalizeSize_negative_returnsBadRequest() {
        AiToolResult<Integer> result = AiToolArguments.normalizeSize(-1);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).contains("size");
    }

    @Test
    void normalizeSize_positiveWithinLimit_returnsSameValue() {
        AiToolResult<Integer> result = AiToolArguments.normalizeSize(7);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(7);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void normalizeSize_exactly10_returns10WithoutWarning() {
        AiToolResult<Integer> result = AiToolArguments.normalizeSize(10);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(10);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void normalizeSize_above10_clampsTo10AndWarns() {
        AiToolResult<Integer> result = AiToolArguments.normalizeSize(20);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(10);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("10");
    }

    @Test
    void requireKeyword_null_returnsBadRequest() {
        AiToolResult<String> result = AiToolArguments.requireKeyword(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void requireKeyword_blankString_returnsBadRequest() {
        AiToolResult<String> result = AiToolArguments.requireKeyword("   ");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void requireKeyword_emptyString_returnsBadRequest() {
        AiToolResult<String> result = AiToolArguments.requireKeyword("");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void requireKeyword_trimsWhitespace() {
        AiToolResult<String> result = AiToolArguments.requireKeyword("  hello  ");

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo("hello");
    }

    @Test
    void requireKeyword_validKeyword_returnsTrimmed() {
        AiToolResult<String> result = AiToolArguments.requireKeyword("Spring Boot");

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo("Spring Boot");
    }

    @Test
    void requireKeyword_exactly100Chars_returnsSuccess() {
        String keyword = "a".repeat(100);
        AiToolResult<String> result = AiToolArguments.requireKeyword(keyword);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(100);
    }

    @Test
    void requireKeyword_over100Chars_returnsBadRequest() {
        String keyword = "a".repeat(101);
        AiToolResult<String> result = AiToolArguments.requireKeyword(keyword);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).contains("100");
    }

    @Test
    void requireNoteId_null_returnsBadRequest() {
        AiToolResult<Long> result = AiToolArguments.requireNoteId(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void requireNoteId_zero_returnsBadRequest() {
        AiToolResult<Long> result = AiToolArguments.requireNoteId(0L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).contains("noteId");
    }

    @Test
    void requireNoteId_negative_returnsBadRequest() {
        AiToolResult<Long> result = AiToolArguments.requireNoteId(-5L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void requireNoteId_positive_returnsSameValue() {
        AiToolResult<Long> result = AiToolArguments.requireNoteId(42L);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(42L);
    }
}
