import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  FileText,
  LoaderCircle,
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

import { chatAiAgent, confirmAiAgentOperation } from "@/api/ai";
import {
  parsePendingOperationAction,
  type AiAgentOperationConfirmResponse,
  type PendingOperationAction,
  type PendingOperationPayload,
} from "@/api/ai-contracts";
import { ApiError } from "@/api/errors";
import {
  trimAgentMessages,
  type AgentOperationResolution,
  type AgentTranscriptAction,
  type AgentTranscriptMessage,
} from "@/features/ai/ai-session-storage";
import { cn } from "@/shared/lib/utils";
import { AiMarkdownContent } from "@/shared/markdown/AiMarkdownContent";

const AGENT_VISIBLE_MESSAGE_STEP = 20;

interface AgentWorkspaceProps {
  initialMessages?: AgentTranscriptMessage[];
  message: string;
  onBeforeSend?: () => Promise<void>;
  onConversationStart?: () => void;
  onMessageChange: (value: string) => void;
  onMessagesChange?: (messages: AgentTranscriptMessage[]) => void;
  resetVersion: number;
}

type AgentMessage = AgentTranscriptMessage;

type ConfirmStatus = "pending" | "submitting" | "executed" | "invalid" | "ignored";

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function describeAgentError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Agent 功能未启用，或待确认操作已过期、已消费、不可访问。";
    }

    if (error.code === 50303) {
      return "AI 对话服务暂不可用，请稍后手动重试。";
    }

    if (error.code === 50302) {
      return "AI 索引服务暂不可用，请稍后手动重试。";
    }

    return error.message;
  }

  return error instanceof Error ? error.message : "请求失败，请稍后手动重试。";
}

function operationTypeLabel(operationType: PendingOperationPayload["operationType"]) {
  return operationType === "CREATE_PRIVATE_NOTE" ? "创建私有笔记" : "批量下架笔记";
}

function OperationResult({
  result,
}: {
  result: AiAgentOperationConfirmResponse;
}) {
  const createdNote =
    result.operationType === "CREATE_PRIVATE_NOTE" ? result.affectedItems[0] : null;

  return (
    <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
      <div className="flex items-center gap-2 font-medium">
        <CheckCircle2 aria-hidden="true" className="size-4" />
        {result.message || "操作已执行"}
      </div>
      <p className="mt-1 text-xs text-emerald-700">
        状态：{result.status} · 影响 {result.affectedCount} 项
      </p>
      {createdNote ? (
        <Link
          className="mt-3 inline-flex rounded-lg bg-white px-3 py-2 text-xs font-medium text-emerald-700 ring-1 ring-emerald-200 hover:text-emerald-900"
          to={`/notes/${createdNote.id}`}
        >
          打开新笔记：{createdNote.title}
        </Link>
      ) : null}
    </div>
  );
}

function DraftPreview({
  payload,
}: {
  payload: Extract<PendingOperationPayload, { operationType: "CREATE_PRIVATE_NOTE" }>;
}) {
  return (
    <div className="mt-4 space-y-3">
      <div className="grid gap-3 border-t border-slate-100 pt-4 text-sm md:grid-cols-[9rem_1fr]">
        <span className="font-medium text-slate-700">标题</span>
        <span className="text-slate-700">{payload.draft.title}</span>
        <span className="font-medium text-slate-700">摘要</span>
        <span className="text-slate-600">{payload.draft.summary || "无摘要"}</span>
        <span className="font-medium text-slate-700">推荐标签</span>
        <span className="flex flex-wrap gap-2">
          {payload.draft.recommendedTags.length > 0 ? (
            payload.draft.recommendedTags.map((tag) => (
              <span
                className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-600"
                key={tag}
              >
                {tag}
              </span>
            ))
          ) : (
            <span className="text-slate-400">无</span>
          )}
        </span>
      </div>
      <div>
        <p className="mb-2 text-sm font-medium text-slate-700">内容预览</p>
        <pre className="max-h-48 overflow-auto whitespace-pre-wrap rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs leading-6 text-slate-600">
          {payload.draft.contentMd}
        </pre>
      </div>
    </div>
  );
}

