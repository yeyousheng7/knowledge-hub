import { BookOpen, BookOpenText } from "lucide-react";
import type { PropsWithChildren } from "react";

export function AuthPageLayout({ children }: PropsWithChildren) {
  return (
    <main className="auth-page relative min-h-svh overflow-hidden px-4 py-8 text-slate-950 sm:px-6">
      <div aria-hidden="true" className="auth-orbit auth-orbit-one" />
      <div aria-hidden="true" className="auth-orbit auth-orbit-two" />
      <div aria-hidden="true" className="auth-wave auth-wave-one" />
      <div aria-hidden="true" className="auth-wave auth-wave-two" />
      <div aria-hidden="true" className="auth-dot-grid left-[15%] top-[31%]" />
      <div aria-hidden="true" className="auth-dot-grid bottom-[25%] right-[14%]" />

      <div className="relative z-10 mx-auto flex min-h-[calc(100svh-4rem)] w-full max-w-md flex-col justify-center">
        <section className="rounded-[20px] border border-white/90 bg-white/95 px-7 py-8 shadow-[0_20px_60px_rgba(49,74,130,0.14)] sm:px-9">
          <header className="text-center">
            <div className="relative mx-auto h-14 w-16 text-blue-600">
              <span className="absolute left-1/2 top-0 size-2.5 -translate-x-1/2 rounded-full bg-blue-400" />
              <BookOpen
                aria-hidden="true"
                className="absolute bottom-0 left-1/2 size-14 -translate-x-1/2"
                strokeWidth={2.25}
              />
            </div>
            <h1 className="mt-3 text-2xl font-bold tracking-tight text-slate-950">
              KnowledgeHub
            </h1>
            <p className="mt-1 text-sm text-slate-500">个人知识库与 AI 助手</p>
          </header>

          {children}

          <div className="mt-5 flex items-start gap-3 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50/70 px-4 py-3 text-left">
            <span className="grid size-9 shrink-0 place-items-center rounded-full bg-blue-100 text-blue-600">
              <BookOpenText aria-hidden="true" className="size-4" strokeWidth={1.8} />
            </span>
            <p className="text-xs leading-5 text-slate-500">
              KnowledgeHub 是你的个人知识库，支持 Markdown 笔记、智能检索与 AI 助手。
            </p>
          </div>
        </section>

        <footer className="mt-5 text-center text-xs text-slate-400">
          © {new Date().getFullYear()} KnowledgeHub. 保留所有权利。
        </footer>
      </div>
    </main>
  );
}
