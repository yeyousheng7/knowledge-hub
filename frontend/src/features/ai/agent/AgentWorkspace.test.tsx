import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  chatAiAgent,
  confirmAiAgentOperation,
} from "@/api/ai";
import { AgentWorkspace } from "@/features/ai/agent/AgentWorkspace";
import { type AgentTranscriptMessage } from "@/features/ai/ai-session-storage";

vi.mock("@/api/ai", () => ({
  chatAiAgent: vi.fn(),
  confirmAiAgentOperation: vi.fn(),
}));

function AgentWorkspaceHarness() {
  const [message, setMessage] = useState("");

  return (
    <MemoryRouter>
      <AgentWorkspace
        message={message}
        onMessageChange={setMessage}
        resetVersion={0}
      />
    </MemoryRouter>
  );
}

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

  it("keeps a confirmed operation disabled after the workspace remounts", async () => {
    const user = userEvent.setup();
    let persistedMessages: AgentTranscriptMessage[] = [
      {
        id: "assistant-1",
        role: "assistant",
        content: "请确认以下操作。",
        actions: [
          {
            type: "PENDING_OPERATION",
            payload: {
              operationId: "operation-persisted",
              operationType: "CREATE_PRIVATE_NOTE",
              preview: "创建一篇私有笔记",
              draft: {
                title: "持久化状态",
                summary: null,
                contentMd: "# 持久化状态",
                recommendedTags: [],
              },
              expiresAt: "2099-01-01T00:00:00Z",
            },
          },
        ],
      },
    ];
    vi.mocked(confirmAiAgentOperation).mockResolvedValue({
      operationId: "operation-persisted",
      operationType: "CREATE_PRIVATE_NOTE",
      status: "EXECUTED",
      affectedCount: 1,
      affectedItems: [{ id: 13, title: "持久化状态" }],
      message: "已创建私有笔记",
    });

    const firstRender = render(
      <MemoryRouter>
        <AgentWorkspace
          initialMessages={persistedMessages}
          message=""
          onMessageChange={() => undefined}
          onMessagesChange={(messages) => {
            persistedMessages = messages;
          }}
          resetVersion={0}
        />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: "确认执行" }));
    await screen.findByText("已执行");
    await waitFor(() =>
      expect(
        persistedMessages[0]?.actions[0]?.operationResolution?.status,
      ).toBe("executed"),
    );

    firstRender.unmount();
    render(
      <MemoryRouter>
        <AgentWorkspace
          initialMessages={persistedMessages}
          message=""
          onMessageChange={() => undefined}
          resetVersion={0}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText("已执行")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "确认执行" })).toBeDisabled();
    expect(confirmAiAgentOperation).toHaveBeenCalledTimes(1);
  });

  it("submits with Enter and keeps Shift+Enter as a line break", async () => {
    const user = userEvent.setup();
    vi.mocked(chatAiAgent).mockResolvedValue({
      answer: "收到。",
      actions: [],
    });

    render(<AgentWorkspaceHarness />);

    const input = screen.getByLabelText("Agent 消息");
    await user.type(input, "第一行{Shift>}{Enter}{/Shift}第二行");

    expect(input).toHaveValue("第一行\n第二行");
    expect(chatAiAgent).not.toHaveBeenCalled();

    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(chatAiAgent).toHaveBeenCalledWith(
        { message: "第一行\n第二行" },
        expect.any(AbortSignal),
      ),
    );
    expect(input).toHaveValue("");
  });

  it("clears the message after submit even when request fails", async () => {
    const user = userEvent.setup();
    vi.mocked(chatAiAgent).mockRejectedValue(new Error("服务异常"));

    render(<AgentWorkspaceHarness />);

    const input = screen.getByLabelText("Agent 消息");
    await user.type(input, "失败也清空");
    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(chatAiAgent).toHaveBeenCalledWith(
        { message: "失败也清空" },
        expect.any(AbortSignal),
      ),
    );
    expect(input).toHaveValue("");
    expect(screen.getByText(/请重新输入后重试/)).toBeInTheDocument();
  });

  it("restores initial messages and trims to the latest 50 rounds", () => {
    const messages = Array.from({ length: 110 }, (_, index) => ({
      id: `message-${index}`,
      role: index % 2 === 0 ? ("user" as const) : ("assistant" as const),
      content: `消息 ${index}`,
      actions: [],
    }));

    render(
      <MemoryRouter>
        <AgentWorkspace
          initialMessages={messages}
          message=""
          onMessageChange={() => undefined}
          resetVersion={0}
        />
      </MemoryRouter>,
    );

    expect(screen.queryByText("消息 0")).not.toBeInTheDocument();
    expect(screen.getByText("消息 109")).toBeInTheDocument();
  });
});
