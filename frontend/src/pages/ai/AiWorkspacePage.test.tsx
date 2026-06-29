import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";

import { AiWorkspacePage } from "@/pages/ai/AiWorkspacePage";

describe("AiWorkspacePage", () => {
  afterEach(cleanup);

  it("keeps independent drafts when switching between RAG and Agent", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <AiWorkspacePage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText("RAG 问题"), "RAG draft");
    await user.click(screen.getByRole("button", { name: "Agent" }));
    await user.type(screen.getByLabelText("Agent 消息"), "Agent draft");
    await user.click(screen.getByRole("button", { name: "RAG" }));

    expect(screen.getByLabelText("RAG 问题")).toHaveValue("RAG draft");
    expect(screen.getByRole("button", { name: "发送 RAG 问题" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Agent" }));
    expect(screen.getByLabelText("Agent 消息")).toHaveValue("Agent draft");
    expect(screen.getByRole("button", { name: "发送 Agent 消息" })).toBeDisabled();
  });
});
