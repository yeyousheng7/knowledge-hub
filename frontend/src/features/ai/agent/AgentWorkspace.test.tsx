import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  chatAiAgent,
  confirmAiAgentOperation,
} from "@/api/ai";
import { AgentWorkspace } from "@/features/ai/agent/AgentWorkspace";

vi.mock("@/api/ai", () => ({
  chatAiAgent: vi.fn(),
  confirmAiAgentOperation: vi.fn(),
}));

describe("AgentWorkspace", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("does not confirm pending operations until the user clicks confirm", async () => {
    const user = userEvent.setup();
    const onMessageChange = vi.fn();
    vi.mocked(chatAiAgent).mockResolvedValue({
      answer: "请确认以下操作。",
      actions: [
        {
          type: "PENDING_OPERATION",
          payload: {
            operationId: "operation-1",
            operationType: "CREATE_PRIVATE_NOTE",
            preview: "创建一篇私有笔记",
            draft: {
              title: "限流设计",
              summary: null,
              contentMd: "# 限流设计",
              recommendedTags: [],
            },
            expiresAt: "2099-01-01T00:00:00Z",
          },
        },
      ],
    });
    vi.mocked(confirmAiAgentOperation).mockResolvedValue({
      operationId: "operation-1",
      operationType: "CREATE_PRIVATE_NOTE",
      status: "EXECUTED",
      affectedCount: 1,
      affectedItems: [{ id: 12, title: "限流设计" }],
      message: "已创建私有笔记",
    });

    render(
      <MemoryRouter>
        <AgentWorkspace
          message="帮我创建一篇笔记"
          onMessageChange={onMessageChange}
          resetVersion={0}
        />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: "发送 Agent 消息" }));

    await screen.findByText("待确认操作");
    expect(confirmAiAgentOperation).not.toHaveBeenCalled();

    await user.click(screen.getByRole("button", { name: "确认执行" }));

    await waitFor(() =>
      expect(confirmAiAgentOperation).toHaveBeenCalledWith("operation-1"),
    );
  });
});
