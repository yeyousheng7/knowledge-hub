import {
  parseApiResponse,
  type DataParser,
} from "@/api/contracts";
import { ApiError, invalidatesAuthentication } from "@/api/errors";

const DEFAULT_API_BASE_URL = "/api/v1";

interface AuthHooks {
  getAccessToken: () => string | null;
  onAuthenticationInvalid: () => void;
}

export interface ApiClientOptions {
  baseUrl?: string;
  authHooks?: Partial<AuthHooks>;
}

export interface ApiRequestOptions<T> extends Omit<RequestInit, "headers"> {
  auth?: boolean;
  headers?: HeadersInit;
  parseData: DataParser<T>;
}

const anonymousAuthHooks: AuthHooks = {
  getAccessToken: () => null,
  onAuthenticationInvalid: () => undefined,
};

function normalizeBaseUrl(value: string | undefined): string {
  const normalized = value?.trim() || DEFAULT_API_BASE_URL;
  return normalized.replace(/\/+$/, "");
}

function userFacingStatusMessage(status: number): string {
  if (status === 403) {
    return "没有权限执行此操作";
  }

  if (status === 404) {
    return "请求的资源不存在或不可访问";
  }

  return "请求失败，请稍后重试";
}

export class ApiClient {
  private readonly baseUrl?: string;
  private authHooks: AuthHooks;

  constructor(options: ApiClientOptions = {}) {
    this.baseUrl = options.baseUrl;
    this.authHooks = {
      ...anonymousAuthHooks,
      ...options.authHooks,
    };
  }

  configureAuth(authHooks: AuthHooks): () => void {
    this.authHooks = authHooks;

    return () => {
      if (this.authHooks === authHooks) {
        this.authHooks = anonymousAuthHooks;
      }
    };
  }

  async request<T>(path: string, options: ApiRequestOptions<T>): Promise<T> {
    const { auth = true, headers: inputHeaders, parseData, ...init } = options;
    const headers = new Headers(inputHeaders);
    const token = auth ? this.authHooks.getAccessToken() : null;

    headers.set("Accept", "application/json");

    if (init.body !== undefined && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }

    let response: Response;

    try {
      response = await fetch(
        `${normalizeBaseUrl(this.baseUrl ?? import.meta.env.VITE_API_BASE_URL)}${
          path.startsWith("/") ? path : `/${path}`
        }`,
        { ...init, headers },
      );
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") {
        throw error;
      }

      throw new ApiError("无法连接服务器，请检查网络后重试", { cause: error });
    }

    const rawBody = await response.text();
    let parsedBody: unknown;

    try {
      parsedBody = JSON.parse(rawBody) as unknown;
    } catch (error) {
      if (invalidatesAuthentication(response.status, null)) {
        this.authHooks.onAuthenticationInvalid();
      }

      throw new ApiError("服务器返回了无法识别的响应", {
        status: response.status,
        cause: error,
      });
    }

    let envelope;

    try {
      envelope = parseApiResponse(parsedBody);
    } catch (error) {
      if (invalidatesAuthentication(response.status, null)) {
        this.authHooks.onAuthenticationInvalid();
      }

      throw new ApiError("服务器响应格式无效", {
        status: response.status,
        cause: error,
      });
    }

    if (!response.ok || envelope.code !== 0) {
      if (invalidatesAuthentication(response.status, envelope.code)) {
        this.authHooks.onAuthenticationInvalid();
      }

      throw new ApiError(
        envelope.msg.trim() || userFacingStatusMessage(response.status),
        {
          status: response.status,
          code: envelope.code,
        },
      );
    }

    try {
      return parseData(envelope.data);
    } catch (error) {
      throw new ApiError("服务器响应格式无效", {
        status: response.status,
        code: envelope.code,
        cause: error,
      });
    }
  }
}

export const apiClient = new ApiClient();
