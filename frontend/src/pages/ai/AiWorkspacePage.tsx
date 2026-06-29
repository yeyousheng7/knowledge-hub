import { Bot, Database, Info, Send, Sparkles } from "lucide-react";
import { useState, type FormEvent } from "react";

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
  const activeInput = mode === "rag" ? ragQuestion : agentMessage;
  const copy = modeCopy[mode];

  function updateActiveInput(value: string) {
    if (mode === "rag") {
      setRagQuestion(value);
      return;
    }

    setAgentMessage(value);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
  }

  return (
    <div className="flex h-full min-w-0 flex-col bg-white">
      <header className="flex min-h-16 shrink-0 items-center border-b border-slate-100 px-7">
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
      </header>

      <main className="min-h-0 flex-1 overflow-y-auto px-8 pb-16 pt-9 xl:px-12">
        <div className="mx-auto flex min-h-full w-full max-w-5xl flex-col">
          <h1 className="text-3xl font-bold tracking-tight text-slate-950">AI</h1>

          <div className="mx-auto mt-16 grid w-full max-w-md grid-cols-2 rounded-2xl border border-slate-200 bg-white p-1 shadow-sm shadow-slate-100">
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

          <section className="mx-auto mt-12 w-full max-w-4xl text-center">
            <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-blue-50 text-blue-600">
              {mode === "rag" ? (
                <Database aria-hidden="true" className="size-6" />
              ) : (
                <Sparkles aria-hidden="true" className="size-6" />
              )}
            </span>
            <h2 className="mt-5 text-lg font-semibold text-slate-900">
              {mode === "rag" ? "从你的知识库开始提问" : "让 Agent 协助整理知识"}
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              {mode === "rag"
                ? "重建索引后，AI 将基于你的真实笔记提供回答和来源。"
                : "Agent 对话将在下一阶段接入，当前可以先准备你的消息。"}
            </p>

            {mode === "rag" ? (
              <button
                className="mt-7 inline-flex h-11 items-center gap-2 rounded-xl border border-blue-200 px-5 text-sm font-medium text-blue-600 disabled:cursor-not-allowed disabled:opacity-60"
                disabled
                type="button"
              >
                <Database aria-hidden="true" className="size-4" />
                重建 RAG 知识库
              </button>
            ) : null}

            <form className="mt-8 text-left" onSubmit={handleSubmit}>
              <label className="sr-only" htmlFor={`ai-${mode}-input`}>
                {mode === "rag" ? "RAG 问题" : "Agent 消息"}
              </label>
              <div className="relative rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-100 focus-within:border-blue-300 focus-within:ring-4 focus-within:ring-blue-50">
                <textarea
                  className="min-h-36 w-full resize-none bg-transparent px-5 pb-16 pt-5 text-sm leading-6 text-slate-800 outline-none placeholder:text-slate-400"
                  id={`ai-${mode}-input`}
                  maxLength={1000}
                  onChange={(event) => updateActiveInput(event.target.value)}
                  placeholder={
                    mode === "rag"
                      ? "输入你的第一个问题，开启知识探索之旅…"
                      : "给 KnowledgeHub AI 发送消息…"
                  }
                  value={activeInput}
                />
                <button
                  aria-label={mode === "rag" ? "发送 RAG 问题" : "发送 Agent 消息"}
                  className="absolute bottom-4 right-4 grid size-10 place-items-center rounded-full bg-blue-100 text-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled
                  title={mode === "rag" ? "RAG 请求将在 F6 接入" : "Agent 请求将在 F7 接入"}
                  type="submit"
                >
                  <Send aria-hidden="true" className="size-4" />
                </button>
              </div>
            </form>
          </section>
        </div>
      </main>
    </div>
  );
}
