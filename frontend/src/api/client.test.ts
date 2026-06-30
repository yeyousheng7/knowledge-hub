import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClient } from "@/api/client";
import { ApiError } from "@/api/errors";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("ApiClient", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("uses the same-origin API path by default", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 0, msg: "OK", data: { value: 42 } }),
    );
    const client = new ApiClient();

    await client.request("/example", {
      method: "GET",
      auth: false,
      parseData: (data) => data,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/example",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("adds the bearer token and unwraps a successful envelope", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 0, msg: "OK", data: { value: 42 } }),
    );
    const client = new ApiClient({
      baseUrl: "http://api.test/api/v1/",
      authHooks: { getAccessToken: () => "secret-token" },
    });

    const result = await client.request("/example", {
      method: "GET",
      parseData: (data) => data as { value: number },
    });

    expect(result).toEqual({ value: 42 });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://api.test/api/v1/example",
      expect.objectContaining({ method: "GET" }),
    );
    const requestInit = fetchMock.mock.calls[0]?.[1];
    expect(new Headers(requestInit?.headers).get("Authorization")).toBe(
      "Bearer secret-token",
    );
  });

  it.each([
    { status: 200, code: 40101 },
    { status: 403, code: 40301 },
  ])(
    "clears authentication for status $status and code $code",
    async ({ status, code }) => {
      const onAuthenticationInvalid = vi.fn();
      fetchMock.mockResolvedValueOnce(
        jsonResponse({ code, msg: "会话无效", data: null }, status),
      );
      const client = new ApiClient({
        baseUrl: "http://api.test/api/v1",
        authHooks: { onAuthenticationInvalid },
      });

      await expect(
        client.request("/protected", {
          method: "GET",
          parseData: (data) => data,
        }),
      ).rejects.toBeInstanceOf(ApiError);
      expect(onAuthenticationInvalid).toHaveBeenCalledOnce();
    },
  );

  it.each([
    { status: 401, code: 40102, message: "用户名或密码错误" },
    { status: 403, code: 40300, message: "权限不足" },
  ])(
    "does not clear authentication for business code $code",
    async ({ status, code, message }) => {
      const onAuthenticationInvalid = vi.fn();
      fetchMock.mockResolvedValueOnce(
        jsonResponse({ code, msg: message, data: null }, status),
      );
      const client = new ApiClient({
        baseUrl: "http://api.test/api/v1",
        authHooks: { onAuthenticationInvalid },
      });

      await expect(
        client.request("/request", {
          method: "GET",
          auth: false,
          parseData: (data) => data,
        }),
      ).rejects.toMatchObject({ message, status, code });
      expect(onAuthenticationInvalid).not.toHaveBeenCalled();
    },
  );

  it("rejects a malformed success response instead of trusting unknown data", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 0, msg: "OK", data: { id: "not-a-number" } }),
    );
    const client = new ApiClient({ baseUrl: "http://api.test/api/v1" });

    await expect(
      client.request("/malformed", {
        auth: false,
        parseData: (data) => {
          if (
            typeof data !== "object" ||
            data === null ||
            typeof (data as { id?: unknown }).id !== "number"
          ) {
            throw new TypeError("Invalid id");
          }

          return data;
        },
      }),
    ).rejects.toMatchObject({ message: "服务器响应格式无效" });
  });
});
