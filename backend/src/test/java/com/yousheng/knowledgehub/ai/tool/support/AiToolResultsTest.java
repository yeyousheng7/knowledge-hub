package com.yousheng.knowledgehub.ai.tool.support;

import com.yousheng.knowledgehub.ai.tool.model.AiToolResult;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiToolResultsTest {

    @Test
    void successData_returnsSuccessResultWithData() {
        AiToolResult<String> result = AiToolResults.success("hello");

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(0);
        assertThat(result.message()).isEqualTo("OK");
        assertThat(result.data()).isEqualTo("hello");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void successDataAndWarnings_returnsSuccessWithWarnings() {
        List<String> warnings = List.of("warning1", "warning2");
        AiToolResult<String> result = AiToolResults.success("hello", warnings);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo(0);
        assertThat(result.data()).isEqualTo("hello");
        assertThat(result.warnings()).containsExactly("warning1", "warning2");
    }

    @Test
    void successDataAndWarnings_nullWarnings_defaultsToEmpty() {
        AiToolResult<String> result = AiToolResults.success("hello", null);

        assertThat(result.success()).isTrue();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void successDataAndWarnings_warningsCopyDefensive() {
        List<String> mutable = new java.util.ArrayList<>(List.of("w1"));
        AiToolResult<String> result = AiToolResults.success("data", mutable);

        mutable.add("w2");

        assertThat(result.warnings()).containsExactly("w1");
    }

    @Test
    void failureErrorCode_returnsFailureWithDefaultMessage() {
        AiToolResult<Void> result = AiToolResults.failure(ErrorCode.NOTE_NOT_FOUND);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.NOTE_NOT_FOUND.getCode());
        assertThat(result.message()).isEqualTo(ErrorCode.NOTE_NOT_FOUND.getDefaultMsg());
        assertThat(result.data()).isNull();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void failureErrorCodeAndMessage_returnsFailureWithCustomMessage() {
        AiToolResult<Void> result = AiToolResults.failure(ErrorCode.BAD_REQUEST, "page 必须大于 0。");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).isEqualTo("page 必须大于 0。");
        assertThat(result.data()).isNull();
    }

    @Test
    void failureBizException_returnsFailureFromBizException() {
        BizException bizException = new BizException(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE, "自定义消息");
        AiToolResult<Void> result = AiToolResults.failure(bizException);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.AI_INDEX_SERVICE_UNAVAILABLE.getCode());
        assertThat(result.message()).isEqualTo("自定义消息");
        assertThat(result.data()).isNull();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void failureErrorCodeMessageAndWarnings_returnsFailureWithWarnings() {
        List<String> warnings = List.of("warn");
        AiToolResult<Void> result = AiToolResults.failure(ErrorCode.BAD_REQUEST, "msg", warnings);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.message()).isEqualTo("msg");
        assertThat(result.warnings()).containsExactly("warn");
    }

    @Test
    void failureErrorCodeMessageAndWarnings_nullWarnings_defaultsToEmpty() {
        AiToolResult<Void> result = AiToolResults.failure(ErrorCode.BAD_REQUEST, "msg", null);

        assertThat(result.success()).isFalse();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void directConstruct_mutableWarnings_doesNotAffectResult() {
        ArrayList<String> mutable = new ArrayList<>(List.of("w1", "w2"));

        AiToolResult<String> result = new AiToolResult<>(true, 0, "OK", "data", mutable);

        mutable.add("w3");

        assertThat(result.warnings()).containsExactly("w1", "w2");
    }

    @Test
    void warningsReturnedList_isUnmodifiable() {
        AiToolResult<String> result = new AiToolResult<>(true, 0, "OK", "data", List.of("w1"));

        assertThatThrownBy(() -> result.warnings().add("w2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
