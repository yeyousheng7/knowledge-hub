import {
  parseAiIndexRebuildResponse,
  parseAiRagAskResponse,
  type AiIndexRebuildResponse,
  type AiRagAskResponse,
} from "@/api/ai-contracts";
import { apiClient } from "@/api/client";

export interface AiRagAskRequest {
  question: string;
}

function validateQuestion(question: string): string {
  if (!question.trim()) {
    throw new RangeError("问题不能为空");
  }

  if (question.length > 1000) {
    throw new RangeError("问题不能超过 1000 个字符");
  }

  return question;
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
    body: JSON.stringify({ question: validateQuestion(request.question) }),
    signal,
    parseData: parseAiRagAskResponse,
  });
}