function BatchPreview({
  payload,
}: {
  payload: Extract<PendingOperationPayload, { operationType: "BATCH_UNPUBLISH_NOTES" }>;
}) {
  return (
    <div className="mt-4 border-t border-slate-100 pt-4">
      <p className="text-sm font-medium text-slate-700">
        受影响笔记（{payload.affectedItems.length}）
      </p>
      <div className="mt-3 grid gap-2">
        {payload.affectedItems.map((item) => (
          <Link
            className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 hover:border-blue-200 hover:text-blue-700"
            key={item.id}
            to={`/notes/${item.id}`}
          >
            <FileText aria-hidden="true" className="size-4 text-blue-500" />
            {item.title}
          </Link>
        ))}
      </div>
    </div>
  );
}

function PendingOperationCard({
  action,
  operationResolution,
  onOperationResolutionChange,
}: {
  action: PendingOperationAction;
  operationResolution?: AgentOperationResolution;
  onOperationResolutionChange: (resolution: AgentOperationResolution) => void;
}) {
  const { payload } = action;
  const expiresAtMs = Date.parse(payload.expiresAt);
  const [status, setStatus] = useState<ConfirmStatus>(
    () => operationResolution?.status ?? "pending",
  );
  const [result, setResult] =
    useState<AiAgentOperationConfirmResponse | null>(
      () => operationResolution?.result ?? null,
    );
  const [error, setError] = useState<string | null>(
    () => operationResolution?.error ?? null,
  );
  const [hasExpired, setHasExpired] = useState(
    () => Number.isFinite(expiresAtMs) && expiresAtMs <= Date.now(),
  );
  const isExpired = hasExpired && status === "pending";
  const isDisabled =
    isExpired ||
    status === "submitting" ||
    status === "executed" ||
    status === "invalid" ||
    status === "ignored";

  useEffect(() => {
    if (!Number.isFinite(expiresAtMs)) {
      return;
    }

    const remainingMs = expiresAtMs - Date.now();
    const timeoutId = window.setTimeout(
      () => setHasExpired(true),
      Math.min(Math.max(0, remainingMs), 2_147_483_647),
    );

    return () => window.clearTimeout(timeoutId);
  }, [expiresAtMs]);

  async function handleConfirm() {
    if (isDisabled) {
      return;
    }

    setStatus("submitting");
    setError(null);

    try {
      const confirmed = await confirmAiAgentOperation(payload.operationId);
      setResult(confirmed);
      setStatus("executed");
      onOperationResolutionChange({
        status: "executed",
        result: confirmed,
        error: null,
      });
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 404) {
        const invalidError =
          "操作已过期、已消费或不存在，请重新发起 Agent 请求。";
        setStatus("invalid");
        setError(invalidError);
        onOperationResolutionChange({
          status: "invalid",
          result: null,
          error: invalidError,
        });
        return;
      }

      setStatus("pending");
      setError(describeAgentError(caughtError));
    }
  }

  return (
    <article className="rounded-2xl border border-blue-200 bg-white px-6 py-5 shadow-sm shadow-blue-50">
      <div className="flex items-start gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-600">
          <FileText aria-hidden="true" className="size-5" />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-slate-900">
                待确认操作
              </h3>
              <p className="mt-1 text-sm text-slate-500">
                操作类型：{operationTypeLabel(payload.operationType)}
              </p>
            </div>
            <span
              className={cn(
                "rounded-full px-3 py-1 text-xs font-medium",
                status === "executed" && "bg-emerald-50 text-emerald-700",
                status === "invalid" && "bg-red-50 text-red-700",
                status === "ignored" && "bg-slate-100 text-slate-500",
                status !== "executed" &&
                  status !== "invalid" &&
                  status !== "ignored" &&
                  (isExpired ? "bg-red-50 text-red-700" : "bg-blue-50 text-blue-700"),
              )}
            >
              {status === "executed"
                ? "已执行"
                : status === "invalid"
                  ? "已失效"
                  : status === "ignored"
                    ? "已忽略"
                    : isExpired
                      ? "已过期"
                      : "等待确认"}
            </span>
          </div>

          <p className="mt-4 rounded-xl bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700">
            {payload.preview}
          </p>

          {payload.operationType === "CREATE_PRIVATE_NOTE" ? (
            <DraftPreview payload={payload} />
          ) : (
            <BatchPreview payload={payload} />
          )}

          <p className="mt-4 text-xs text-slate-400">
            过期时间：{formatDateTime(payload.expiresAt)}
          </p>

          {error ? (
            <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          {result ? <OperationResult result={result} /> : null}

          <div className="mt-5 flex justify-end gap-3">
            <button
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={status !== "pending" || isExpired}
              onClick={() => {
                setStatus("ignored");
                setError(null);
                onOperationResolutionChange({
                  status: "ignored",
                  result: null,
                  error: null,
                });
              }}
              type="button"
            >
              忽略
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm shadow-blue-200 transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:text-blue-400 disabled:shadow-none"
              disabled={isDisabled}
              onClick={() => void handleConfirm()}
              type="button"
            >
              {status === "submitting" ? (
                <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
              ) : null}
              确认执行
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}

function ActionCard({
  action,
  onOperationResolutionChange,
}: {
  action: AgentTranscriptAction;
  onOperationResolutionChange: (resolution: AgentOperationResolution) => void;
}) {
  if (action.type === "PENDING_OPERATION") {
    let pendingAction: PendingOperationAction | undefined;
    let malformedPayload = false;

    try {
      pendingAction = parsePendingOperationAction(action);
    } catch {
      malformedPayload = true;
    }

    if (!malformedPayload && pendingAction) {
      return (
        <PendingOperationCard
          action={pendingAction}
          onOperationResolutionChange={onOperationResolutionChange}
          operationResolution={action.operationResolution}
        />
      );
    }

    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        收到待确认操作，但 payload 格式不符合契约。前端已安全忽略，不会执行。
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
      收到暂不支持的 Agent 动作：{action.type || "UNKNOWN"}。前端已安全忽略，不会执行。
    </div>
  );
}

function findScrollParent(element: HTMLElement | null): HTMLElement | null {
  let current = element?.parentElement ?? null;

  while (current) {
    const overflowY = window.getComputedStyle(current).overflowY;

    if (overflowY === "auto" || overflowY === "scroll") {
      return current;
    }

    current = current.parentElement;
  }

  return document.scrollingElement instanceof HTMLElement
    ? document.scrollingElement
    : null;
}

export function AgentWorkspace({
  initialMessages = [],
  message,
  onBeforeSend,
  onConversationStart,
  onMessageChange,
  onMessagesChange,
  resetVersion,
}: AgentWorkspaceProps) {
  const [messages, setMessages] = useState<AgentMessage[]>(() =>
    trimAgentMessages(initialMessages),
  );
  const [chatError, setChatError] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);
  const [visibleMessageCount, setVisibleMessageCount] = useState(
    AGENT_VISIBLE_MESSAGE_STEP,
  );
  const idCounter = useRef(initialMessages.length);
  const lastResetVersion = useRef(resetVersion);
  const chatController = useRef<AbortController | null>(null);
  const messageInputRef = useRef<HTMLTextAreaElement | null>(null);
  const sectionRef = useRef<HTMLElement | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const hasScrolledToInitialBottom = useRef(false);
  const canSend = message.trim().length > 0 && message.length <= 1000;
  const hasConversation = messages.length > 0 || chatError !== null || isSending;

  useEffect(() => () => chatController.current?.abort(), []);

  useEffect(() => {
    onMessagesChange?.(messages);
  }, [messages, onMessagesChange]);

  useEffect(() => {
    if (lastResetVersion.current !== resetVersion) {
      lastResetVersion.current = resetVersion;
      chatController.current?.abort();
      setMessages([]);
      setChatError(null);
      setIsSending(false);
    }
  }, [resetVersion]);

  useEffect(() => {
    const input = messageInputRef.current;

    if (!input) {
      return;
    }

    input.style.height = "0px";
    input.style.height = `${Math.min(Math.max(input.scrollHeight, 72), 160)}px`;
    input.style.overflowY = input.scrollHeight > 160 ? "auto" : "hidden";
  }, [message]);

  useEffect(() => {
    if (!hasConversation || hasScrolledToInitialBottom.current) {
      return;
    }

    hasScrolledToInitialBottom.current = true;
    window.setTimeout(() => {
      bottomRef.current?.scrollIntoView?.({ block: "end" });
    }, 0);
  }, [hasConversation]);

  useEffect(() => {
    if (!hasConversation || !hasScrolledToInitialBottom.current) {
      return;
    }

    window.setTimeout(() => {
      bottomRef.current?.scrollIntoView?.({ block: "end", behavior: "smooth" });
    }, 0);
  }, [chatError, hasConversation, isSending, messages.length]);

  useEffect(() => {
    if (!hasConversation) {
      return;
    }

    const scrollParent = findScrollParent(sectionRef.current);

    if (!scrollParent) {
      return;
    }

    const parent = scrollParent;

    function handleScroll() {
      if (parent.scrollTop > 80) {
        return;
      }

      setVisibleMessageCount((current) =>
        Math.min(current + AGENT_VISIBLE_MESSAGE_STEP, messages.length),
      );
    }

    parent.addEventListener("scroll", handleScroll, { passive: true });

    return () => parent.removeEventListener("scroll", handleScroll);
  }, [hasConversation, messages.length]);

  function nextMessageId(prefix: AgentMessage["role"]): string {
    idCounter.current += 1;
    return `${prefix}-${idCounter.current}`;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canSend || isSending) {
      if (!message.trim()) {
        setChatError("请输入消息后再发送。");
      }
      return;
    }

    const sentMessage = message;
    const controller = new AbortController();
    chatController.current = controller;
    setChatError(null);
    setIsSending(true);
    onMessageChange("");

    try {
      await onBeforeSend?.();
      onConversationStart?.();
      setMessages((current) =>
        trimAgentMessages([
          ...current,
          {
            id: nextMessageId("user"),
            role: "user",
            content: sentMessage,
            actions: [],
          },
        ]),
      );
      const response = await chatAiAgent(
        { message: sentMessage },
        controller.signal,
      );
      setMessages((current) =>
        trimAgentMessages([
          ...current,
          {
            id: nextMessageId("assistant"),
            role: "assistant",
            content: response.answer,
            actions: response.actions,
          },
        ]),
      );
    } catch (caughtError) {
      if (!controller.signal.aborted) {
        setChatError(`${describeAgentError(caughtError)} 请重新输入后重试。`);
      }
    } finally {
      if (!controller.signal.aborted) {
        setIsSending(false);
      }
    }
  }

  function handleMessageKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
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

  function handleOperationResolutionChange(
    messageId: string,
    actionIndex: number,
    resolution: AgentOperationResolution,
  ) {
    setMessages((current) =>
      current.map((item) =>
        item.id === messageId
          ? {
              ...item,
              actions: item.actions.map((action, index) =>
                index === actionIndex
                  ? { ...action, operationResolution: resolution }
                  : action,
              ),
            }
          : item,
      ),
    );
  }

  const visibleMessages = messages.slice(-visibleMessageCount);

  return (
    <section
      className="mx-auto mt-12 flex w-full max-w-4xl flex-1 flex-col"
      ref={sectionRef}
    >
      {!hasConversation ? (
        <div className="text-center">
          <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-blue-50 text-blue-600">
            <Sparkles aria-hidden="true" className="size-6" />
          </span>
          <h2 className="mt-5 text-lg font-semibold text-slate-900">
            让 Agent 协助整理知识
          </h2>
          <p className="mt-2 text-sm text-slate-500">
            Agent 可以基于对话生成待确认操作，只有你点击确认后才会执行。
          </p>
        </div>
      ) : (
        <div aria-live="polite" className="space-y-5">
          {messages.length > visibleMessages.length ? (
            <div className="text-center text-xs text-slate-400">
              向上滚动加载更早对话
            </div>
          ) : null}

          {visibleMessages.map((item) =>
            item.role === "user" ? (
              <div
                className="ml-auto max-w-[78%] rounded-2xl rounded-tr-md bg-blue-50 px-5 py-4 text-sm leading-6 text-slate-800"
                key={item.id}
              >
                {item.content}
              </div>
            ) : (
              <div className="space-y-4" key={item.id}>
                <div className="flex items-start gap-3">
                  <span className="mt-1 grid size-10 shrink-0 place-items-center rounded-full bg-blue-600 text-white shadow-sm shadow-blue-200">
                    <Bot aria-hidden="true" className="size-5" />
                  </span>
                  <div className="min-w-0 flex-1 rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm shadow-slate-100">
                    <AiMarkdownContent content={item.content} />
                  </div>
                </div>
                {item.actions.length > 0 ? (
                  <div className="ml-[3.25rem] space-y-3">
                    {item.actions.map((action, index) => (
                      <ActionCard
                        action={action}
                        key={`${item.id}-${action.type}-${index}`}
                        onOperationResolutionChange={(resolution) =>
                          handleOperationResolutionChange(
                            item.id,
                            index,
                            resolution,
                          )
                        }
                      />
                    ))}
                  </div>
                ) : null}
              </div>
            ),
          )}

          {isSending ? (
            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-5 text-sm text-slate-500">
              <LoaderCircle aria-hidden="true" className="size-5 animate-spin text-blue-500" />
              Agent 正在生成完整回答…
            </div>
          ) : null}

          {chatError ? (
            <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              <AlertTriangle aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
              {chatError}
            </div>
          ) : null}
        </div>
      )}

      <form
        className={cn(
          "z-10 mx-auto w-full max-w-2xl text-left",
          hasConversation ? "sticky bottom-2 mt-auto pt-8" : "mt-auto pt-8",
        )}
        onSubmit={(event) => void handleSubmit(event)}
      >
        <label className="sr-only" htmlFor="ai-agent-input">
          Agent 消息
        </label>
        <div className="relative rounded-2xl border border-slate-200 bg-white/95 shadow-xl shadow-slate-200/70 backdrop-blur focus-within:border-blue-300 focus-within:ring-4 focus-within:ring-blue-50">
          <textarea
            className="max-h-40 min-h-[4.5rem] w-full resize-none bg-transparent px-5 pb-14 pt-4 text-sm leading-6 text-slate-800 outline-none placeholder:text-slate-400"
            disabled={isSending}
            id="ai-agent-input"
            maxLength={1000}
            onChange={(event) => onMessageChange(event.target.value)}
            onKeyDown={handleMessageKeyDown}
            placeholder="让 Agent 帮你整理、创建或调整笔记…"
            ref={messageInputRef}
            value={message}
          />
          <span className="absolute bottom-4 left-5 text-xs text-slate-400">
            {message.length}/1000
          </span>
          <button
            aria-label="发送 Agent 消息"
            className="absolute bottom-3 right-4 grid size-10 place-items-center rounded-full bg-blue-600 text-white shadow-sm shadow-blue-200 transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:text-blue-400 disabled:shadow-none"
            disabled={!canSend || isSending}
            type="submit"
          >
            {isSending ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : (
              <Send aria-hidden="true" className="size-4" />
            )}
          </button>
        </div>
        <p className="mt-2 text-center text-xs text-slate-400">
          AI 生成内容仅供参考；Agent 操作必须手动确认后才会执行。
        </p>
        <p className="sr-only">
          Agent 返回待确认操作时，不会自动执行；必须手动点击确认。
        </p>
      </form>
      <div ref={bottomRef} />
    </section>
  );
}
