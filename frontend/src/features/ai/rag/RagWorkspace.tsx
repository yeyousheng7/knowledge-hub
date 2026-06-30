import {
  Database,
  ChevronDown,
  Globe2,
  LoaderCircle,
  LockKeyhole,
  Send,
  Sparkles,
} from "lucide-react";
import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type KeyboardEvent,
} from "react";
import { Link } from "react-router-dom";

import { type AiRagSourceResponse } from "@/api/ai-contracts";
import { askAiRag } from "@/api/ai";
import { ApiError } from "@/api/errors";
import {
  trimRagTurns,
  type RagTranscriptTurn,
} from "@/features/ai/ai-session-storage";
import { cn } from "@/shared/lib/utils";
import { AiMarkdownContent } from "@/shared/markdown/AiMarkdownContent";

interface RagWorkspaceProps {
  initialTurns?: RagTranscriptTurn[];
  onConversationStart?: () => void;
  onQuestionChange: (value: string) => void;
  onTurnsChange?: (turns: RagTranscriptTurn[]) => void;
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

export function RagWorkspace({
  initialTurns = [],
  onConversationStart,
  onQuestionChange,
  onTurnsChange,
  question,
}: RagWorkspaceProps) {
  const [turns, setTurns] = useState<RagTranscriptTurn[]>(() =>
    trimRagTurns(initialTurns),
  );
  const [pendingQuestion, setPendingQuestion] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [isAsking, setIsAsking] = useState(false);
  const [expandedSourceTurnIds, setExpandedSourceTurnIds] = useState<Set<string>>(
    () => new Set(),
  );
  const askController = useRef<AbortController | null>(null);
  const idCounter = useRef(initialTurns.length);
  const canAsk = question.trim().length > 0 && question.length <= 1000;

  useEffect(() => () => askController.current?.abort(), []);

  useEffect(() => {
    onTurnsChange?.(turns);
  }, [onTurnsChange, turns]);

  async function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canAsk || isAsking) {
      if (!question.trim()) {
        setFormError("请输入问题后再发送。");
      }
      return;
    }

    const sentQuestion = question;
    const controller = new AbortController();
    askController.current = controller;
    setIsAsking(true);
    setFormError(null);
    setPendingQuestion(sentQuestion);
    onQuestionChange("");
    onConversationStart?.();

    try {
      const answer = await askAiRag({ question: sentQuestion }, controller.signal);
      if (!controller.signal.aborted) {
        setTurns((current) =>
          trimRagTurns([
            ...current,
            {
              id: nextTurnId(),
              question: sentQuestion,
              answer,
              error: null,
            },
          ]),
        );
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setTurns((current) =>
          trimRagTurns([
            ...current,
            {
              id: nextTurnId(),
              question: sentQuestion,
              answer: null,
              error: describeAiError(error),
            },
          ]),
        );
      }
    } finally {
      if (!controller.signal.aborted) {
        setPendingQuestion("");
        setIsAsking(false);
      }
    }
  }

  function nextTurnId(): string {
    idCounter.current += 1;
    return `rag-turn-${idCounter.current}`;
  }

  function handleQuestionKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (
      event.key !== "Enter" ||
      event.shiftKey ||
      event.nativeEvent.isComposing
    ) {
      return;
    }

    event.preventDefault();
    event.currentTarget.form?.requestSubmit();
  }

  function toggleSources(turnId: string) {
    setExpandedSourceTurnIds((current) => {
      const next = new Set(current);

      if (next.has(turnId)) {
        next.delete(turnId);
      } else {
        next.add(turnId);
      }

      return next;
    });
  }

  const hasConversation =
    turns.length > 0 || pendingQuestion || isAsking || formError !== null;

  return (
    <section className="mx-auto mt-12 w-full max-w-4xl">
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
          {formError ? (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {formError}
            </div>
          ) : null}

          {turns.map((turn) => (
            <div className="space-y-5" key={turn.id}>
              {turn.question ? (
                <div className="ml-auto max-w-[78%] rounded-2xl rounded-tr-md bg-blue-50 px-5 py-4 text-sm leading-6 text-slate-800">
                  {turn.question}
                </div>
              ) : null}

              {turn.error ? (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {turn.error} 请重新输入后重试。
                </div>
              ) : null}

              {turn.answer ? (
                <div className="space-y-3">
                  <div className="flex items-start gap-3">
                  <span className="mt-1 grid size-10 shrink-0 place-items-center rounded-full bg-blue-600 text-white shadow-sm shadow-blue-200">
                    <Sparkles aria-hidden="true" className="size-5" />
                  </span>
                  <div className="min-w-0 flex-1 rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm shadow-slate-100">
                    <AiMarkdownContent content={turn.answer.answer} />
                    {turn.answer.sources.length > 0 ? (
                      <div className="mt-4 flex border-t border-slate-100 pt-3">
                        <button
                          aria-expanded={expandedSourceTurnIds.has(turn.id)}
                          className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-medium text-slate-500 transition hover:bg-slate-50 hover:text-blue-700"
                          onClick={() => toggleSources(turn.id)}
                          type="button"
                        >
                          来源（{turn.answer.sources.length}）
                          <ChevronDown
                            aria-hidden="true"
                            className={cn(
                              "size-3.5 transition-transform",
                              expandedSourceTurnIds.has(turn.id) && "rotate-180",
                            )}
                          />
                        </button>
                      </div>
                    ) : null}
                  </div>
                </div>

                  {turn.answer.sources.length > 0 &&
                  expandedSourceTurnIds.has(turn.id) ? (
                    <div className="ml-[3.25rem] grid gap-3 md:grid-cols-2">
                      {turn.answer.sources.map((source) => (
                        <SourceCard
                          key={`${turn.id}-${source.noteId}-${source.chunkIndex}`}
                          source={source}
                        />
                      ))}
                    </div>
                  ) : null}
                  </div>
              ) : null}
            </div>
          ))}

          {pendingQuestion ? (
            <div className="ml-auto max-w-[78%] rounded-2xl rounded-tr-md bg-blue-50 px-5 py-4 text-sm leading-6 text-slate-800">
              {pendingQuestion}
            </div>
          ) : null}

          {isAsking ? (
            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-5 text-sm text-slate-500">
              <LoaderCircle aria-hidden="true" className="size-5 animate-spin text-blue-500" />
              正在基于知识库生成完整回答…
            </div>
          ) : null}
        </div>
      )}

      <form className="mt-8 text-left" onSubmit={(event) => void handleAsk(event)}>
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
            onKeyDown={handleQuestionKeyDown}
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
