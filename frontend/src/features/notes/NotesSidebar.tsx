import {
  ChevronLeft,
  ChevronRight,
  Folder,
  FolderOpen,
  FolderMinus,
  LoaderCircle,
  Search,
} from "lucide-react";
import type { FormEvent } from "react";

import type {
  NoteListItemResponse,
  NoteListResponse,
} from "@/api/note-contracts";
import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { formatNoteDate } from "@/features/notes/note-display";
import { NoteStatus } from "@/features/notes/NoteStatus";
import { PageState } from "@/shared/layout/PageState";
import { cn } from "@/shared/lib/utils";

interface NotesSidebarProps {
  list: NoteListResponse | null;
  listLoading: boolean;
  listError: string | null;
  categories: CategoryListItemResponse[] | null;
  categoriesLoading: boolean;
  categoriesError: string | null;
  tags: TagListItemResponse[] | null;
  tagsLoading: boolean;
  tagsError: string | null;
  selectedNoteId: number | null;
  selectedCategoryId?: number;
  selectedTagId?: number;
  searchText: string;
  searchError: string | null;
  onSearchTextChange: (value: string) => void;
  onSearch: (event: FormEvent<HTMLFormElement>) => void;
  onSelectNote: (noteId: number) => void;
  onSelectCategory: (categoryId?: number) => void;
  onSelectTag: (tagId?: number) => void;
  onPageChange: (page: number) => void;
  onRetryList: () => void;
  onRetryCategories: () => void;
  onRetryTags: () => void;
}

function NoteListItem({
  note,
  isSelected,
  onSelect,
}: {
  note: NoteListItemResponse;
  isSelected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      className={cn(
        "block w-full rounded-xl border border-transparent px-3 py-3 text-left transition",
        "hover:border-slate-200 hover:bg-slate-50",
        isSelected && "border-blue-100 bg-blue-50/80 hover:border-blue-100 hover:bg-blue-50/80",
      )}
      onClick={onSelect}
      type="button"
    >
      <div className="flex items-start gap-3">
        <div className="min-w-0 flex-1">
          <h3
            className={cn(
              "truncate text-sm font-semibold text-slate-800",
              isSelected && "text-primary",
            )}
          >
            {note.title}
          </h3>
          <p className="mt-1 line-clamp-2 min-h-10 text-xs leading-5 text-slate-500">
            {note.summary?.trim() || "暂无摘要"}
          </p>
          <div className="mt-2 flex items-center justify-between gap-2">
            <time className="text-[11px] text-slate-400" dateTime={note.updatedAt}>
              {formatNoteDate(note.updatedAt)}
            </time>
            <NoteStatus
              compact
              moderationStatus={note.moderationStatus}
              visibility={note.visibility}
            />
          </div>
        </div>
      </div>
    </button>
  );
}

