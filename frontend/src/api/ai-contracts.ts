import {
  isRecord,
  readArray,
  readFiniteNumber,
  readSafeInteger,
  readString,
} from "@/api/contracts";

export type AiSourceVisibility = "PRIVATE" | "PUBLIC";

export interface AiIndexRebuildResponse {
  userId: number;
  chunkCount: number;
  indexedAt: string;
}

export interface AiRagSourceResponse {
  noteId: number;
  title: string;
  snippet: string;
  chunkIndex: number;
  distance: number;
  visibility: AiSourceVisibility;
  updatedAt: string;
}

export interface AiRagAskResponse {
  answer: string;
  sources: AiRagSourceResponse[];
}

function readDateTime(record: Record<string, unknown>, key: string): string {
  const value = readString(record, key);

  if (!Number.isFinite(Date.parse(value))) {
    throw new TypeError(`Expected ${key} to be an ISO date-time string`);
  }

  return value;
}

function parseSourceVisibility(value: unknown): AiSourceVisibility {
  if (value !== "PRIVATE" && value !== "PUBLIC") {
    throw new TypeError("Expected a supported source visibility");
  }

  return value;
}

export function parseAiIndexRebuildResponse(
  value: unknown,
): AiIndexRebuildResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI index rebuild data");
  }

  const response = {
    userId: readSafeInteger(value, "userId"),
    chunkCount: readSafeInteger(value, "chunkCount"),
    indexedAt: readDateTime(value, "indexedAt"),
  };

  if (response.userId < 1 || response.chunkCount < 0) {
    throw new TypeError("Expected valid AI index rebuild data");
  }

  return response;
}

export function parseAiRagSourceResponse(
  value: unknown,
): AiRagSourceResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI RAG source data");
  }

  const source = {
    noteId: readSafeInteger(value, "noteId"),
    title: readString(value, "title"),
    snippet: readString(value, "snippet"),
    chunkIndex: readSafeInteger(value, "chunkIndex"),
    distance: readFiniteNumber(value, "distance"),
    visibility: parseSourceVisibility(value.visibility),
    updatedAt: readDateTime(value, "updatedAt"),
  };

  if (source.noteId < 1 || source.chunkIndex < 0 || source.distance < 0) {
    throw new TypeError("Expected valid AI RAG source data");
  }

  return source;
}

export function parseAiRagAskResponse(value: unknown): AiRagAskResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI RAG answer data");
  }

  return {
    answer: readString(value, "answer"),
    sources: readArray(value, "sources").map(parseAiRagSourceResponse),
  };
}
