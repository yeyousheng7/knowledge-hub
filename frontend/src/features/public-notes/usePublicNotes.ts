import { useEffect, useState } from "react";

import {
  getPublicNoteDetail,
  getPublicUserProfile,
  listPublicNotes,
  listPublicUserNotes,
} from "@/api/public-notes";
import type {
  PublicNoteDetailResponse,
  PublicNoteListResponse,
  PublicUserProfileResponse,
} from "@/api/public-note-contracts";

interface AsyncState<T> {
  data: T | null;
  error: string | null;
  isLoading: boolean;
}

interface StoredAsyncState<T> extends AsyncState<T> {
  key: string;
}

function describePublicError(error: unknown): string {
  return error instanceof Error ? error.message : "公开内容加载失败，请稍后重试。";
}

export function usePublicNoteList(query: {
  page: number;
  size: number;
  keyword?: string;
}): AsyncState<PublicNoteListResponse> {
  const { keyword, page, size } = query;
  const key = `${page}:${size}:${keyword ?? ""}`;
  const [state, setState] = useState<StoredAsyncState<PublicNoteListResponse>>({
    data: null,
    error: null,
    key: "",
    isLoading: true,
  });

  useEffect(() => {
    const controller = new AbortController();

    listPublicNotes({ page, size, keyword }, controller.signal)
      .then((data) => {
        setState({ data, error: null, isLoading: false, key });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setState({
            data: null,
            error: describePublicError(error),
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

export function usePublicNoteDetail(
  noteId: number | null,
): AsyncState<PublicNoteDetailResponse> {
  const key = noteId === null ? "invalid" : String(noteId);
  const [state, setState] = useState<StoredAsyncState<PublicNoteDetailResponse>>({
    data: null,
    error: null,
    key: "",
    isLoading: true,
  });

  useEffect(() => {
    if (noteId === null) {
      return;
    }

    const controller = new AbortController();

    getPublicNoteDetail(noteId, controller.signal)
      .then((data) => {
        setState({ data, error: null, isLoading: false, key });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setState({
            data: null,
            error: describePublicError(error),
            isLoading: false,
            key,
          });
        }
      });

    return () => controller.abort();
  }, [key, noteId]);

  if (noteId === null) {
    return {
      data: null,
      error: "公开笔记地址无效。",
      isLoading: false,
    };
  }

  return {
    data: state.key === key ? state.data : null,
    error: state.key === key ? state.error : null,
    isLoading: state.key !== key || state.isLoading,
  };
}

export function usePublicUserPage(query: {
  username: string | null;
  page: number;
  size: number;
}): AsyncState<{
  profile: PublicUserProfileResponse;
  notes: PublicNoteListResponse;
}> {
  const { page, size, username } = query;
  const key = username === null ? "invalid" : `${username}:${page}:${size}`;
  const [state, setState] = useState<
    StoredAsyncState<{
      profile: PublicUserProfileResponse;
      notes: PublicNoteListResponse;
    }>
  >({
    data: null,
    error: null,
    key: "",
    isLoading: true,
  });

  useEffect(() => {
    if (username === null) {
      return;
    }

    const controller = new AbortController();

    Promise.all([
      getPublicUserProfile(username, controller.signal),
      listPublicUserNotes(
        {
          username,
          page,
          size,
        },
        controller.signal,
      ),
    ])
      .then(([profile, notes]) => {
        setState({
          data: { profile, notes },
          error: null,
          isLoading: false,
          key,
        });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setState({
            data: null,
            error: describePublicError(error),
            isLoading: false,
            key,
          });
        }
      });

    return () => controller.abort();
  }, [key, page, size, username]);

  if (username === null) {
    return {
      data: null,
      error: "公开用户地址无效。",
      isLoading: false,
    };
  }

  return {
    data: state.key === key ? state.data : null,
    error: state.key === key ? state.error : null,
    isLoading: state.key !== key || state.isLoading,
  };
}
