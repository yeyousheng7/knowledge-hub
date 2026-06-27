import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  RouterProvider,
  createMemoryRouter,
} from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { applicationRoutes } from "@/app/router";
import { AuthProvider } from "@/features/auth/AuthProvider";
import { AUTH_STORAGE_KEY } from "@/features/auth/auth-storage";

const fetchMock = vi.fn<typeof fetch>();

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderRoute(path: string) {
  const router = createMemoryRouter(applicationRoutes, {
    initialEntries: [path],
  });

  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
}

describe("ApplicationShell", () => {
  beforeEach(() => {
    fetchMock.mockReset();
    window.localStorage.clear();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("keeps public navigation available without authentication", async () => {
    const user = userEvent.setup();
    renderRoute("/feed");

    expect(screen.getByRole("heading", { name: "Feed" })).toBeVisible();
    expect(screen.getByRole("link", { name: "登录" })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.queryByText("设置")).not.toBeInTheDocument();

    await user.click(screen.getByRole("link", { name: "公开" }));
    expect(screen.getByRole("heading", { name: "公开内容" })).toBeVisible();
    expect(screen.getByRole("link", { name: "公开" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("redirects an anonymous user away from a protected navigation target", async () => {
    renderRoute("/notes");

    expect(
      await screen.findByRole("heading", { name: "登录知识库" }),
    ).toBeVisible();
  });

  it("shows the cached user identity and active protected navigation", async () => {
    window.localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        accessToken: "stored-token",
        expiresAt: Date.now() + 60_000,
        user: {
          id: 8,
          username: "ming_user",
          nickname: "小明",
          role: "USER",
        },
      }),
    );
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        code: 0,
        msg: "OK",
        data: {
          id: 8,
          username: "ming_user",
          nickname: "小明",
          role: "USER",
        },
      }),
    );

    renderRoute("/notes");

    expect(
      await screen.findByRole("img", { name: "小明 的头像" }),
    ).toHaveTextContent("小");
    expect(screen.getByRole("link", { name: "笔记" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });
});
