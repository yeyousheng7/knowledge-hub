import { createContext, useContext } from "react";

import type {
  LoginRequest,
  LoginUserResponse,
} from "@/api/contracts";

export type AuthStatus =
  | "loading"
  | "anonymous"
  | "authenticated"
  | "error";

export interface AuthContextValue {
  status: AuthStatus;
  user: LoginUserResponse | null;
  restoreError: string | null;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  retrySession: () => void;
  discardSession: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
