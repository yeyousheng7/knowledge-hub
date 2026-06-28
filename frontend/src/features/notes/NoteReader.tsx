import {
  AlertTriangle,
  Clock3,
  Folder,
  Globe2,
  LoaderCircle,
  LockKeyhole,
  Pencil,
  Tag,
  Trash2,
} from "lucide-react";

import type { NoteDetailResponse } from "@/api/note-contracts";
import {
  formatNoteDate,
  isNotFoundError,
} from "@/features/notes/note-display";
import { NoteStatus } from "@/features/notes/NoteStatus";
import { MarkdownContent } from "@/shared/markdown/MarkdownContent";
import { PageState } from "@/shared/layout/PageState";

interface NoteReaderProps {
  note: NoteDetailResponse | null;
  isLoading: boolean;
  error: string | null;
  rawError: unknown;
  hasSelection: boolean;
  invalidSelection: boolean;
  categoryName: string;
  onRetry: () => void;
  onEdit: () => void;
  onTogglePublish: () => void;
  onDelete: () => void;
  isMutating: boolean;
  actionError: string | null;
}

export function NoteReader({
  note,
  isLoading,
  error,
  rawError,
  hasSelection,
  invalidSelection,
  categoryName,
  onRetry,
  onEdit,
  onTogglePublish,
  onDelete,
  isMutating,
  actionError,
}: NoteReaderProps) {
  if (invalidSelection) {
    return (
      <PageState
        description="笔记 ID 必须是有效的正整数。"
        mode="error"
        title="笔记地址无效"
      />
    );
  }

  if (!hasSelection) {
    return (
      <PageState
        description="从左侧列表选择一篇笔记以查看完整 Markdown 内容。"
        mode="empty"
        title="选择一篇笔记"
      />
    );
  }

  if (isLoading) {
    return (
      <PageState
        description="正在获取完整 Markdown 正文。"
        mode="loading"
        title="加载笔记详情"
      />
    );
  }

  if (error || !note) {
    const notFound = isNotFoundError(rawError);

    return (
      <PageState
        actionLabel={notFound ? undefined : "重试"}
        description={
          notFound ? "该笔记不存在、已删除或当前账号无权访问。" : error ?? "笔记详情不可用"
        }
        mode="error"
        onAction={notFound ? undefined : onRetry}
        title={notFound ? "笔记不存在" : "笔记详情加载失败"}
      />
    );
  }

  return (
    <div className="flex h-full min-w-0 flex-col bg-white">
      <header className="flex min-h-16 shrink-0 flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-7 py-2">
        <p className="text-xs font-medium text-slate-400">我的笔记 / 阅读</p>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <button
            className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-50"
            disabled={isMutating}
            onClick={onEdit}
            type="button"
          >
            <Pencil aria-hidden="true" className="size-3.5" />
            编辑
          </button>
          <button
            className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-50"
            disabled={isMutating}
            onClick={onTogglePublish}
            type="button"
          >
            {isMutating ? (
              <LoaderCircle aria-hidden="true" className="size-3.5 animate-spin" />
            ) : note.visibility === "PUBLIC" ? (
              <LockKeyhole aria-hidden="true" className="size-3.5" />
            ) : (
              <Globe2 aria-hidden="true" className="size-3.5" />
            )}
            {note.visibility === "PUBLIC" ? "取消发布" : "发布"}
          </button>
          <button
            aria-label="删除笔记"
            className="grid size-9 place-items-center rounded-lg border border-red-100 text-red-500 transition hover:bg-red-50 disabled:opacity-50"
            disabled={isMutating}
            onClick={onDelete}
            type="button"
          >
            <Trash2 aria-hidden="true" className="size-3.5" />
          </button>
        </div>
      </header>

      <article className="min-h-0 flex-1 overflow-y-auto px-8 pb-16 pt-9 xl:px-12">
        <div className="mx-auto max-w-4xl">
          {actionError ? (
            <div className="mb-6 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
              {actionError}
            </div>
          ) : null}
          {note.moderationStatus === "TAKEN_DOWN" ? (
            <div className="mb-6 flex items-start gap-3 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
              <AlertTriangle aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
              该笔记已被下架。它仍可在个人工作台查看，但不会出现在公开页面。
            </div>
          ) : null}

          <h1 className="text-3xl font-bold tracking-tight text-slate-950">
            {note.title}
          </h1>

          <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-slate-200 pb-6 text-xs text-slate-500">
            <NoteStatus
              moderationStatus={note.moderationStatus}
              visibility={note.visibility}
            />
            <span className="inline-flex items-center gap-1.5">
              <Folder aria-hidden="true" className="size-3.5" />
              {categoryName}
            </span>
            {note.tags.map((tag) => (
              <span
                className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-1"
                key={tag.id}
              >
                <Tag aria-hidden="true" className="size-3" />
                {tag.name}
              </span>
            ))}
            <span className="ml-auto inline-flex items-center gap-1.5">
              <Clock3 aria-hidden="true" className="size-3.5" />
              {formatNoteDate(note.updatedAt)}
            </span>
          </div>

          <div className="pt-8">
            <MarkdownContent content={note.contentMd} />
          </div>
        </div>
      </article>
    </div>
  );
}
