import {
  Navigate,
  createBrowserRouter,
  type RouteObject,
} from "react-router-dom";

import { RequireAuth } from "@/features/auth/RequireAuth";
import { LoginPage } from "@/pages/login/LoginPage";
import { NotesWorkspaceRoute } from "@/pages/notes/NotesWorkspaceRoute";
import { PlaceholderPage } from "@/pages/placeholders/PlaceholderPage";
import { ApplicationShell } from "@/shared/layout/ApplicationShell";

export const applicationRoutes: RouteObject[] = [
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    element: <ApplicationShell />,
    children: [
      {
        element: <RequireAuth />,
        children: [
          {
            path: "/notes",
            element: <NotesWorkspaceRoute />,
          },
          {
            path: "/notes/:noteId",
            element: <NotesWorkspaceRoute />,
          },
          {
            path: "/ai",
            element: (
              <PlaceholderPage
                description="RAG 与 Agent 工作区将在后续 AI 阶段实现。"
                title="AI 工作区"
              />
            ),
          },
        ],
      },
      {
        path: "/feed",
        element: (
          <PlaceholderPage
            description="公开内容流将在 F8 接入真实接口。"
            title="Feed"
          />
        ),
      },
      {
        path: "/public",
        element: (
          <PlaceholderPage
            description="公开笔记浏览将在 F8 实现。"
            title="公开内容"
          />
        ),
      },
    ],
  },
  {
    path: "*",
    element: <Navigate replace to="/notes" />,
  },
];

export const router = createBrowserRouter(applicationRoutes);
