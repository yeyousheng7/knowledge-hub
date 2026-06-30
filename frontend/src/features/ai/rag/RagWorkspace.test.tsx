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
  });
});
