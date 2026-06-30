import { LoaderCircle, ShieldAlert } from "lucide-react";

interface AuthStatusPanelProps {
  mode: "loading" | "error";
  message?: string | null;
  onRetry?: () => void;
  onDiscard?: () => void;
}

export function AuthStatusPanel({
  mode,
  message,
  onRetry,
  onDiscard,
}: AuthStatusPanelProps) {
  return (
    <main className="grid min-h-svh place-items-center bg-background px-6 text-foreground">
      <section className="w-full max-w-md rounded-2xl border bg-white p-8 text-center shadow-sm">
        {mode === "loading" ? (
          <LoaderCircle
            aria-hidden="true"
            className="mx-auto size-7 animate-spin text-primary"
          />
        ) : (
          <ShieldAlert
            aria-hidden="true"
            className="mx-auto size-7 text-red-600"
          />
        )}
        <h1 className="mt-4 text-lg font-semibold">
          {mode === "loading" ? "正在恢复登录状态" : "无法恢复登录状态"}
        </h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          {mode === "loading" ? "正在验证当前会话，请稍候。" : message}
        </p>
        {mode === "error" ? (
          <div className="mt-6 flex justify-center gap-3">
            <button
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
              onClick={onRetry}
              type="button"
            >
              重试
            </button>
            <button
              className="rounded-lg border px-4 py-2 text-sm font-medium"
              onClick={onDiscard}
              type="button"
            >
              重新登录
            </button>
          </div>
        ) : null}
      </section>
    </main>
  );
}
