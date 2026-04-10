"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/auth";
import { useThemeStore } from "@/stores/theme";
import { t } from "@/lib/i18n";
import { apiBaseUrl } from "@/lib/utils";

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const { theme, setTheme } = useThemeStore();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setMobileOpen(false);
    router.push("/");
  };

  const publicLinks = [
    { href: "/", label: t("nav.map") },
  ];

  const authLinks = [
    { href: "/favorites", label: t("nav.favorites") },
    { href: "/my-spots", label: t("nav.mySpots") },
  ];

  const sharedLinks = [
    { href: "/leaderboards", label: t("nav.leaderboards") },
  ];

  const profileLinks = [
    { href: "/profile", label: t("nav.profile") },
    ...(user?.role === "ADMIN" ? [{ href: "/admin", label: t("nav.admin") }] : []),
  ];

  const allLinks = isAuthenticated
    ? [...publicLinks, ...authLinks, ...sharedLinks, ...profileLinks]
    : [...publicLinks, ...sharedLinks];

  return (
    <nav className="sticky top-0 z-50 border-b border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2 text-xl font-bold text-blue-600">
          <svg className="h-7 w-7" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z" />
          </svg>
          ParkHere
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          {allLinks.map((link) => (
            <Link key={link.href} href={link.href} className="text-sm font-medium text-gray-700 hover:text-blue-600 dark:text-gray-200 dark:hover:text-blue-400">
              {link.label}
            </Link>
          ))}

          <button
            onClick={() => setTheme(theme === "light" ? "dark" : theme === "dark" ? "system" : "light")}
            className="rounded-md p-1.5 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
            title={theme === "light" ? t("nav.theme.light") : theme === "dark" ? t("nav.theme.dark") : t("nav.theme.system")}
          >
            {theme === "dark" ? (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
            ) : theme === "light" ? (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
            ) : (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
            )}
          </button>

          {isAuthenticated ? (
            <>
              <div className="flex items-center gap-2">
                {user?.profilePicUrl ? (
                  <img src={`${apiBaseUrl()}${user.profilePicUrl}`} alt={user?.name || ""} className="h-7 w-7 rounded-full object-cover" />
                ) : (
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-100 text-xs font-bold text-blue-700">
                    {(user?.nickname || user?.name || "?").charAt(0).toUpperCase()}
                  </div>
                )}
                <span className="text-sm text-gray-500 dark:text-gray-300">{user?.nickname || user?.name}</span>
              </div>
              <button
                onClick={handleLogout}
                className="rounded-md bg-gray-100 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
              >
                {t("nav.logout")}
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="text-sm font-medium text-gray-700 hover:text-blue-600">{t("nav.login")}</Link>
              <Link href="/register" className="rounded-md bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700">{t("nav.register")}</Link>
            </>
          )}
        </div>

        <button onClick={() => setMobileOpen(!mobileOpen)} className="flex items-center md:hidden" aria-label="Toggle menu">
          <svg className="h-6 w-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            {mobileOpen
              ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />}
          </svg>
        </button>
      </div>

      {mobileOpen && (
        <div className="border-t border-gray-200 bg-white px-4 pb-4 dark:border-gray-700 dark:bg-gray-800 md:hidden">
          <div className="flex flex-col gap-3 pt-3">
            {allLinks.map((link) => (
              <Link key={link.href} href={link.href} className="text-sm font-medium text-gray-700 dark:text-gray-200" onClick={() => setMobileOpen(false)}>
                {link.label}
              </Link>
            ))}
            {isAuthenticated ? (
              <div className="border-t border-gray-100 pt-3">
                <span className="text-sm text-gray-500">{user?.name}</span>
                <button onClick={handleLogout} className="mt-2 w-full rounded-md bg-gray-100 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-200">
                  {t("nav.logout")}
                </button>
              </div>
            ) : (
              <div className="border-t border-gray-100 pt-3 flex flex-col gap-2">
                <Link href="/login" className="text-sm font-medium text-gray-700" onClick={() => setMobileOpen(false)}>{t("nav.login")}</Link>
                <Link href="/register" className="text-sm font-medium text-blue-600" onClick={() => setMobileOpen(false)}>{t("nav.register")}</Link>
              </div>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
