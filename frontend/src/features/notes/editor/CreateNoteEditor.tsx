import { Info, LoaderCircle, Save, X } from "lucide-react";
import { useRef, useState, type FormEvent } from "react";

import type { NoteWriteRequest } from "@/api/note-contracts";
import type {
  CategoryListItemResponse,
  TagListItemResponse,
} from "@/api/taxonomy-contracts";
import { apiErrorMessage } from "@/features/notes/note-display";
import { NoteTaxonomyFields } from "@/features/notes/editor/NoteTaxonomyFields";
import { VditorMarkdownEditor } from "@/features/notes/editor/VditorMarkdownEditor";

interface CreateNoteEditorProps {
  categories: CategoryListItemResponse[];
  onCancel: () => void;
  onCategoryCreated: (category: CategoryListItemResponse) => void;
  onCreate: (request: NoteWriteRequest) => Promise<void>;
  onTagCreated: (tag: TagListItemResponse) => void;
  tags: TagListItemResponse[];
}

export function CreateNoteEditor({
  categories,
  onCancel,
  onCategoryCreated,
  onCreate,
  onTagCreated,
  tags,
}: CreateNoteEditorProps) {
  const formRef = useRef<HTMLFormElement>(null);
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [contentMd, setContentMd] = useState("");
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [tagIds, setTagIds] = useState<number[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

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

    setFormError(null);
    setIsSaving(true);

    try {
      await onCreate({
        title: normalizedTitle,
        contentMd,
        summary: normalizedSummary || null,
        categoryId,
        tagIds,
      });
    } catch (error) {
      setFormError(apiErrorMessage(error, "创建失败，请稍后重试"));
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
            <p className="text-xs font-medium text-blue-600">我的笔记 / 新建</p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
              新建笔记
            </h1>
          </div>
          <button
            aria-label="取消创建"
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
              正文始终以原始 Markdown 保存。创建成功后默认进入阅读态，后续可在阅读页原位编辑。
            </p>
          </div>

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
              {isSaving ? "正在创建" : "创建笔记"}
            </button>
            <button
              className="h-11 rounded-lg border border-slate-200 px-5 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
              onClick={onCancel}
              type="button"
            >
              取消
            </button>
            <span className="text-xs text-slate-400">Ctrl / ⌘ + Enter 创建</span>
          </footer>
        </div>
      </form>
    </div>
  );
}
