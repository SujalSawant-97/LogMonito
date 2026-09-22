import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiClient } from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('logmonito_user');
    try {
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const res = await apiClient.login({ email, password });
      apiClient.setToken(res.token);
      const userData = { userId: res.userId, email: res.email, name: res.name };
      localStorage.setItem('logmonito_user', JSON.stringify(userData));
      setUser(userData);
      return res;
    } finally {
      setLoading(false);
    }
  };

  const register = async (email, password, name) => {
    setLoading(true);
    try {
      const res = await apiClient.register({ email, password, name });
      apiClient.setToken(res.token);
      const userData = { userId: res.userId, email: res.email, name: res.name };
      localStorage.setItem('logmonito_user', JSON.stringify(userData));
      setUser(userData);
      return res;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    apiClient.setToken(null);
    localStorage.removeItem('logmonito_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
