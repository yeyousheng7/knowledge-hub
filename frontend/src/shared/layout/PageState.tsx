import {
  AlertCircle,
  FileQuestion,
  LoaderCircle,
  type LucideIcon,
} from "lucide-react";

import { cn } from "@/shared/lib/utils";

type PageStateMode = "loading" | "error" | "empty";

interface PageStateProps {
  mode: PageStateMode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  compact?: boolean;
}

const icons: Record<PageStateMode, LucideIcon> = {
  loading: LoaderCircle,
  error: AlertCircle,
  empty: FileQuestion,
};

export function PageState({
  mode,
  title,
  description,
  actionLabel,
  onAction,
  compact = false,
}: PageStateProps) {
  const Icon = icons[mode];

  return (
    <div
      className={cn(
        "grid place-items-center px-6 text-center",
        compact ? "min-h-52" : "min-h-full",
      )}
    >
      <div className="max-w-sm">
        <span
          className={cn(
            "mx-auto grid size-11 place-items-center rounded-xl",
            mode === "error"
              ? "bg-red-50 text-red-600"
              : "bg-blue-50 text-primary",
          )}
        >
          <Icon
            aria-hidden="true"
            className={cn("size-5", mode === "loading" && "animate-spin")}
          />
        </span>
        <h2 className="mt-4 text-base font-semibold text-slate-900">{title}</h2>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          {description}
        </p>
        {actionLabel && onAction ? (
          <button
            className="mt-5 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:border-blue-200 hover:text-primary"
            onClick={onAction}
            type="button"
          >
            {actionLabel}
          </button>
        ) : null}
      </div>
    </div>
  );
}
