import { LoaderCircle, RotateCcw } from "lucide-react";
import { useState } from "react";

import { clearAiAgentSession } from "@/api/ai";
import { ApiError } from "@/api/errors";

interface AgentSessionClearControlProps {
  onCleared: () => void;
}

function describeClearError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Agent 功能未启用，请确认后端已加载 AI 配置。";
    }

    if (error.code === 50303) {
      return "AI 对话服务暂不可用，请稍后手动重试。";
    }

    return error.message;
  }

  return error instanceof Error ? error.message : "清除上下文失败，请稍后重试。";
}

export function AgentSessionClearControl({
  onCleared,
}: AgentSessionClearControlProps) {
  const [isClearing, setIsClearing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleClear() {
    if (isClearing) {
      return;
    }

    setIsClearing(true);
    setMessage(null);
    setError(null);

    try {
      await clearAiAgentSession();
      onCleared();
      setMessage("上下文已清除");
    } catch (caughtError) {
      setError(describeClearError(caughtError));
    } finally {
      setIsClearing(false);
    }
  }

  return (
    <div className="flex min-w-0 items-center gap-3">
      {message ? <span className="text-xs text-emerald-600">{message}</span> : null}
      {error ? (
        <span className="max-w-72 truncate text-xs text-red-600" title={error}>
          {error}
        </span>
      ) : null}
      <button
        className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-medium text-slate-600 shadow-sm shadow-slate-100 transition hover:border-blue-200 hover:text-blue-600 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isClearing}
        onClick={() => void handleClear()}
        type="button"
      >
        {isClearing ? (
          <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
        ) : (
          <RotateCcw aria-hidden="true" className="size-4" />
        )}
        清除当前上下文
      </button>
    </div>
  );
}
