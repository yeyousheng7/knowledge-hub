package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAgentActionEnvelopeParserTest {

    private final AiAgentActionEnvelopeParser parser = new AiAgentActionEnvelopeParser();

    @Test
    void parsePlainText_returnsTextResponse() {
        AiAgentChatResponse response = parser.parse("plain answer");

        assertThat(response.answer()).isEqualTo("plain answer");
        assertThat(response.actions()).isEmpty();
    }

    @Test
    void parseActionEnvelope_returnsActions() {
        String json = """
                {"answer":"Draft prepared.","actions":[{"type":"NOTE_DRAFT","payload":{"title":"Redis note","contentMd":"body"}}]}
                """;

        AiAgentChatResponse response = parser.parse(json);

        assertThat(response.answer()).isEqualTo("Draft prepared.");
        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().get(0).type()).isEqualTo("NOTE_DRAFT");
        assertThat(response.actions().get(0).payload()).containsEntry("title", "Redis note");
    }

    @Test
    void parseActionEnvelopeWithUnknownTopLevelField_ignoresUnknownField() {
        String json = """
                {"answer":"ok","actions":[],"extra":1}
                """;

        AiAgentChatResponse response = parser.parse(json);

        assertThat(response.answer()).isEqualTo("ok");
        assertThat(response.actions()).isEmpty();
    }
}
