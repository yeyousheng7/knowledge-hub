import { apiClient } from "@/api/client";
import {
  parseNoteDetailResponse,
  parseNoteListResponse,
  type NoteDetailResponse,
  type NoteListResponse,
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
