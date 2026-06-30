import { Bot, Database, Info, MessageSquarePlus } from "lucide-react";
import { useCallback, useState } from "react";

import { clearAiAgentSession } from "@/api/ai";
import {
  clearAiTranscript,
  readAgentTranscript,
  readRagTranscript,
  writeAgentTranscript,
  writeRagTranscript,
  type AgentTranscriptMessage,
  type RagTranscriptTurn,
} from "@/features/ai/ai-session-storage";
import { AgentWorkspace } from "@/features/ai/agent/AgentWorkspace";
import { RagRebuildControl } from "@/features/ai/rag/RagRebuildControl";
import { RagWorkspace } from "@/features/ai/rag/RagWorkspace";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/shared/lib/utils";

type AiMode = "rag" | "agent";

const modeCopy: Record<AiMode, { label: string; description: string }> = {
  rag: {
    label: "RAG",
    description: "基于已索引的知识库进行回答",
  },
  agent: {
    label: "Agent",
    description: "支持本轮短期上下文",
  },
};

export function AiWorkspacePage() {
  const auth = useAuth();
  const userId = auth.user?.id ?? null;
  const [mode, setMode] = useState<AiMode>("rag");
  const [ragQuestion, setRagQuestion] = useState("");
  const [agentMessage, setAgentMessage] = useState("");
  const [agentResetVersion, setAgentResetVersion] = useState(0);
  const [workspaceResetVersion, setWorkspaceResetVersion] = useState(0);
  const [newConversationError, setNewConversationError] = useState<string | null>(
    null,
  );
  const [isStartingNewConversation, setIsStartingNewConversation] = useState(false);
  const [ragTurnsSnapshot, setRagTurnsSnapshot] = useState<RagTranscriptTurn[]>(
    () => readRagTranscript(userId),
  );
  const [agentMessagesSnapshot, setAgentMessagesSnapshot] = useState<
    AgentTranscriptMessage[]
  >(() => readAgentTranscript(userId));
  const [sessionMode, setSessionMode] = useState<AiMode | null>(() => {
    if (readRagTranscript(userId).length > 0) {
      return "rag";
    }

    if (readAgentTranscript(userId).length > 0) {
      return "agent";
    }

    return null;
  });
  const activeMode = sessionMode ?? mode;
  const copy = modeCopy[activeMode];

  const handleRagTurnsChange = useCallback(
    (turns: RagTranscriptTurn[]) => {
      setRagTurnsSnapshot(turns);
      writeRagTranscript(userId, turns);
    },
    [userId],
  );

  const handleAgentMessagesChange = useCallback(
    (messages: AgentTranscriptMessage[]) => {
      setAgentMessagesSnapshot(messages);
      writeAgentTranscript(userId, messages);
    },
    [userId],
  );

  function handleConversationStarted(startedMode: AiMode) {
    setSessionMode((current) => current ?? startedMode);
    setNewConversationError(null);
  }

  async function handleNewConversation() {
    if (isStartingNewConversation) {
      return;
    }

    setIsStartingNewConversation(true);
    setNewConversationError(null);

    try {
      if (sessionMode === "agent") {
        await clearAiAgentSession();
      }

      setRagQuestion("");
      setAgentMessage("");
      setRagTurnsSnapshot([]);
      setAgentMessagesSnapshot([]);
      clearAiTranscript(userId);
      setSessionMode(null);
      setMode("rag");
      setAgentResetVersion((current) => current + 1);
      setWorkspaceResetVersion((current) => current + 1);
    } catch {
      setNewConversationError(
        "新对话创建失败：Agent 后端记忆未清除，请稍后重试。",
      );
    } finally {
      setIsStartingNewConversation(false);
    }
  }

  return (
    <div className="flex h-full min-w-0 flex-col bg-white">
      <header className="flex min-h-16 shrink-0 items-center justify-between gap-6 border-b border-slate-100 px-7">
        {sessionMode ? (
          <div className="flex items-center gap-2 text-sm text-slate-500">
            {sessionMode === "rag" ? (
              <Database aria-hidden="true" className="size-4 text-blue-500" />
            ) : (
              <Bot aria-hidden="true" className="size-4 text-blue-500" />
            )}
            <span>当前模式：</span>
            <strong className="font-semibold text-slate-800">{copy.label}</strong>
            <span className="text-slate-300">·</span>
            <span>{copy.description}</span>
            <Info aria-hidden="true" className="size-3.5 text-slate-400" />
          </div>
        ) : (
          <div className="text-sm text-slate-400">选择模式后发送第一条消息</div>
        )}
        <div className="flex items-center gap-3">
          {sessionMode === "rag" ? <RagRebuildControl /> : null}
          {sessionMode ? (
            <button
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 shadow-sm shadow-slate-100 transition hover:border-blue-200 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isStartingNewConversation}
              onClick={() => void handleNewConversation()}
              type="button"
            >
              <MessageSquarePlus aria-hidden="true" className="size-4" />
              {isStartingNewConversation ? "正在创建…" : "新对话"}
            </button>
          ) : null}
        </div>
      </header>

      <main className="min-h-0 flex-1 overflow-y-auto px-8 pb-6 pt-9 xl:px-12">
        <div className="mx-auto flex min-h-full w-full max-w-5xl flex-col">
          {!sessionMode ? (
            <div className="relative mx-auto mt-10 grid w-full max-w-md grid-cols-2 rounded-2xl border border-slate-200 bg-white p-1 shadow-sm shadow-slate-100">
              <span
                aria-hidden="true"
                className={cn(
                  "absolute inset-y-1 left-1 w-[calc(50%-0.25rem)] rounded-xl bg-blue-50 shadow-sm ring-1 ring-blue-200 transition-transform duration-300 ease-out",
                  mode === "agent" && "translate-x-full",
                )}
              />
              {(["rag", "agent"] as const).map((itemMode) => {
                const Icon = itemMode === "rag" ? Database : Bot;
                const isActive = mode === itemMode;

                return (
                  <button
                    aria-label={modeCopy[itemMode].label}
                    aria-pressed={isActive}
                    className={cn(
                      "relative z-10 inline-flex h-11 items-center justify-center gap-2 rounded-xl text-sm font-medium transition-colors duration-200",
                      isActive ? "text-primary" : "text-slate-600 hover:text-slate-900",
                    )}
                    key={itemMode}
                    onClick={() => setMode(itemMode)}
                    type="button"
                  >
                    <Icon aria-hidden="true" className="size-4" />
                    {modeCopy[itemMode].label}
                  </button>
                );
              })}
            </div>
          ) : null}

          {newConversationError ? (
            <div className="mx-auto mt-6 w-full max-w-4xl rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {newConversationError}
            </div>
          ) : null}

          {activeMode === "rag" ? (
            <RagWorkspace
              initialTurns={ragTurnsSnapshot}
              key={`rag-${userId ?? "anonymous"}-${workspaceResetVersion}`}
              onConversationStart={() => handleConversationStarted("rag")}
              onQuestionChange={setRagQuestion}
              onTurnsChange={handleRagTurnsChange}
              question={ragQuestion}
            />
          ) : (
            <AgentWorkspace
              initialMessages={agentMessagesSnapshot}
              key={`agent-${userId ?? "anonymous"}-${workspaceResetVersion}`}
              message={agentMessage}
              onBeforeSend={async () => {
                if (!sessionMode) {
                  await clearAiAgentSession();
                }
              }}
              onConversationStart={() => handleConversationStarted("agent")}
              onMessagesChange={handleAgentMessagesChange}
              onMessageChange={setAgentMessage}
              resetVersion={agentResetVersion}
            />
          )}
        </div>
      </main>
    </div>
  );
}
