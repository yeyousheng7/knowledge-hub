export class ApiError extends Error {
  readonly status: number | null;
  readonly code: number | null;

  constructor(
    message: string,
    options: {
      status?: number | null;
      code?: number | null;
      cause?: unknown;
    } = {},
  ) {
    super(message, { cause: options.cause });
    this.name = "ApiError";
    this.status = options.status ?? null;
    this.code = options.code ?? null;
  }
}

export function invalidatesAuthentication(
  status: number | null,
  code: number | null,
): boolean {
  if (code === 40102) {
    return false;
  }

  return (
    status === 401 || code === 40100 || code === 40101 || code === 40301
  );
}

export function isAuthenticationInvalidError(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    invalidatesAuthentication(error.status, error.code)
  );
}
