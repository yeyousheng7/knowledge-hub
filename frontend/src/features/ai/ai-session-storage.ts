import {
  parseAiAgentOperationConfirmResponse,
  type AiAgentAction,
  type AiAgentOperationConfirmResponse,
  type AiRagAskResponse,
} from "@/api/ai-contracts";

const AI_SESSION_STORAGE_PREFIX = "knowledgehub.ai.session.v1";
export const AI_TRANSCRIPT_MAX_TURNS = 50;
const AI_AGENT_MAX_MESSAGES = AI_TRANSCRIPT_MAX_TURNS * 2;

export interface RagTranscriptTurn {
  id: string;
  question: string;
  answer: AiRagAskResponse | null;
  error: string | null;
}

export interface AgentTranscriptMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  actions: AgentTranscriptAction[];
}

export type AgentOperationResolutionStatus = "executed" | "invalid" | "ignored";

export interface AgentOperationResolution {
  status: AgentOperationResolutionStatus;
  result: AiAgentOperationConfirmResponse | null;
  error: string | null;
}

export interface AgentTranscriptAction extends AiAgentAction {
  operationResolution?: AgentOperationResolution;
}

interface AiSessionSnapshot {
  ragTurns: RagTranscriptTurn[];
  agentMessages: AgentTranscriptMessage[];
}

function storageKey(userId: number): string {
  return `${AI_SESSION_STORAGE_PREFIX}:${userId}`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseRagTurn(value: unknown): RagTranscriptTurn | null {
  if (!isRecord(value)) {
    return null;
  }

  if (
    typeof value.id !== "string" ||
    typeof value.question !== "string" ||
    (value.error !== null && typeof value.error !== "string")
  ) {
    return null;
  }

  return {
    id: value.id,
    question: value.question,
    answer: isRecord(value.answer) ? (value.answer as unknown as AiRagAskResponse) : null,
    error: value.error,
  };
}

function parseAgentMessage(value: unknown): AgentTranscriptMessage | null {
  if (!isRecord(value)) {
    return null;
  }

  if (
    typeof value.id !== "string" ||
    (value.role !== "user" && value.role !== "assistant") ||
    typeof value.content !== "string" ||
    !Array.isArray(value.actions)
  ) {
    return null;
  }

  return {
    id: value.id,
    role: value.role,
    content: value.content,
    actions: value.actions.filter(isRecord).map(parseAgentAction),
  };
}

function parseAgentAction(action: Record<string, unknown>): AgentTranscriptAction {
  const parsedAction: AgentTranscriptAction = {
    type: typeof action.type === "string" ? action.type : "",
    payload: isRecord(action.payload) ? action.payload : {},
  };
  const resolution = parseAgentOperationResolution(action.operationResolution);

  return resolution
    ? { ...parsedAction, operationResolution: resolution }
    : parsedAction;
}

function parseAgentOperationResolution(
  value: unknown,
): AgentOperationResolution | null {
  if (!isRecord(value)) {
    return null;
  }

  if (
    value.status !== "executed" &&
    value.status !== "invalid" &&
    value.status !== "ignored"
  ) {
    return null;
  }

  let result: AiAgentOperationConfirmResponse | null = null;

  if (isRecord(value.result)) {
    try {
      result = parseAiAgentOperationConfirmResponse(value.result);
    } catch {
      result = null;
    }
  }

  return {
    status: value.status,
    result,
    error: typeof value.error === "string" ? value.error : null,
  };
}

function parseSnapshot(value: unknown): AiSessionSnapshot {
  if (!isRecord(value)) {
    return { ragTurns: [], agentMessages: [] };
  }

  const ragTurns = Array.isArray(value.ragTurns)
    ? value.ragTurns.map(parseRagTurn).filter((turn) => turn !== null)
    : [];
  const agentMessages = Array.isArray(value.agentMessages)
    ? value.agentMessages
        .map(parseAgentMessage)
        .filter((message) => message !== null)
    : [];

  return {
    ragTurns: trimRagTurns(ragTurns),
    agentMessages: trimAgentMessages(agentMessages),
  };
}

function readSnapshot(userId: number): AiSessionSnapshot {
  if (typeof window === "undefined") {
    return { ragTurns: [], agentMessages: [] };
  }

  try {
    const rawValue = window.sessionStorage.getItem(storageKey(userId));
    return rawValue
      ? parseSnapshot(JSON.parse(rawValue) as unknown)
      : { ragTurns: [], agentMessages: [] };
  } catch {
    window.sessionStorage.removeItem(storageKey(userId));
    return { ragTurns: [], agentMessages: [] };
  }
}

function writeSnapshot(userId: number, snapshot: AiSessionSnapshot): void {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.setItem(
    storageKey(userId),
    JSON.stringify({
      ragTurns: trimRagTurns(snapshot.ragTurns),
      agentMessages: trimAgentMessages(snapshot.agentMessages),
    }),
  );
}

export function trimRagTurns(turns: RagTranscriptTurn[]): RagTranscriptTurn[] {
  return turns.slice(-AI_TRANSCRIPT_MAX_TURNS);
}

export function trimAgentMessages(
  messages: AgentTranscriptMessage[],
): AgentTranscriptMessage[] {
  return messages.slice(-AI_AGENT_MAX_MESSAGES);
}

export function readRagTranscript(userId: number | null): RagTranscriptTurn[] {
  return userId ? readSnapshot(userId).ragTurns : [];
}

export function writeRagTranscript(
  userId: number | null,
  turns: RagTranscriptTurn[],
): void {
  if (!userId) {
    return;
  }

  const snapshot = readSnapshot(userId);
  writeSnapshot(userId, {
    ...snapshot,
    ragTurns: trimRagTurns(turns),
  });
}

export function readAgentTranscript(
  userId: number | null,
): AgentTranscriptMessage[] {
  return userId ? readSnapshot(userId).agentMessages : [];
}

export function writeAgentTranscript(
  userId: number | null,
  messages: AgentTranscriptMessage[],
): void {
  if (!userId) {
    return;
  }

  const snapshot = readSnapshot(userId);
  writeSnapshot(userId, {
    ...snapshot,
    agentMessages: trimAgentMessages(messages),
  });
}

export function clearAgentTranscript(userId: number | null): void {
  if (!userId) {
    return;
  }

  const snapshot = readSnapshot(userId);
  writeSnapshot(userId, {
    ...snapshot,
    agentMessages: [],
  });
}

export function clearAiTranscript(userId: number | null): void {
  if (!userId) {
    return;
  }

  writeSnapshot(userId, {
    ragTurns: [],
    agentMessages: [],
  });
}