export function NotesSidebar(props: NotesSidebarProps) {
  const {
    list,
    listLoading,
    listError,
    categories,
    categoriesLoading,
    categoriesError,
    tags,
    tagsLoading,
    tagsError,
    selectedNoteId,
    selectedCategoryId,
    selectedTagId,
    searchText,
    searchError,
    onSearchTextChange,
    onSearch,
    onSelectNote,
    onSelectCategory,
    onSelectTag,
    onPageChange,
    onRetryList,
    onRetryCategories,
    onRetryTags,
  } = props;
  const totalPages = list ? Math.max(1, Math.ceil(list.total / list.size)) : 1;
  const hasUncategorized =
    list?.items.some((note) => note.categoryId === null) ?? false;

  return (
    <aside className="flex h-full w-[360px] shrink-0 flex-col border-r border-slate-200 bg-white">
      <div className="border-b border-slate-100 px-4 pb-4 pt-5">
        <form onSubmit={onSearch}>
          <label className="relative block" htmlFor="note-search">
            <span className="sr-only">搜索笔记</span>
            <Search
              aria-hidden="true"
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400"
            />
            <input
              className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
              id="note-search"
              onChange={(event) => onSearchTextChange(event.target.value)}
              placeholder="搜索笔记"
              type="search"
              value={searchText}
            />
          </label>
          {searchError ? (
            <p className="mt-2 text-xs text-red-600" role="alert">
              {searchError}
            </p>
          ) : null}
        </form>

        <div className="mt-5 flex items-center justify-between">
          <h2 className="text-xs font-medium tracking-wide text-slate-500">分类</h2>
          {categoriesLoading ? (
            <LoaderCircle aria-label="正在加载分类" className="size-3.5 animate-spin text-slate-400" />
          ) : null}
        </div>
        <div className="mt-2 max-h-40 space-y-1 overflow-y-auto pr-1">
          <button
            className={cn(
              "flex h-9 w-full items-center gap-2 rounded-lg px-2.5 text-left text-sm text-slate-600 transition hover:bg-slate-50",
              selectedCategoryId === undefined && "bg-blue-50 text-primary",
            )}
            onClick={() => onSelectCategory(undefined)}
            type="button"
          >
            <FolderOpen aria-hidden="true" className="size-4" strokeWidth={1.8} />
            全部
          </button>
          {categories?.map((category) => (
            <button
              className={cn(
                "flex h-9 w-full items-center gap-2 rounded-lg px-2.5 text-left text-sm text-slate-600 transition hover:bg-slate-50",
                selectedCategoryId === category.id && "bg-blue-50 text-primary",
              )}
              key={category.id}
              onClick={() => onSelectCategory(category.id)}
              type="button"
            >
              <Folder aria-hidden="true" className="size-4" strokeWidth={1.8} />
              <span className="truncate">{category.name}</span>
            </button>
          ))}
          {hasUncategorized ? (
            <div
              aria-disabled="true"
              className="flex h-9 items-center gap-2 rounded-lg px-2.5 text-sm text-slate-400"
              title="当前 API 不支持全量未分类筛选"
            >
              <FolderMinus aria-hidden="true" className="size-4" strokeWidth={1.8} />
              未分类（仅标识）
            </div>
          ) : null}
        </div>
        {categoriesError ? (
          <button
            className="mt-2 text-xs text-red-600 underline-offset-2 hover:underline"
            onClick={onRetryCategories}
            type="button"
          >
            {categoriesError}，重试
          </button>
        ) : null}

        <div className="mt-4">
          <label className="text-xs font-medium tracking-wide text-slate-500" htmlFor="tag-filter">
            标签
          </label>
          <select
            className="mt-2 h-9 w-full rounded-lg border border-slate-200 bg-white px-2.5 text-sm text-slate-600 outline-none focus:border-blue-300 focus:ring-2 focus:ring-blue-100"
            disabled={tagsLoading || Boolean(tagsError)}
            id="tag-filter"
            onChange={(event) => {
              const value = event.target.value;
              onSelectTag(value ? Number(value) : undefined);
            }}
            value={selectedTagId ?? ""}
          >
            <option value="">全部标签</option>
            {tags?.map((tag) => (
              <option key={tag.id} value={tag.id}>
                {tag.name}
              </option>
            ))}
          </select>
          {tagsError ? (
            <button
              className="mt-2 text-xs text-red-600 underline-offset-2 hover:underline"
              onClick={onRetryTags}
              type="button"
            >
              {tagsError}，重试
            </button>
          ) : null}
        </div>
      </div>

      <div className="flex min-h-0 flex-1 flex-col px-2 pb-3 pt-4">
        <div className="flex items-center justify-between px-2">
          <h2 className="text-xs font-medium tracking-wide text-slate-500">我的笔记</h2>
          <span className="text-xs text-slate-400">
            {list ? `共 ${list.total} 篇` : ""}
          </span>
        </div>

        <div className="relative mt-2 min-h-0 flex-1 overflow-y-auto">
          {listLoading && list ? (
            <div className="sticky top-0 z-10 flex justify-center py-1">
              <span className="rounded-full bg-white/90 p-1 shadow-sm">
                <LoaderCircle aria-label="正在刷新笔记" className="size-4 animate-spin text-primary" />
              </span>
            </div>
          ) : null}
          {listError ? (
            <PageState
              actionLabel="重试"
              compact
              description={listError}
              mode="error"
              onAction={onRetryList}
              title="笔记列表加载失败"
            />
          ) : null}
          {!listError && listLoading && !list ? (
            <PageState
              compact
              description="正在获取你的笔记。"
              mode="loading"
              title="加载笔记"
            />
          ) : null}
          {!listError && !listLoading && list?.items.length === 0 ? (
            <PageState
              compact
              description="当前筛选条件下没有可显示的笔记。"
              mode="empty"
              title="还没有笔记"
            />
          ) : null}
          {list?.items.map((note) => (
            <NoteListItem
              isSelected={selectedNoteId === note.id}
              key={note.id}
              note={note}
              onSelect={() => onSelectNote(note.id)}
            />
          ))}
        </div>

        {list && list.total > list.size ? (
          <div className="mt-2 flex items-center justify-between border-t border-slate-100 px-2 pt-3">
            <button
              aria-label="上一页"
              className="grid size-8 place-items-center rounded-lg border text-slate-500 disabled:cursor-not-allowed disabled:opacity-40"
              disabled={list.page <= 1}
              onClick={() => onPageChange(list.page - 1)}
              type="button"
            >
              <ChevronLeft aria-hidden="true" className="size-4" />
            </button>
            <span className="text-xs text-slate-500">
              {list.page} / {totalPages}
            </span>
            <button
              aria-label="下一页"
              className="grid size-8 place-items-center rounded-lg border text-slate-500 disabled:cursor-not-allowed disabled:opacity-40"
              disabled={list.page >= totalPages}
              onClick={() => onPageChange(list.page + 1)}
              type="button"
            >
              <ChevronRight aria-hidden="true" className="size-4" />
            </button>
          </div>
        ) : null}
      </div>
    </aside>
  );
}
