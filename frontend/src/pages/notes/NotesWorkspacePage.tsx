import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";

import type { NoteListQuery } from "@/api/notes";
import { NoteReader } from "@/features/notes/NoteReader";
import { NotesSidebar } from "@/features/notes/NotesSidebar";
import { parseNoteId } from "@/features/notes/note-display";
import { useNotesWorkspace } from "@/features/notes/useNotesWorkspace";

export function NotesWorkspacePage() {
  const { noteId: noteIdParameter } = useParams();
  const navigate = useNavigate();
  const selectedNoteId = parseNoteId(noteIdParameter);
  const invalidSelection = noteIdParameter !== undefined && selectedNoteId === null;
  const workspace = useNotesWorkspace(selectedNoteId);
  const [searchText, setSearchText] = useState("");
  const [searchError, setSearchError] = useState<string | null>(null);

  useEffect(() => {
    if (
      noteIdParameter === undefined &&
      !workspace.list.isLoading &&
      workspace.list.data?.items.length
    ) {
      void navigate(`/notes/${workspace.list.data.items[0].id}`, { replace: true });
    }
  }, [navigate, noteIdParameter, workspace.list.data, workspace.list.isLoading]);

  const categoryName = useMemo(() => {
    const categoryId = workspace.detail.data?.categoryId;

    if (categoryId === null) {
      return "未分类";
    }

    if (categoryId === undefined) {
      return "";
    }

    return (
      workspace.categories.data?.find((category) => category.id === categoryId)?.name ??
      "分类信息不可用"
    );
  }, [workspace.categories.data, workspace.detail.data?.categoryId]);

  function updateQuery(
    updater: (current: NoteListQuery) => NoteListQuery,
  ): void {
    void navigate("/notes");
    workspace.updateQuery(updater);
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const keyword = searchText.trim();

    if (keyword.length > 100) {
      setSearchError("搜索关键词不能超过 100 个字符");
      return;
    }

    setSearchError(null);
    updateQuery((current) => ({
      ...current,
      page: 1,
      keyword: keyword || undefined,
    }));
  }

  return (
    <div className="flex h-full min-w-0">
      <NotesSidebar
        categories={workspace.categories.data}
        categoriesError={workspace.categories.error}
        categoriesLoading={workspace.categories.isLoading}
        list={workspace.list.data}
        listError={workspace.list.error}
        listLoading={workspace.list.isLoading}
        onPageChange={(page) =>
          updateQuery((current) => ({ ...current, page }))
        }
        onRetryCategories={workspace.retryCategories}
        onRetryList={workspace.retryList}
        onRetryTags={workspace.retryTags}
        onSearch={handleSearch}
        onSearchTextChange={setSearchText}
        onSelectCategory={(categoryId) =>
          updateQuery((current) => ({ ...current, page: 1, categoryId }))
        }
        onSelectNote={(noteId) => void navigate(`/notes/${noteId}`)}
        onSelectTag={(tagId) =>
          updateQuery((current) => ({ ...current, page: 1, tagId }))
        }
        searchError={searchError}
        searchText={searchText}
        selectedCategoryId={workspace.query.categoryId}
        selectedNoteId={selectedNoteId}
        selectedTagId={workspace.query.tagId}
        tags={workspace.tags.data}
        tagsError={workspace.tags.error}
        tagsLoading={workspace.tags.isLoading}
      />

      <section className="min-w-0 flex-1">
        <NoteReader
          categoryName={categoryName}
          error={workspace.detail.error}
          hasSelection={selectedNoteId !== null}
          invalidSelection={invalidSelection}
          isLoading={workspace.detail.isLoading}
          note={workspace.detail.data}
          onRetry={workspace.retryDetail}
          rawError={workspace.detail.rawError}
        />
      </section>
    </div>
  );
}
