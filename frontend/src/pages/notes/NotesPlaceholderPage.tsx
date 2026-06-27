import { BookOpenText, LogOut } from "lucide-react";
import { useState } from "react";

import { useAuth } from "@/features/auth/auth-context";

export function NotesPlaceholderPage() {
  const auth = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  async function handleLogout() {
    setIsLoggingOut(true);

    try {
      await auth.logout();
    } catch {
      // Local session is intentionally cleared even if server logout fails.
    }
  }

  return (
    <main className="grid min-h-svh place-items-center bg-background px-6 text-foreground">
      <section className="w-full max-w-lg rounded-2xl border bg-white p-10 text-center shadow-sm">
        <div className="mx-auto grid size-12 place-items-center rounded-xl bg-primary text-primary-foreground">
          <BookOpenText aria-hidden="true" className="size-6" strokeWidth={1.8} />
        </div>
        <p className="mt-6 text-sm text-muted-foreground">
          已登录为 {auth.user?.nickname || auth.user?.username}
        </p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">笔记工作区</h1>
        <p className="mt-3 text-sm leading-6 text-muted-foreground">
          F1 仅建立鉴权和受保护路由；笔记内容将在后续阶段实现。
        </p>
        <button
          className="mx-auto mt-7 flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isLoggingOut}
          onClick={() => void handleLogout()}
          type="button"
        >
          <LogOut aria-hidden="true" className="size-4" />
          {isLoggingOut ? "正在退出" : "退出登录"}
        </button>
      </section>
    </main>
  );
}
