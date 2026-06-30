import {
  isRecord,
  readArray,
  readFiniteNumber,
  readNullableString,
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

export interface AiAgentAction {
  type: string;
  payload: Record<string, unknown>;
}

export interface AiAgentChatResponse {
  answer: string;
  actions: AiAgentAction[];
}

export interface AiAgentSessionClearResponse {
  cleared: boolean;
}

export type PendingOperationType =
  | "BATCH_UNPUBLISH_NOTES"
  | "CREATE_PRIVATE_NOTE";

export interface AiAgentOperationAffectedItem {
  id: number;
  title: string;
}

export interface AiAgentOperationConfirmResponse {
  operationId: string;
  operationType: PendingOperationType;
  status: "EXECUTED" | string;
  affectedCount: number;
  affectedItems: AiAgentOperationAffectedItem[];
  message: string;
}

interface PendingOperationBasePayload {
  operationId: string;
  operationType: PendingOperationType;
  preview: string;
  expiresAt: string;
}

export interface BatchUnpublishPendingOperationPayload
  extends PendingOperationBasePayload {
  operationType: "BATCH_UNPUBLISH_NOTES";
  affectedItems: AiAgentOperationAffectedItem[];
}

export interface CreatePrivateNotePendingOperationPayload
  extends PendingOperationBasePayload {
  operationType: "CREATE_PRIVATE_NOTE";
  draft: {
    title: string;
    summary: string | null;
    contentMd: string;
    recommendedTags: string[];
  };
}

export type PendingOperationPayload =
  | BatchUnpublishPendingOperationPayload
  | CreatePrivateNotePendingOperationPayload;

export interface PendingOperationAction {
  type: "PENDING_OPERATION";
  payload: PendingOperationPayload;
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

function parsePendingOperationType(value: unknown): PendingOperationType {
  if (value !== "BATCH_UNPUBLISH_NOTES" && value !== "CREATE_PRIVATE_NOTE") {
    throw new TypeError("Expected a supported pending operation type");
  }

  return value;
}

function parseAffectedItem(value: unknown): AiAgentOperationAffectedItem {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI Agent affected item data");
  }

  const item = {
    id: readSafeInteger(value, "id"),
    title: readString(value, "title"),
  };

  if (item.id < 1) {
    throw new TypeError("Expected valid AI Agent affected item id");
  }

  return item;
}

function parsePendingOperationBasePayload(
  value: Record<string, unknown>,
): PendingOperationBasePayload {
  const operationId = readString(value, "operationId");
  const operationType = parsePendingOperationType(value.operationType);
  const payload = {
    operationId,
    operationType,
    preview: readString(value, "preview"),
    expiresAt: readDateTime(value, "expiresAt"),
  };

  if (!payload.operationId.trim()) {
    throw new TypeError("Expected a usable pending operation id");
  }

  return payload;
}

function parseBatchUnpublishPendingPayload(
  value: Record<string, unknown>,
): BatchUnpublishPendingOperationPayload {
  return {
    ...parsePendingOperationBasePayload(value),
    operationType: "BATCH_UNPUBLISH_NOTES",
    affectedItems: readArray(value, "affectedItems").map(parseAffectedItem),
  };
}

function parseCreatePrivateNotePendingPayload(
  value: Record<string, unknown>,
): CreatePrivateNotePendingOperationPayload {
  const draft = value.draft;

  if (!isRecord(draft)) {
    throw new TypeError("Expected AI Agent draft preview data");
  }

  return {
    ...parsePendingOperationBasePayload(value),
    operationType: "CREATE_PRIVATE_NOTE",
    draft: {
      title: readString(draft, "title"),
      summary: readNullableString(draft, "summary"),
      contentMd: readString(draft, "contentMd"),
      recommendedTags: readArray(draft, "recommendedTags").map((tag) => {
        if (typeof tag !== "string") {
          throw new TypeError("Expected recommendedTags item to be a string");
        }

        return tag;
      }),
    },
  };
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

export function parseAiAgentAction(value: unknown): AiAgentAction {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI Agent action data");
  }

  const payload = value.payload;

  if (!isRecord(payload)) {
    throw new TypeError("Expected AI Agent action payload data");
  }

  return {
    type: readString(value, "type"),
    payload,
  };
}

export function parseAiAgentChatResponse(
  value: unknown,
): AiAgentChatResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI Agent chat data");
  }

  return {
    answer: readString(value, "answer"),
    actions: readArray(value, "actions").map(parseAiAgentAction),
  };
}

export function parseAiAgentSessionClearResponse(
  value: unknown,
): AiAgentSessionClearResponse {
  if (!isRecord(value) || typeof value.cleared !== "boolean") {
    throw new TypeError("Expected AI Agent session clear data");
  }

  return {
    cleared: value.cleared,
  };
}

export function parseAiAgentOperationConfirmResponse(
  value: unknown,
): AiAgentOperationConfirmResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected AI Agent operation confirm data");
  }

  const response = {
    operationId: readString(value, "operationId"),
    operationType: parsePendingOperationType(value.operationType),
    status: readString(value, "status"),
    affectedCount: readSafeInteger(value, "affectedCount"),
    affectedItems: readArray(value, "affectedItems").map(parseAffectedItem),
    message: readString(value, "message"),
  };

  if (!response.operationId.trim() || response.affectedCount < 0) {
    throw new TypeError("Expected valid AI Agent operation confirm data");
  }

  return response;
}

export function parsePendingOperationAction(
  action: AiAgentAction,
): PendingOperationAction {
  if (action.type !== "PENDING_OPERATION") {
    throw new TypeError("Expected pending operation action");
  }

  const basePayload = parsePendingOperationBasePayload(action.payload);
  const payload =
    basePayload.operationType === "BATCH_UNPUBLISH_NOTES"
      ? parseBatchUnpublishPendingPayload(action.payload)
      : parseCreatePrivateNotePendingPayload(action.payload);

  return {
    type: "PENDING_OPERATION",
    payload,
  };
}
