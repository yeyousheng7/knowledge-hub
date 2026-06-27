import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/api/client";
import { parseNoteListItemResponse } from "@/api/note-contracts";
import { buildNoteListPath, getNotes } from "@/api/notes";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("notes API", () => {
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
    resetAuth();
    vi.unstubAllGlobals();
  });

  it("serializes numeric pagination and combined filters", () => {
    expect(
      buildNoteListPath({
        page: 2,
        size: 20,
        keyword: "  spring ai  ",
        categoryId: 3,
        tagId: 9,
      }),
    ).toBe("/notes?page=2&size=20&keyword=spring+ai&categoryId=3&tagId=9");
  });

  it("rejects invalid query boundaries before making a request", () => {
    expect(() => buildNoteListPath({ page: 0, size: 20 })).toThrow(RangeError);
    expect(() => buildNoteListPath({ page: 1, size: 101 })).toThrow(RangeError);
    expect(() =>
      buildNoteListPath({ page: 1, size: 20, keyword: "x".repeat(101) }),
    ).toThrow(RangeError);
  });

  it("unwraps and validates a note list response", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        code: 0,
        msg: "OK",
        data: {
          items: [
            {
              id: 1,
              title: "Public note",
              summary: null,
              categoryId: null,
              tags: [],
              visibility: "PUBLIC",
              moderationStatus: "NORMAL",
              createdAt: "2026-06-01T10:00:00Z",
              updatedAt: "2026-06-02T10:00:00Z",
              publishedAt: null,
            },
          ],
          total: 1,
          page: 1,
          size: 20,
        },
      }),
    );

    const response = await getNotes({ page: 1, size: 20 });

    expect(response.items[0]).toMatchObject({
      visibility: "PUBLIC",
      publishedAt: null,
      categoryId: null,
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/notes?page=1&size=20",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("rejects unsupported visibility values", () => {
    expect(() =>
      parseNoteListItemResponse({
        id: 1,
        title: "Invalid",
        summary: "",
        categoryId: null,
        tags: [],
        visibility: "DRAFT",
        moderationStatus: "NORMAL",
        createdAt: "2026-06-01T10:00:00Z",
        updatedAt: "2026-06-01T10:00:00Z",
        publishedAt: null,
      }),
    ).toThrow("supported note visibility");
  });
});
