import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/api/client";
import { createCategory, createTag } from "@/api/taxonomy";

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ code: 0, msg: "OK", data }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

describe("taxonomy API", () => {
  const fetchMock = vi.fn<typeof fetch>();
  let resetAuth: () => void;

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
    resetAuth = apiClient.configureAuth({
      getAccessToken: () => "taxonomy-token",
      onAuthenticationInvalid: () => undefined,
    });
  });

  afterEach(() => {
    resetAuth();
    vi.unstubAllGlobals();
  });

  it.each([
    ["category", createCategory, "/api/v1/categories"],
    ["tag", createTag, "/api/v1/tags"],
  ] as const)("creates and validates a %s", async (_type, create, path) => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        id: 6,
        name: "backend",
        createdAt: "2026-06-01T10:00:00Z",
        updatedAt: "2026-06-01T10:00:00Z",
      }),
    );

    await expect(create("  backend  ")).resolves.toMatchObject({
      id: 6,
      name: "backend",
    });
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe(path);
    expect(init).toMatchObject({ method: "POST" });
    expect(JSON.parse(String(init?.body))).toEqual({ name: "backend" });
    expect(() => create("   ")).toThrow("name must not be blank");
    expect(() => create("x".repeat(31))).toThrow("name must not exceed 30");
  });
});
