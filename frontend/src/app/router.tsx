import {
  Navigate,
  createBrowserRouter,
  type RouteObject,
} from "react-router-dom";

import { RequireAuth } from "@/features/auth/RequireAuth";
import { AiWorkspacePage } from "@/pages/ai/AiWorkspacePage";
import { LoginPage } from "@/pages/login/LoginPage";
import { NotesWorkspaceRoute } from "@/pages/notes/NotesWorkspaceRoute";
import { FeedPage } from "@/pages/public/FeedPage";
import { PublicNoteDetailPage } from "@/pages/public/PublicNoteDetailPage";
import { PublicNotesPage } from "@/pages/public/PublicNotesPage";
import { PublicUserPage } from "@/pages/public/PublicUserPage";
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
          {
            path: "/public",
            element: <PublicNotesPage />,
          },
        ],
      },
      {
        path: "/feed",
        element: <FeedPage />,
      },
      {
        path: "/public/notes/:noteId",
        element: <PublicNoteDetailPage />,
      },
      {
        path: "/public/users/:username",
        element: <PublicUserPage />,
      },
    ],
  },
  {
    path: "*",
    element: <Navigate replace to="/notes" />,
  },
];

export const router = createBrowserRouter(applicationRoutes);
