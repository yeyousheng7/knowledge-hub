import {
  CheckCircle2,
  Database,
  Globe2,
  LoaderCircle,
  LockKeyhole,
  RotateCw,
  Send,
  Sparkles,
} from "lucide-react";
import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { Link } from "react-router-dom";

import {
  type AiIndexRebuildResponse,
  type AiRagAskResponse,
  type AiRagSourceResponse,
} from "@/api/ai-contracts";
import { askAiRag, rebuildAiIndex } from "@/api/ai";
import { ApiError } from "@/api/errors";
import { AiMarkdownContent } from "@/shared/markdown/AiMarkdownContent";

interface RagWorkspaceProps {
  onQuestionChange: (value: string) => void;
  question: string;
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function describeAiError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "AI 功能未启用，请确认后端已加载 AI 配置后手动重试。";
    }

    if (error.code === 50302) {
      return "RAG 索引服务暂不可用，请稍后手动重试。";
    }

    if (error.code === 50303) {
      return "AI 对话服务暂不可用，请稍后手动重试。";
    }

    return error.message;
  }

  return error instanceof Error ? error.message : "请求失败，请稍后手动重试。";
}

function sourcePath(source: AiRagSourceResponse): string {
  return source.visibility === "PUBLIC"
    ? `/public/notes/${source.noteId}`
    : `/notes/${source.noteId}`;
}

function SourceCard({ source }: { source: AiRagSourceResponse }) {
  const VisibilityIcon = source.visibility === "PUBLIC" ? Globe2 : LockKeyhole;

  return (
    <Link
      className="group flex min-w-0 flex-col rounded-xl border border-slate-200 bg-white p-4 text-left transition hover:border-blue-200 hover:shadow-sm"
      to={sourcePath(source)}
    >
      <div className="flex items-start gap-3">
        <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-blue-50 text-blue-600">
          <Database aria-hidden="true" className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
          <h4 className="truncate text-sm font-semibold text-slate-900 group-hover:text-blue-700">
            {source.title}
          </h4>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-400">
            <span className="inline-flex items-center gap-1">
              <VisibilityIcon aria-hidden="true" className="size-3" />
              {source.visibility === "PUBLIC" ? "公开" : "私有"}
            </span>
            <span>更新于 {formatDateTime(source.updatedAt)}</span>
            <span title="向量距离，数值越小通常表示检索结果越接近">
              距离 {source.distance.toFixed(4)}
            </span>
          </div>
        </div>
      </div>
      <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-600">
        {source.snippet}
      </p>
    </Link>
  );
}

