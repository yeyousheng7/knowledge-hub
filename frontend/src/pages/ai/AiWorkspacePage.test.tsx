import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";

import { AuthContext } from "@/features/auth/auth-context";
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
});
