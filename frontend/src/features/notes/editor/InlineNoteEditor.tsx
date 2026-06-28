import {
  AlertTriangle,
  LoaderCircle,
  Save,
  Settings2,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";

import type {
  NoteDetailResponse,
  NoteWriteRequest,
} from "@/api/note-contracts";
import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { apiErrorMessage } from "@/features/notes/note-display";
import {
  NoteSettingsDialog,
  type NoteSettingsValue,
} from "@/features/notes/editor/NoteSettingsDialog";
import { VditorMarkdownEditor } from "@/features/notes/editor/VditorMarkdownEditor";

interface InlineNoteEditorProps {
  categories: CategoryListItemResponse[];
  note: NoteDetailResponse;
  onCancel: () => void;
  onCategoryCreated: (category: CategoryListItemResponse) => void;
  onDirtyChange: (isDirty: boolean) => void;
  onSave: (request: NoteWriteRequest) => Promise<void>;
  onTagCreated: (tag: TagListItemResponse) => void;
  tags: TagListItemResponse[];
}

function sameIds(left: number[], right: number[]): boolean {
  return left.length === right.length && left.every((id, index) => id === right[index]);
}

export function InlineNoteEditor({
  categories,
  note,
  onCancel,
  onCategoryCreated,
  onDirtyChange,
  onSave,
  onTagCreated,
  tags,
}: InlineNoteEditorProps) {
  const formRef = useRef<HTMLFormElement>(null);
  const [title, setTitle] = useState(note.title);
  const [contentMd, setContentMd] = useState(note.contentMd);
  const [summary, setSummary] = useState(note.summary);
  const [categoryId, setCategoryId] = useState(note.categoryId);
  const [tagIds, setTagIds] = useState(note.tags.map((tag) => tag.id));
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isDirty = useMemo(
    () =>
      title !== note.title ||
      contentMd !== note.contentMd ||
      summary !== note.summary ||
      categoryId !== note.categoryId ||
      !sameIds(tagIds, note.tags.map((tag) => tag.id)),
    [categoryId, contentMd, note, summary, tagIds, title],
  );

  useEffect(() => {
    onDirtyChange(isDirty);
  }, [isDirty, onDirtyChange]);

  useEffect(() => {
    if (!isDirty) {
      return undefined;
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isDirty]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedTitle = title.trim();

    if (!normalizedTitle) {
      setError("请输入笔记标题");
      return;
    }
    if (normalizedTitle.length > 100) {
      setError("标题不能超过 100 个字符");
      return;
    }
    if ((summary?.length ?? 0) > 300) {
      setError("摘要不能超过 300 个字符");
      return;
    }
    if (contentMd.length > 100_000) {
      setError("正文不能超过 100000 个字符");
      return;
    }

    setError(null);
    setIsSaving(true);

    try {
      await onSave({
        title: normalizedTitle,
        contentMd,
        summary: summary?.trim() || null,
        categoryId,
        tagIds,
      });
      onDirtyChange(false);
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "保存失败，请稍后重试"));
    } finally {
      setIsSaving(false);
    }
  }

  function handleCancel() {
    if (
      isDirty &&
      !window.confirm("当前修改尚未保存，确定放弃并返回阅读态吗？")
    ) {
      return;
    }

    onDirtyChange(false);
    onCancel();
  }

  async function applySettings(value: NoteSettingsValue) {
    setTitle(value.title);
    setSummary(value.summary);
    setCategoryId(value.categoryId);
    setTagIds(value.tagIds);
    setIsSettingsOpen(false);
  }

  return (
    <div className="flex h-full min-w-0 flex-col bg-white">
      <form
        className="contents"
        noValidate
        onSubmit={handleSubmit}
        ref={formRef}
      >
        <header className="flex min-h-16 shrink-0 flex-wrap items-center justify-end gap-3 border-b border-slate-100 px-7 py-2">
          <div className="flex flex-wrap items-center justify-end gap-2">
            <button
              className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-50"
              disabled={isSaving}
              onClick={() => setIsSettingsOpen(true)}
              type="button"
            >
              <Settings2 aria-hidden="true" className="size-3.5" />
              笔记设置
            </button>
            <button
              className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-3 text-xs font-medium text-white transition hover:bg-blue-700 disabled:opacity-60"
              disabled={isSaving || !isDirty}
              type="submit"
            >
              {isSaving ? (
                <LoaderCircle aria-hidden="true" className="size-3.5 animate-spin" />
              ) : (
                <Save aria-hidden="true" className="size-3.5" />
              )}
              {isSaving ? "正在保存" : "保存"}
            </button>
            <button
              className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 text-xs font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-50"
              disabled={isSaving}
              onClick={handleCancel}
              type="button"
            >
              <X aria-hidden="true" className="size-3.5" />
              取消
            </button>
          </div>
        </header>

        <article className="min-h-0 flex-1 overflow-y-auto px-8 pb-16 pt-6 xl:px-12">
          <div className="mx-auto max-w-4xl">
            {note.moderationStatus === "TAKEN_DOWN" ? (
              <div className="mb-6 flex items-start gap-3 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
                <AlertTriangle aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                该笔记已被下架。修改后仍需管理员恢复，才会重新出现在公开页面。
              </div>
            ) : null}

            <VditorMarkdownEditor
              hideToolbar
              onChange={setContentMd}
              onSaveShortcut={() => formRef.current?.requestSubmit()}
              value={contentMd}
            />

            {error ? (
              <p className="mt-5 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
                {error}
              </p>
            ) : null}

            <p className="mt-4 text-xs text-slate-400">
              手动保存后返回阅读态。Ctrl / ⌘ + Enter 可快速保存。
            </p>
          </div>
        </article>
      </form>

      {isSettingsOpen ? (
        <NoteSettingsDialog
          categories={categories}
          initialValue={{ title, summary, categoryId, tagIds }}
          onCancel={() => setIsSettingsOpen(false)}
          onCategoryCreated={onCategoryCreated}
          onSubmit={applySettings}
          onTagCreated={onTagCreated}
          submitLabel="应用设置"
          tags={tags}
        />
      ) : null}
    </div>
  );
}
