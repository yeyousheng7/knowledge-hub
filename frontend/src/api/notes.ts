import { apiClient } from "@/api/client";
import {
  parseNoteCreateResponse,
  parseNoteDetailResponse,
  parseNoteListResponse,
  type NoteCreateResponse,
  type NoteDetailResponse,
  type NoteListResponse,
  type NoteWriteRequest,
} from "@/api/note-contracts";

export interface NoteListQuery {
  page: number;
  size: number;
  keyword?: string;
  categoryId?: number;
  tagId?: number;
}

function requirePositiveInteger(value: number, field: string): void {
  if (!Number.isSafeInteger(value) || value < 1) {
    throw new RangeError(`${field} must be a positive integer`);
  }
}

export function validateNoteWriteRequest(
  request: NoteWriteRequest,
): NoteWriteRequest {
  if (!request.title.trim()) {
    throw new RangeError("title must not be blank");
  }
  if (request.title.length > 100) {
    throw new RangeError("title must not exceed 100 characters");
  }
  if (request.contentMd.length > 100_000) {
    throw new RangeError("contentMd must not exceed 100000 characters");
  }
  if (request.summary !== null && request.summary.length > 300) {
    throw new RangeError("summary must not exceed 300 characters");
  }
  if (request.categoryId !== null) {
    requirePositiveInteger(request.categoryId, "categoryId");
  }
  if (request.tagIds.length > 10) {
    throw new RangeError("tagIds must not contain more than 10 items");
  }

  const uniqueTagIds = new Set(request.tagIds);
  if (uniqueTagIds.size !== request.tagIds.length) {
    throw new RangeError("tagIds must not contain duplicates");
  }
  request.tagIds.forEach((tagId) => requirePositiveInteger(tagId, "tagId"));

  return request;
}

export function buildNoteListPath(query: NoteListQuery): string {
  requirePositiveInteger(query.page, "page");
  requirePositiveInteger(query.size, "size");

  if (query.size > 100) {
    throw new RangeError("size must not exceed 100");
  }

  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
  });
  const keyword = query.keyword?.trim();

  if (keyword) {
    if (keyword.length > 100) {
      throw new RangeError("keyword must not exceed 100 characters");
    }

    params.set("keyword", keyword);
  }

  if (query.categoryId !== undefined) {
    requirePositiveInteger(query.categoryId, "categoryId");
    params.set("categoryId", String(query.categoryId));
  }

  if (query.tagId !== undefined) {
    requirePositiveInteger(query.tagId, "tagId");
    params.set("tagId", String(query.tagId));
  }

  return `/notes?${params.toString()}`;
}

export function getNotes(
  query: NoteListQuery,
  signal?: AbortSignal,
): Promise<NoteListResponse> {
  return apiClient.request(buildNoteListPath(query), {
    method: "GET",
    signal,
    parseData: parseNoteListResponse,
  });
}

export function getNoteDetail(
  noteId: number,
  signal?: AbortSignal,
): Promise<NoteDetailResponse> {
  requirePositiveInteger(noteId, "noteId");

  return apiClient.request(`/notes/${noteId}`, {
    method: "GET",
    signal,
    parseData: parseNoteDetailResponse,
  });
}

export function createNote(
  request: NoteWriteRequest,
): Promise<NoteCreateResponse> {
  return apiClient.request("/notes", {
    method: "POST",
    body: JSON.stringify(validateNoteWriteRequest(request)),
    parseData: parseNoteCreateResponse,
  });
}

export function updateNote(
  noteId: number,
  request: NoteWriteRequest,
): Promise<NoteDetailResponse> {
  requirePositiveInteger(noteId, "noteId");

  return apiClient.request(`/notes/${noteId}`, {
    method: "PUT",
    body: JSON.stringify(validateNoteWriteRequest(request)),
    parseData: parseNoteDetailResponse,
  });
}

export function deleteNote(noteId: number): Promise<void> {
  requirePositiveInteger(noteId, "noteId");

  return apiClient.request(`/notes/${noteId}`, {
    method: "DELETE",
    parseData: () => undefined,
  });
}

export function publishNote(noteId: number): Promise<NoteDetailResponse> {
  requirePositiveInteger(noteId, "noteId");

  return apiClient.request(`/notes/${noteId}/publish`, {
    method: "POST",
    parseData: parseNoteDetailResponse,
  });
}

export function unpublishNote(noteId: number): Promise<NoteDetailResponse> {
  requirePositiveInteger(noteId, "noteId");

  return apiClient.request(`/notes/${noteId}/unpublish`, {
    method: "POST",
    parseData: parseNoteDetailResponse,
  });
}
