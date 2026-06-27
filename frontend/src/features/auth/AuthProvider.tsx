import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";

import { apiClient } from "@/api/client";
import type { LoginRequest, LoginUserResponse } from "@/api/contracts";
import { ApiError, isAuthenticationInvalidError } from "@/api/errors";
import { getCurrentUser, login as requestLogin, logout as requestLogout } from "@/features/auth/auth-api";
import {
  AuthContext,
  type AuthContextValue,
  type AuthStatus,
} from "@/features/auth/auth-context";
import type { StoredSession } from "@/features/auth/auth-storage";
import {
  clearSession,
  getAccessToken,
  getCurrentSession,
  initializeSession,
  saveSession,
} from "@/features/auth/auth-session";

interface AuthState {
  status: AuthStatus;
  user: LoginUserResponse | null;
  restoreError: string | null;
}

function restoreErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return "无法恢复登录状态，请稍后重试";
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [state, setState] = useState<AuthState>(() => {
    const initialSession = initializeSession();

    return {
      status: initialSession ? "loading" : "anonymous",
      user: null,
      restoreError: null,
    };
  });

  const discardSession = useCallback(() => {
    clearSession();
    setState({
      status: "anonymous",
      user: null,
      restoreError: null,
    });
  }, []);

  useEffect(
    () =>
      apiClient.configureAuth({
        getAccessToken,
        onAuthenticationInvalid: discardSession,
      }),
    [discardSession],
  );

  useEffect(() => {
    const storedSession = getCurrentSession();

    if (!storedSession || state.status !== "loading") {
      return undefined;
    }

    const controller = new AbortController();

    void getCurrentUser(controller.signal)
      .then((user) => {
        const refreshedSession = { ...storedSession, user };
        saveSession(refreshedSession);
        setState({
          status: "authenticated",
          user,
          restoreError: null,
        });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        if (isAuthenticationInvalidError(error)) {
          discardSession();
          return;
        }

        setState({
          status: "error",
          user: null,
          restoreError: restoreErrorMessage(error),
        });
      });

    return () => controller.abort();
  }, [discardSession, state.status]);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await requestLogin(request);
    const nextSession: StoredSession = {
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresIn * 1_000,
      user: response.user,
    };

    saveSession(nextSession);
    setState({
      status: "authenticated",
      user: response.user,
      restoreError: null,
    });
  }, []);

  const logout = useCallback(async () => {
    let requestError: unknown;

    try {
      await requestLogout();
    } catch (error) {
      requestError = error;
    } finally {
      discardSession();
    }

    if (requestError) {
      throw requestError;
    }
  }, [discardSession]);

  const retrySession = useCallback(() => {
    if (!getCurrentSession()) {
      discardSession();
      return;
    }

    setState({ status: "loading", user: null, restoreError: null });
  }, [discardSession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      status: state.status,
      user: state.user,
      restoreError: state.restoreError,
      login,
      logout,
      retrySession,
      discardSession,
    }),
    [discardSession, login, logout, retrySession, state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
