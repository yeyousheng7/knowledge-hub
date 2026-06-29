import { CalendarDays, Edit3, Eye, FileText, Tag, UserRound } from "lucide-react";
import { Link } from "react-router-dom";

import type { NoteListItemResponse } from "@/api/note-contracts";
import type { PublicNoteListItemResponse } from "@/api/public-note-contracts";
import { Avatar } from "@/shared/avatar/Avatar";
import {
  authorDisplayName,
  formatPublicDateTime,
  formatRelativePublicTime,
} from "@/features/public-notes/public-note-display";

interface PublicNoteCardProps {
  note: PublicNoteListItemResponse;
}

export function FeedNoteCard({ note }: PublicNoteCardProps) {
  const authorName = authorDisplayName(note.author);

  return (
    <article className="grid min-h-36 grid-cols-[12rem_1px_1fr] rounded-2xl border border-slate-200 bg-white px-5 py-5 shadow-sm shadow-slate-100 transition hover:border-blue-200 hover:shadow-md hover:shadow-blue-50">
      <Link
        className="flex min-w-0 items-start gap-3 pr-5"
        to={`/public/users/${note.author.username}`}
      >
        <Avatar
          className="size-11"
          nickname={note.author.nickname}
          username={note.author.username}
        />
        <span className="min-w-0">
          <span className="block truncate text-sm font-semibold text-slate-800">
            {authorName}
          </span>
          <span className="mt-1 block text-xs text-slate-400">
            {formatRelativePublicTime(note.publishedAt)}
          </span>
        </span>
      </Link>

      <div className="bg-slate-200" />

      <Link className="group min-w-0 pl-7" to={`/public/notes/${note.id}`}>
        <div className="flex items-start justify-between gap-5">
          <div className="min-w-0">
            <h2 className="truncate text-xl font-bold tracking-tight text-slate-950 group-hover:text-primary">
              {note.title}
            </h2>
            <p className="mt-3 line-clamp-2 text-sm leading-6 text-slate-600">
              {note.summary || "这篇公开笔记暂未提供摘要。"}
            </p>
          </div>
          <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-600">
            <Eye aria-hidden="true" className="size-3.5" />
            预览
          </span>
        </div>
        <div className="mt-5 flex flex-wrap gap-2">
          {note.tags.map((tag) => (
            <span
              className="rounded-lg bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500"
              key={tag.name}
            >
              {tag.name}
            </span>
          ))}
        </div>
      </Link>
    </article>
  );
}

export function PublicNoteListCard({ note }: PublicNoteCardProps) {
  const authorName = authorDisplayName(note.author);

  return (
    <article className="rounded-2xl border border-slate-200 bg-white px-6 py-5 shadow-sm shadow-slate-100 transition hover:border-blue-200 hover:shadow-md hover:shadow-blue-50">
      <div className="flex items-start gap-6">
        <Link className="min-w-0 flex-1" to={`/public/notes/${note.id}`}>
          <h2 className="text-xl font-bold tracking-tight text-slate-950 hover:text-primary">
            {note.title}
          </h2>
          <p className="mt-3 line-clamp-2 max-w-3xl text-sm leading-6 text-slate-600">
            {note.summary || "这篇公开笔记暂未提供摘要。"}
          </p>
        </Link>
        <Link
          className="inline-flex shrink-0 items-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-medium text-primary transition hover:bg-blue-100"
          to={`/public/notes/${note.id}`}
        >
          <Eye aria-hidden="true" className="size-4" />
          预览
        </Link>
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-3 text-sm text-slate-500">
        <Link
          className="inline-flex items-center gap-2 font-medium text-slate-600 hover:text-primary"
          to={`/public/users/${note.author.username}`}
        >
          <UserRound aria-hidden="true" className="size-4" />
          {authorName}
        </Link>
        <span className="h-4 w-px bg-slate-200" />
        <span className="inline-flex items-center gap-2">
          <CalendarDays aria-hidden="true" className="size-4" />
          发布于 {formatPublicDateTime(note.publishedAt)}
        </span>
        <span className="ml-auto flex flex-wrap gap-2">
          {note.tags.map((tag) => (
            <span
              className="inline-flex items-center gap-1 rounded-lg bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500"
              key={tag.name}
            >
              <Tag aria-hidden="true" className="size-3" />
              {tag.name}
            </span>
          ))}
        </span>
      </div>
    </article>
  );
}

export function OwnPublicNoteCard({ note }: { note: NoteListItemResponse }) {
  const publishedAt = note.publishedAt ?? note.updatedAt;

  return (
    <article className="rounded-2xl border border-slate-200 bg-white px-6 py-5 shadow-sm shadow-slate-100 transition hover:border-blue-200 hover:shadow-md hover:shadow-blue-50">
      <div className="flex items-start gap-6">
        <Link className="min-w-0 flex-1" to={`/public/notes/${note.id}`}>
          <h2 className="text-xl font-bold tracking-tight text-slate-950 hover:text-primary">
            {note.title}
          </h2>
          <p className="mt-3 line-clamp-2 max-w-3xl text-sm leading-6 text-slate-600">
            {note.summary || "这篇公开笔记暂未提供摘要。"}
          </p>
        </Link>
        <div className="flex shrink-0 gap-3">
          <Link
            className="inline-flex items-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-medium text-primary transition hover:bg-blue-100"
            to={`/public/notes/${note.id}`}
          >
            <Eye aria-hidden="true" className="size-4" />
            预览
          </Link>
          <Link
            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-blue-200 hover:text-primary"
            to={`/notes/${note.id}/edit`}
          >
            <Edit3 aria-hidden="true" className="size-4" />
            编辑
          </Link>
        </div>
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-3 text-sm text-slate-500">
        <span className="inline-flex items-center gap-2">
          <CalendarDays aria-hidden="true" className="size-4" />
          发布于 {formatPublicDateTime(publishedAt)}
        </span>
        <span className="ml-auto flex flex-wrap gap-2">
          {note.tags.map((tag) => (
            <span
              className="inline-flex items-center gap-1 rounded-lg bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500"
              key={tag.id}
            >
              <Tag aria-hidden="true" className="size-3" />
              {tag.name}
            </span>
          ))}
        </span>
      </div>
    </article>
  );
}

export function CompactPublicNoteCard({ note }: PublicNoteCardProps) {
  return (
    <Link
      className="group flex min-w-0 items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 transition hover:border-blue-200 hover:shadow-sm"
      to={`/public/notes/${note.id}`}
    >
      <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-blue-50 text-primary">
        <FileText aria-hidden="true" className="size-4" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-semibold text-slate-900 group-hover:text-primary">
          {note.title}
        </span>
        <span className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">
          {note.summary || "这篇公开笔记暂未提供摘要。"}
        </span>
      </span>
    </Link>
  );
}
