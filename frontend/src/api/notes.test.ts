import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/api/client";
import {
  parseNoteListItemResponse,
  type NoteWriteRequest,
} from "@/api/note-contracts";
import {
  buildNoteListPath,
  createNote,
  deleteNote,
  getNotes,
  publishNote,
  unpublishNote,
  updateNote,
  validateNoteWriteRequest,
} from "@/api/notes";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const writeRequest: NoteWriteRequest = {
  title: "Rate limiting",
  contentMd: "# Rate limiting\n\nBody",
  summary: "Summary",
  categoryId: 2,
  tagIds: [3, 4],
};

function detailResponse(overrides: Record<string, unknown> = {}) {
  return {
    id: 8,
    title: "Rate limiting",
    contentMd: "# Rate limiting\n\nBody",
    summary: "Summary",
    categoryId: 2,
    tags: [{ id: 3, name: "backend" }],
    visibility: "PRIVATE",
    moderationStatus: "NORMAL",
    createdAt: "2026-06-01T10:00:00Z",
    updatedAt: "2026-06-02T10:00:00Z",
    publishedAt: null,
    ...overrides,
  };
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
      "/api/v1/notes?page=1&size=20",
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

  it("validates every write boundary before making a request", () => {
    expect(() =>
      validateNoteWriteRequest({ ...writeRequest, title: "   " }),
    ).toThrow("title must not be blank");
    expect(() =>
      validateNoteWriteRequest({ ...writeRequest, title: "x".repeat(101) }),
    ).toThrow("title must not exceed 100");
    expect(() =>
      validateNoteWriteRequest({
        ...writeRequest,
        contentMd: "x".repeat(100_001),
      }),
    ).toThrow("contentMd must not exceed 100000");
    expect(() =>
      validateNoteWriteRequest({
        ...writeRequest,
        summary: "x".repeat(301),
      }),
    ).toThrow("summary must not exceed 300");
    expect(() =>
      validateNoteWriteRequest({
        ...writeRequest,
        tagIds: Array.from({ length: 11 }, (_, index) => index + 1),
      }),
    ).toThrow("tagIds must not contain more than 10");
    expect(() =>
      validateNoteWriteRequest({ ...writeRequest, tagIds: [3, 3] }),
    ).toThrow("tagIds must not contain duplicates");
  });

  it("creates a private note with the complete write contract", async () => {
    const detail = detailResponse();
    const created = {
      id: detail.id,
      title: detail.title,
      summary: detail.summary,
      categoryId: detail.categoryId,
      tags: detail.tags,
      visibility: detail.visibility,
      moderationStatus: detail.moderationStatus,
      createdAt: detail.createdAt,
      updatedAt: detail.updatedAt,
    };
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ code: 0, msg: "OK", data: created }),
    );

    const response = await createNote(writeRequest);

    expect(response).toMatchObject({ id: 8, visibility: "PRIVATE" });
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe("/api/v1/notes");
    expect(init).toMatchObject({ method: "POST" });
    expect(JSON.parse(String(init?.body))).toEqual(writeRequest);
  });

  it("updates, publishes, unpublishes, and deletes by note id", async () => {
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse({ code: 0, msg: "OK", data: detailResponse() }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          code: 0,
          msg: "OK",
          data: detailResponse({ visibility: "PUBLIC", publishedAt: "2026-06-03T10:00:00Z" }),
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ code: 0, msg: "OK", data: detailResponse() }),
      )
      .mockResolvedValueOnce(jsonResponse({ code: 0, msg: "OK", data: null }));

    await updateNote(8, writeRequest);
    await publishNote(8);
    await unpublishNote(8);
    await deleteNote(8);

    expect(fetchMock.mock.calls.map(([url, init]) => [url, init?.method])).toEqual([
      ["/api/v1/notes/8", "PUT"],
      ["/api/v1/notes/8/publish", "POST"],
      ["/api/v1/notes/8/unpublish", "POST"],
      ["/api/v1/notes/8", "DELETE"],
    ]);
  });
});
