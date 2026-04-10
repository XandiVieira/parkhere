import { create } from "zustand";

type Theme = "light" | "dark" | "system";

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  hydrate: () => void;
}

function applyTheme(theme: Theme) {
  if (typeof window === "undefined") return;
  const isDark =
    theme === "dark" ||
    (theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
  document.documentElement.classList.toggle("dark", isDark);
}

export const useThemeStore = create<ThemeState>((set) => ({
  theme: "system",

  setTheme: (theme) => {
    localStorage.setItem("parkhere-theme", theme);
    applyTheme(theme);
    set({ theme });
  },

  hydrate: () => {
    const stored = localStorage.getItem("parkhere-theme") as Theme | null;
    const theme = stored || "system";
    applyTheme(theme);
    set({ theme });

    // Listen for system preference changes when using "system"
    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
      const current = localStorage.getItem("parkhere-theme") as Theme | null;
      if (!current || current === "system") applyTheme("system");
    });
  },
}));
