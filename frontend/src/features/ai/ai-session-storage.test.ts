import { afterEach, describe, expect, it } from "vitest";

import {
  AI_TRANSCRIPT_MAX_TURNS,
  readAgentTranscript,
  readRagTranscript,
  writeAgentTranscript,
  writeRagTranscript,
} from "@/features/ai/ai-session-storage";

describe("ai-session-storage", () => {
  afterEach(() => {
    window.sessionStorage.clear();
  });

  it("stores RAG turns per user and trims the oldest turns", () => {
    writeRagTranscript(
      1,
      Array.from({ length: AI_TRANSCRIPT_MAX_TURNS + 5 }, (_, index) => ({
        id: `turn-${index}`,
        question: `问题 ${index}`,
        answer: null,
        error: null,
      })),
    );

    const turns = readRagTranscript(1);

    expect(turns).toHaveLength(AI_TRANSCRIPT_MAX_TURNS);
    expect(turns[0]?.question).toBe("问题 5");
  });

  it("stores Agent messages per user and trims to 50 rounds", () => {
    writeAgentTranscript(
      1,
      Array.from({ length: AI_TRANSCRIPT_MAX_TURNS * 2 + 2 }, (_, index) => ({
        id: `message-${index}`,
        role: index % 2 === 0 ? "user" : "assistant",
        content: `消息 ${index}`,
        actions: [],
      })),
    );

    const messages = readAgentTranscript(1);

    expect(messages).toHaveLength(AI_TRANSCRIPT_MAX_TURNS * 2);
    expect(messages[0]?.content).toBe("消息 2");
  });

  it("restores the terminal state of an Agent pending operation", () => {
    writeAgentTranscript(1, [
      {
        id: "assistant-1",
        role: "assistant",
        content: "操作已执行",
        actions: [
          {
            type: "PENDING_OPERATION",
            payload: { operationId: "operation-1" },
            operationResolution: {
              status: "executed",
              result: {
                operationId: "operation-1",
                operationType: "CREATE_PRIVATE_NOTE",
                status: "EXECUTED",
                affectedCount: 1,
                affectedItems: [{ id: 12, title: "新笔记" }],
                message: "已创建",
              },
              error: null,
            },
          },
        ],
      },
    ]);

    const action = readAgentTranscript(1)[0]?.actions[0];

    expect(action?.operationResolution?.status).toBe("executed");
    expect(action?.operationResolution?.result?.affectedItems[0]?.id).toBe(12);
  });
});
