export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export type UserRole = "USER" | "ADMIN";

export interface LoginUserResponse {
  id: number;
  username: string;
  nickname: string;
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: LoginUserResponse;
}

export type DataParser<T> = (data: unknown) => T;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readString(record: Record<string, unknown>, key: string): string {
  const value = record[key];

  if (typeof value !== "string") {
    throw new TypeError(`Expected ${key} to be a string`);
  }

  return value;
}

function readFiniteNumber(record: Record<string, unknown>, key: string): number {
  const value = record[key];

  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new TypeError(`Expected ${key} to be a finite number`);
  }

  return value;
}

export function parseApiResponse(value: unknown): ApiResponse<unknown> {
  if (!isRecord(value) || !("data" in value)) {
    throw new TypeError("Expected an API response envelope");
  }

  return {
    code: readFiniteNumber(value, "code"),
    msg: readString(value, "msg"),
    data: value.data,
  };
}

export function parseLoginUserResponse(value: unknown): LoginUserResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected login user data");
  }

  const role = readString(value, "role");

  if (role !== "USER" && role !== "ADMIN") {
    throw new TypeError("Expected a supported user role");
  }

  return {
    id: readFiniteNumber(value, "id"),
    username: readString(value, "username"),
    nickname: readString(value, "nickname"),
    role,
  };
}

export function parseLoginResponse(value: unknown): LoginResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected login response data");
  }

  const accessToken = readString(value, "accessToken");
  const expiresIn = readFiniteNumber(value, "expiresIn");

  if (accessToken.trim() === "" || expiresIn <= 0) {
    throw new TypeError("Expected a usable access token");
  }

  return {
    accessToken,
    tokenType: readString(value, "tokenType"),
    expiresIn,
    user: parseLoginUserResponse(value.user),
  };
}
