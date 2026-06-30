import { Bot, Database, Info } from "lucide-react";
import { useCallback, useState } from "react";

import {
  clearAgentTranscript,
  readAgentTranscript,
  readRagTranscript,
  writeAgentTranscript,
  writeRagTranscript,
  type AgentTranscriptMessage,
  type RagTranscriptTurn,
} from "@/features/ai/ai-session-storage";
import { AgentSessionClearControl } from "@/features/ai/agent/AgentSessionClearControl";
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
    description: "协助完成笔记操作",
  },
};

export function AiWorkspacePage() {
  const auth = useAuth();
  const userId = auth.user?.id ?? null;
  const [mode, setMode] = useState<AiMode>("rag");
  const [ragQuestion, setRagQuestion] = useState("");
  const [agentMessage, setAgentMessage] = useState("");
  const [agentResetVersion, setAgentResetVersion] = useState(0);
  const [ragTurnsSnapshot, setRagTurnsSnapshot] = useState<RagTranscriptTurn[]>(
    () => readRagTranscript(userId),
  );
  const [agentMessagesSnapshot, setAgentMessagesSnapshot] = useState<
    AgentTranscriptMessage[]
  >(() => readAgentTranscript(userId));
  const copy = modeCopy[mode];

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

  function handleAgentCleared() {
    setAgentMessage("");
    clearAgentTranscript(userId);
    setAgentMessagesSnapshot([]);
    setAgentResetVersion((current) => current + 1);
  }

  return (
    <div className="flex h-full min-w-0 flex-col bg-white">
      <header className="flex min-h-16 shrink-0 items-center justify-between gap-6 border-b border-slate-100 px-7">
        <div className="flex items-center gap-2 text-sm text-slate-500">
          {mode === "rag" ? (
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
        {mode === "rag" ? (
          <RagRebuildControl />
        ) : (
          <AgentSessionClearControl onCleared={handleAgentCleared} />
        )}
      </header>

      <main className="min-h-0 flex-1 overflow-y-auto px-8 pb-16 pt-9 xl:px-12">
        <div className="mx-auto flex min-h-full w-full max-w-5xl flex-col">
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

          {mode === "rag" ? (
            <RagWorkspace
              initialTurns={ragTurnsSnapshot}
              key={`rag-${userId ?? "anonymous"}`}
              onQuestionChange={setRagQuestion}
              onTurnsChange={handleRagTurnsChange}
              question={ragQuestion}
            />
          ) : (
            <AgentWorkspace
              initialMessages={agentMessagesSnapshot}
              key={`agent-${userId ?? "anonymous"}`}
              message={agentMessage}
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