export function RagWorkspace({ question, onQuestionChange }: RagWorkspaceProps) {
  const [answer, setAnswer] = useState<AiRagAskResponse | null>(null);
  const [answeredQuestion, setAnsweredQuestion] = useState("");
  const [askError, setAskError] = useState<string | null>(null);
  const [isAsking, setIsAsking] = useState(false);
  const [rebuildResult, setRebuildResult] =
    useState<AiIndexRebuildResponse | null>(null);
  const [rebuildError, setRebuildError] = useState<string | null>(null);
  const [isRebuilding, setIsRebuilding] = useState(false);
  const askController = useRef<AbortController | null>(null);
  const rebuildController = useRef<AbortController | null>(null);
  const canAsk = question.trim().length > 0 && question.length <= 1000;

  useEffect(
    () => () => {
      askController.current?.abort();
      rebuildController.current?.abort();
    },
    [],
  );

  async function handleRebuild() {
    if (isRebuilding) {
      return;
    }

    const controller = new AbortController();
    rebuildController.current = controller;
    setIsRebuilding(true);
    setRebuildError(null);
    setRebuildResult(null);

    try {
      setRebuildResult(await rebuildAiIndex(controller.signal));
    } catch (error) {
      if (!controller.signal.aborted) {
        setRebuildError(describeAiError(error));
      }
    } finally {
      if (!controller.signal.aborted) {
        setIsRebuilding(false);
      }
    }
  }

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canAsk || isAsking) {
      if (!question.trim()) {
        setAskError("请输入问题后再发送。");
      }
      return;
    }

    const controller = new AbortController();
    askController.current = controller;
    setIsAsking(true);
    setAskError(null);
    setAnsweredQuestion(question);
    setAnswer(null);

    try {
      setAnswer(await askAiRag({ question }, controller.signal));
    } catch (error) {
      if (!controller.signal.aborted) {
        setAskError(describeAiError(error));
      }
    } finally {
      if (!controller.signal.aborted) {
        setIsAsking(false);
      }
    }
  }

  const hasConversation = answer !== null || askError !== null || isAsking;

  return (
    <section className="mx-auto mt-10 w-full max-w-4xl">
      {!hasConversation ? (
        <div className="text-center">
          <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-blue-50 text-blue-600">
            <Database aria-hidden="true" className="size-6" />
          </span>
          <h2 className="mt-5 text-lg font-semibold text-slate-900">
            从你的知识库开始提问
          </h2>
          <p className="mt-2 text-sm text-slate-500">
            重建索引后，AI 将基于你的真实笔记提供回答和来源。
          </p>
        </div>
      ) : (
        <div aria-live="polite" className="space-y-5">
          {answeredQuestion ? (
            <div className="ml-auto max-w-[78%] rounded-2xl rounded-tr-md bg-blue-50 px-5 py-4 text-sm leading-6 text-slate-800">
              {answeredQuestion}
            </div>
          ) : null}

          {isAsking ? (
            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-5 text-sm text-slate-500">
              <LoaderCircle aria-hidden="true" className="size-5 animate-spin text-blue-500" />
              正在基于知识库生成完整回答…
            </div>
          ) : null}

          {askError ? (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {askError} 当前问题已保留，可直接重新发送。
            </div>
          ) : null}

          {answer ? (
            <div className="flex items-start gap-3">
              <span className="mt-1 grid size-10 shrink-0 place-items-center rounded-full bg-blue-600 text-white shadow-sm shadow-blue-200">
                <Sparkles aria-hidden="true" className="size-5" />
              </span>
              <div className="min-w-0 flex-1 rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm shadow-slate-100">
                <AiMarkdownContent content={answer.answer} />
              </div>
            </div>
          ) : null}

          {answer && answer.sources.length > 0 ? (
            <div>
              <h3 className="mb-3 text-sm font-semibold text-slate-700">
                参考来源（{answer.sources.length}）
              </h3>
              <div className="grid gap-3 md:grid-cols-2">
                {answer.sources.map((source) => (
                  <SourceCard
                    key={`${source.noteId}-${source.chunkIndex}`}
                    source={source}
                  />
                ))}
              </div>
            </div>
          ) : null}
        </div>
      )}

      <div className="mt-7 text-center">
        <button
          className="inline-flex h-11 items-center gap-2 rounded-xl border border-blue-200 px-5 text-sm font-medium text-blue-600 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60"
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
        <div aria-live="polite" className="mt-2 min-h-5 text-xs">
          {rebuildResult ? (
            <span className="inline-flex items-center gap-1.5 text-emerald-700">
              <CheckCircle2 aria-hidden="true" className="size-3.5" />
              已索引 {rebuildResult.chunkCount} 个内容块，完成于 {formatDateTime(rebuildResult.indexedAt)}
            </span>
          ) : null}
          {rebuildError ? <span className="text-red-600">{rebuildError}</span> : null}
        </div>
      </div>

      <form className="mt-5 text-left" onSubmit={(event) => void handleAsk(event)}>
        <label className="sr-only" htmlFor="ai-rag-input">
          RAG 问题
        </label>
        <div className="relative rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-100 focus-within:border-blue-300 focus-within:ring-4 focus-within:ring-blue-50">
          <textarea
            className="min-h-36 w-full resize-none bg-transparent px-5 pb-16 pt-5 text-sm leading-6 text-slate-800 outline-none placeholder:text-slate-400"
            disabled={isAsking}
            id="ai-rag-input"
            maxLength={1000}
            onChange={(event) => onQuestionChange(event.target.value)}
            placeholder="输入你的第一个问题，开启知识探索之旅…"
            value={question}
          />
          <span className="absolute bottom-5 left-5 text-xs text-slate-400">
            {question.length}/1000
          </span>
          <button
            aria-label="发送 RAG 问题"
            className="absolute bottom-4 right-4 grid size-10 place-items-center rounded-full bg-blue-600 text-white shadow-sm shadow-blue-200 transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:text-blue-400 disabled:shadow-none"
            disabled={!canAsk || isAsking}
            type="submit"
          >
            {isAsking ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : (
              <Send aria-hidden="true" className="size-4" />
            )}
          </button>
        </div>
      </form>
    </section>
  );
}
