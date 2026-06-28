import { useCallback, useEffect, useState } from "react";

import type {
  NoteDetailResponse,
  NoteListResponse,
} from "@/api/note-contracts";
import {
  getNoteDetail,
  getNotes,
  type NoteListQuery,
} from "@/api/notes";
import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { getCategories, getTags } from "@/api/taxonomy";
import { apiErrorMessage } from "@/features/notes/note-display";

interface ResourceState<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  rawError: unknown;
}

interface DetailResult {
  noteId: number | null;
  data: NoteDetailResponse | null;
  error: string | null;
  rawError: unknown;
}

const initialResourceState = <T,>(): ResourceState<T> => ({
  data: null,
  isLoading: true,
  error: null,
  rawError: null,
});

export function useNotesWorkspace(selectedNoteId: number | null) {
  const [query, setQuery] = useState<NoteListQuery>({ page: 1, size: 20 });
  const [listRetry, setListRetry] = useState(0);
  const [detailRetry, setDetailRetry] = useState(0);
  const [categoryRetry, setCategoryRetry] = useState(0);
  const [tagRetry, setTagRetry] = useState(0);
  const [list, setList] = useState<ResourceState<NoteListResponse>>(
    initialResourceState,
  );
  const [detailResult, setDetailResult] = useState<DetailResult>({
    noteId: null,
    data: null,
    error: null,
    rawError: null,
  });
  const [categories, setCategories] = useState<
    ResourceState<CategoryListItemResponse[]>
  >(initialResourceState);
  const [tags, setTags] = useState<ResourceState<TagListItemResponse[]>>(
    initialResourceState,
  );

  useEffect(() => {
    const controller = new AbortController();

    void getNotes(query, controller.signal)
      .then((data) => {
        setList({ data, isLoading: false, error: null, rawError: null });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setList({
          data: null,
          isLoading: false,
          error: apiErrorMessage(error, "无法加载笔记列表，请稍后重试"),
          rawError: error,
        });
      });

    return () => controller.abort();
  }, [listRetry, query]);

  useEffect(() => {
    const controller = new AbortController();

    void getCategories(controller.signal)
      .then((data) => {
        setCategories({ data, isLoading: false, error: null, rawError: null });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setCategories({
          data: null,
          isLoading: false,
          error: apiErrorMessage(error, "分类加载失败"),
          rawError: error,
        });
      });

    return () => controller.abort();
  }, [categoryRetry]);

  useEffect(() => {
    const controller = new AbortController();

    void getTags(controller.signal)
      .then((data) => {
        setTags({ data, isLoading: false, error: null, rawError: null });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setTags({
          data: null,
          isLoading: false,
          error: apiErrorMessage(error, "标签加载失败"),
          rawError: error,
        });
      });

    return () => controller.abort();
  }, [tagRetry]);

  useEffect(() => {
    if (selectedNoteId === null) {
      return undefined;
    }

    const controller = new AbortController();

    void getNoteDetail(selectedNoteId, controller.signal)
      .then((data) => {
        setDetailResult({
          noteId: selectedNoteId,
          data,
          error: null,
          rawError: null,
        });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setDetailResult({
          noteId: selectedNoteId,
          data: null,
          error: apiErrorMessage(error, "无法加载笔记详情，请稍后重试"),
          rawError: error,
        });
      });

    return () => controller.abort();
  }, [detailRetry, selectedNoteId]);

  const updateQuery = useCallback(
    (updater: (current: NoteListQuery) => NoteListQuery) => {
      setList(initialResourceState());
      setQuery(updater);
    },
    [],
  );

  const replaceDetail = useCallback((note: NoteDetailResponse) => {
    setDetailResult({
      noteId: note.id,
      data: note,
      error: null,
      rawError: null,
    });
  }, []);

  const addCategory = useCallback((category: CategoryListItemResponse) => {
    setCategories((current) => ({
      data: current.data
        ? [...current.data.filter((item) => item.id !== category.id), category]
        : [category],
      isLoading: false,
      error: null,
      rawError: null,
    }));
  }, []);

  const addTag = useCallback((tag: TagListItemResponse) => {
    setTags((current) => ({
      data: current.data
        ? [...current.data.filter((item) => item.id !== tag.id), tag]
        : [tag],
      isLoading: false,
      error: null,
      rawError: null,
    }));
  }, []);

  const detail: ResourceState<NoteDetailResponse> =
    selectedNoteId === null
      ? { data: null, isLoading: false, error: null, rawError: null }
      : detailResult.noteId === selectedNoteId
        ? { ...detailResult, isLoading: false }
        : { data: null, isLoading: true, error: null, rawError: null };

  return {
    query,
    list,
    detail,
    categories,
    tags,
    updateQuery,
    replaceDetail,
    addCategory,
    addTag,
    retryList: () => {
      setList(initialResourceState());
      setListRetry((value) => value + 1);
    },
    retryDetail: () => {
      setDetailResult({ noteId: null, data: null, error: null, rawError: null });
      setDetailRetry((value) => value + 1);
    },
    retryCategories: () => {
      setCategories(initialResourceState());
      setCategoryRetry((value) => value + 1);
    },
    retryTags: () => {
      setTags(initialResourceState());
      setTagRetry((value) => value + 1);
    },
  };
}
