"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setMobileOpen(false);
    router.push("/");
  };

  const publicLinks = [
    { href: "/", label: t("nav.map") },
    { href: "/leaderboards", label: t("nav.leaderboards") },
  ];

  const authLinks = [
    { href: "/my-spots", label: t("nav.mySpots") },
    { href: "/favorites", label: t("nav.favorites") },
    { href: "/profile", label: t("nav.profile") },
    ...(user?.role === "ADMIN" ? [{ href: "/admin", label: "Admin" }] : []),
  ];

  const allLinks = isAuthenticated ? [...publicLinks, ...authLinks] : publicLinks;

  return (
    <nav className="sticky top-0 z-50 border-b border-gray-200 bg-white shadow-sm">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2 text-xl font-bold text-blue-600">
          <svg className="h-7 w-7" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z" />
          </svg>
          ParkHere
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          {allLinks.map((link) => (
            <Link key={link.href} href={link.href} className="text-sm font-medium text-gray-700 hover:text-blue-600">
              {link.label}
            </Link>
          ))}

          {isAuthenticated ? (
            <>
              <div className="flex items-center gap-2">
                {user?.profilePicUrl ? (
                  <img src={`${(process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080")}${user.profilePicUrl}`} alt="" className="h-7 w-7 rounded-full object-cover" />
                ) : (
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-100 text-xs font-bold text-blue-700">
                    {(user?.nickname || user?.name || "?").charAt(0).toUpperCase()}
                  </div>
                )}
                <span className="text-sm text-gray-500">{user?.nickname || user?.name}</span>
              </div>
              <button
                onClick={handleLogout}
                className="rounded-md bg-gray-100 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-200"
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
        <div className="border-t border-gray-200 bg-white px-4 pb-4 md:hidden">
          <div className="flex flex-col gap-3 pt-3">
            {allLinks.map((link) => (
              <Link key={link.href} href={link.href} className="text-sm font-medium text-gray-700" onClick={() => setMobileOpen(false)}>
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
