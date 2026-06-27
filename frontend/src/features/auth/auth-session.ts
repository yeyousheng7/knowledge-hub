import {
  clearStoredSession,
  readStoredSession,
  writeStoredSession,
  type StoredSession,
} from "@/features/auth/auth-storage";

let currentSession: StoredSession | null = null;

export function initializeSession(): StoredSession | null {
  currentSession = readStoredSession();
  return currentSession;
}

export function getCurrentSession(): StoredSession | null {
  return currentSession;
}

export function getAccessToken(): string | null {
  return currentSession?.accessToken ?? null;
}

export function saveSession(session: StoredSession): void {
  writeStoredSession(session);
  currentSession = session;
}

export function clearSession(): void {
  currentSession = null;
  clearStoredSession();
}
