/**
 * ================================================================
 *  AuthContext – Global Authentication State
 *
 *  Provides user info + auth actions to all React components via:
 *    const { user, login, logout, isAuthenticated } = useAuth();
 *
 *  State is initialized from localStorage on app start,
 *  so the user stays logged in across page refreshes.
 * ================================================================
 */
import React, { createContext, useContext, useState, useCallback } from 'react';
import { authApi } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Initialize from localStorage — persists across refreshes
  const [user, setUser] = useState(() => authApi.getCurrentUser());
  const [loading, setLoading] = useState(false);
  const [error, setError]   = useState(null);

  /**
   * Logs in with username + password.
   * On success, stores tokens and sets user state.
   */
  const login = useCallback(async (username, password) => {
    setLoading(true);
    setError(null);
    try {
      const data = await authApi.login(username, password);
      setUser({
        username:   data.username,
        fullName:   data.fullName,
        role:       data.role,
        branchCode: data.branchCode,
        employeeId: data.employeeId,
        email:      data.email,
      });
      return data;
    } catch (err) {
      const message = err.response?.data?.message || 'Login failed. Please try again.';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  /** Clears all auth state and redirects to login. */
  const logout = useCallback(() => {
    setUser(null);
    authApi.logout();
  }, []);

  /** Helper: checks if current user has a given role. */
  const hasRole = useCallback((role) => {
    return user?.role === `ROLE_${role}` || user?.role === role;
  }, [user]);

  /** Helper: checks if current user has any of the given roles. */
  const hasAnyRole = useCallback((...roles) => {
    return roles.some(r => hasRole(r));
  }, [hasRole]);

  const value = {
    user,
    loading,
    error,
    login,
    logout,
    hasRole,
    hasAnyRole,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** Hook to consume auth context. Must be used inside <AuthProvider>. */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
