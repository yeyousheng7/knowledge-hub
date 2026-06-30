import { Navigate, Outlet, useLocation } from "react-router-dom";

import { AuthStatusPanel } from "@/features/auth/AuthStatusPanel";
import { useAuth } from "@/features/auth/auth-context";

export function RequireAuth() {
  const auth = useAuth();
  const location = useLocation();

  if (auth.status === "loading") {
    return <AuthStatusPanel mode="loading" />;
  }

  if (auth.status === "error") {
    return (
      <AuthStatusPanel
        mode="error"
        message={auth.restoreError}
        onDiscard={auth.discardSession}
        onRetry={auth.retrySession}
      />
    );
  }

  if (auth.status === "anonymous") {
    const from = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate replace state={{ from }} to="/login" />;
  }

  return <Outlet />;
}
