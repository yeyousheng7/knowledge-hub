import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AuthProvider } from "@/features/auth/AuthProvider";
import { LoginPage } from "@/pages/login/LoginPage";
import { RegisterPage } from "@/pages/register/RegisterPage";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderAuthRoutes(initialEntry: "/login" | "/register") {
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route element={<LoginPage />} path="/login" />
          <Route element={<RegisterPage />} path="/register" />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

async function fillRegistrationForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("用户名"), "new_user");
  await user.type(screen.getByLabelText("密码"), "password123");
  await user.type(screen.getByLabelText("确认密码"), "password123");
  await user.type(screen.getByLabelText("昵称"), "New User");
  await user.type(screen.getByLabelText("邀请码"), "invite-code");
}

describe("registration flow", () => {
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

  it("links the login and registration routes", async () => {
    const user = userEvent.setup();
    renderAuthRoutes("/login");

    await user.click(screen.getByRole("link", { name: "注册账号" }));

    expect(
      screen.getByRole("heading", { name: "创建你的账号" }),
    ).toBeVisible();
    expect(
      screen.getByRole("link", { name: "已有账号？去登录" }),
    ).toHaveAttribute("href", "/login");
  });

  it("validates matching passwords before sending registration data", async () => {
    const user = userEvent.setup();
    renderAuthRoutes("/register");

    await user.type(screen.getByLabelText("用户名"), "new_user");
    await user.type(screen.getByLabelText("密码"), "password123");
    await user.type(screen.getByLabelText("确认密码"), "different123");
    await user.type(screen.getByLabelText("邀请码"), "invite-code");
    await user.click(screen.getByRole("button", { name: "注册" }));

    expect(screen.getByRole("alert")).toHaveTextContent("两次输入的密码不一致");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("toggles password visibility without changing the value", async () => {
    const user = userEvent.setup();
    renderAuthRoutes("/register");
    const password = screen.getByLabelText("密码");

    await user.type(password, "password123");
    await user.click(screen.getByRole("button", { name: "显示密码" }));

    expect(password).toHaveAttribute("type", "text");
    expect(password).toHaveValue("password123");
  });

  it("registers through the real contract and returns to a prefilled login", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        code: 0,
        msg: "OK",
        data: { id: 12, username: "new_user", nickname: "New User" },
      }),
    );
    const user = userEvent.setup();
    renderAuthRoutes("/register");

    await fillRegistrationForm(user);
    await user.click(screen.getByRole("button", { name: "注册" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "注册成功，请登录新账号",
    );
    expect(screen.getByLabelText("用户名")).toHaveValue("new_user");
    expect(screen.getByLabelText("密码")).toHaveValue("");

    const [requestUrl, requestInit] = fetchMock.mock.calls[0] ?? [];
    expect(requestUrl).toBe("/api/v1/auth/register");
    expect(requestInit).toMatchObject({ method: "POST" });
    expect(JSON.parse(String(requestInit?.body))).toEqual({
      username: "new_user",
      password: "password123",
      nickname: "New User",
      inviteCode: "invite-code",
    });
    expect(new Headers(requestInit?.headers).has("Authorization")).toBe(false);
  });

  it("shows backend registration errors without leaving the form", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 40901, msg: "用户名已存在", data: null }, 409),
    );
    const user = userEvent.setup();
    renderAuthRoutes("/register");

    await fillRegistrationForm(user);
    await user.click(screen.getByRole("button", { name: "注册" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("用户名已存在");
    expect(screen.getByRole("heading", { name: "创建你的账号" })).toBeVisible();
  });
});
