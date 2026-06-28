import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";

import type { NoteWriteRequest } from "@/api/note-contracts";
import {
  createNote,
  deleteNote,
  publishNote,
  unpublishNote,
  updateNote,
  type NoteListQuery,
} from "@/api/notes";
import { DeleteNoteDialog } from "@/features/notes/DeleteNoteDialog";
import { NoteReader } from "@/features/notes/NoteReader";
import { NotesSidebar } from "@/features/notes/NotesSidebar";
import { CreateNoteEditor } from "@/features/notes/editor/CreateNoteEditor";
import { InlineNoteEditor } from "@/features/notes/editor/InlineNoteEditor";
import {
  NoteSettingsDialog,
  type NoteSettingsValue,
} from "@/features/notes/editor/NoteSettingsDialog";
import { apiErrorMessage, parseNoteId } from "@/features/notes/note-display";
import { useNotesWorkspace } from "@/features/notes/useNotesWorkspace";
import { PageState } from "@/shared/layout/PageState";

export function NotesWorkspacePage() {
  const { noteId: noteIdParameter } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const isCreateMode = location.pathname === "/notes/new";
  const isEditMode = location.pathname.endsWith("/edit");
  const isReadMode = !isCreateMode && !isEditMode;
  const selectedNoteId = isCreateMode ? null : parseNoteId(noteIdParameter);
  const invalidSelection =
    !isCreateMode && noteIdParameter !== undefined && selectedNoteId === null;
  const workspace = useNotesWorkspace(selectedNoteId);
  const [searchText, setSearchText] = useState("");
  const [searchError, setSearchError] = useState<string | null>(null);
  const [isMutating, setIsMutating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isSettingsDialogOpen, setIsSettingsDialogOpen] = useState(false);
  const [isEditDirty, setIsEditDirty] = useState(false);

  useEffect(() => {
    if (
      noteIdParameter === undefined &&
      isReadMode &&
      !workspace.list.isLoading &&
      workspace.list.data?.items.length
    ) {
      void navigate(`/notes/${workspace.list.data.items[0].id}`, { replace: true });
    }
  }, [isReadMode, navigate, noteIdParameter, workspace.list.data, workspace.list.isLoading]);

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
    if (!confirmDiscardEdit()) {
      return;
    }

    setIsEditDirty(false);
    setIsSettingsDialogOpen(false);
    void navigate("/notes");
    workspace.updateQuery(updater);
  }

  function confirmDiscardEdit(): boolean {
    return (
      !isEditMode ||
      !isEditDirty ||
      window.confirm("当前修改尚未保存，确定放弃并离开编辑态吗？")
    );
  }

  function navigateFromWorkspace(path: string): void {
    if (!confirmDiscardEdit()) {
      return;
    }

    setIsEditDirty(false);
    setActionError(null);
    setIsDeleteDialogOpen(false);
    setIsSettingsDialogOpen(false);
    void navigate(path);
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

  async function handleCreate(request: NoteWriteRequest) {
    const created = await createNote(request);
    workspace.retryList();
    void navigate(`/notes/${created.id}`, { replace: true });
  }

  async function handleEditSave(request: NoteWriteRequest) {
    if (!isEditMode || selectedNoteId === null) {
      throw new Error("当前笔记地址无效，无法保存");
    }

    const updated = await updateNote(selectedNoteId, request);
    workspace.replaceDetail(updated);
    workspace.retryList();
    setIsEditDirty(false);
    void navigate(`/notes/${updated.id}`, { replace: true });
  }

  async function handleSettingsSave(value: NoteSettingsValue) {
    const note = workspace.detail.data;

    if (!note) {
      throw new Error("当前笔记不可用，无法保存设置");
    }

    const updated = await updateNote(note.id, {
      title: value.title,
      contentMd: note.contentMd,
      summary: value.summary,
      categoryId: value.categoryId,
      tagIds: value.tagIds,
    });
    workspace.replaceDetail(updated);
    workspace.retryList();
    setIsSettingsDialogOpen(false);
  }

  async function handleTogglePublish() {
    const note = workspace.detail.data;

    if (!note) {
      return;
    }

    setIsMutating(true);
    setActionError(null);

    try {
      const updated =
        note.visibility === "PUBLIC"
          ? await unpublishNote(note.id)
          : await publishNote(note.id);
      workspace.replaceDetail(updated);
      workspace.retryList();
    } catch (error) {
      setActionError(
        apiErrorMessage(
          error,
          note.visibility === "PUBLIC" ? "取消发布失败" : "发布失败",
        ),
      );
    } finally {
      setIsMutating(false);
    }
  }

  async function handleDelete() {
    const note = workspace.detail.data;

    if (!note) {
      return;
    }

    setIsMutating(true);
    setActionError(null);

    try {
      await deleteNote(note.id);
      setIsDeleteDialogOpen(false);
      workspace.retryList();
      void navigate("/notes", { replace: true });
    } catch (error) {
      setActionError(apiErrorMessage(error, "删除失败，请稍后重试"));
      setIsDeleteDialogOpen(false);
    } finally {
      setIsMutating(false);
    }
  }

  const taxonomyLoading =
    workspace.categories.isLoading || workspace.tags.isLoading;
  const taxonomyError = workspace.categories.error || workspace.tags.error;
  let mainContent: ReactNode;

  if ((isCreateMode || isEditMode) && taxonomyLoading) {
    mainContent = (
      <PageState
        description="正在准备分类、标签和 Markdown 编辑器。"
        mode="loading"
        title="加载笔记编辑器"
      />
    );
  } else if ((isCreateMode || isEditMode) && taxonomyError) {
    mainContent = (
      <PageState
        actionLabel="重试"
        description={taxonomyError}
        mode="error"
        onAction={() => {
          workspace.retryCategories();
          workspace.retryTags();
        }}
        title="无法加载编辑所需数据"
      />
    );
  } else if (isCreateMode) {
    mainContent = (
      <CreateNoteEditor
        categories={workspace.categories.data ?? []}
        onCancel={() => void navigate("/notes")}
        onCategoryCreated={workspace.addCategory}
        onCreate={handleCreate}
        onTagCreated={workspace.addTag}
        tags={workspace.tags.data ?? []}
      />
    );
  } else if (isEditMode) {
    if (invalidSelection) {
      mainContent = (
        <PageState
          description="笔记 ID 必须是有效的正整数。"
          mode="error"
          title="笔记地址无效"
        />
      );
    } else if (workspace.detail.isLoading) {
      mainContent = (
        <PageState
          description="正在获取可编辑的完整 Markdown 正文。"
          mode="loading"
          title="加载笔记"
        />
      );
    } else if (workspace.detail.error || !workspace.detail.data) {
      mainContent = (
        <PageState
          actionLabel="重试"
          description={workspace.detail.error ?? "笔记详情不可用"}
          mode="error"
          onAction={workspace.retryDetail}
          title="无法编辑笔记"
        />
      );
    } else {
      mainContent = (
        <InlineNoteEditor
          categories={workspace.categories.data ?? []}
          key={workspace.detail.data.id}
          note={workspace.detail.data}
          onCancel={() => {
            setIsEditDirty(false);
            void navigate(`/notes/${workspace.detail.data?.id}`);
          }}
          onCategoryCreated={workspace.addCategory}
          onDirtyChange={setIsEditDirty}
          onSave={handleEditSave}
          onTagCreated={workspace.addTag}
          tags={workspace.tags.data ?? []}
        />
      );
    }
  } else {
    mainContent = (
      <NoteReader
        actionError={actionError}
        categoryName={categoryName}
        error={workspace.detail.error}
        hasSelection={selectedNoteId !== null}
        invalidSelection={invalidSelection}
        isLoading={workspace.detail.isLoading}
        isMutating={isMutating}
        isSettingsDisabled={taxonomyLoading || Boolean(taxonomyError)}
        note={workspace.detail.data}
        onDelete={() => setIsDeleteDialogOpen(true)}
        onEdit={() => {
          setActionError(null);
          if (selectedNoteId !== null) {
            void navigate(`/notes/${selectedNoteId}/edit`);
          }
        }}
        onRetry={workspace.retryDetail}
        onSettings={() => setIsSettingsDialogOpen(true)}
        onTogglePublish={() => void handleTogglePublish()}
        rawError={workspace.detail.rawError}
      />
    );
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
        onCreateNote={() => {
          navigateFromWorkspace("/notes/new");
        }}
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
        onSelectNote={(noteId) => {
          navigateFromWorkspace(`/notes/${noteId}`);
        }}
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
        {mainContent}
      </section>

      {isDeleteDialogOpen && workspace.detail.data ? (
        <DeleteNoteDialog
          isDeleting={isMutating}
          noteTitle={workspace.detail.data.title}
          onCancel={() => setIsDeleteDialogOpen(false)}
          onConfirm={() => void handleDelete()}
        />
      ) : null}

      {isSettingsDialogOpen && isReadMode && workspace.detail.data ? (
        <NoteSettingsDialog
          categories={workspace.categories.data ?? []}
          initialValue={{
            title: workspace.detail.data.title,
            summary: workspace.detail.data.summary,
            categoryId: workspace.detail.data.categoryId,
            tagIds: workspace.detail.data.tags.map((tag) => tag.id),
          }}
          onCancel={() => setIsSettingsDialogOpen(false)}
          onCategoryCreated={workspace.addCategory}
          onSubmit={handleSettingsSave}
          onTagCreated={workspace.addTag}
          tags={workspace.tags.data ?? []}
        />
      ) : null}
    </div>
  );
}
