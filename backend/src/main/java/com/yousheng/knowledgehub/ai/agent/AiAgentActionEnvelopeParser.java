package com.yousheng.knowledgehub.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentAction;
import com.yousheng.knowledgehub.ai.agent.dto.ActionEnvelope;
import com.yousheng.knowledgehub.ai.agent.dto.AiAgentChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiAgentActionEnvelopeParser {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AiAgentActionEnvelopeParser() {
        this(new ObjectMapper());
    }

    AiAgentActionEnvelopeParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiAgentChatResponse parse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root == null || !root.isObject()) {
                return AiAgentChatResponse.text(content);
            }

            ActionEnvelope envelope = new ActionEnvelope(readAnswer(root), readActions(root));
            if ((envelope.answer() == null || envelope.answer().isBlank()) && envelope.actions().isEmpty()) {
                return AiAgentChatResponse.text(content);
            }
            return envelope.toResponse();
        } catch (Exception e) {
            return AiAgentChatResponse.text(content);
        }
    }

    private String readAnswer(JsonNode root) {
        JsonNode answerNode = root.get("answer");
        return answerNode != null && answerNode.isTextual() ? answerNode.asText() : null;
    }

    private List<AiAgentAction> readActions(JsonNode root) {
        JsonNode actionsNode = root.get("actions");
        if (actionsNode == null || !actionsNode.isArray()) {
            return List.of();
        }

        List<AiAgentAction> actions = new ArrayList<>();
        for (JsonNode actionNode : actionsNode) {
            if (!actionNode.isObject()) {
                continue;
            }
            JsonNode typeNode = actionNode.get("type");
            if (typeNode == null || !typeNode.isTextual() || typeNode.asText().isBlank()) {
                continue;
            }
            actions.add(new AiAgentAction(typeNode.asText(), readPayload(actionNode)));
        }
        return actions;
    }

    private Map<String, Object> readPayload(JsonNode actionNode) {
        JsonNode payloadNode = actionNode.get("payload");
        return payloadNode != null && payloadNode.isObject()
                ? objectMapper.convertValue(payloadNode, PAYLOAD_TYPE)
                : Map.of();
    }
}
