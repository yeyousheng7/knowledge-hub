import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/api/client";
import { NotesWorkspacePage } from "@/pages/notes/NotesWorkspacePage";

vi.mock("@/features/notes/editor/VditorMarkdownEditor", () => ({
  VditorMarkdownEditor: ({
    value,
    onChange,
  }: {
    value: string;
    onChange: (value: string) => void;
  }) => (
    <textarea
      aria-label="Markdown 正文"
      onChange={(event) => onChange(event.target.value)}
      value={value}
    />
  ),
}));

interface DeferredResponse {
  promise: Promise<Response>;
  resolve: (response: Response) => void;
}

function deferredResponse(): DeferredResponse {
  let resolve!: (response: Response) => void;
  const promise = new Promise<Response>((resolver) => {
    resolve = resolver;
  });
  return { promise, resolve };
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function envelope(data: unknown) {
  return { code: 0, msg: "OK", data };
}

function noteItem(
  id: number,
  options: {
    visibility?: "PRIVATE" | "PUBLIC";
    moderationStatus?: "NORMAL" | "TAKEN_DOWN";
    categoryId?: number | null;
  } = {},
) {
  return {
    id,
    title: `Note ${id}`,
    summary: `Summary ${id}`,
    categoryId: options.categoryId === undefined ? 10 : options.categoryId,
    tags: [{ id: 20, name: "React" }],
    visibility: options.visibility ?? "PRIVATE",
    moderationStatus: options.moderationStatus ?? "NORMAL",
    createdAt: "2026-06-01T10:00:00Z",
    updatedAt: "2026-06-02T10:00:00Z",
    publishedAt: null,
  };
}

function noteDetail(
  id: number,
  options: Parameters<typeof noteItem>[1] = {},
) {
  return {
    ...noteItem(id, options),
    contentMd: `# Detail ${id}\n\nMarkdown content ${id}`,
  };
}

function listResponse(items: ReturnType<typeof noteItem>[]) {
  return envelope({ items, total: items.length, page: 1, size: 20 });
}

function taxonomyResponse() {
  return {
    categories: envelope({
      items: [
        {
          id: 10,
          name: "Backend",
          createdAt: "2026-06-01T10:00:00Z",
          updatedAt: "2026-06-01T10:00:00Z",
        },
      ],
    }),
    tags: envelope({
      items: [
        {
          id: 20,
          name: "React",
          createdAt: "2026-06-01T10:00:00Z",
          updatedAt: "2026-06-01T10:00:00Z",
        },
      ],
    }),
  };
}

function renderWorkspace(initialEntry = "/notes") {
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route element={<NotesWorkspacePage />} path="/notes" />
        <Route element={<NotesWorkspacePage />} path="/notes/new" />
        <Route element={<NotesWorkspacePage />} path="/notes/:noteId" />
        <Route element={<NotesWorkspacePage />} path="/notes/:noteId/edit" />
      </Routes>
    </MemoryRouter>,
  );
}

