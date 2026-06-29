import { Newspaper } from "lucide-react";
import { useState } from "react";

import { FeedNoteCard } from "@/features/public-notes/PublicNoteCards";
import { PublicPagination } from "@/features/public-notes/PublicPagination";
import {
  FEED_PAGE_SIZE,
  totalPages,
} from "@/features/public-notes/public-note-display";
import { usePublicNoteList } from "@/features/public-notes/usePublicNotes";
import { PageState } from "@/shared/layout/PageState";

export function FeedPage() {
  const [page, setPage] = useState(1);
  const { data, error, isLoading } = usePublicNoteList({
    page,
    size: FEED_PAGE_SIZE,
  });

  return (
    <div className="h-full min-h-0 overflow-y-auto bg-white px-8 py-8 xl:px-10">
      <div className="mx-auto flex min-h-full w-full max-w-6xl flex-col">
        <header className="flex items-start justify-between gap-6">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-slate-950">
              Feed
            </h1>
            <div className="mt-7 inline-flex items-center gap-2 text-sm font-medium text-slate-600">
              <Newspaper aria-hidden="true" className="size-4 text-primary" />
              后端默认公开发布时间排序
            </div>
          </div>
        </header>

        <div className="mt-6 flex-1">
          {isLoading ? (
            <PageState
              compact
              description="正在加载公开笔记流。"
              mode="loading"
              title="加载 Feed"
            />
          ) : error ? (
            <PageState
              compact
              description={error}
              mode="error"
              title="Feed 加载失败"
            />
          ) : data && data.items.length > 0 ? (
            <div className="space-y-3">
              {data.items.map((note) => (
                <FeedNoteCard key={note.id} note={note} />
              ))}
            </div>
          ) : (
            <PageState
              compact
              description="当前还没有可展示的公开笔记。"
              mode="empty"
              title="暂无公开内容"
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
