"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";

type User = {
  userId: number;
  schoolId: number | null;
  role: string;
};

type AuthContextType = {
  user: User | null;
  token: string | null;
  login: (token: string, user: User) => void;
  logout: () => void;
  isLoading: boolean;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    if (typeof window === "undefined") return null;
    const storedUser = localStorage.getItem("user");
    if (!storedUser) return null;
    try {
      return JSON.parse(storedUser) as User;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState<string | null>(() => {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("token");
  });
  const isLoading = false;
  const router = useRouter();
  const pathname = usePathname();

  function login(newToken: string, newUser: User) {
    localStorage.setItem("token", newToken);
    localStorage.setItem("user", JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);

    // ✅ FIXED: Role-based redirect
    const role = newUser.role?.toUpperCase();

    switch (role) {
      case "SUPER_ADMIN":
      case "PLATFORM_ADMIN":
        router.push("/schools"); // Platform admins manage schools
        break;
      case "SCHOOL_ADMIN":
        router.push("/"); // Dashboard with school overview
        break;
      case "TEACHER":
        router.push("/attendance"); // Teachers mark attendance first
        break;
      case "ACCOUNTANT":
        router.push("/fees/collect"); // Accountants collect fees
        break;
      default:
        router.push("/"); // Fallback to dashboard
    }
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
    router.push("/login");
  }

  // Protect routes
  useEffect(() => {
    if (!isLoading) {
      const isAuthGroup = pathname?.startsWith("/login");
      if (!token && !isAuthGroup) {
        router.push("/login");
      }
    }
  }, [token, pathname, isLoading, router]);

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
