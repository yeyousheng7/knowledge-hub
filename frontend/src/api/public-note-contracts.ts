import {
  isRecord,
  readArray,
  readSafeInteger,
  readString,
} from "@/api/contracts";

export interface PublicNoteAuthorResponse {
  username: string;
  nickname: string;
}

export interface PublicNoteTagResponse {
  name: string;
}

export interface PublicNoteListItemResponse {
  id: number;
  title: string;
  summary: string;
  tags: PublicNoteTagResponse[];
  author: PublicNoteAuthorResponse;
  publishedAt: string;
  updatedAt: string;
}

export interface PublicNoteListResponse {
  items: PublicNoteListItemResponse[];
  total: number;
  page: number;
  size: number;
}

export interface PublicNoteDetailResponse extends PublicNoteListItemResponse {
  contentMd: string;
}

export interface PublicUserProfileResponse {
  username: string;
  nickname: string;
  bio: string;
  createdAt: string;
}

function readDateTime(record: Record<string, unknown>, key: string): string {
  const value = readString(record, key);

  if (!Number.isFinite(Date.parse(value))) {
    throw new TypeError(`Expected ${key} to be an ISO date-time string`);
  }

  return value;
}

function parsePublicNoteAuthorResponse(
  value: unknown,
): PublicNoteAuthorResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected public note author data");
  }

  return {
    username: readString(value, "username"),
    nickname: readString(value, "nickname"),
  };
}

function parsePublicNoteTagResponse(value: unknown): PublicNoteTagResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected public note tag data");
  }

  return {
    name: readString(value, "name"),
  };
}

export function parsePublicNoteListItemResponse(
  value: unknown,
): PublicNoteListItemResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected public note list item data");
  }

  const item = {
    id: readSafeInteger(value, "id"),
    title: readString(value, "title"),
    summary: readString(value, "summary"),
    tags: readArray(value, "tags").map(parsePublicNoteTagResponse),
    author: parsePublicNoteAuthorResponse(value.author),
    publishedAt: readDateTime(value, "publishedAt"),
    updatedAt: readDateTime(value, "updatedAt"),
  };

  if (item.id < 1) {
    throw new TypeError("Expected valid public note id");
  }

  return item;
}

export function parsePublicNoteListResponse(
  value: unknown,
): PublicNoteListResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected public note list data");
  }

  const response = {
    items: readArray(value, "items").map(parsePublicNoteListItemResponse),
    total: readSafeInteger(value, "total"),
    page: readSafeInteger(value, "page"),
    size: readSafeInteger(value, "size"),
  };

  if (response.total < 0 || response.page < 1 || response.size < 1) {
    throw new TypeError("Expected valid public note pagination data");
  }

  return response;
}

export function parsePublicNoteDetailResponse(
  value: unknown,
): PublicNoteDetailResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected public note detail data");
  }

  return {
    ...parsePublicNoteListItemResponse(value),
    contentMd: readString(value, "contentMd"),
  };
}

export function parsePublicUserProfileResponse(
  value: unknown,
): PublicUserProfileResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected public user profile data");
  }

  return {
    username: readString(value, "username"),
    nickname: readString(value, "nickname"),
    bio: readString(value, "bio"),
    createdAt: readDateTime(value, "createdAt"),
  };
}
