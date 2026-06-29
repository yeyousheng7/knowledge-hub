import { apiClient } from "@/api/client";
import {
  parsePublicNoteDetailResponse,
  parsePublicNoteListResponse,
  parsePublicUserProfileResponse,
  type PublicNoteDetailResponse,
  type PublicNoteListResponse,
  type PublicUserProfileResponse,
} from "@/api/public-note-contracts";

export interface PublicNoteListQuery {
  page: number;
  size: number;
  keyword?: string;
}

export interface PublicUserNoteListQuery {
  username: string;
  page: number;
  size: number;
}

function requirePositiveInteger(value: number, field: string): void {
  if (!Number.isSafeInteger(value) || value < 1) {
    throw new RangeError(`${field} must be a positive integer`);
  }
}

function validateUsername(username: string): string {
  if (!/^[0-9a-zA-Z_]{3,30}$/.test(username)) {
    throw new RangeError("username must be 3-30 letters, numbers or underscores");
  }

  return username;
}

export function buildPublicNoteListPath(query: PublicNoteListQuery): string {
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

  return `/public/notes?${params.toString()}`;
}

export function buildPublicUserNotesPath(
  query: PublicUserNoteListQuery,
): string {
  requirePositiveInteger(query.page, "page");
  requirePositiveInteger(query.size, "size");

  if (query.size > 100) {
    throw new RangeError("size must not exceed 100");
  }

  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
  });

  return `/public/users/${validateUsername(query.username)}/notes?${params.toString()}`;
}

export function listPublicNotes(
  query: PublicNoteListQuery,
  signal?: AbortSignal,
): Promise<PublicNoteListResponse> {
  return apiClient.request(buildPublicNoteListPath(query), {
    method: "GET",
    auth: false,
    signal,
    parseData: parsePublicNoteListResponse,
  });
}

export function getPublicNoteDetail(
  noteId: number,
  signal?: AbortSignal,
): Promise<PublicNoteDetailResponse> {
  requirePositiveInteger(noteId, "noteId");

  return apiClient.request(`/public/notes/${noteId}`, {
    method: "GET",
    auth: false,
    signal,
    parseData: parsePublicNoteDetailResponse,
  });
}

export function getPublicUserProfile(
  username: string,
  signal?: AbortSignal,
): Promise<PublicUserProfileResponse> {
  return apiClient.request(`/public/users/${validateUsername(username)}`, {
    method: "GET",
    auth: false,
    signal,
    parseData: parsePublicUserProfileResponse,
  });
}

export function listPublicUserNotes(
  query: PublicUserNoteListQuery,
  signal?: AbortSignal,
): Promise<PublicNoteListResponse> {
  return apiClient.request(buildPublicUserNotesPath(query), {
    method: "GET",
    auth: false,
    signal,
    parseData: parsePublicNoteListResponse,
  });
}
