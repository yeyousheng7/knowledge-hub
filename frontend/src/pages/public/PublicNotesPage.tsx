import { Search } from "lucide-react";
import { useState, type FormEvent } from "react";

import { PublicNoteListCard } from "@/features/public-notes/PublicNoteCards";
import { PublicPagination } from "@/features/public-notes/PublicPagination";
import {
  PUBLIC_NOTE_PAGE_SIZE,
  totalPages,
} from "@/features/public-notes/public-note-display";
import { usePublicNoteList } from "@/features/public-notes/usePublicNotes";
import { PageState } from "@/shared/layout/PageState";

export function PublicNotesPage() {
  const [page, setPage] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const { data, error, isLoading } = usePublicNoteList({
    page,
    size: PUBLIC_NOTE_PAGE_SIZE,
    keyword,
  });

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setKeyword(keywordDraft.trim());
    setPage(1);
  }

  return (
    <div className="h-full min-h-0 overflow-y-auto bg-white px-8 py-8 xl:px-10">
      <div className="mx-auto flex min-h-full w-full max-w-6xl flex-col">
        <header>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950">
            公开笔记
          </h1>
          <div className="mt-7 flex items-center justify-between gap-6">
            <form className="w-full max-w-sm" onSubmit={handleSearch}>
              <label className="sr-only" htmlFor="public-note-keyword">
                搜索公开笔记
              </label>
              <div className="relative">
                <Search
                  aria-hidden="true"
                  className="absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400"
                />
                <input
                  className="h-12 w-full rounded-xl border border-slate-200 bg-white pl-11 pr-4 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-blue-300 focus:ring-4 focus:ring-blue-50"
                  id="public-note-keyword"
                  maxLength={100}
                  onChange={(event) => setKeywordDraft(event.target.value)}
                  placeholder="搜索公开笔记"
                  value={keywordDraft}
                />
              </div>
            </form>
            <span className="shrink-0 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">
              后端默认排序
            </span>
          </div>
        </header>

        <div className="mt-8 flex-1">
          {isLoading ? (
            <PageState
              compact
              description="正在加载公开笔记。"
              mode="loading"
              title="加载公开笔记"
            />
          ) : error ? (
            <PageState
              compact
              description={error}
              mode="error"
              title="公开笔记加载失败"
            />
          ) : data && data.items.length > 0 ? (
            <div className="space-y-3">
              {data.items.map((note) => (
                <PublicNoteListCard key={note.id} note={note} />
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
              title="暂无公开笔记"
            />
          )}
        </div>

        <PublicPagination
          onPageChange={setPage}
          page={page}
          totalPages={totalPages(data)}
        />
      </div>
    </div>
  );
}
