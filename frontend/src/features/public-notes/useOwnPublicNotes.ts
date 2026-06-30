import { useEffect, useState } from "react";

import { getNotes } from "@/api/notes";
import type {
  NoteListItemResponse,
  NoteListResponse,
} from "@/api/note-contracts";

interface OwnPublicNoteListQuery {
  page: number;
  size: number;
  keyword?: string;
}

interface AsyncState<T> {
  data: T | null;
  error: string | null;
  isLoading: boolean;
}

interface StoredAsyncState<T> extends AsyncState<T> {
  key: string;
}

function describeOwnPublicNotesError(error: unknown): string {
  return error instanceof Error ? error.message : "我的公开笔记加载失败，请稍后重试。";
}

async function listOwnPublicNotes(
  query: OwnPublicNoteListQuery,
  signal?: AbortSignal,
): Promise<NoteListResponse> {
  const remotePageSize = 100;
  const notesById = new Map<number, NoteListItemResponse>();
  let remotePage = 1;
  let hasMore = true;

  while (hasMore) {
    const response = await getNotes(
      {
        page: remotePage,
        size: remotePageSize,
        keyword: query.keyword,
      },
      signal,
    );

    response.items.forEach((note) => {
      if (note.visibility === "PUBLIC") {
        notesById.set(note.id, note);
      }
    });

    hasMore =
      response.items.length === remotePageSize &&
      remotePage * remotePageSize < response.total;
    remotePage += 1;
  }

  const filteredItems = [...notesById.values()];
  const offset = (query.page - 1) * query.size;

  return {
    items: filteredItems.slice(offset, offset + query.size),
    total: filteredItems.length,
    page: query.page,
    size: query.size,
  };
}

export function useOwnPublicNotes(
  query: OwnPublicNoteListQuery,
): AsyncState<NoteListResponse> {
  const { keyword, page, size } = query;
  const key = `${page}:${size}:${keyword ?? ""}`;
  const [state, setState] = useState<StoredAsyncState<NoteListResponse>>({
    data: null,
    error: null,
    isLoading: true,
    key: "",
  });

  useEffect(() => {
    const controller = new AbortController();

    listOwnPublicNotes({ page, size, keyword }, controller.signal)
      .then((data) => {
        setState({ data, error: null, isLoading: false, key });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setState({
            data: null,
            error: describeOwnPublicNotesError(error),
            isLoading: false,
            key,
          });
        }
      });

    return () => controller.abort();
  }, [key, keyword, page, size]);

  return {
    data: state.key === key ? state.data : null,
    error: state.key === key ? state.error : null,
    isLoading: state.key !== key || state.isLoading,
  };
}
