import {
  parseAiAgentChatResponse,
  parseAiAgentOperationConfirmResponse,
  parseAiAgentSessionClearResponse,
  parseAiIndexRebuildResponse,
  parseAiRagAskResponse,
  type AiAgentChatResponse,
  type AiAgentOperationConfirmResponse,
  type AiAgentSessionClearResponse,
  type AiIndexRebuildResponse,
  type AiRagAskResponse,
} from "@/api/ai-contracts";
import { apiClient } from "@/api/client";

export interface AiRagAskRequest {
  question: string;
}

export interface AiAgentChatRequest {
  message: string;
}

function validateAiText(value: string, fieldLabel: string): string {
  if (!value.trim()) {
    throw new RangeError(`${fieldLabel}不能为空`);
  }

  if (value.length > 1000) {
    throw new RangeError(`${fieldLabel}不能超过 1000 个字符`);
  }

  return value;
}

export function rebuildAiIndex(
  signal?: AbortSignal,
): Promise<AiIndexRebuildResponse> {
  return apiClient.request("/ai/index/rebuild", {
    method: "POST",
    signal,
    parseData: parseAiIndexRebuildResponse,
  });
}

export function askAiRag(
  request: AiRagAskRequest,
  signal?: AbortSignal,
): Promise<AiRagAskResponse> {
  return apiClient.request("/ai/rag/ask", {
    method: "POST",
    body: JSON.stringify({ question: validateAiText(request.question, "问题") }),
    signal,
    parseData: parseAiRagAskResponse,
  });
}

export function chatAiAgent(
  request: AiAgentChatRequest,
  signal?: AbortSignal,
): Promise<AiAgentChatResponse> {
  return apiClient.request("/ai/agent/chat", {
    method: "POST",
    body: JSON.stringify({ message: validateAiText(request.message, "消息") }),
    signal,
    parseData: parseAiAgentChatResponse,
  });
}

export function clearAiAgentSession(
  signal?: AbortSignal,
): Promise<AiAgentSessionClearResponse> {
  return apiClient.request("/ai/agent/session/clear", {
    method: "POST",
    signal,
    parseData: parseAiAgentSessionClearResponse,
  });
}

export function confirmAiAgentOperation(
  operationId: string,
): Promise<AiAgentOperationConfirmResponse> {
  if (!operationId.trim()) {
    throw new RangeError("operationId must not be blank");
  }

  return apiClient.request(
    `/ai/operations/${encodeURIComponent(operationId)}/confirm`,
    {
      method: "POST",
      parseData: parseAiAgentOperationConfirmResponse,
    },
  );
}
