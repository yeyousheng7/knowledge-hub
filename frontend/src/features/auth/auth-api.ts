import {
  parseLoginResponse,
  parseRegisterResponse,
  parseLoginUserResponse,
  type LoginRequest,
  type LoginResponse,
  type LoginUserResponse,
  type RegisterRequest,
  type RegisterResponse,
} from "@/api/contracts";
import { apiClient } from "@/api/client";

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiClient.request("/auth/login", {
    method: "POST",
    auth: false,
    body: JSON.stringify(request),
    parseData: parseLoginResponse,
  });
}

export function register(request: RegisterRequest): Promise<RegisterResponse> {
  return apiClient.request("/auth/register", {
    method: "POST",
    auth: false,
    body: JSON.stringify(request),
    parseData: parseRegisterResponse,
  });
}

export function getCurrentUser(signal?: AbortSignal): Promise<LoginUserResponse> {
  return apiClient.request("/auth/me", {
    method: "GET",
    signal,
    parseData: parseLoginUserResponse,
  });
}

export function logout(): Promise<void> {
  return apiClient.request("/auth/logout", {
    method: "POST",
    parseData: () => undefined,
  });
}
