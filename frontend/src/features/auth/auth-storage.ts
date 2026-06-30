import {
  parseLoginUserResponse,
  type LoginUserResponse,
} from "@/api/contracts";

export const AUTH_STORAGE_KEY = "knowledgehub.auth.v1";

export interface StoredSession {
  accessToken: string;
  expiresAt: number;
  user: LoginUserResponse;
}

function parseStoredSession(value: unknown): StoredSession {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new TypeError("Expected a stored session");
  }

  const record = value as Record<string, unknown>;

  if (
    typeof record.accessToken !== "string" ||
    record.accessToken.trim() === "" ||
    typeof record.expiresAt !== "number" ||
    !Number.isFinite(record.expiresAt)
  ) {
    throw new TypeError("Stored session is invalid");
  }

  return {
    accessToken: record.accessToken,
    expiresAt: record.expiresAt,
    user: parseLoginUserResponse(record.user),
  };
}

function removeStoredValue(): void {
  try {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
  } catch {
    // In-memory auth state is still cleared by the caller.
  }
}

export function readStoredSession(): StoredSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const rawSession = window.localStorage.getItem(AUTH_STORAGE_KEY);

    if (!rawSession) {
      return null;
    }

    const session = parseStoredSession(JSON.parse(rawSession) as unknown);

    if (session.expiresAt <= Date.now()) {
      removeStoredValue();
      return null;
    }

    return session;
  } catch {
    removeStoredValue();
    return null;
  }
}

export function writeStoredSession(session: StoredSession): void {
  try {
    window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  } catch (error) {
    throw new Error("无法保存登录状态，请检查浏览器存储设置", { cause: error });
  }
}

export function clearStoredSession(): void {
  if (typeof window !== "undefined") {
    removeStoredValue();
  }
}
