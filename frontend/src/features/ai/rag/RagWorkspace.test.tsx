import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

import { askAiRag } from "@/api/ai";
import { RagWorkspace } from "@/features/ai/rag/RagWorkspace";

vi.mock("@/api/ai", () => ({
  askAiRag: vi.fn(),
}));

function RagWorkspaceHarness() {
  const [question, setQuestion] = useState("");

  return (
    <MemoryRouter>
      <RagWorkspace question={question} onQuestionChange={setQuestion} />
    </MemoryRouter>
  );
}

describe("RagWorkspace", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("submits with Enter and keeps Shift+Enter as a line break", async () => {
    const user = userEvent.setup();
    vi.mocked(askAiRag).mockResolvedValue({
      answer: "收到。",
      sources: [],
    });

    render(<RagWorkspaceHarness />);

    const input = screen.getByLabelText("RAG 问题");
    await user.type(input, "第一行{Shift>}{Enter}{/Shift}第二行");

    expect(input).toHaveValue("第一行\n第二行");
    expect(askAiRag).not.toHaveBeenCalled();

    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(askAiRag).toHaveBeenCalledWith(
        { question: "第一行\n第二行" },
        expect.any(AbortSignal),
      ),
    );
    expect(input).toHaveValue("");
  });

  it("clears the question after submit even when request fails", async () => {
    const user = userEvent.setup();
    vi.mocked(askAiRag).mockRejectedValue(new Error("服务异常"));

    render(<RagWorkspaceHarness />);

    const input = screen.getByLabelText("RAG 问题");
    await user.type(input, "失败也清空");
    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(askAiRag).toHaveBeenCalledWith(
        { question: "失败也清空" },
        expect.any(AbortSignal),
      ),
    );
    expect(input).toHaveValue("");
    expect(screen.getByText(/请重新输入后重试/)).toBeInTheDocument();
  });

  it("restores initial turns and trims to the latest 50 turns", () => {
    const turns = Array.from({ length: 55 }, (_, index) => ({
      id: `turn-${index}`,
      question: `问题 ${index}`,
      answer: {
        answer: `回答 ${index}`,
        sources: [],
      },
      error: null,
    }));

    render(
      <MemoryRouter>
        <RagWorkspace
          initialTurns={turns}
          onQuestionChange={() => undefined}
          question=""
        />
      </MemoryRouter>,
    );

    expect(screen.queryByText("问题 0")).not.toBeInTheDocument();
    expect(screen.getByText("问题 54")).toBeInTheDocument();
  });
});
