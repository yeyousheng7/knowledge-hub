import {
  ArrowUpRight,
  BookOpenText,
  Globe2,
  LogIn,
  LogOut,
  Newspaper,
  NotebookTabs,
  PanelLeftClose,
  PanelLeftOpen,
  Sparkles,
  type LucideIcon,
} from "lucide-react";
import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";

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
  { label: "公开", path: "/public", icon: Globe2 },
];

export function ApplicationShell() {
  const auth = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  async function handleLogout() {
    setIsLoggingOut(true);

    try {
      await auth.logout();
    } catch {
      // AuthProvider still clears the local session when server logout fails.
    }
  }

  return (
    <div className="flex h-svh min-h-[640px] min-w-[1180px] overflow-hidden bg-slate-50 text-foreground">
      <aside
        className={cn(
          "relative flex shrink-0 flex-col border-r border-slate-200/80 bg-white py-5 transition-[width,padding] duration-200 ease-out",
          isSidebarCollapsed ? "w-20 px-2" : "w-[232px] px-4",
        )}
        id="application-sidebar"
      >
        {isSidebarCollapsed ? (
          <div className="flex h-12 items-center justify-center">
            <button
              aria-controls="application-sidebar"
              aria-expanded="false"
              aria-label="展开主导航"
              className="group relative grid size-10 place-items-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-blue-200 transition hover:bg-blue-50 hover:text-primary hover:shadow-none"
              onClick={() => setIsSidebarCollapsed(false)}
              title="展开主导航"
              type="button"
            >
              <BookOpenText
                aria-hidden="true"
                className="size-5 transition-opacity group-hover:opacity-0"
                strokeWidth={1.9}
              />
              <PanelLeftOpen
                aria-hidden="true"
                className="absolute size-5 opacity-0 transition-opacity group-hover:opacity-100"
                strokeWidth={1.8}
              />
            </button>
          </div>
        ) : (
          <div className="flex h-12 items-center gap-2 px-2">
            <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-blue-200">
              <BookOpenText aria-hidden="true" className="size-5" strokeWidth={1.9} />
            </span>
            <span
              aria-label="KnowledgeHub"
              className="min-w-0 flex-1 truncate text-[17px] font-semibold tracking-tight text-slate-950"
              title="KnowledgeHub"
            >
              KHub
            </span>
            <button
              aria-controls="application-sidebar"
              aria-expanded="true"
              aria-label="折叠主导航"
              className="grid size-8 shrink-0 place-items-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
              onClick={() => setIsSidebarCollapsed(true)}
              title="折叠主导航"
              type="button"
            >
              <PanelLeftClose aria-hidden="true" className="size-4" />
            </button>
          </div>
        )}

        <nav aria-label="主导航" className="mt-9 space-y-2">
          {navigationItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                className={({ isActive }) =>
                  cn(
                    "flex h-12 items-center rounded-xl text-[15px] font-medium text-slate-600 transition",
                    isSidebarCollapsed ? "justify-center px-0" : "gap-3 px-4",
                    "hover:bg-slate-50 hover:text-slate-950",
                    isActive && "bg-blue-50 text-primary hover:bg-blue-50 hover:text-primary",
                  )
                }
                key={item.path}
                title={isSidebarCollapsed ? item.label : undefined}
                to={item.path}
              >
                <Icon aria-hidden="true" className="size-5" strokeWidth={1.8} />
                <span className={cn(isSidebarCollapsed && "sr-only")}>
                  {item.label}
                </span>
              </NavLink>
            );
          })}
        </nav>

        <div className="mt-auto space-y-3 border-t border-slate-100 pt-4">
          <Link
            className={cn(
              "group relative flex h-11 items-center rounded-xl text-sm font-medium text-slate-500 transition hover:bg-blue-50 hover:text-primary",
              isSidebarCollapsed ? "justify-center px-0" : "gap-3 px-3",
            )}
            title={isSidebarCollapsed ? "Feed" : "打开公开知识 Feed"}
            to="/"
          >
            <Newspaper aria-hidden="true" className="size-4" />
            <span className={cn(isSidebarCollapsed && "sr-only")}>Feed</span>
            <ArrowUpRight
              aria-hidden="true"
              className={cn(
                "size-3.5 text-blue-500 opacity-0 transition group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:opacity-100",
                isSidebarCollapsed ? "absolute right-2 top-2" : "ml-auto",
              )}
            />
          </Link>

          {auth.status === "authenticated" && auth.user ? (
            <div
              className={cn(
                "flex items-center rounded-xl py-2",
                isSidebarCollapsed ? "flex-col gap-2 px-0" : "gap-3 px-2",
              )}
            >
              <Avatar
                nickname={auth.user.nickname}
                username={auth.user.username}
              />
              <div className={cn("min-w-0 flex-1", isSidebarCollapsed && "sr-only")}>
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
              className={cn(
                "flex h-11 items-center rounded-xl text-sm font-medium text-slate-600 transition hover:bg-slate-50 hover:text-primary",
                isSidebarCollapsed ? "justify-center px-0" : "gap-3 px-3",
              )}
              title={isSidebarCollapsed ? "登录" : undefined}
              to="/login"
            >
              <LogIn aria-hidden="true" className="size-4" />
              <span className={cn(isSidebarCollapsed && "sr-only")}>登录</span>
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
