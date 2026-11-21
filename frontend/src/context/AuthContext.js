import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { getCurrentUser } from "../api/auth";

const DEFAULT_AUTH_CONTEXT = {
  currentUser: null,
  loggedIn: false,
  loading: false,
  refreshCurrentUser: async () => null,
  setAuthState: () => {},
  clearAuth: () => {},
};

const AuthContext = createContext(DEFAULT_AUTH_CONTEXT);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshCurrentUser = useCallback(async () => {
    try {
      const user = await getCurrentUser();
      setCurrentUser(user || null);
      return user || null;
    } catch (error) {
      setCurrentUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshCurrentUser();
  }, [refreshCurrentUser]);

  const setAuthState = useCallback((nextLoggedIn, user = null) => {
    setCurrentUser(nextLoggedIn ? user : null);
    setLoading(false);
  }, []);

  const clearAuth = useCallback(() => {
    setCurrentUser(null);
    setLoading(false);
  }, []);

  const value = useMemo(
    () => ({
      currentUser,
      loggedIn: Boolean(currentUser),
      loading,
      refreshCurrentUser,
      setAuthState,
      clearAuth,
    }),
    [clearAuth, currentUser, loading, refreshCurrentUser, setAuthState]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
