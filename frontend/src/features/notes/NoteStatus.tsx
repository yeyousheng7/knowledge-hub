import { Globe2, LockKeyhole } from "lucide-react";

import type {
  NoteModerationStatus,
  NoteVisibility,
} from "@/api/note-contracts";
import { cn } from "@/shared/lib/utils";

interface NoteStatusProps {
  visibility: NoteVisibility;
  moderationStatus: NoteModerationStatus;
  compact?: boolean;
}

export function NoteStatus({
  visibility,
  moderationStatus,
  compact = false,
}: NoteStatusProps) {
  const isPublic = visibility === "PUBLIC";
  const VisibilityIcon = isPublic ? Globe2 : LockKeyhole;
  const visibilityLabel = isPublic ? "公开" : "私有";

  return (
    <span className="inline-flex items-center gap-1.5">
      <span
        aria-label={visibilityLabel}
        className={cn(
          "inline-flex items-center gap-1.5 text-slate-500",
          !compact && "rounded-full bg-slate-100 px-2.5 py-1 text-xs",
        )}
      >
        <VisibilityIcon aria-hidden="true" className="size-3.5" strokeWidth={1.8} />
        {compact ? <span className="sr-only">{visibilityLabel}</span> : visibilityLabel}
      </span>
      {moderationStatus === "TAKEN_DOWN" ? (
        <span className="rounded-full bg-red-50 px-2.5 py-1 text-xs font-medium text-red-700">
          已下架
        </span>
      ) : null}
    </span>
  );
}
