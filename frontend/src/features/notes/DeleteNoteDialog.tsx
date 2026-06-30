import { LoaderCircle, Trash2 } from "lucide-react";

interface DeleteNoteDialogProps {
  noteTitle: string;
  isDeleting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

export function DeleteNoteDialog({
  noteTitle,
  isDeleting,
  onCancel,
  onConfirm,
}: DeleteNoteDialogProps) {
  return (
    <div
      aria-labelledby="delete-note-title"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/30 px-4"
      role="dialog"
    >
      <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
        <span className="grid size-11 place-items-center rounded-xl bg-red-50 text-red-600">
          <Trash2 aria-hidden="true" className="size-5" />
        </span>
        <h2 className="mt-4 text-lg font-semibold text-slate-950" id="delete-note-title">
          确认删除这篇笔记？
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-500">
          “{noteTitle}”删除后不会出现在笔记列表中，当前版本不提供回收站入口。
        </p>
        <div className="mt-6 flex justify-end gap-3">
          <button
            className="h-10 rounded-lg border border-slate-200 px-4 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-50"
            disabled={isDeleting}
            onClick={onCancel}
            type="button"
          >
            取消
          </button>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-red-600 px-4 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
            disabled={isDeleting}
            onClick={onConfirm}
            type="button"
          >
            {isDeleting ? (
              <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
            ) : null}
            {isDeleting ? "正在删除" : "确认删除"}
          </button>
        </div>
      </section>
    </div>
  );
}
