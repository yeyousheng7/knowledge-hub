import { Bot, Database, Info } from "lucide-react";
import { useState } from "react";

import { AgentSessionClearControl } from "@/features/ai/agent/AgentSessionClearControl";
import { AgentWorkspace } from "@/features/ai/agent/AgentWorkspace";
import { RagRebuildControl } from "@/features/ai/rag/RagRebuildControl";
import { RagWorkspace } from "@/features/ai/rag/RagWorkspace";
import { cn } from "@/shared/lib/utils";

type AiMode = "rag" | "agent";

const modeCopy: Record<AiMode, { label: string; description: string }> = {
  rag: {
    label: "RAG",
    description: "基于已索引笔记进行检索增强回答",
  },
  agent: {
    label: "Agent",
    description: "使用知识库上下文协助完成笔记操作",
  },
};

export function AiWorkspacePage() {
  const [mode, setMode] = useState<AiMode>("rag");
  const [ragQuestion, setRagQuestion] = useState("");
  const [agentMessage, setAgentMessage] = useState("");
  const [agentResetVersion, setAgentResetVersion] = useState(0);
  const copy = modeCopy[mode];

  function handleAgentCleared() {
    setAgentMessage("");
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
          <div className="mx-auto mt-10 grid w-full max-w-md grid-cols-2 rounded-2xl border border-slate-200 bg-white p-1 shadow-sm shadow-slate-100">
            {(["rag", "agent"] as const).map((itemMode) => {
              const Icon = itemMode === "rag" ? Database : Bot;
              const isActive = mode === itemMode;

              return (
                <button
                  aria-pressed={isActive}
                  className={cn(
                    "inline-flex h-11 items-center justify-center gap-2 rounded-xl text-sm font-medium text-slate-600 transition",
                    isActive && "bg-blue-50 text-primary shadow-sm ring-1 ring-blue-200",
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
              onQuestionChange={setRagQuestion}
              question={ragQuestion}
            />
          ) : (
            <AgentWorkspace
              message={agentMessage}
              onMessageChange={setAgentMessage}
              resetVersion={agentResetVersion}
            />
          )}
        </div>
      </main>
    </div>
  );
}
