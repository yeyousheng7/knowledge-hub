import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useLocation } from "react-router-dom";
import {
  MemoryRouter,
  Route,
  Routes,
} from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/api/client";
import { AuthProvider } from "@/features/auth/AuthProvider";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { useAuth } from "@/features/auth/auth-context";
import { AUTH_STORAGE_KEY } from "@/features/auth/auth-storage";
import { LoginPage } from "@/pages/login/LoginPage";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function ProtectedProbe() {
  const auth = useAuth();
  const location = useLocation();

  return (
    <div>
      <p>
        protected:{location.pathname}{location.search}:{auth.user?.username}
      </p>
      <button
        onClick={() => void auth.logout().catch(() => undefined)}
        type="button"
      >
        test logout
      </button>
    </div>
  );
}

function TestApplication({ initialEntry }: { initialEntry: string }) {
  return (
    <AuthProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route element={<LoginPage />} path="/login" />
          <Route element={<RequireAuth />}>
            <Route element={<ProtectedProbe />} path="/notes" />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
}

function storedSession(): string {
  return JSON.stringify({
    accessToken: "stored-token",
    expiresAt: Date.now() + 60_000,
    user: {
      id: 7,
      username: "stored_user",
      nickname: "Stored User",
      role: "USER",
    },
  });
}

describe("authentication flow", () => {
  const fetchMock = vi.fn<typeof fetch>();

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

  it("redirects an anonymous user to login and restores the original target", async () => {
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse({
          code: 0,
          msg: "OK",
          data: {
            accessToken: "new-token",
            tokenType: "Bearer",
            expiresIn: 3600,
            user: {
              id: 1,
              username: "test_user",
              nickname: "Test User",
              role: "USER",
            },
          },
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ code: 0, msg: "OK", data: { ready: true } }),
      );
    const user = userEvent.setup();

    render(<TestApplication initialEntry="/notes?keyword=react" />);

    expect(await screen.findByRole("heading", { name: "登录知识库" })).toBeVisible();
    await user.type(screen.getByLabelText("用户名"), "test_user");
    await user.type(screen.getByLabelText("密码"), "password123");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(
      await screen.findByText("protected:/notes?keyword=react:test_user"),
    ).toBeVisible();
    const storedValue = window.localStorage.getItem(AUTH_STORAGE_KEY);
    expect(storedValue).toContain("new-token");
    expect(storedValue).not.toContain("password123");

    await apiClient.request("/probe", {
      parseData: (data) => data,
    });
    const protectedRequest = fetchMock.mock.calls[1]?.[1];
    expect(new Headers(protectedRequest?.headers).get("Authorization")).toBe(
      "Bearer new-token",
    );
  });

  it("validates form fields before sending credentials", async () => {
    const user = userEvent.setup();
    render(<TestApplication initialEntry="/login" />);

    await user.type(screen.getByLabelText("用户名"), "x!");
    await user.type(screen.getByLabelText("密码"), "short");
    await user.click(screen.getByRole("button", { name: "登录" }));

    expect(screen.getByRole("alert")).toHaveTextContent(
      "用户名需为 3–30 位字母、数字或下划线",
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it.each(["short", "x".repeat(73)])(
    "rejects an invalid password length before sending: %s",
    async (password) => {
      const user = userEvent.setup();
      render(<TestApplication initialEntry="/login" />);

      await user.type(screen.getByLabelText("用户名"), "valid_user");
      await user.type(screen.getByLabelText("密码"), password);
      await user.click(screen.getByRole("button", { name: "登录" }));

      expect(screen.getByRole("alert")).toHaveTextContent(
        "密码长度需为 8–72 个字符",
      );
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it("validates a stored token with auth/me before opening a protected route", async () => {
    window.localStorage.setItem(AUTH_STORAGE_KEY, storedSession());
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        code: 0,
        msg: "OK",
        data: {
          id: 7,
          username: "stored_user",
          nickname: "Refreshed User",
          role: "USER",
        },
      }),
    );

    render(<TestApplication initialEntry="/notes" />);

    expect(
      await screen.findByText("protected:/notes:stored_user"),
    ).toBeVisible();
    const requestInit = fetchMock.mock.calls[0]?.[1];
    expect(new Headers(requestInit?.headers).get("Authorization")).toBe(
      "Bearer stored-token",
    );
  });

  it("clears an invalid stored token and returns to login", async () => {
    window.localStorage.setItem(AUTH_STORAGE_KEY, storedSession());
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 40101, msg: "登录已过期", data: null }, 401),
    );

    render(<TestApplication initialEntry="/notes" />);

    expect(await screen.findByRole("heading", { name: "登录知识库" })).toBeVisible();
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
  });

  it("keeps the token for an ordinary 403 and exposes a retry state", async () => {
    window.localStorage.setItem(AUTH_STORAGE_KEY, storedSession());
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 40300, msg: "权限不足", data: null }, 403),
    );

    render(<TestApplication initialEntry="/notes" />);

    expect(
      await screen.findByRole("heading", { name: "无法恢复登录状态" }),
    ).toBeVisible();
    expect(screen.getByText("权限不足")).toBeVisible();
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).not.toBeNull();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledOnce());
  });

  it("clears the local session when server logout fails", async () => {
    window.localStorage.setItem(AUTH_STORAGE_KEY, storedSession());
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse({
          code: 0,
          msg: "OK",
          data: {
            id: 7,
            username: "stored_user",
            nickname: "Stored User",
            role: "USER",
          },
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ code: 50300, msg: "服务暂不可用", data: null }, 503),
      );
    const user = userEvent.setup();
    render(<TestApplication initialEntry="/notes" />);

    expect(
      await screen.findByText("protected:/notes:stored_user"),
    ).toBeVisible();
    await user.click(screen.getByRole("button", { name: "test logout" }));

    expect(await screen.findByRole("heading", { name: "登录知识库" })).toBeVisible();
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
  });
});
