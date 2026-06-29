import { ArrowLeft, CalendarDays, Tag } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";

import {
  authorDisplayName,
  formatPublicDateTime,
} from "@/features/public-notes/public-note-display";
import { usePublicNoteDetail } from "@/features/public-notes/usePublicNotes";
import { Avatar } from "@/shared/avatar/Avatar";
import { PageState } from "@/shared/layout/PageState";
import { MarkdownContent } from "@/shared/markdown/MarkdownContent";

function parseNoteId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null;
  }

  const noteId = Number(value);
  return Number.isSafeInteger(noteId) && noteId > 0 ? noteId : null;
}

export function PublicNoteDetailPage() {
  const params = useParams();
  const navigate = useNavigate();
  const noteId = parseNoteId(params.noteId);
  const { data, error, isLoading } = usePublicNoteDetail(noteId);

  function handleBack() {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }

    navigate("/");
  }

  if (isLoading) {
    return (
      <PageState
        description="正在加载公开笔记详情。"
        mode="loading"
        title="加载公开笔记"
      />
    );
  }

  if (error || !data) {
    return (
      <PageState
        description={error ?? "公开笔记不存在或不可访问。"}
        mode="error"
        title="公开笔记不可访问"
      />
    );
  }

  const authorName = authorDisplayName(data.author);

  return (
    <div className="min-h-svh overflow-y-auto bg-white px-8 py-6">
      <button
        className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 shadow-sm transition hover:border-blue-200 hover:text-primary"
        onClick={handleBack}
        type="button"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        返回
      </button>

      <article className="mx-auto w-full max-w-4xl py-6 xl:px-4">
        <header className="border-b border-slate-100 pb-8">
          <h1 className="text-4xl font-bold tracking-tight text-slate-950">
            {data.title}
          </h1>
          {data.summary ? (
            <p className="mt-4 text-base leading-7 text-slate-600">
              {data.summary}
            </p>
          ) : null}

          <div className="mt-6 flex flex-wrap items-center gap-x-4 gap-y-3 text-sm text-slate-500">
            <Link
              className="inline-flex items-center gap-2 font-medium text-slate-700 hover:text-primary"
              to={`/public/users/${data.author.username}`}
            >
              <Avatar
                className="size-8"
                nickname={data.author.nickname}
                username={data.author.username}
              />
              {authorName}
            </Link>
            <span className="h-4 w-px bg-slate-200" />
            <span className="inline-flex items-center gap-2">
              <CalendarDays aria-hidden="true" className="size-4" />
              发布于 {formatPublicDateTime(data.publishedAt)}
            </span>
          </div>

          {data.tags.length > 0 ? (
            <div className="mt-5 flex flex-wrap gap-2">
              {data.tags.map((tag) => (
                <span
                  className="inline-flex items-center gap-1 rounded-lg bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500"
                  key={tag.name}
                >
                  <Tag aria-hidden="true" className="size-3" />
                  {tag.name}
                </span>
              ))}
            </div>
          ) : null}
        </header>

        <MarkdownContent content={data.contentMd} />
      </article>
    </div>
  );
}
