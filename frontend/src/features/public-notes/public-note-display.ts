import type { PublicNoteAuthorResponse } from "@/api/public-note-contracts";

export const PUBLIC_NOTE_PAGE_SIZE = 10;
export const FEED_PAGE_SIZE = 8;

export function authorDisplayName(author: PublicNoteAuthorResponse): string {
  return author.nickname.trim() || author.username;
}

export function formatPublicDateTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function formatRelativePublicTime(value: string): string {
  return formatPublicDateTime(value);
}

export function totalPages(response: { total: number; size: number } | null): number {
  if (!response) {
    return 1;
  }

  return Math.max(1, Math.ceil(response.total / response.size));
}
