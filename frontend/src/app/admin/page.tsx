"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { adminApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import type { Page } from "@/types/api";

type Tab = "users" | "spots" | "reports";

interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: string;
  reputationScore: number;
  emailVerified: boolean;
  createdAt: string;
}

interface AdminSpot {
  id: string;
  name: string;
  type: string;
  trustLevel: string;
  totalConfirmations: number;
  createdBy: string;
  createdAt: string;
}

interface AdminReport {
  id: string;
  availabilityStatus: string;
  safetyRating: number;
  createdAt: string;
}

export default function AdminPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();
  const [tab, setTab] = useState<Tab>("users");
  const [stats, setStats] = useState<{ totalUsers: number; totalSpots: number; totalReports: number } | null>(null);
  const [users, setUsers] = useState<Page<AdminUser> | null>(null);
  const [spots, setSpots] = useState<Page<AdminSpot> | null>(null);
  const [reports, setReports] = useState<Page<AdminReport> | null>(null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (!isAuthenticated || user?.role !== "ADMIN") {
      router.push("/");
    }
  }, [isAuthenticated, user, router]);

  useEffect(() => {
    adminApi.getStats().then(res => setStats(res.data)).catch(() => {});
  }, []);

  const fetchTab = useCallback(async () => {
    try {
      if (tab === "users") {
        const res = await adminApi.getUsers(page);
        setUsers(res.data);
      } else if (tab === "spots") {
        const res = await adminApi.getSpots(page);
        setSpots(res.data);
      } else {
        const res = await adminApi.getReports(page);
        setReports(res.data);
      }
    } catch { /* silent */ }
  }, [tab, page]);

  useEffect(() => {
    fetchTab();
  }, [fetchTab]);

  const handleBan = async (id: string) => {
    await adminApi.banUser(id);
    fetchTab();
  };
  const handleUnban = async (id: string) => {
    await adminApi.unbanUser(id);
    fetchTab();
  };
  const handleDeactivateSpot = async (id: string) => {
    await adminApi.deactivateSpot(id);
    fetchTab();
  };
  const handleDeleteReport = async (id: string) => {
    await adminApi.deleteReport(id);
    fetchTab();
  };

  if (!isAuthenticated || user?.role !== "ADMIN") return null;

  return (
    <div className="mx-auto w-full max-w-5xl px-4 py-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{t("admin.title")}</h1>

      {/* Stats */}
      {stats && (
        <div className="mb-6 grid grid-cols-3 gap-4">
          <div className="rounded-lg bg-blue-50 p-4 text-center">
            <p className="text-xs text-gray-500">{t("admin.totalUsers")}</p>
            <p className="text-2xl font-bold text-blue-600">{stats.totalUsers}</p>
          </div>
          <div className="rounded-lg bg-green-50 p-4 text-center">
            <p className="text-xs text-gray-500">{t("admin.totalSpots")}</p>
            <p className="text-2xl font-bold text-green-600">{stats.totalSpots}</p>
          </div>
          <div className="rounded-lg bg-purple-50 p-4 text-center">
            <p className="text-xs text-gray-500">{t("admin.totalReports")}</p>
            <p className="text-2xl font-bold text-purple-600">{stats.totalReports}</p>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="mb-6 flex gap-2">
        {(["users", "spots", "reports"] as Tab[]).map(t_tab => (
          <button key={t_tab} onClick={() => { setTab(t_tab); setPage(0); }}
            className={`rounded-md px-4 py-2 text-sm font-medium ${tab === t_tab ? "bg-blue-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"}`}>
            {t(`admin.${t_tab}` as any)}
          </button>
        ))}
      </div>

      {/* Users */}
      {tab === "users" && users && (
        <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="px-4 py-3">{t("auth.name")}</th>
                <th className="px-4 py-3">{t("auth.email")}</th>
                <th className="px-4 py-3">{t("admin.role")}</th>
                <th className="px-4 py-3">{t("profile.reputation")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {users.content.map(u => (
                <tr key={u.id} className="border-b border-gray-100">
                  <td className="px-4 py-3">{u.name}</td>
                  <td className="px-4 py-3 text-gray-500">{u.email}</td>
                  <td className="px-4 py-3"><span className={`rounded px-2 py-0.5 text-xs ${u.role === "ADMIN" ? "bg-purple-100 text-purple-700" : "bg-gray-100 text-gray-600"}`}>{u.role}</span></td>
                  <td className="px-4 py-3">{u.reputationScore.toFixed(0)}</td>
                  <td className="px-4 py-3">
                    {u.role !== "ADMIN" && (
                      <button onClick={() => handleBan(u.id)} className="text-xs text-red-600 hover:underline">{t("admin.ban")}</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Spots */}
      {tab === "spots" && spots && (
        <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="px-4 py-3">{t("newSpot.name")}</th>
                <th className="px-4 py-3">{t("newSpot.type")}</th>
                <th className="px-4 py-3">{t("spot.trustScore")}</th>
                <th className="px-4 py-3">{t("spot.confirmations")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {spots.content.map(s => (
                <tr key={s.id} className="border-b border-gray-100">
                  <td className="px-4 py-3"><a href={`/spots/${s.id}`} className="text-blue-600 hover:underline">{s.name}</a></td>
                  <td className="px-4 py-3">{t(`type.${s.type}` as any)}</td>
                  <td className="px-4 py-3">{s.trustLevel}</td>
                  <td className="px-4 py-3">{s.totalConfirmations}</td>
                  <td className="px-4 py-3">
                    <button onClick={() => handleDeactivateSpot(s.id)} className="text-xs text-red-600 hover:underline">{t("admin.deactivate")}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Reports */}
      {tab === "reports" && reports && (
        <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">{t("spot.availability")}</th>
                <th className="px-4 py-3">{t("report.safety")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {reports.content.map(r => (
                <tr key={r.id} className="border-b border-gray-100">
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{r.id.slice(0, 8)}...</td>
                  <td className="px-4 py-3">{r.availabilityStatus}</td>
                  <td className="px-4 py-3">{"★".repeat(r.safetyRating)}{"☆".repeat(5 - r.safetyRating)}</td>
                  <td className="px-4 py-3">
                    <button onClick={() => handleDeleteReport(r.id)} className="text-xs text-red-600 hover:underline">{t("admin.deleteReport")}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {((tab === "users" && users && users.totalPages > 1) ||
        (tab === "spots" && spots && spots.totalPages > 1) ||
        (tab === "reports" && reports && reports.totalPages > 1)) && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50">{t("common.previous")}</button>
          <span className="text-sm text-gray-500">{t("common.page")} {page + 1}</span>
          <button onClick={() => setPage(p => p + 1)}
            className="rounded-md bg-gray-100 px-3 py-1 text-sm">{t("common.next")}</button>
        </div>
      )}
    </div>
  );
}
