import { describe, expect, it } from "vitest";

import {
  parseAiAgentChatResponse,
  parsePendingOperationAction,
} from "@/api/ai-contracts";

describe("AI Agent contracts", () => {
  it("parses pending create-note operations without executing them", () => {
    const response = parseAiAgentChatResponse({
      answer: "请确认创建。",
      actions: [
        {
          type: "PENDING_OPERATION",
          payload: {
            operationId: "op-1",
            operationType: "CREATE_PRIVATE_NOTE",
            preview: "创建一篇私有笔记",
            draft: {
              title: "高并发限流设计",
              summary: null,
              contentMd: "# 限流",
              recommendedTags: ["架构", "Redis"],
            },
            expiresAt: "2099-01-01T00:00:00Z",
          },
        },
      ],
    });

    const pendingAction = parsePendingOperationAction(response.actions[0]);

    expect(pendingAction.payload.operationId).toBe("op-1");
    expect(pendingAction.payload.operationType).toBe("CREATE_PRIVATE_NOTE");
    if (pendingAction.payload.operationType === "CREATE_PRIVATE_NOTE") {
      expect(pendingAction.payload.draft.recommendedTags).toEqual([
        "架构",
        "Redis",
      ]);
    }
  });

  it("rejects malformed pending operation payloads", () => {
    const response = parseAiAgentChatResponse({
      answer: "格式错误。",
      actions: [
        {
          type: "PENDING_OPERATION",
          payload: {
            operationId: "op-2",
            operationType: "CREATE_PRIVATE_NOTE",
            preview: "创建一篇私有笔记",
            expiresAt: "not-a-date",
          },
        },
      ],
    });

    expect(() => parsePendingOperationAction(response.actions[0])).toThrow(
      /ISO date-time/,
    );
  });
});
