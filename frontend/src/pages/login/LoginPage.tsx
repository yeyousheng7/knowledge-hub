import { ArrowRight, LoaderCircle, UserRound } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/errors";
import { AuthFormField, PasswordField } from "@/features/auth/AuthFormField";
import { AuthPageLayout } from "@/features/auth/AuthPageLayout";
import { AuthStatusPanel } from "@/features/auth/AuthStatusPanel";
import { useAuth } from "@/features/auth/auth-context";

const USERNAME_PATTERN = /^[0-9A-Za-z_]{3,30}$/;

interface LoginLocationState {
  from?: unknown;
  registeredUsername?: unknown;
}

function redirectTarget(state: unknown): string {
  const from = (state as LoginLocationState | null)?.from;

  if (
    typeof from !== "string" ||
    !from.startsWith("/") ||
    from.startsWith("//") ||
    from.startsWith("/login") ||
    from.startsWith("/register")
  ) {
    return "/notes";
  }

  return from;
}

function registeredUsername(state: unknown): string {
  const value = (state as LoginLocationState | null)?.registeredUsername;
  return typeof value === "string" ? value : "";
}

function loginErrorMessage(error: unknown): string {
  if (error instanceof ApiError || error instanceof Error) {
    return error.message;
  }

  return "登录失败，请稍后重试";
}

export function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const destination = redirectTarget(location.state);
  const initialUsername = registeredUsername(location.state);
  const [username, setUsername] = useState(initialUsername);
  const [password, setPassword] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (auth.status === "loading") {
    return <AuthStatusPanel mode="loading" />;
  }

  if (auth.status === "error") {
    return (
      <AuthStatusPanel
        mode="error"
        message={auth.restoreError}
        onDiscard={auth.discardSession}
        onRetry={auth.retrySession}
      />
    );
  }

  if (auth.status === "authenticated") {
    return <Navigate replace to={destination} />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedUsername = username.trim();

    if (!USERNAME_PATTERN.test(normalizedUsername)) {
      setFormError("用户名需为 3–30 位字母、数字或下划线");
      return;
    }

    if (password.length < 8 || password.length > 72) {
      setFormError("密码长度需为 8–72 个字符");
      return;
    }

    setFormError(null);
    setIsSubmitting(true);

    try {
      await auth.login({ username: normalizedUsername, password });
      navigate(destination, { replace: true });
    } catch (error) {
      setFormError(loginErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthPageLayout>
      <h2 className="sr-only">登录知识库</h2>

      {initialUsername ? (
        <p
          className="mt-5 rounded-lg border border-emerald-100 bg-emerald-50 px-3 py-2 text-center text-sm text-emerald-700"
          role="status"
        >
          注册成功，请登录新账号
        </p>
      ) : null}

      <form className="mt-6 space-y-3" noValidate onSubmit={handleSubmit}>
        <AuthFormField
          autoComplete="username"
          icon={UserRound}
          label="用户名"
          maxLength={30}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="用户名"
          required
          value={username}
        />
        <PasswordField
          autoComplete="current-password"
          label="密码"
          minLength={8}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="密码"
          required
          value={password}
        />

        {formError ? (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {formError}
          </p>
        ) : null}

        <button
          className="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-gradient-to-r from-blue-600 to-indigo-600 px-4 text-sm font-medium text-white shadow-sm shadow-blue-200 transition hover:from-blue-700 hover:to-indigo-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-200 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? (
            <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
          ) : null}
          {isSubmitting ? "正在登录" : "登录"}
          {isSubmitting ? null : <ArrowRight aria-hidden="true" className="size-4" />}
        </button>
      </form>

      <div className="my-4 flex items-center gap-3 text-xs text-slate-400">
        <span className="h-px flex-1 bg-slate-200" />
        或
        <span className="h-px flex-1 bg-slate-200" />
      </div>

      <Link
        className="flex h-11 w-full items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white text-sm font-medium text-blue-600 transition hover:border-blue-300 hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
        state={{ from: destination }}
        to="/register"
      >
        注册账号
        <ArrowRight aria-hidden="true" className="size-4" />
      </Link>
    </AuthPageLayout>
  );
}
