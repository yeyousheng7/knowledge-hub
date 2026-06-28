import { ArrowRight, KeyRound, LoaderCircle, UserRound } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/errors";
import { AuthFormField, PasswordField } from "@/features/auth/AuthFormField";
import { AuthPageLayout } from "@/features/auth/AuthPageLayout";
import { AuthStatusPanel } from "@/features/auth/AuthStatusPanel";
import { register } from "@/features/auth/auth-api";
import { useAuth } from "@/features/auth/auth-context";

const USERNAME_PATTERN = /^[0-9A-Za-z_]{3,30}$/;

interface RegisterLocationState {
  from?: unknown;
}

function registerErrorMessage(error: unknown): string {
  if (error instanceof ApiError || error instanceof Error) {
    return error.message;
  }

  return "注册失败，请稍后重试";
}

export function RegisterPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const from = (location.state as RegisterLocationState | null)?.from;
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [inviteCode, setInviteCode] = useState("");
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
    return <Navigate replace to="/notes" />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedUsername = username.trim();
    const normalizedNickname = nickname.trim();
    const normalizedInviteCode = inviteCode.trim();

    if (!USERNAME_PATTERN.test(normalizedUsername)) {
      setFormError("用户名需为 3–30 位字母、数字或下划线");
      return;
    }

    if (password.length < 8 || password.length > 72) {
      setFormError("密码长度需为 8–72 个字符");
      return;
    }

    if (password !== confirmPassword) {
      setFormError("两次输入的密码不一致");
      return;
    }

    if (
      normalizedNickname &&
      (normalizedNickname.length < 3 || normalizedNickname.length > 30)
    ) {
      setFormError("昵称需为 3–30 个字符，或留空使用用户名");
      return;
    }

    if (!normalizedInviteCode) {
      setFormError("请输入注册邀请码");
      return;
    }

    setFormError(null);
    setIsSubmitting(true);

    try {
      const response = await register({
        username: normalizedUsername,
        password,
        ...(normalizedNickname ? { nickname: normalizedNickname } : {}),
        inviteCode: normalizedInviteCode,
      });
      navigate("/login", {
        replace: true,
        state: {
          ...(typeof from === "string" ? { from } : {}),
          registeredUsername: response.username,
        },
      });
    } catch (error) {
      setFormError(registerErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthPageLayout>
      <h2 className="mt-5 text-center text-sm font-medium text-slate-700">
        创建你的账号
      </h2>

      <form className="mt-4 space-y-3" noValidate onSubmit={handleSubmit}>
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
          autoComplete="new-password"
          label="密码"
          maxLength={72}
          minLength={8}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="密码（8–72 个字符）"
          required
          value={password}
        />
        <PasswordField
          autoComplete="new-password"
          label="确认密码"
          maxLength={72}
          minLength={8}
          onChange={(event) => setConfirmPassword(event.target.value)}
          placeholder="确认密码"
          required
          value={confirmPassword}
        />
        <AuthFormField
          autoComplete="nickname"
          icon={UserRound}
          label="昵称"
          maxLength={30}
          onChange={(event) => setNickname(event.target.value)}
          placeholder="昵称（可选）"
          value={nickname}
        />
        <AuthFormField
          autoComplete="off"
          icon={KeyRound}
          label="邀请码"
          onChange={(event) => setInviteCode(event.target.value)}
          placeholder="注册邀请码"
          required
          value={inviteCode}
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
          {isSubmitting ? "正在注册" : "注册"}
          {isSubmitting ? null : <ArrowRight aria-hidden="true" className="size-4" />}
        </button>
      </form>

      <div className="my-4 flex items-center gap-3 text-xs text-slate-400">
        <span className="h-px flex-1 bg-slate-200" />
        或
        <span className="h-px flex-1 bg-slate-200" />
      </div>

      <Link
        className="flex h-11 w-full items-center justify-center rounded-lg border border-blue-200 bg-white text-sm font-medium text-blue-600 transition hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
        state={typeof from === "string" ? { from } : undefined}
        to="/login"
      >
        已有账号？去登录
      </Link>
    </AuthPageLayout>
  );
}
