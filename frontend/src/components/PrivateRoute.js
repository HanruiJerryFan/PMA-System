import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { hasAnyAuthority } from "../utils/authorities";

function PrivateRoute({ children, requiredAuthorities = [] }) {
  const location = useLocation();
  const { currentUser, loggedIn, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!loggedIn || !currentUser) {
    return <Navigate to="/login" />;
  }

  if (currentUser?.forcePasswordChange) {
    return <Navigate to="/change-password" replace />;
  }

  const authorized = hasAnyAuthority(currentUser, requiredAuthorities);
  return authorized ? children : (
    <Navigate
      to="/permission-denied"
      replace
      state={{
        from: location.pathname,
        reason: "You do not have permission to open this page.",
      }}
    />
  );
}

export default PrivateRoute;
