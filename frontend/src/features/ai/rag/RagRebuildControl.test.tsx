import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { rebuildAiIndex } from "@/api/ai";
import { RagRebuildControl } from "@/features/ai/rag/RagRebuildControl";

vi.mock("@/api/ai", () => ({
  rebuildAiIndex: vi.fn(),
}));

function pendingPromise<T>(): Promise<T> {
  return new Promise(() => undefined);
}

describe("RagRebuildControl", () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.resetAllMocks();
    window.localStorage.clear();
  });

  it("shows transient success before falling back to persisted rebuild history", async () => {
    vi.useFakeTimers();
    vi.mocked(rebuildAiIndex).mockResolvedValue({
      userId: 1,
      chunkCount: 3,
      indexedAt: "2026-06-30T01:06:00Z",
    });

    render(<RagRebuildControl />);

    act(() => {
      fireEvent.click(screen.getByRole("button", { name: "重建 RAG 知识库" }));
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText(/已索引 3 个内容块/)).toBeInTheDocument();
    expect(screen.queryByText(/上次重建/)).not.toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(4_000);
    });

    expect(screen.queryByText(/已索引 3 个内容块/)).not.toBeInTheDocument();
    expect(screen.getByText(/上次重建/)).toBeInTheDocument();
  });

  it("restores only persisted rebuild history after remount", () => {
    window.localStorage.setItem(
      "knowledgehub.ai.rag.lastRebuild.v1",
      JSON.stringify({
        chunkCount: 3,
        indexedAt: "2026-06-30T01:06:00Z",
      }),
    );

    render(<RagRebuildControl />);

    expect(screen.queryByText(/已索引 3 个内容块/)).not.toBeInTheDocument();
    expect(screen.getByText(/上次重建/)).toBeInTheDocument();
  });

  it("hides persisted rebuild history while rebuilding", () => {
    window.localStorage.setItem(
      "knowledgehub.ai.rag.lastRebuild.v1",
      JSON.stringify({
        chunkCount: 3,
        indexedAt: "2026-06-30T01:06:00Z",
      }),
    );
    vi.mocked(rebuildAiIndex).mockReturnValue(pendingPromise());

    render(<RagRebuildControl />);

    expect(screen.getByText(/上次重建/)).toBeInTheDocument();

    act(() => {
      fireEvent.click(screen.getByRole("button", { name: "重建 RAG 知识库" }));
    });

    expect(screen.queryByText(/上次重建/)).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "正在重建…" })).toBeDisabled();
  });
});
