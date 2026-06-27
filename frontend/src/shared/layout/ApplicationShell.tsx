import {
  BookOpenText,
  Globe2,
  LogIn,
  LogOut,
  Newspaper,
  NotebookTabs,
  Sparkles,
  type LucideIcon,
} from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";
import { Avatar } from "@/shared/avatar/Avatar";
import { cn } from "@/shared/lib/utils";

interface NavigationItem {
  label: string;
  path: string;
  icon: LucideIcon;
}

const navigationItems: NavigationItem[] = [
  { label: "笔记", path: "/notes", icon: NotebookTabs },
  { label: "AI", path: "/ai", icon: Sparkles },
  { label: "Feed", path: "/feed", icon: Newspaper },
  { label: "公开", path: "/public", icon: Globe2 },
];

export function ApplicationShell() {
  const auth = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  async function handleLogout() {
    setIsLoggingOut(true);

    try {
      await auth.logout();
    } catch {
      // AuthProvider still clears the local session when server logout fails.
    }
  }

  return (
    <div className="flex h-svh min-h-[640px] min-w-[960px] overflow-hidden bg-slate-50 text-foreground">
      <aside className="flex w-[232px] shrink-0 flex-col border-r border-slate-200/80 bg-white px-4 py-5">
        <div className="flex h-12 items-center gap-3 px-3">
          <span className="grid size-9 place-items-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-blue-200">
            <BookOpenText aria-hidden="true" className="size-5" strokeWidth={1.9} />
          </span>
          <span className="text-[17px] font-semibold tracking-tight text-slate-950">
            KnowledgeHub
          </span>
        </div>

        <nav aria-label="主导航" className="mt-9 space-y-2">
          {navigationItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                className={({ isActive }) =>
                  cn(
                    "flex h-12 items-center gap-3 rounded-xl px-4 text-[15px] font-medium text-slate-600 transition",
                    "hover:bg-slate-50 hover:text-slate-950",
                    isActive && "bg-blue-50 text-primary hover:bg-blue-50 hover:text-primary",
                  )
                }
                key={item.path}
                to={item.path}
              >
                <Icon aria-hidden="true" className="size-5" strokeWidth={1.8} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>

        <div className="mt-auto border-t border-slate-100 pt-4">
          {auth.status === "authenticated" && auth.user ? (
            <div className="flex items-center gap-3 rounded-xl px-2 py-2">
              <Avatar
                nickname={auth.user.nickname}
                username={auth.user.username}
              />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-800">
                  {auth.user.nickname.trim() || auth.user.username}
                </p>
                <p className="truncate text-xs text-slate-400">@{auth.user.username}</p>
              </div>
              <button
                aria-label="退出登录"
                className="grid size-9 place-items-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 disabled:opacity-50"
                disabled={isLoggingOut}
                onClick={() => void handleLogout()}
                title="退出登录"
                type="button"
              >
                <LogOut aria-hidden="true" className="size-4" />
              </button>
            </div>
          ) : (
            <NavLink
              className="flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium text-slate-600 transition hover:bg-slate-50 hover:text-primary"
              to="/login"
            >
              <LogIn aria-hidden="true" className="size-4" />
              登录
            </NavLink>
          )}
        </div>
      </aside>

      <main className="min-w-0 flex-1 p-3">
        <div className="h-full min-h-0 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm shadow-slate-200/40">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
