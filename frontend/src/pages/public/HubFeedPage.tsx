import {
  BookOpenText,
  LogIn,
  Search,
  Sparkles,
} from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import type { PublicNoteListItemResponse } from "@/api/public-note-contracts";
import { useAuth } from "@/features/auth/auth-context";
import { PublicPagination } from "@/features/public-notes/PublicPagination";
import {
  FEED_PAGE_SIZE,
  authorDisplayName,
  formatPublicDateTime,
  totalPages,
} from "@/features/public-notes/public-note-display";
import { usePublicNoteList } from "@/features/public-notes/usePublicNotes";
import { Avatar } from "@/shared/avatar/Avatar";
import { PageState } from "@/shared/layout/PageState";

function HubFeedNoteItem({ note }: { note: PublicNoteListItemResponse }) {
  return (
    <Link
      className="group block rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm shadow-slate-100 transition hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-lg hover:shadow-blue-100/50"
      to={`/public/notes/${note.id}`}
    >
      <div className="flex items-start gap-4">
        <span className="grid size-16 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-blue-50 to-indigo-100 text-blue-600">
          <BookOpenText aria-hidden="true" className="size-7" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-lg font-bold tracking-tight text-slate-950 group-hover:text-primary">
            {note.title}
          </span>
          <span className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">
            {note.summary || "这篇公开笔记暂未提供摘要。"}
          </span>
          <span className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-2 text-xs text-slate-400">
            <span className="inline-flex items-center gap-2 text-slate-600">
              <Avatar
                className="size-6 text-[11px]"
                nickname={note.author.nickname}
                username={note.author.username}
              />
              {authorDisplayName(note.author)}
            </span>
            <span>发布于 {formatPublicDateTime(note.publishedAt)}</span>
            {note.tags.map((tag) => (
              <span
                className="rounded-md bg-blue-50 px-2 py-1 font-medium text-blue-600"
                key={tag.name}
              >
                {tag.name}
              </span>
            ))}
          </span>
        </span>
      </div>
    </Link>
  );
}

export function HubFeedPage() {
  const auth = useAuth();
  const [page, setPage] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const { data, error, isLoading } = usePublicNoteList({
    page,
    size: FEED_PAGE_SIZE,
    keyword,
  });

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setKeyword(keywordDraft.trim());
    setPage(1);
  }

  return (
    <main className="min-h-svh bg-[linear-gradient(180deg,#f8fbff_0%,#ffffff_38%,#f8fafc_100%)] text-slate-950">
      <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
          <Link className="flex items-center gap-2" to="/">
            <span className="grid size-9 place-items-center rounded-xl bg-primary text-white shadow-sm shadow-blue-200">
              <BookOpenText aria-hidden="true" className="size-5" />
            </span>
            <span className="text-lg font-bold tracking-tight">KnowledgeHub</span>
          </Link>
          <nav
            aria-label="公开导航"
            className="hidden items-center gap-8 text-sm font-medium text-slate-500 md:flex"
          >
            <Link className="border-b-2 border-primary py-5 text-primary" to="/">
              公开知识 Feed
            </Link>
            <span className="cursor-default py-5">探索标签</span>
            <span className="cursor-default py-5">社区精选</span>
            <span className="cursor-default py-5">关于我们</span>
          </nav>
          <div className="flex items-center gap-3">
            {auth.status === "authenticated" ? (
              <Link
                className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white shadow-sm shadow-blue-200 transition hover:bg-blue-700"
                to="/notes"
              >
                进入工作台
              </Link>
            ) : (
              <>
                <Link
                  className="hidden rounded-xl px-3 py-2 text-sm font-medium text-slate-500 transition hover:text-primary sm:inline-flex"
                  to="/register"
                >
                  注册
                </Link>
                <Link
                  className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white shadow-sm shadow-blue-200 transition hover:bg-blue-700"
                  to="/login"
                >
                  <LogIn aria-hidden="true" className="size-4" />
                  登录
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <section className="mx-auto grid max-w-6xl gap-10 px-6 pb-8 pt-14 lg:grid-cols-[1fr_24rem] lg:items-center">
        <div>
          <p className="inline-flex items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-primary">
            <Sparkles aria-hidden="true" className="size-4" />
            公开知识社区
          </p>
          <h1 className="mt-5 text-5xl font-bold tracking-tight text-slate-950">
            发现<span className="text-primary">优质</span>公开笔记
          </h1>
          <p className="mt-5 max-w-2xl text-base leading-8 text-slate-600">
            浏览社区中分享的知识笔记，覆盖技术、产品、学习方法等领域，从优秀内容中获得信息灵感。
          </p>
        </div>
        <div className="relative hidden min-h-56 overflow-hidden rounded-[2rem] bg-blue-50/80 p-8 lg:block">
          <div className="absolute right-8 top-8 size-32 rounded-full bg-blue-200/40 blur-2xl" />
          <div className="relative rounded-3xl border border-white/80 bg-white/80 p-5 shadow-xl shadow-blue-100">
            <div className="mb-4 flex items-center justify-between">
              <span className="h-3 w-24 rounded-full bg-blue-100" />
              <span className="size-7 rounded-full bg-blue-50" />
            </div>
            <div className="space-y-3">
              <span className="block h-3 w-11/12 rounded-full bg-slate-100" />
              <span className="block h-3 w-9/12 rounded-full bg-slate-100" />
              <span className="block h-3 w-10/12 rounded-full bg-slate-100" />
            </div>
            <div className="mt-6 grid h-24 place-items-center rounded-2xl bg-gradient-to-br from-blue-100 to-indigo-100 text-primary">
              <BookOpenText aria-hidden="true" className="size-10" />
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 pb-16">
        <div className="mb-5 flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm shadow-slate-100 md:flex-row md:items-center md:justify-between">
          <div className="inline-flex rounded-xl bg-slate-50 p-1 text-sm font-medium text-slate-500">
            <span className="rounded-lg bg-white px-4 py-2 text-primary shadow-sm">
              最新
            </span>
          </div>
          <form className="w-full md:max-w-sm" onSubmit={handleSearch}>
            <label className="sr-only" htmlFor="hub-feed-search">
              搜索公开笔记
            </label>
            <div className="relative">
              <Search
                aria-hidden="true"
                className="absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400"
              />
              <input
                className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-11 pr-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-300 focus:ring-4 focus:ring-blue-50"
                id="hub-feed-search"
                maxLength={100}
                onChange={(event) => setKeywordDraft(event.target.value)}
                placeholder="搜索公开笔记、标签或作者"
                value={keywordDraft}
              />
            </div>
          </form>
        </div>

        {isLoading ? (
          <PageState
            compact
            description="正在加载公开知识 Feed。"
            mode="loading"
            title="加载公开 Feed"
          />
        ) : error ? (
          <PageState
            compact
            description={error}
            mode="error"
            title="公开 Feed 加载失败"
          />
        ) : data && data.items.length > 0 ? (
          <div className="space-y-3">
            {data.items.map((note) => (
              <HubFeedNoteItem key={note.id} note={note} />
            ))}
          </div>
        ) : (
          <PageState
            compact
            description={
              keyword
                ? "没有找到匹配的公开笔记。"
                : "当前还没有可展示的公开笔记。"
            }
            mode="empty"
            title="暂无公开内容"
          />
        )}

        <PublicPagination
          onPageChange={setPage}
          page={page}
          totalPages={totalPages(data)}
        />
      </section>
    </main>
  );
}
