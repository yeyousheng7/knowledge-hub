import { Navigate, createBrowserRouter } from "react-router-dom";

import { RequireAuth } from "@/features/auth/RequireAuth";
import { LoginPage } from "@/pages/login/LoginPage";
import { NotesPlaceholderPage } from "@/pages/notes/NotesPlaceholderPage";

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    element: <RequireAuth />,
    children: [
      {
        path: "/notes",
        element: <NotesPlaceholderPage />,
      },
    ],
  },
  {
    path: "*",
    element: <Navigate replace to="/notes" />,
  },
]);
