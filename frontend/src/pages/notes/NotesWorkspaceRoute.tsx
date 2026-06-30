import { lazy, Suspense } from "react";

import { PageState } from "@/shared/layout/PageState";

const NotesWorkspacePage = lazy(async () => {
  const module = await import("@/pages/notes/NotesWorkspacePage");
  return { default: module.NotesWorkspacePage };
});

export function NotesWorkspaceRoute() {
  return (
    <Suspense
      fallback={
        <PageState
          description="正在准备笔记阅读工作台。"
          mode="loading"
          title="加载笔记工作台"
        />
      }
    >
      <NotesWorkspacePage />
    </Suspense>
  );
}
