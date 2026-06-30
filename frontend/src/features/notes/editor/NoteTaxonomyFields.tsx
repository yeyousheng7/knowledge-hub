import { CirclePlus, FolderPlus, LoaderCircle, Tag } from "lucide-react";
import { useState } from "react";

import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { createCategory, createTag } from "@/api/taxonomy";
import { apiErrorMessage } from "@/features/notes/note-display";
import { cn } from "@/shared/lib/utils";

interface NoteTaxonomyFieldsProps {
  categories: CategoryListItemResponse[];
  categoryId: number | null;
  disabled?: boolean;
  onCategoryCreated: (category: CategoryListItemResponse) => void;
  onCategoryIdChange: (categoryId: number | null) => void;
  onTagCreated: (tag: TagListItemResponse) => void;
  onTagIdsChange: (tagIds: number[]) => void;
  tagIds: number[];
  tags: TagListItemResponse[];
}

export function NoteTaxonomyFields({
  categories,
  categoryId,
  disabled = false,
  onCategoryCreated,
  onCategoryIdChange,
  onTagCreated,
  onTagIdsChange,
  tagIds,
  tags,
}: NoteTaxonomyFieldsProps) {
  const [availableCategories, setAvailableCategories] = useState(categories);
  const [availableTags, setAvailableTags] = useState(tags);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newTagName, setNewTagName] = useState("");
  const [isCreatingCategory, setIsCreatingCategory] = useState(false);
  const [isCreatingTag, setIsCreatingTag] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function toggleTag(tagId: number) {
    setError(null);

    if (tagIds.includes(tagId)) {
      onTagIdsChange(tagIds.filter((id) => id !== tagId));
      return;
    }
    if (tagIds.length >= 10) {
      setError("每篇笔记最多选择 10 个标签");
      return;
    }

    onTagIdsChange([...tagIds, tagId]);
  }

  async function handleCreateCategory() {
    const name = newCategoryName.trim();

    if (!name) {
      setError("请输入分类名称");
      return;
    }

    const existing = availableCategories.find((category) => category.name === name);
    if (existing) {
      onCategoryIdChange(existing.id);
      setNewCategoryName("");
      setError(null);
      return;
    }

    setIsCreatingCategory(true);
    setError(null);

    try {
      const created = await createCategory(name);
      setAvailableCategories((current) => [...current, created]);
      onCategoryIdChange(created.id);
      setNewCategoryName("");
      onCategoryCreated(created);
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "分类创建失败，请稍后重试"));
    } finally {
      setIsCreatingCategory(false);
    }
  }

  async function handleCreateTag() {
    const name = newTagName.trim();

    if (!name) {
      setError("请输入标签名称");
      return;
    }
    if (tagIds.length >= 10) {
      setError("每篇笔记最多选择 10 个标签");
      return;
    }

    const existing = availableTags.find((tag) => tag.name === name);
    if (existing) {
      if (!tagIds.includes(existing.id)) {
        onTagIdsChange([...tagIds, existing.id]);
      }
      setNewTagName("");
      setError(null);
      return;
    }

    setIsCreatingTag(true);
    setError(null);

    try {
      const created = await createTag(name);
      setAvailableTags((current) => [...current, created]);
      onTagIdsChange([...tagIds, created.id]);
      setNewTagName("");
      onTagCreated(created);
    } catch (requestError) {
      setError(apiErrorMessage(requestError, "标签创建失败，请稍后重试"));
    } finally {
      setIsCreatingTag(false);
    }
  }

  return (
    <div>
      <div className="grid gap-5 lg:grid-cols-2">
        <fieldset className="rounded-xl border border-slate-200 p-4" disabled={disabled}>
          <legend className="px-1 text-sm font-semibold text-slate-800">
            分类（可选）
          </legend>
          <select
            aria-label="分类"
            className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-4 focus:ring-blue-100/70"
            onChange={(event) =>
              onCategoryIdChange(event.target.value ? Number(event.target.value) : null)
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

        <fieldset className="rounded-xl border border-slate-200 p-4" disabled={disabled}>
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

      {error ? (
        <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
