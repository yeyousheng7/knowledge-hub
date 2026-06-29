import { CalendarDays, UserRound } from "lucide-react";
import { useState } from "react";
import { useParams } from "react-router-dom";

import { CompactPublicNoteCard } from "@/features/public-notes/PublicNoteCards";
import { PublicPagination } from "@/features/public-notes/PublicPagination";
import {
  PUBLIC_NOTE_PAGE_SIZE,
  authorDisplayName,
  formatPublicDateTime,
  totalPages,
} from "@/features/public-notes/public-note-display";
import { usePublicUserPage } from "@/features/public-notes/usePublicNotes";
import { Avatar } from "@/shared/avatar/Avatar";
import { PageState } from "@/shared/layout/PageState";

function parseUsername(value: string | undefined): string | null {
  return value && /^[0-9a-zA-Z_]{3,30}$/.test(value) ? value : null;
}

export function PublicUserPage() {
  const params = useParams();
  const username = parseUsername(params.username);
  const [page, setPage] = useState(1);
  const { data, error, isLoading } = usePublicUserPage({
    username,
    page,
    size: PUBLIC_NOTE_PAGE_SIZE,
  });

  if (isLoading) {
    return (
      <PageState
        description="正在加载公开用户主页。"
        mode="loading"
        title="加载公开用户"
      />
    );
  }

  if (error || !data) {
    return (
      <PageState
        description={error ?? "公开用户不存在或不可访问。"}
        mode="error"
        title="公开用户不可访问"
      />
    );
  }

  const displayName = authorDisplayName(data.profile);

  return (
    <div className="min-h-svh overflow-y-auto bg-white px-8 py-8 xl:px-10">
      <div className="mx-auto w-full max-w-5xl">
        <header className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm shadow-slate-100">
          <div className="flex items-start gap-5">
            <Avatar
              className="size-16 text-xl"
              nickname={data.profile.nickname}
              username={data.profile.username}
            />
            <div className="min-w-0 flex-1">
              <h1 className="text-3xl font-bold tracking-tight text-slate-950">
                {displayName}
              </h1>
              <p className="mt-1 inline-flex items-center gap-2 text-sm text-slate-500">
                <UserRound aria-hidden="true" className="size-4" />
                @{data.profile.username}
              </p>
              <p className="mt-4 max-w-3xl text-sm leading-6 text-slate-600">
                {data.profile.bio || "这个用户暂未填写公开简介。"}
              </p>
              <p className="mt-4 inline-flex items-center gap-2 text-xs text-slate-400">
                <CalendarDays aria-hidden="true" className="size-3.5" />
                加入于 {formatPublicDateTime(data.profile.createdAt)}
              </p>
            </div>
          </div>
        </header>

        <section className="mt-8">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">公开笔记</h2>
            <span className="text-sm text-slate-400">
              共 {data.notes.total} 篇
            </span>
          </div>

          {data.notes.items.length > 0 ? (
            <div className="grid gap-3 md:grid-cols-2">
              {data.notes.items.map((note) => (
                <CompactPublicNoteCard key={note.id} note={note} />
              ))}
            </div>
          ) : (
            <PageState
              compact
              description="该用户当前没有公开笔记。"
              mode="empty"
              title="暂无公开笔记"
            />
          )}

          <PublicPagination
            onPageChange={setPage}
            page={page}
            totalPages={totalPages(data.notes)}
          />
        </section>
      </div>
    </div>
  );
}
