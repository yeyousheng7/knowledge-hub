import { ChevronLeft, ChevronRight } from "lucide-react";

import { cn } from "@/shared/lib/utils";

interface PublicPaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

function visiblePages(page: number, totalPages: number): number[] {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages = new Set([1, totalPages, page - 1, page, page + 1]);
  return [...pages]
    .filter((item) => item >= 1 && item <= totalPages)
    .sort((left, right) => left - right);
}

export function PublicPagination({
  page,
  totalPages,
  onPageChange,
}: PublicPaginationProps) {
  const pages = visiblePages(page, totalPages);

  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav
      aria-label="公开笔记分页"
      className="flex items-center justify-center gap-3 pt-5"
    >
      <button
        aria-label="上一页"
        className="grid size-10 place-items-center rounded-xl border border-slate-200 bg-white text-slate-400 transition hover:border-blue-200 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
        type="button"
      >
        <ChevronLeft aria-hidden="true" className="size-4" />
      </button>

      {pages.map((item, index) => {
        const previous = pages[index - 1];
        const needsGap = previous !== undefined && item - previous > 1;

        return (
          <span className="flex items-center gap-3" key={item}>
            {needsGap ? <span className="text-slate-400">…</span> : null}
            <button
              aria-current={item === page ? "page" : undefined}
              className={cn(
                "grid size-10 place-items-center rounded-xl text-sm font-medium text-slate-600 transition hover:bg-blue-50 hover:text-primary",
                item === page &&
                  "border border-blue-200 bg-blue-50 text-primary shadow-sm shadow-blue-100",
              )}
              onClick={() => onPageChange(item)}
              type="button"
            >
              {item}
            </button>
          </span>
        );
      })}

      <button
        aria-label="下一页"
        className="grid size-10 place-items-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:border-blue-200 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
        type="button"
      >
        <ChevronRight aria-hidden="true" className="size-4" />
      </button>
    </nav>
  );
}
