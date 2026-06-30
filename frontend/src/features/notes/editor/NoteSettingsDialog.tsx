import { LoaderCircle, Settings2, X } from "lucide-react";
import { useState, type FormEvent } from "react";

import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { apiErrorMessage } from "@/features/notes/note-display";
import { NoteTaxonomyFields } from "@/features/notes/editor/NoteTaxonomyFields";

export interface NoteSettingsValue {
  title: string;
  summary: string | null;
  categoryId: number | null;
  tagIds: number[];
}

interface NoteSettingsDialogProps {
  categories: CategoryListItemResponse[];
  initialValue: NoteSettingsValue;
  onCancel: () => void;
  onCategoryCreated: (category: CategoryListItemResponse) => void;
  onSubmit: (value: NoteSettingsValue) => Promise<void>;
  onTagCreated: (tag: TagListItemResponse) => void;
  submitLabel?: string;
  tags: TagListItemResponse[];
}

export function NoteSettingsDialog({
  categories,
  initialValue,
  onCancel,
  onCategoryCreated,
  onSubmit,
  onTagCreated,
  submitLabel = "保存设置",
  tags,
}: NoteSettingsDialogProps) {
  const [title, setTitle] = useState(initialValue.title);
  const [summary, setSummary] = useState(initialValue.summary ?? "");
  const [categoryId, setCategoryId] = useState(initialValue.categoryId);
  const [tagIds, setTagIds] = useState(initialValue.tagIds);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedTitle = title.trim();
    const normalizedSummary = summary.trim();

    if (!normalizedTitle) {
      setError("请输入文件名");
      return;
    }
    if (normalizedTitle.length > 100) {
      setError("文件名不能超过 100 个字符");
      return;
    }
    if (summary.length > 300) {
      setError("摘要不能超过 300 个字符");
      return;
    }
    if (tagIds.length > 10) {
      setError("每篇笔记最多选择 10 个标签");
      return;
    }

    setError(null);
    setIsSaving(true);

    try {
      await onSubmit({
        title: normalizedTitle,
        summary: normalizedSummary || null,
        categoryId,
        tagIds,
      });
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "设置保存失败，请稍后重试"));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div
      aria-labelledby="note-settings-title"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/30 px-4 py-8"
      role="dialog"
    >
      <form
        className="max-h-full w-full max-w-3xl overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl"
        noValidate
        onSubmit={handleSubmit}
      >
        <header className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-600">
              <Settings2 aria-hidden="true" className="size-5" />
            </span>
            <div>
              <h2 className="text-lg font-semibold text-slate-950" id="note-settings-title">
                笔记设置
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                管理文件名、摘要、分类和标签；正文在阅读页的编辑态中修改。
              </p>
            </div>
          </div>
          <button
            aria-label="关闭笔记设置"
            className="grid size-9 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700"
            disabled={isSaving}
            onClick={onCancel}
            type="button"
          >
            <X aria-hidden="true" className="size-4" />
          </button>
        </header>

        <label className="mt-6 block">
          <span className="text-sm font-semibold text-slate-800">文件名</span>
          <input
            aria-label="文件名"
            className="mt-2 h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/70"
            disabled={isSaving}
            maxLength={100}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="请输入文件名"
            value={title}
          />
          <span className="mt-1 block text-right text-xs text-slate-400">
            {title.length}/100
          </span>
        </label>

        <label className="mt-5 block">
          <span className="text-sm font-semibold text-slate-800">摘要（可选）</span>
          <textarea
            className="mt-2 min-h-24 w-full resize-y rounded-xl border border-slate-200 px-4 py-3 text-sm leading-6 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/70"
            disabled={isSaving}
            maxLength={300}
            onChange={(event) => setSummary(event.target.value)}
            placeholder="简要描述这篇笔记的内容；留空时由后端从正文生成"
            value={summary}
          />
          <span className="mt-1 block text-right text-xs text-slate-400">
            {summary.length}/300
          </span>
        </label>

        <div className="mt-5">
          <NoteTaxonomyFields
            categories={categories}
            categoryId={categoryId}
            disabled={isSaving}
            onCategoryCreated={onCategoryCreated}
            onCategoryIdChange={setCategoryId}
            onTagCreated={onTagCreated}
            onTagIdsChange={setTagIds}
            tagIds={tagIds}
            tags={tags}
          />
        </div>

        {error ? (
          <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {error}
          </p>
        ) : null}

        <footer className="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-5">
          <button
            className="h-10 rounded-lg border border-slate-200 px-4 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-50"
            disabled={isSaving}
            onClick={onCancel}
            type="button"
          >
            取消
          </button>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
            disabled={isSaving}
            type="submit"
          >
            {isSaving ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : null}
            {isSaving ? "正在保存" : submitLabel}
          </button>
        </footer>
      </form>
    </div>
  );
}
