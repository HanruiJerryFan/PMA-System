import React, { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getCurrentUser } from "../api/auth";
import { hasAnyAuthority } from "../utils/authorities";

function PrivateRoute({ children, requiredAuthorities = [] }) {
  const location = useLocation();
  const [authenticated, setAuthenticated] = useState(null);
  const [authorized, setAuthorized] = useState(true);
  const [forcePasswordChange, setForcePasswordChange] = useState(false);

  useEffect(() => {
    async function checkAuth() {
      try {
        const user = await getCurrentUser();
        setAuthenticated(!!user);
        setForcePasswordChange(Boolean(user?.forcePasswordChange));
        setAuthorized(hasAnyAuthority(user, requiredAuthorities));
      } catch (error) {
        setAuthenticated(false);
        setAuthorized(false);
        setForcePasswordChange(false);
      }
    }

    checkAuth();
  }, [requiredAuthorities]);

  if (authenticated === null) {
    return <div>Loading...</div>;
  }

  if (!authenticated) {
    return <Navigate to="/login" />;
  }

  if (forcePasswordChange) {
    return <Navigate to="/change-password" replace />;
  }

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
