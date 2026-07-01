import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import { AiMarkdownContent } from "@/shared/markdown/AiMarkdownContent";

function renderMarkdown(content: string) {
  render(
    <MemoryRouter initialEntries={["/ai"]}>
      <Routes>
        <Route element={<AiMarkdownContent content={content} />} path="/ai" />
        <Route element={<p>私有笔记详情</p>} path="/notes/:noteId" />
        <Route element={<p>公开笔记详情</p>} path="/public/notes/:noteId" />
      </Routes>
    </MemoryRouter>,
  );
}

describe("AiMarkdownContent", () => {
  afterEach(cleanup);

  it("navigates valid kh-source links inside the application", async () => {
    const user = userEvent.setup();
    renderMarkdown("[查看来源](kh-source://note/12)");

    await user.click(screen.getByRole("link", { name: "查看来源" }));
    expect(screen.getByText("私有笔记详情")).toBeVisible();
  });

  it.each([
    ["加粗标题", "[**加粗标题**](kh-source://public-note/14)"],
    ["斜体标题", "[_斜体标题_](kh-source://public-note/14)"],
    ["代码标题", "[`代码标题`](kh-source://public-note/14)"],
    ["《混合标题》", "[《**混合标题**》](kh-source://public-note/14)"],
  ])("uses nested Markdown text as the source title: %s", (title, markdown) => {
    renderMarkdown(markdown);

    expect(screen.getByRole("link", { name: title })).toHaveAttribute(
      "href",
      "/public/notes/14",
    );
    expect(screen.queryByText("知识来源")).not.toBeInTheDocument();
  });

  it("renders invalid kh-source links as non-interactive text", () => {
    renderMarkdown("[危险来源](kh-source://unknown/12)");

    expect(screen.queryByRole("link", { name: "危险来源" })).not.toBeInTheDocument();
    expect(screen.getByText("危险来源")).toBeVisible();
  });
});
