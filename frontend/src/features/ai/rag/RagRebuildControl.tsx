import {
  CheckCircle2,
  LoaderCircle,
  RotateCw,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { type AiIndexRebuildResponse } from "@/api/ai-contracts";
import { rebuildAiIndex } from "@/api/ai";
import { ApiError } from "@/api/errors";

const LAST_REBUILD_STORAGE_KEY = "knowledgehub.ai.rag.lastRebuild.v1";
const SUCCESS_MESSAGE_VISIBLE_MS = 4_000;

interface LastRebuildSnapshot {
  chunkCount: number;
  indexedAt: string;
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function readLastRebuildSnapshot(): LastRebuildSnapshot | null {
  if (typeof window === "undefined") {
    return null;
  }

  const rawValue = window.localStorage.getItem(LAST_REBUILD_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    const parsedValue = JSON.parse(rawValue) as Partial<LastRebuildSnapshot>;

    if (
      typeof parsedValue.indexedAt !== "string" ||
      typeof parsedValue.chunkCount !== "number"
    ) {
      return null;
    }

    return {
      chunkCount: parsedValue.chunkCount,
      indexedAt: parsedValue.indexedAt,
    };
  } catch {
    return null;
  }
}

function writeLastRebuildSnapshot(snapshot: LastRebuildSnapshot) {
  window.localStorage.setItem(
    LAST_REBUILD_STORAGE_KEY,
    JSON.stringify(snapshot),
  );
}

function describeRebuildError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "AI 功能未启用，请确认后端已加载 AI 配置。";
    }

    if (error.code === 50302) {
      return "RAG 索引服务暂不可用，请稍后手动重试。";
    }

    return error.message;
  }

  return "重建失败，请稍后手动重试。";
}

export function RagRebuildControl() {
  const [result, setResult] = useState<AiIndexRebuildResponse | null>(null);
  const [lastRebuild, setLastRebuild] = useState<LastRebuildSnapshot | null>(
    () => readLastRebuildSnapshot(),
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isRebuilding, setIsRebuilding] = useState(false);
  const controllerRef = useRef<AbortController | null>(null);
  const successTimerRef = useRef<ReturnType<typeof window.setTimeout> | null>(
    null,
  );

  useEffect(() => () => {
    controllerRef.current?.abort();

    if (successTimerRef.current) {
      window.clearTimeout(successTimerRef.current);
    }
  }, []);

  async function handleRebuild() {
    if (isRebuilding) {
      return;
    }

    const controller = new AbortController();
    controllerRef.current = controller;
    if (successTimerRef.current) {
      window.clearTimeout(successTimerRef.current);
      successTimerRef.current = null;
    }
    setIsRebuilding(true);
    setResult(null);
    setErrorMessage(null);

    try {
      const rebuildResult = await rebuildAiIndex(controller.signal);
      const snapshot = {
        chunkCount: rebuildResult.chunkCount,
        indexedAt: rebuildResult.indexedAt,
      };

      setResult(rebuildResult);
      setLastRebuild(snapshot);
      writeLastRebuildSnapshot(snapshot);
      successTimerRef.current = window.setTimeout(() => {
        setResult(null);
        successTimerRef.current = null;
      }, SUCCESS_MESSAGE_VISIBLE_MS);
    } catch (error) {
      if (!controller.signal.aborted) {
        setErrorMessage(describeRebuildError(error));
      }
    } finally {
      if (!controller.signal.aborted) {
        setIsRebuilding(false);
      }
    }
  }

  return (
    <div className="flex min-w-0 items-center justify-end gap-3">
      <div
        aria-live="polite"
        className="min-w-0 max-w-md truncate text-right text-xs"
      >
        {result ? (
          <span
            className="inline-flex max-w-full items-center gap-1.5 text-emerald-700"
            title={`已索引 ${result.chunkCount} 个内容块，完成于 ${formatDateTime(result.indexedAt)}`}
          >
            <CheckCircle2 aria-hidden="true" className="size-3.5 shrink-0" />
            <span className="truncate">
              已索引 {result.chunkCount} 个内容块 · {formatDateTime(result.indexedAt)}
            </span>
          </span>
        ) : null}
        {!result && lastRebuild ? (
          <span
            className="text-slate-400"
            title={`上次重建索引 ${lastRebuild.chunkCount} 个内容块`}
          >
            上次重建：{formatDateTime(lastRebuild.indexedAt)}
          </span>
        ) : null}
        {errorMessage ? (
          <span className="text-red-600" title={errorMessage}>
            {errorMessage}
          </span>
        ) : null}
      </div>
      <button
        className="inline-flex h-9 shrink-0 items-center gap-2 rounded-lg border border-blue-200 px-4 text-sm font-medium text-blue-600 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isRebuilding}
        onClick={() => void handleRebuild()}
        type="button"
      >
        {isRebuilding ? (
          <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
        ) : (
          <RotateCw aria-hidden="true" className="size-4" />
        )}
        {isRebuilding ? "正在重建…" : "重建 RAG 知识库"}
      </button>
    </div>
  );
}
