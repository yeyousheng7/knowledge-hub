export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  nickname?: string;
  inviteCode: string;
}

export interface RegisterResponse {
  id: number;
  username: string;
  nickname: string;
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

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function readString(
  record: Record<string, unknown>,
  key: string,
): string {
  const value = record[key];

  if (typeof value !== "string") {
    throw new TypeError(`Expected ${key} to be a string`);
  }

  return value;
}

export function readFiniteNumber(
  record: Record<string, unknown>,
  key: string,
): number {
  const value = record[key];

  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new TypeError(`Expected ${key} to be a finite number`);
  }

  return value;
}

export function readSafeInteger(
  record: Record<string, unknown>,
  key: string,
): number {
  const value = readFiniteNumber(record, key);

  if (!Number.isSafeInteger(value)) {
    throw new TypeError(`Expected ${key} to be a safe integer`);
  }

  return value;
}

export function readNullableString(
  record: Record<string, unknown>,
  key: string,
): string | null {
  return record[key] === null ? null : readString(record, key);
}

export function readNullableSafeInteger(
  record: Record<string, unknown>,
  key: string,
): number | null {
  return record[key] === null ? null : readSafeInteger(record, key);
}

export function readArray(
  record: Record<string, unknown>,
  key: string,
): unknown[] {
  const value = record[key];

  if (!Array.isArray(value)) {
    throw new TypeError(`Expected ${key} to be an array`);
  }

  return value;
}

export function parseApiResponse(value: unknown): ApiResponse<unknown> {
  if (!isRecord(value) || !("data" in value)) {
    throw new TypeError("Expected an API response envelope");
  }

  return {
    code: readSafeInteger(value, "code"),
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
    id: readSafeInteger(value, "id"),
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
  const expiresIn = readSafeInteger(value, "expiresIn");

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

export function parseRegisterResponse(value: unknown): RegisterResponse {
  if (!isRecord(value)) {
    throw new TypeError("Expected register response data");
  }

  return {
    id: readSafeInteger(value, "id"),
    username: readString(value, "username"),
    nickname: readString(value, "nickname"),
  };
}
