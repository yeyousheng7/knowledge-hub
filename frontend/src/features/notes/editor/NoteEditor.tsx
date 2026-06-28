import {
  CirclePlus,
  FolderPlus,
  Info,
  LoaderCircle,
  Save,
  Tag,
  X,
} from "lucide-react";
import { useRef, useState, type FormEvent } from "react";

import type {
  NoteDetailResponse,
  NoteWriteRequest,
} from "@/api/note-contracts";
import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { createCategory, createTag } from "@/api/taxonomy";
import { apiErrorMessage } from "@/features/notes/note-display";
import { VditorMarkdownEditor } from "@/features/notes/editor/VditorMarkdownEditor";
import { cn } from "@/shared/lib/utils";

interface NoteEditorProps {
  mode: "create" | "edit";
  note?: NoteDetailResponse;
  categories: CategoryListItemResponse[];
  tags: TagListItemResponse[];
  onCancel: () => void;
  onCategoryCreated: (category: CategoryListItemResponse) => void;
  onSave: (request: NoteWriteRequest) => Promise<void>;
  onTagCreated: (tag: TagListItemResponse) => void;
}

export function NoteEditor({
  mode,
  note,
  categories,
  tags,
  onCancel,
  onCategoryCreated,
  onSave,
  onTagCreated,
}: NoteEditorProps) {
  const formRef = useRef<HTMLFormElement>(null);
  const [title, setTitle] = useState(note?.title ?? "");
  const [summary, setSummary] = useState(note?.summary ?? "");
  const [contentMd, setContentMd] = useState(note?.contentMd ?? "");
  const [categoryId, setCategoryId] = useState<number | null>(
    note?.categoryId ?? null,
  );
  const [tagIds, setTagIds] = useState<number[]>(
    note?.tags.map((tag) => tag.id) ?? [],
  );
  const [availableCategories, setAvailableCategories] = useState(categories);
  const [availableTags, setAvailableTags] = useState(tags);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newTagName, setNewTagName] = useState("");
  const [isCreatingCategory, setIsCreatingCategory] = useState(false);
  const [isCreatingTag, setIsCreatingTag] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [taxonomyError, setTaxonomyError] = useState<string | null>(null);

  function toggleTag(tagId: number) {
    setFormError(null);
    setTagIds((current) => {
      if (current.includes(tagId)) {
        return current.filter((id) => id !== tagId);
      }
      if (current.length >= 10) {
        setFormError("每篇笔记最多选择 10 个标签");
        return current;
      }

      return [...current, tagId];
    });
  }

  async function handleCreateCategory() {
    const name = newCategoryName.trim();

    if (!name) {
      setTaxonomyError("请输入分类名称");
      return;
    }

    const existing = availableCategories.find((category) => category.name === name);
    if (existing) {
      setCategoryId(existing.id);
      setNewCategoryName("");
      setTaxonomyError(null);
      return;
    }

    setIsCreatingCategory(true);
    setTaxonomyError(null);

    try {
      const created = await createCategory(name);
      setAvailableCategories((current) => [...current, created]);
      setCategoryId(created.id);
      setNewCategoryName("");
      onCategoryCreated(created);
    } catch (error) {
      setTaxonomyError(apiErrorMessage(error, "分类创建失败，请稍后重试"));
    } finally {
      setIsCreatingCategory(false);
    }
  }

  async function handleCreateTag() {
    const name = newTagName.trim();

    if (!name) {
      setTaxonomyError("请输入标签名称");
      return;
    }
    if (tagIds.length >= 10) {
      setFormError("每篇笔记最多选择 10 个标签");
      return;
    }

    const existing = availableTags.find((tag) => tag.name === name);
    if (existing) {
      setTagIds((current) =>
        current.includes(existing.id) ? current : [...current, existing.id],
      );
      setNewTagName("");
      setTaxonomyError(null);
      return;
    }

    setIsCreatingTag(true);
    setTaxonomyError(null);

    try {
      const created = await createTag(name);
      setAvailableTags((current) => [...current, created]);
      setTagIds((current) => [...current, created.id]);
      setNewTagName("");
      onTagCreated(created);
    } catch (error) {
      setTaxonomyError(apiErrorMessage(error, "标签创建失败，请稍后重试"));
    } finally {
      setIsCreatingTag(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedTitle = title.trim();
    const normalizedSummary = summary.trim();

    if (!normalizedTitle) {
      setFormError("请输入笔记标题");
      return;
    }
    if (normalizedTitle.length > 100) {
      setFormError("标题不能超过 100 个字符");
      return;
    }
    if (summary.length > 300) {
      setFormError("摘要不能超过 300 个字符");
      return;
    }
    if (contentMd.length > 100_000) {
      setFormError("正文不能超过 100000 个字符");
      return;
    }
    if (tagIds.length > 10) {
      setFormError("每篇笔记最多选择 10 个标签");
      return;
    }

    setFormError(null);
    setIsSaving(true);

    try {
      await onSave({
        title: normalizedTitle,
        contentMd,
        summary: normalizedSummary || null,
        categoryId,
        tagIds,
      });
    } catch (error) {
      setFormError(apiErrorMessage(error, "保存失败，请稍后重试"));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="h-full overflow-y-auto bg-white">
      <form
        className="mx-auto w-full max-w-5xl px-8 pb-12 pt-9 xl:px-12"
        noValidate
        onSubmit={handleSubmit}
        ref={formRef}
      >
        <header className="mb-8 flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium text-blue-600">
              {mode === "create" ? "我的笔记 / 新建" : "我的笔记 / 编辑"}
            </p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
              {mode === "create" ? "新建笔记" : "编辑笔记"}
            </h1>
          </div>
          <button
            aria-label="取消编辑"
            className="grid size-10 place-items-center rounded-lg border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-900"
            onClick={onCancel}
            type="button"
          >
            <X aria-hidden="true" className="size-4" />
          </button>
        </header>

        <div className="space-y-6">
          <label className="block">
            <span className="text-sm font-semibold text-slate-800">
              标题 <span className="text-red-500">*</span>
            </span>
            <input
              className="mt-2 h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/70"
              maxLength={100}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="请输入笔记标题"
              value={title}
            />
            <span className="mt-1 block text-right text-xs text-slate-400">
              {title.length}/100
            </span>
          </label>

          <label className="block">
            <span className="text-sm font-semibold text-slate-800">摘要（可选）</span>
            <textarea
              className="mt-2 min-h-24 w-full resize-y rounded-xl border border-slate-200 px-4 py-3 text-sm leading-6 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100/70"
              maxLength={300}
              onChange={(event) => setSummary(event.target.value)}
              placeholder="简要描述这篇笔记的内容；留空时由后端从正文生成"
              value={summary}
            />
            <span className="mt-1 block text-right text-xs text-slate-400">
              {summary.length}/300
            </span>
          </label>

          <div className="grid gap-5 lg:grid-cols-2">
            <fieldset className="rounded-xl border border-slate-200 p-4">
              <legend className="px-1 text-sm font-semibold text-slate-800">
                分类（可选）
              </legend>
              <select
                aria-label="分类"
                className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100/70"
                onChange={(event) =>
                  setCategoryId(event.target.value ? Number(event.target.value) : null)
                }
                value={categoryId ?? ""}
              >
                <option value="">未分类</option>
                {availableCategories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              <div className="mt-3 flex gap-2">
                <input
                  aria-label="新分类名称"
                  className="h-9 min-w-0 flex-1 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400"
                  maxLength={30}
                  onChange={(event) => setNewCategoryName(event.target.value)}
                  placeholder="新分类名称"
                  value={newCategoryName}
                />
                <button
                  className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-blue-200 px-3 text-xs font-medium text-blue-600 transition hover:bg-blue-50 disabled:opacity-50"
                  disabled={isCreatingCategory}
                  onClick={() => void handleCreateCategory()}
                  type="button"
                >
                  {isCreatingCategory ? (
                    <LoaderCircle aria-hidden="true" className="size-3.5 animate-spin" />
                  ) : (
                    <FolderPlus aria-hidden="true" className="size-3.5" />
                  )}
                  新建
                </button>
              </div>
            </fieldset>

            <fieldset className="rounded-xl border border-slate-200 p-4">
              <legend className="px-1 text-sm font-semibold text-slate-800">
                标签（最多 10 个）
              </legend>
              <div className="flex min-h-10 flex-wrap gap-2">
                {availableTags.length ? (
                  availableTags.map((tag) => {
                    const isSelected = tagIds.includes(tag.id);

                    return (
                      <button
                        aria-pressed={isSelected}
                        className={cn(
                          "inline-flex h-8 items-center gap-1.5 rounded-full border px-3 text-xs transition",
                          isSelected
                            ? "border-blue-200 bg-blue-50 text-blue-700"
                            : "border-slate-200 text-slate-500 hover:border-slate-300",
                        )}
                        key={tag.id}
                        onClick={() => toggleTag(tag.id)}
                        type="button"
                      >
                        <Tag aria-hidden="true" className="size-3" />
                        {tag.name}
                      </button>
                    );
                  })
                ) : (
                  <p className="text-xs text-slate-400">还没有标签</p>
                )}
              </div>
              <div className="mt-3 flex gap-2">
                <input
                  aria-label="新标签名称"
                  className="h-9 min-w-0 flex-1 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400"
                  maxLength={30}
                  onChange={(event) => setNewTagName(event.target.value)}
                  placeholder="输入新标签"
                  value={newTagName}
                />
                <button
                  className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-blue-200 px-3 text-xs font-medium text-blue-600 transition hover:bg-blue-50 disabled:opacity-50"
                  disabled={isCreatingTag}
                  onClick={() => void handleCreateTag()}
                  type="button"
                >
                  {isCreatingTag ? (
                    <LoaderCircle aria-hidden="true" className="size-3.5 animate-spin" />
                  ) : (
                    <CirclePlus aria-hidden="true" className="size-3.5" />
                  )}
                  添加
                </button>
              </div>
            </fieldset>
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between gap-3">
              <label className="text-sm font-semibold text-slate-800">
                Markdown 正文
              </label>
              <span className="text-xs text-slate-400">
                {contentMd.length}/100000
              </span>
            </div>
            <VditorMarkdownEditor
              onChange={setContentMd}
              onSaveShortcut={() => formRef.current?.requestSubmit()}
              value={contentMd}
            />
          </div>

          <div className="flex items-start gap-3 rounded-xl border border-blue-100 bg-blue-50/60 px-4 py-3 text-sm text-slate-600">
            <Info aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-blue-600" />
            <p>
              正文始终以原始 Markdown 保存。当前不启用图片上传、自动保存或草稿状态；请使用下方按钮手动保存。
            </p>
          </div>

          {taxonomyError ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {taxonomyError}
            </p>
          ) : null}
          {formError ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {formError}
            </p>
          ) : null}

          <footer className="flex flex-wrap items-center gap-3 border-t border-slate-100 pt-6">
            <button
              className="inline-flex h-11 items-center gap-2 rounded-lg bg-blue-600 px-5 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isSaving}
              type="submit"
            >
              {isSaving ? (
                <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
              ) : (
                <Save aria-hidden="true" className="size-4" />
              )}
              {isSaving
                ? "正在保存"
                : mode === "create"
                  ? "创建并继续编辑"
                  : "保存更改"}
            </button>
            <button
              className="h-11 rounded-lg border border-slate-200 px-5 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
              onClick={onCancel}
              type="button"
            >
              取消
            </button>
            <span className="text-xs text-slate-400">Ctrl / ⌘ + Enter 保存</span>
          </footer>
        </div>
      </form>
    </div>
  );
}
