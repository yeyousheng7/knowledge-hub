import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";

import { AuthContext } from "@/features/auth/auth-context";
import { writeAgentTranscript } from "@/features/ai/ai-session-storage";
import { AiWorkspacePage } from "@/pages/ai/AiWorkspacePage";

const authValue = {
  status: "authenticated" as const,
  user: {
    id: 1,
    username: "test",
    nickname: "test",
    role: "USER" as const,
  },
  restoreError: null,
  login: async () => undefined,
  logout: async () => undefined,
  retrySession: () => undefined,
  discardSession: () => undefined,
};

function renderAiWorkspacePage() {
  render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter>
        <AiWorkspacePage />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe("AiWorkspacePage", () => {
  afterEach(() => {
    cleanup();
    window.sessionStorage.clear();
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows the rebuild control before the first RAG message", async () => {
    const user = userEvent.setup();
    renderAiWorkspacePage();

    expect(
      screen.getByRole("button", { name: "重建 RAG 知识库" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Agent" }));
    expect(
      screen.queryByRole("button", { name: "重建 RAG 知识库" }),
    ).not.toBeInTheDocument();
  });

  it("keeps independent drafts when switching between RAG and Agent", async () => {
    const user = userEvent.setup();
    renderAiWorkspacePage();

    await user.type(screen.getByLabelText("RAG 问题"), "RAG draft");
    await user.click(screen.getByRole("button", { name: "Agent" }));
    await user.type(screen.getByLabelText("Agent 消息"), "Agent draft");
    await user.click(screen.getByRole("button", { name: "RAG" }));

    expect(screen.getByLabelText("RAG 问题")).toHaveValue("RAG draft");
    expect(screen.getByRole("button", { name: "发送 RAG 问题" })).toBeEnabled();

    await user.click(screen.getByRole("button", { name: "Agent" }));
    expect(screen.getByLabelText("Agent 消息")).toHaveValue("Agent draft");
    expect(screen.getByRole("button", { name: "发送 Agent 消息" })).toBeEnabled();
  });

  it("keeps Agent selected when starting a new conversation from Agent", async () => {
    writeAgentTranscript(1, [
      {
        id: "agent-message-1",
        role: "user",
        content: "previous question",
        actions: [],
      },
    ]);
    vi.stubGlobal(
      "fetch",
      vi.fn<typeof fetch>().mockResolvedValue(
        new Response(
          JSON.stringify({ code: 0, msg: "ok", data: { cleared: true } }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );
    const user = userEvent.setup();
    renderAiWorkspacePage();

    expect(screen.getByText("当前模式：")).toBeVisible();
    expect(screen.getByText("Agent", { selector: "strong" })).toBeVisible();
    await user.click(screen.getByRole("button", { name: "新对话" }));

    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Agent" })).toHaveAttribute(
        "aria-pressed",
        "true",
      ),
    );
    expect(screen.getByLabelText("Agent 消息")).toBeVisible();
  });
});
