/**
 * Authentication Store (Zustand)
 *
 * Think of this like a Java singleton service that holds the logged-in user's state.
 * Any component can read from it with: const { user, isAuthenticated } = useAuthStore()
 * Any component can call actions: const { login, logout } = useAuthStore()
 *
 * The state is persisted to localStorage so it survives page refreshes.
 * The `hydrate()` function restores state from localStorage on app startup.
 */
import { create } from "zustand";
import type { UserResponse } from "@/types/api";

interface AuthState {
  // Current state
  user: UserResponse | null;
  token: string | null;
  isAuthenticated: boolean;

  // Actions (like Java service methods)
  login: (token: string, user: UserResponse) => void;
  logout: () => void;
  hydrate: () => void; // Restores state from localStorage on page load
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  login: (token, user) => {
    // Save to localStorage (persists across page refreshes)
    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(user));
    // Update in-memory state (triggers re-render of all components using this store)
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    set({ token: null, user: null, isAuthenticated: false });
  },

  hydrate: () => {
    // Called once on app startup (from AuthHydrator component) to restore saved session
    const token = localStorage.getItem("token");
    const userStr = localStorage.getItem("user");
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr) as UserResponse;
        set({ token, user, isAuthenticated: true });
      } catch {
        // Corrupted data — clear it
        localStorage.removeItem("token");
        localStorage.removeItem("user");
      }
    }
  },
}));