describe("NotesWorkspacePage", () => {
  const fetchMock = vi.fn<typeof fetch>();
  let resetAuth: () => void;

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
    resetAuth = apiClient.configureAuth({
      getAccessToken: () => "notes-token",
      onAuthenticationInvalid: () => undefined,
    });
  });

  afterEach(() => {
    cleanup();
    resetAuth();
    vi.unstubAllGlobals();
  });

  it("loads the three-pane reading state and uses visibility instead of publishedAt", async () => {
    const requests: string[] = [];
    const taxonomy = taxonomyResponse();
    fetchMock.mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");
      requests.push(url.toString());

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }
      if (url.pathname.endsWith("/notes/1")) {
        return jsonResponse(
          envelope(
            noteDetail(1, {
              visibility: "PUBLIC",
              moderationStatus: "TAKEN_DOWN",
            }),
          ),
        );
      }

      return jsonResponse(
        listResponse([
          noteItem(1, {
            visibility: "PUBLIC",
            moderationStatus: "TAKEN_DOWN",
          }),
          noteItem(2, { categoryId: null }),
        ]),
      );
    });
    const user = userEvent.setup();

    renderWorkspace();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Note 1" }),
    ).toBeVisible();
    expect(screen.getAllByLabelText("公开").length).toBeGreaterThan(0);
    expect(screen.getByRole("alert")).toHaveTextContent("已被下架");
    expect(screen.getAllByText("Backend").length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText("未分类（仅标识）")).toHaveAttribute(
      "aria-disabled",
      "true",
    );
    expect(screen.queryByText(/浏览|评论/)).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Backend" }));
    await user.selectOptions(screen.getByLabelText("标签"), "20");
    await waitFor(() =>
      expect(
        requests.some(
          (request) =>
            request.includes("categoryId=10") && request.includes("tagId=20"),
        ),
      ).toBe(true),
    );

    await user.type(screen.getByRole("searchbox", { name: "搜索笔记" }), "spring");
    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(
        requests.some((request) => request.includes("keyword=spring")),
      ).toBe(true),
    );
  });

  it("loads the next page and updates pagination controls", async () => {
    const taxonomy = taxonomyResponse();
    const requestedPages: string[] = [];
    fetchMock.mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }
      if (url.pathname.endsWith("/notes/1")) {
        return jsonResponse(envelope(noteDetail(1)));
      }
      if (url.pathname.endsWith("/notes/21")) {
        return jsonResponse(envelope(noteDetail(21)));
      }

      const page = Number(url.searchParams.get("page") ?? "1");
      requestedPages.push(url.search);

      return jsonResponse(
        envelope({
          items: [noteItem(page === 1 ? 1 : 21)],
          total: 21,
          page,
          size: 20,
        }),
      );
    });
    const user = userEvent.setup();

    renderWorkspace();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Note 1" }),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: "上一页" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "下一页" })).toBeEnabled();

    await user.click(screen.getByRole("button", { name: "下一页" }));

    expect(
      await screen.findByRole("heading", { level: 1, name: "Note 21" }),
    ).toBeVisible();
    expect(requestedPages.some((query) => query.includes("page=2"))).toBe(true);
    expect(screen.getByText("2 / 2")).toBeVisible();
    expect(screen.getByRole("button", { name: "上一页" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "下一页" })).toBeDisabled();
  });

  it("keeps the latest detail when users switch notes quickly", async () => {
    const firstDetail = deferredResponse();
    const taxonomy = taxonomyResponse();
    const requestedPaths: string[] = [];
    fetchMock.mockImplementation((input) => {
      const url = new URL(String(input), "http://localhost");
      requestedPaths.push(url.pathname);

      if (url.pathname.endsWith("/categories")) {
        return Promise.resolve(jsonResponse(taxonomy.categories));
      }
      if (url.pathname.endsWith("/tags")) {
        return Promise.resolve(jsonResponse(taxonomy.tags));
      }
      if (url.pathname.endsWith("/notes/1")) {
        return firstDetail.promise;
      }
      if (url.pathname.endsWith("/notes/2")) {
        return Promise.resolve(jsonResponse(envelope(noteDetail(2))));
      }

      return Promise.resolve(jsonResponse(listResponse([noteItem(1), noteItem(2)])));
    });
    const user = userEvent.setup();
    renderWorkspace();

    await waitFor(() => expect(requestedPaths).toContain("/api/v1/notes/1"));
    await user.click(screen.getByRole("button", { name: /Note 2/ }));

    expect(
      await screen.findByRole("heading", { level: 1, name: "Note 2" }),
    ).toBeVisible();
    firstDetail.resolve(jsonResponse(envelope(noteDetail(1))));
    await waitFor(() =>
      expect(
        screen.getByRole("heading", { level: 1, name: "Note 2" }),
      ).toBeVisible(),
    );
  });

  it("shows explicit empty states", async () => {
    const taxonomy = taxonomyResponse();
    fetchMock.mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }

      return jsonResponse(listResponse([]));
    });

    renderWorkspace();

    expect(await screen.findByText("还没有笔记")).toBeVisible();
    expect(screen.getByText("选择一篇笔记")).toBeVisible();
  });

  it("shows a retryable list error without issuing detail requests", async () => {
    const taxonomy = taxonomyResponse();
    const requestedPaths: string[] = [];
    fetchMock.mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");
      requestedPaths.push(url.pathname);

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }

      return jsonResponse(
        { code: 50000, msg: "列表服务异常", data: null },
        500,
      );
    });

    renderWorkspace();

    expect(await screen.findByText("笔记列表加载失败")).toBeVisible();
    expect(screen.getByText("列表服务异常")).toBeVisible();
    expect(requestedPaths.filter((path) => /\/notes\/\d+$/.test(path))).toHaveLength(0);
  });

  it("shows a terminal not-found state for an unavailable detail", async () => {
    const taxonomy = taxonomyResponse();
    fetchMock.mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }
      if (url.pathname.endsWith("/notes/99")) {
        return jsonResponse(
          { code: 40400, msg: "笔记不存在", data: null },
          404,
        );
      }

      return jsonResponse(listResponse([noteItem(1)]));
    });

    renderWorkspace("/notes/99");

    expect(await screen.findByText("笔记不存在")).toBeVisible();
    expect(screen.queryByRole("button", { name: "重试" })).not.toBeInTheDocument();
  });

  it("creates taxonomy first, then creates a private note and opens its editor", async () => {
    const taxonomy = taxonomyResponse();
    let createdRequest: Record<string, unknown> | null = null;
    fetchMock.mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");

      if (url.pathname.endsWith("/categories") && init?.method === "POST") {
        return jsonResponse(
          envelope({
            id: 11,
            name: "Architecture",
            createdAt: "2026-06-03T10:00:00Z",
            updatedAt: "2026-06-03T10:00:00Z",
          }),
        );
      }
      if (url.pathname.endsWith("/tags") && init?.method === "POST") {
        return jsonResponse(
          envelope({
            id: 21,
            name: "limits",
            createdAt: "2026-06-03T10:00:00Z",
            updatedAt: "2026-06-03T10:00:00Z",
          }),
        );
      }
      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }
      if (url.pathname.endsWith("/notes/31")) {
        return jsonResponse(
          envelope({
            ...noteDetail(31, { categoryId: 11 }),
            title: "Distributed limiter",
            summary: "Design notes",
            contentMd: "# Limiter\n\nRaw Markdown",
            tags: [{ id: 21, name: "limits" }],
          }),
        );
      }
      if (url.pathname.endsWith("/notes") && init?.method === "POST") {
        createdRequest = JSON.parse(String(init.body)) as Record<string, unknown>;
        return jsonResponse(
          envelope({
            id: 31,
            title: "Distributed limiter",
            summary: "Design notes",
            categoryId: 11,
            tags: [{ id: 21, name: "limits" }],
            visibility: "PRIVATE",
            moderationStatus: "NORMAL",
            createdAt: "2026-06-03T10:00:00Z",
            updatedAt: "2026-06-03T10:00:00Z",
          }),
        );
      }

      return jsonResponse(listResponse([]));
    });
    const user = userEvent.setup();
    renderWorkspace("/notes/new");

    expect(
      await screen.findByRole("heading", { name: "新建笔记" }),
    ).toBeVisible();
    await user.type(screen.getByPlaceholderText("请输入笔记标题"), "Distributed limiter");
    await user.type(screen.getByPlaceholderText(/简要描述/), "Design notes");
    await user.type(screen.getByLabelText("Markdown 正文"), "# Limiter\n\nRaw Markdown");
    await user.type(screen.getByLabelText("新分类名称"), "Architecture");
    await user.click(screen.getByRole("button", { name: "新建" }));
    await user.type(screen.getByLabelText("新标签名称"), "limits");
    await user.click(screen.getByRole("button", { name: "添加" }));
    await user.click(screen.getByRole("button", { name: "创建并继续编辑" }));

    expect(
      await screen.findByRole("heading", { name: "编辑笔记" }),
    ).toBeVisible();
    expect(createdRequest).toEqual({
      title: "Distributed limiter",
      contentMd: "# Limiter\n\nRaw Markdown",
      summary: "Design notes",
      categoryId: 11,
      tagIds: [21],
    });
  });

  it("edits raw Markdown and returns to the synchronized reader", async () => {
    const taxonomy = taxonomyResponse();
    let updatedRequest: Record<string, unknown> | null = null;
    fetchMock.mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }
      if (url.pathname.endsWith("/notes/1") && init?.method === "PUT") {
        updatedRequest = JSON.parse(String(init.body)) as Record<string, unknown>;
        return jsonResponse(
          envelope({
            ...noteDetail(1),
            title: "Updated note",
            contentMd: "## Updated\n\nMarkdown only",
          }),
        );
      }
      if (url.pathname.endsWith("/notes/1")) {
        return jsonResponse(envelope(noteDetail(1)));
      }

      return jsonResponse(listResponse([noteItem(1)]));
    });
    const user = userEvent.setup();
    renderWorkspace("/notes/1/edit");

    const title = await screen.findByPlaceholderText("请输入笔记标题");
    await user.clear(title);
    await user.type(title, "Updated note");
    const markdown = screen.getByLabelText("Markdown 正文");
    await user.clear(markdown);
    await user.type(markdown, "## Updated\n\nMarkdown only");
    await user.click(screen.getByRole("button", { name: "保存更改" }));

    expect(
      await screen.findByRole("heading", { level: 1, name: "Updated note" }),
    ).toBeVisible();
    expect(updatedRequest).toMatchObject({
      title: "Updated note",
      contentMd: "## Updated\n\nMarkdown only",
      categoryId: 10,
      tagIds: [20],
    });
  });

  it("publishes, unpublishes, and deletes only after confirmation", async () => {
    const taxonomy = taxonomyResponse();
    let deleted = false;
    fetchMock.mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");

      if (url.pathname.endsWith("/categories")) {
        return jsonResponse(taxonomy.categories);
      }
      if (url.pathname.endsWith("/tags")) {
        return jsonResponse(taxonomy.tags);
      }
      if (url.pathname.endsWith("/notes/1/publish")) {
        return jsonResponse(
          envelope(
            noteDetail(1, { visibility: "PUBLIC" }),
          ),
        );
      }
      if (url.pathname.endsWith("/notes/1/unpublish")) {
        return jsonResponse(envelope(noteDetail(1)));
      }
      if (url.pathname.endsWith("/notes/1") && init?.method === "DELETE") {
        deleted = true;
        return jsonResponse(envelope(null));
      }
      if (url.pathname.endsWith("/notes/1")) {
        return jsonResponse(envelope(noteDetail(1)));
      }

      return jsonResponse(listResponse(deleted ? [] : [noteItem(1)]));
    });
    const user = userEvent.setup();
    renderWorkspace("/notes/1");

    await screen.findByRole("heading", { level: 1, name: "Note 1" });
    await user.click(screen.getByRole("button", { name: "发布" }));
    expect(await screen.findByRole("button", { name: "取消发布" })).toBeVisible();
    await user.click(screen.getByRole("button", { name: "取消发布" }));
    expect(await screen.findByRole("button", { name: "发布" })).toBeVisible();

    await user.click(screen.getByRole("button", { name: "删除笔记" }));
    expect(screen.getByRole("dialog")).toBeVisible();
    await user.click(screen.getByRole("button", { name: "取消" }));
    expect(deleted).toBe(false);
    await user.click(screen.getByRole("button", { name: "删除笔记" }));
    await user.click(screen.getByRole("button", { name: "确认删除" }));

    expect(await screen.findByText("还没有笔记")).toBeVisible();
    expect(screen.getByText("选择一篇笔记")).toBeVisible();
    expect(deleted).toBe(true);
  });
});
