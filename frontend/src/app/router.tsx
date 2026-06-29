import {
  Navigate,
  createBrowserRouter,
  type RouteObject,
} from "react-router-dom";

import { RequireAuth } from "@/features/auth/RequireAuth";
import { AiWorkspacePage } from "@/pages/ai/AiWorkspacePage";
import { LoginPage } from "@/pages/login/LoginPage";
import { NotesWorkspaceRoute } from "@/pages/notes/NotesWorkspaceRoute";
import { PlaceholderPage } from "@/pages/placeholders/PlaceholderPage";
import { RegisterPage } from "@/pages/register/RegisterPage";
import { ApplicationShell } from "@/shared/layout/ApplicationShell";

export const applicationRoutes: RouteObject[] = [
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/register",
    element: <RegisterPage />,
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
            path: "/notes/new",
            element: <NotesWorkspaceRoute />,
          },
          {
            path: "/notes/:noteId",
            element: <NotesWorkspaceRoute />,
          },
          {
            path: "/notes/:noteId/edit",
            element: <NotesWorkspaceRoute />,
          },
          {
            path: "/ai",
            element: <AiWorkspacePage />,
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
