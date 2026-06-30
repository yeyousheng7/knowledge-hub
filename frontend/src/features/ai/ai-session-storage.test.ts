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
});
