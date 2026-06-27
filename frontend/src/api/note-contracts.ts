import {
  isRecord,
  readArray,
  readNullableSafeInteger,
  readNullableString,
  readSafeInteger,
  readString,
} from "@/api/contracts";

export type NoteVisibility = "PRIVATE" | "PUBLIC";
export type NoteModerationStatus = "NORMAL" | "TAKEN_DOWN";

export interface NoteTagResponse {
  id: number;
  name: string;
}

export interface NoteListItemResponse {
  id: number;
  title: string;
  summary: string | null;
  categoryId: number | null;
  tags: NoteTagResponse[];
  visibility: NoteVisibility;
  moderationStatus: NoteModerationStatus;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}

export interface NoteListResponse {
  items: NoteListItemResponse[];
  total: number;
  page: number;
  size: number;
}

export interface NoteDetailResponse extends NoteListItemResponse {
  contentMd: string;
}

function parseVisibility(value: unknown): NoteVisibility {
  if (value !== "PRIVATE" && value !== "PUBLIC") {
    throw new TypeError("Expected a supported note visibility");
  }

  return value;
}

function parseModerationStatus(value: unknown): NoteModerationStatus {
  if (value !== "NORMAL" && value !== "TAKEN_DOWN") {
    throw new TypeError("Expected a supported moderation status");
  }

  return value;
}

export function parseNoteTagResponse(value: unknown): NoteTagResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected note tag data");
  }

  return {
    id: readSafeInteger(value, "id"),
    name: readString(value, "name"),
  };
}

export function parseNoteListItemResponse(
  value: unknown,
): NoteListItemResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected note list item data");
  }

  return {
    id: readSafeInteger(value, "id"),
    title: readString(value, "title"),
    summary: readNullableString(value, "summary"),
    categoryId: readNullableSafeInteger(value, "categoryId"),
    tags: readArray(value, "tags").map(parseNoteTagResponse),
    visibility: parseVisibility(value.visibility),
    moderationStatus: parseModerationStatus(value.moderationStatus),
    createdAt: readString(value, "createdAt"),
    updatedAt: readString(value, "updatedAt"),
    publishedAt: readNullableString(value, "publishedAt"),
  };
}

export function parseNoteListResponse(value: unknown): NoteListResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected note list data");
  }

  const response = {
    items: readArray(value, "items").map(parseNoteListItemResponse),
    total: readSafeInteger(value, "total"),
    page: readSafeInteger(value, "page"),
    size: readSafeInteger(value, "size"),
  };

  if (response.total < 0 || response.page < 1 || response.size < 1) {
    throw new TypeError("Expected valid note pagination data");
  }

  return response;
}

export function parseNoteDetailResponse(value: unknown): NoteDetailResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected note detail data");
  }

  return {
    ...parseNoteListItemResponse(value),
    contentMd: readString(value, "contentMd"),
  };
}
