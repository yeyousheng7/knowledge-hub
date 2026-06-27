import { useState, type FormEvent } from "react";
import { BookOpenText, LoaderCircle } from "lucide-react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/errors";
import { AuthStatusPanel } from "@/features/auth/AuthStatusPanel";
import { useAuth } from "@/features/auth/auth-context";

const USERNAME_PATTERN = /^[0-9A-Za-z_]{3,30}$/;

interface LoginLocationState {
  from?: unknown;
}

function redirectTarget(state: unknown): string {
  const from = (state as LoginLocationState | null)?.from;

  if (
    typeof from !== "string" ||
    !from.startsWith("/") ||
    from.startsWith("//") ||
    from.startsWith("/login")
  ) {
    return "/notes";
  }

  return from;
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
  const [username, setUsername] = useState("");
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
    <main className="grid min-h-svh place-items-center bg-background px-6 py-10 text-foreground">
      <section className="w-full max-w-sm rounded-2xl border bg-white p-8 shadow-sm">
        <div className="grid size-11 place-items-center rounded-xl bg-primary text-primary-foreground">
          <BookOpenText aria-hidden="true" className="size-5" strokeWidth={1.8} />
        </div>
        <p className="mt-6 text-sm font-medium text-primary">KnowledgeHub</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">登录知识库</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          使用现有账号进入个人 Markdown 工作区。
        </p>

        <form className="mt-8 space-y-5" noValidate onSubmit={handleSubmit}>
          <div>
            <label className="text-sm font-medium" htmlFor="username">
              用户名
            </label>
            <input
              autoComplete="username"
              className="mt-2 w-full rounded-lg border bg-white px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
              id="username"
              onChange={(event) => setUsername(event.target.value)}
              required
              value={username}
            />
          </div>
          <div>
            <label className="text-sm font-medium" htmlFor="password">
              密码
            </label>
            <input
              autoComplete="current-password"
              className="mt-2 w-full rounded-lg border bg-white px-3 py-2.5 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
              id="password"
              minLength={8}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </div>

          {formError ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {formError}
            </p>
          ) : null}

          <button
            className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : null}
            {isSubmitting ? "正在登录" : "登录"}
          </button>
        </form>
      </section>
    </main>
  );
}
