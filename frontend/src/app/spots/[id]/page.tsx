"use client";

import { use, useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { spotsApi, reportsApi, removalApi, usersApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import type {
  SpotResponse,
  SpotSummaryResponse,
  ReportResponse,
  SpotAnalyticsResponse,
  Page,
} from "@/types/api";
import TrustBadge from "@/components/spots/TrustBadge";
import ReportForm from "@/components/reports/ReportForm";
import { formatPrice, formatDate, spotTypeLabel, cleanAddress } from "@/lib/utils";

type Tab = "summary" | "reports" | "analytics";

const TAB_LABELS: Record<Tab, () => string> = {
  summary: () => t("spot.summary"),
  reports: () => t("spot.reports"),
  analytics: () => t("spot.analytics"),
};

export default function SpotDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();

  const [spot, setSpot] = useState<SpotResponse | null>(null);
  const [summary, setSummary] = useState<SpotSummaryResponse | null>(null);
  const [reports, setReports] = useState<Page<ReportResponse> | null>(null);
  const [analytics, setAnalytics] = useState<SpotAnalyticsResponse | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>("summary");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isFavorite, setIsFavorite] = useState(false);
  const [showReportForm, setShowReportForm] = useState(false);
  const [removalLoading, setRemovalLoading] = useState(false);
  const [reportPage, setReportPage] = useState(0);

  const fetchSpot = useCallback(async () => {
    try {
      const [spotRes, summaryRes] = await Promise.all([
        spotsApi.getById(id),
        spotsApi.getSummary(id),
      ]);
      setSpot(spotRes.data);
      setSummary(summaryRes.data);
    } catch {
      setError(t("spot.failedLoad"));
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchReports = useCallback(async (page: number) => {
    try {
      const res = await reportsApi.getForSpot(id, page);
      setReports(res.data);
    } catch {
      // silent
    }
  }, [id]);

  const fetchAnalytics = useCallback(async () => {
    try {
      const res = await spotsApi.getAnalytics(id);
      setAnalytics(res.data);
    } catch {
      // silent
    }
  }, [id]);

  useEffect(() => {
    fetchSpot();
  }, [fetchSpot]);

  useEffect(() => {
    if (activeTab === "reports") fetchReports(reportPage);
    if (activeTab === "analytics" && !analytics) fetchAnalytics();
  }, [activeTab, reportPage, fetchReports, fetchAnalytics, analytics]);

  // Check if already favorited on load
  useEffect(() => {
    if (!isAuthenticated) return;
    usersApi.getFavorites(0, 100).then(res => {
      const favIds = res.data.content.map((s: SpotResponse) => s.id);
      if (favIds.includes(id)) setIsFavorite(true);
    }).catch(() => {});
  }, [id, isAuthenticated]);

  const toggleFavorite = async () => {
    try {
      if (isFavorite) {
        await spotsApi.removeFavorite(id);
        setIsFavorite(false);
      } else {
        try {
          await spotsApi.addFavorite(id);
          setIsFavorite(true);
        } catch (err: any) {
          if (err?.response?.status === 409) {
            setIsFavorite(true);
          } else throw err;
        }
      }
    } catch {
      // silent
    }
  };

  const requestRemoval = async () => {
    if (!confirm(t("spot.removalConfirm"))) return;
    setRemovalLoading(true);
    try {
      await removalApi.request(id, t("spot.removalReason"));
      alert(t("spot.removalSent"));
    } catch {
      alert(t("spot.removalFailed"));
    } finally {
      setRemovalLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-gray-500">{t("common.loading")}</div>
      </div>
    );
  }

  if (error || !spot) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <p className="text-red-600">{error || t("spot.notFound")}</p>
          <button onClick={() => router.back()} className="mt-2 text-sm text-blue-600 hover:underline">
            {t("common.goBack")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6">
      {/* Header */}
      <div className="mb-6">
        <div className="mb-3 flex items-center gap-4">
          <button onClick={() => router.back()} className="text-sm text-blue-600 hover:underline">
            &larr; {t("common.back")}
          </button>
          <a href={`/?lat=${spot.latitude}&lng=${spot.longitude}`} className="text-sm text-green-600 hover:underline">
            📍 {t("map.viewOnMap")}
          </a>
          <a
            href={`https://www.google.com/maps/dir/?api=1&destination=${spot.latitude},${spot.longitude}`}
            target="_blank" rel="noopener noreferrer"
            className="flex items-center gap-1 rounded bg-green-600 px-3 py-1 text-sm font-medium text-white hover:bg-green-700"
          >
            🧭 {t("map.navigate")}
          </a>
          {isAuthenticated && user && spot.createdBy === user.id && (
            <a href={`/spots/edit?id=${spot.id}`} className="text-sm text-blue-600 hover:underline">
              {t("common.edit")}
            </a>
          )}
        </div>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{spot.name}</h1>
            <p className="mt-1 text-sm text-gray-500">{spotTypeLabel(spot.type)}</p>
            {spot.address && <p className="mt-1 text-sm text-gray-600">{cleanAddress(spot.address)}</p>}
          </div>
          <TrustBadge level={spot.trustLevel} className="mt-1" />
        </div>

        <div className="mt-4 flex flex-wrap gap-4 text-sm text-gray-700">
          <span>
            <strong>{t("spot.price")}:</strong> {formatPrice(spot.priceMin, spot.priceMax)}
          </span>
          <span>
            <strong>{t("spot.trustScore")}:</strong> {(spot.trustScore * 100).toFixed(0)}%
          </span>
          <span>
            <strong>{t("spot.confirmations")}:</strong> {spot.totalConfirmations}
          </span>
          {spot.estimatedSpots !== null && (
            <span>
              <strong>{t("spot.estimatedSpots")}:</strong> {spot.estimatedSpots}
            </span>
          )}
          {spot.requiresBooking && (
            <span className="rounded bg-yellow-100 px-2 py-0.5 text-xs font-medium text-yellow-800">
              {t("spot.requiresBooking")}
            </span>
          )}
        </div>

        {/* Informal charge warning */}
        {spot.informalChargeFrequency && spot.informalChargeFrequency !== "UNKNOWN" && spot.informalChargeFrequency !== "NEVER" && (
          <div className={`mt-3 flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium ${
            spot.informalChargeFrequency === "ALWAYS" ? "bg-red-100 text-red-800" :
            spot.informalChargeFrequency === "OFTEN" ? "bg-red-50 text-red-700" :
            "bg-orange-50 text-orange-700"
          }`}>
            🚨 {t("informal.label")}: {t(`informal.${spot.informalChargeFrequency}` as any)}
          </div>
        )}

        {/* Schedules */}
        {spot.schedules.length > 0 && (
          <div className="mt-4">
            <h3 className="mb-2 text-sm font-semibold text-gray-700">{t("spot.schedules")}</h3>
            <div className="grid grid-cols-1 gap-1 sm:grid-cols-2">
              {spot.schedules.map((s) => (
                <div key={s.id} className="flex justify-between rounded bg-gray-50 px-3 py-1.5 text-xs">
                  <span className="font-medium text-gray-700">{s.dayOfWeek}</span>
                  <span className="text-gray-500">
                    {s.openTime} - {s.closeTime}
                    {s.paidOnly && (
                      <span className="ml-1 text-yellow-600">({t("spot.paid")})</span>
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Actions — always visible, redirect to register if not logged in */}
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            onClick={() => isAuthenticated ? toggleFavorite() : router.push("/register")}
            className={`flex items-center gap-1 rounded-md px-3 py-1.5 text-sm font-medium ${
              isFavorite
                ? "bg-red-50 text-red-600 hover:bg-red-100"
                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }`}
          >
            {isFavorite ? "❤️" : "🤍"} {isFavorite ? t("spot.favorited") : t("spot.favorite")}
          </button>
          <button
            onClick={() => isAuthenticated ? setShowReportForm(!showReportForm) : router.push("/register")}
            className="rounded-md bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-200"
          >
            {showReportForm ? t("report.cancelReport") : t("report.submit")}
          </button>
          <button
            onClick={() => isAuthenticated ? requestRemoval() : router.push("/register")}
            disabled={removalLoading}
            className="rounded-md bg-orange-100 px-3 py-1.5 text-sm font-medium text-orange-700 hover:bg-orange-200 disabled:opacity-50"
          >
            {t("spot.requestRemoval")}
          </button>
        </div>
      </div>

      {/* Report Form */}
      {showReportForm && isAuthenticated && (
        <div className="mb-6">
          <ReportForm
            spotId={id}
            onSuccess={() => {
              setShowReportForm(false);
              fetchReports(0);
            }}
            onCancel={() => setShowReportForm(false)}
          />
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-6">
          {(["summary", "reports", "analytics"] as Tab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`border-b-2 pb-3 text-sm font-medium transition ${
                activeTab === tab
                  ? "border-blue-600 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              {TAB_LABELS[tab]()}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="mt-6">
        {activeTab === "summary" && summary && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <SummaryCard
                label={t("spot.availability")}
                value={summary.dominantAvailability === "AVAILABLE" ? t("report.available") : summary.dominantAvailability === "UNAVAILABLE" ? t("report.unavailable") : t("report.unknown")}
                color={
                  summary.dominantAvailability === "AVAILABLE"
                    ? "text-green-700"
                    : summary.dominantAvailability === "UNAVAILABLE"
                      ? "text-red-700"
                      : "text-gray-500"
                }
              />
              <SummaryCard
                label={t("spot.avgPrice")}
                value={summary.avgEstimatedPrice !== null ? `R$${summary.avgEstimatedPrice.toFixed(0)}` : "N/A"}
              />
              <SummaryCard
                label={t("spot.avgSafety")}
                value={summary.avgSafetyRating !== null ? `${Number.isInteger(summary.avgSafetyRating) ? summary.avgSafetyRating : summary.avgSafetyRating.toFixed(1)}/5` : "N/A"}
              />
              <SummaryCard
                label={t("spot.informalCharge")}
                value={`${summary.informalChargePercentage.toFixed(0)}%`}
                color={summary.informalChargeReportedRecently ? "text-red-600" : undefined}
              />
            </div>
            {summary.lastReportAt && (
              <p className="text-sm text-gray-500">
                {t("spot.lastReport")}: {formatDate(summary.lastReportAt)}
              </p>
            )}
          </div>
        )}

        {activeTab === "reports" && (
          <div>
            {reports && reports.content.length > 0 ? (
              <>
                <div className="space-y-3">
                  {reports.content.map((report) => (
                    <div key={report.id} className="rounded-lg border border-gray-200 bg-white p-4">
                      <div className="flex items-center justify-between">
                        <span
                          className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                            report.availabilityStatus === "AVAILABLE"
                              ? "bg-green-100 text-green-700"
                              : report.availabilityStatus === "UNAVAILABLE"
                                ? "bg-red-100 text-red-700"
                                : "bg-gray-100 text-gray-700"
                          }`}
                        >
                          {report.availabilityStatus === "AVAILABLE" ? t("report.available") : report.availabilityStatus === "UNAVAILABLE" ? t("report.unavailable") : t("report.unknown")}
                        </span>
                        <span className="text-xs text-gray-400">{formatDate(report.createdAt)}</span>
                      </div>
                      <div className="mt-2 flex flex-wrap gap-3 text-sm text-gray-600">
                        {report.estimatedPrice !== null && (
                          <span>{t("spot.price")}: R${report.estimatedPrice}</span>
                        )}
                        {report.safetyRating !== null && (
                          <span>{t("report.safety")}: {"★".repeat(report.safetyRating)}{"☆".repeat(5 - report.safetyRating)}</span>
                        )}
                        {report.informalChargeReported && (
                          <span className="rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                            🚨 {report.informalChargeType || t("spot.informalCharge")}
                            {report.informalChargeAmount ? ` R$${report.informalChargeAmount}` : ""}
                          </span>
                        )}
                      </div>
                      {report.note && (
                        <p className="mt-2 text-sm text-gray-700">{report.note}</p>
                      )}
                      {report.images.length > 0 && (
                        <div className="mt-2 flex gap-2 overflow-x-auto">
                          {report.images.map((img) => (
                            <a key={img.filename}
                              href={`${process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"}/api/v1/images/${img.filename}`}
                              target="_blank" rel="noopener noreferrer">
                              <img
                                src={`${process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"}/api/v1/images/${img.filename}`}
                                alt={img.originalFilename}
                                className="h-20 w-20 rounded-md object-cover"
                              />
                            </a>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                {/* Pagination */}
                {reports.totalPages > 1 && (
                  <div className="mt-4 flex items-center justify-center gap-2">
                    <button
                      onClick={() => setReportPage((p) => Math.max(0, p - 1))}
                      disabled={reports.first}
                      className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
                    >
                      {t("common.previous")}
                    </button>
                    <span className="text-sm text-gray-500">
                      {t("common.page")} {reports.number + 1} {t("common.of")} {reports.totalPages}
                    </span>
                    <button
                      onClick={() => setReportPage((p) => p + 1)}
                      disabled={reports.last}
                      className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
                    >
                      {t("common.next")}
                    </button>
                  </div>
                )}
              </>
            ) : (
              <p className="text-center text-sm text-gray-500">{t("spot.noReports")}</p>
            )}
          </div>
        )}

        {activeTab === "analytics" && (
          <div>
            {analytics && analytics.days.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-gray-200">
                      <th className="pb-2 pr-4 font-medium text-gray-700">{t("spot.analytics.day")}</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">{t("spot.analytics.hour")}</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">{t("spot.availability")}</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">{t("spot.analytics.avgPrice")}</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">{t("spot.analytics.safety")}</th>
                      <th className="pb-2 font-medium text-gray-700">{t("spot.analytics.reports")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.days.flatMap((day) =>
                      day.hours.map((hour) => (
                        <tr key={`${day.dayOfWeek}-${hour.hour}`} className="border-b border-gray-100">
                          <td className="py-2 pr-4 text-gray-700">{day.dayOfWeek}</td>
                          <td className="py-2 pr-4 text-gray-600">{String(hour.hour).padStart(2, "0")}:00</td>
                          <td className="py-2 pr-4">
                            <div className="flex items-center gap-1">
                              <div
                                className="h-2 rounded-full bg-green-500"
                                style={{ width: `${hour.availabilityRate * 100}%`, maxWidth: "60px" }}
                              />
                              <span className="text-xs text-gray-500">
                                {(hour.availabilityRate * 100).toFixed(0)}%
                              </span>
                            </div>
                          </td>
                          <td className="py-2 pr-4 text-gray-600">
                            {hour.avgPrice !== null ? `R$${hour.avgPrice.toFixed(0)}` : "-"}
                          </td>
                          <td className="py-2 pr-4 text-gray-600">
                            {hour.avgSafetyRating !== null ? `${Number.isInteger(hour.avgSafetyRating) ? hour.avgSafetyRating : hour.avgSafetyRating.toFixed(1)}/5` : "-"}
                          </td>
                          <td className="py-2 text-gray-600">{hour.reportCount}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-center text-sm text-gray-500">{t("spot.analytics.noData")}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function SummaryCard({
  label,
  value,
  color,
}: {
  label: string;
  value: string;
  color?: string;
}) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-3 text-center">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`mt-1 text-lg font-semibold ${color || "text-gray-900"}`}>{value}</p>
    </div>
  );
}
