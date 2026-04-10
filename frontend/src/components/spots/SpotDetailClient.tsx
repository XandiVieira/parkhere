"use client";

import { useEffect, useState, useCallback } from "react";
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
import { formatPrice, formatDate, spotTypeLabel, cleanAddress, apiBaseUrl } from "@/lib/utils";

type Tab = "summary" | "reports" | "analytics";

const TAB_LABELS: Record<Tab, () => string> = {
  summary: () => t("spot.summary"),
  reports: () => t("spot.reports"),
  analytics: () => t("spot.analytics"),
};

interface SpotDetailClientProps {
  initialSpot: SpotResponse;
  initialSummary: SpotSummaryResponse | null;
}

export default function SpotDetailClient({ initialSpot, initialSummary }: SpotDetailClientProps) {
  const id = initialSpot.id;
  const router = useRouter();
  const { isAuthenticated, user } = useAuthStore();

  const [spot] = useState<SpotResponse>(initialSpot);
  const [summary] = useState<SpotSummaryResponse | null>(initialSummary);
  const [reports, setReports] = useState<Page<ReportResponse> | null>(null);
  const [analytics, setAnalytics] = useState<SpotAnalyticsResponse | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>("summary");
  const [isFavorite, setIsFavorite] = useState(false);
  const [showReportForm, setShowReportForm] = useState(false);
  const [removalLoading, setRemovalLoading] = useState(false);
  const [shareCopied, setShareCopied] = useState(false);
  const [reportPage, setReportPage] = useState(0);

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
    if (activeTab === "reports") fetchReports(reportPage);
    if (activeTab === "analytics" && !analytics) fetchAnalytics();
  }, [activeTab, reportPage, fetchReports, fetchAnalytics, analytics]);

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

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6">
      {/* Cover Image */}
      {spot.coverImageUrl && (
        <div className="mb-4 overflow-hidden rounded-lg">
          <img
            src={`${apiBaseUrl()}${spot.coverImageUrl}`}
            alt={spot.name}
            className="h-48 w-full object-cover sm:h-64"
          />
        </div>
      )}

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

        {/* Actions */}
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
            onClick={async () => {
              const url = `${window.location.origin}/spots/${id}`;
              if (navigator.share) {
                try { await navigator.share({ title: spot.name, text: `${spot.name} — ParkHere`, url }); } catch { /* cancelled */ }
              } else {
                await navigator.clipboard.writeText(url);
                setShareCopied(true);
                setTimeout(() => setShareCopied(false), 2000);
              }
            }}
            className="rounded-md bg-gray-100 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-200"
          >
            {shareCopied ? t("spot.shareCopied") : t("spot.share")}
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
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
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
              <SummaryCard label={t("spot.avgPrice")} value={summary.avgEstimatedPrice !== null ? `R$${summary.avgEstimatedPrice.toFixed(0)}` : "N/A"} />
              <SummaryCard
                label={t("spot.avgSafety")}
                value={summary.avgSafetyRating !== null ? `${Number.isInteger(summary.avgSafetyRating) ? summary.avgSafetyRating : summary.avgSafetyRating.toFixed(1)}/5` : "N/A"}
              />
              <SummaryCard
                label={t("spot.availabilityRate")}
                value={`${(summary.availabilityRate * 100).toFixed(0)}%`}
                color={summary.availabilityRate >= 0.6 ? "text-green-600" : summary.availabilityRate >= 0.3 ? "text-yellow-600" : "text-red-600"}
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
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                          report.availabilityStatus === "AVAILABLE" ? "bg-green-100 text-green-700" :
                          report.availabilityStatus === "UNAVAILABLE" ? "bg-red-100 text-red-700" : "bg-gray-100 text-gray-700"
                        }`}>
                          {report.availabilityStatus === "AVAILABLE" ? t("report.available") : report.availabilityStatus === "UNAVAILABLE" ? t("report.unavailable") : t("report.unknown")}
                        </span>
                        <span className="text-xs text-gray-400">{formatDate(report.createdAt)}</span>
                      </div>
                      <div className="mt-2 flex flex-wrap gap-3 text-sm text-gray-600">
                        {report.estimatedPrice !== null && <span>{t("spot.price")}: R${report.estimatedPrice}</span>}
                        {report.safetyRating !== null && <span>{t("report.safety")}: {"★".repeat(report.safetyRating)}{"☆".repeat(5 - report.safetyRating)}</span>}
                        {report.informalChargeReported && (
                          <span className="rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                            🚨 {report.informalChargeType || t("spot.informalCharge")}
                            {report.informalChargeAmount ? ` R$${report.informalChargeAmount}` : ""}
                          </span>
                        )}
                      </div>
                      {report.note && <p className="mt-2 text-sm text-gray-700">{report.note}</p>}
                      {report.images.length > 0 && (
                        <div className="mt-2 flex gap-2 overflow-x-auto">
                          {report.images.map((img) => (
                            <a key={img.filename} href={`${apiBaseUrl()}/api/v1/images/${img.filename}`} target="_blank" rel="noopener noreferrer">
                              <img src={`${apiBaseUrl()}/api/v1/images/${img.filename}`} alt={img.originalFilename} className="h-20 w-20 rounded-md object-cover" />
                            </a>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                {reports.totalPages > 1 && (
                  <div className="mt-4 flex items-center justify-center gap-2">
                    <button onClick={() => setReportPage((p) => Math.max(0, p - 1))} disabled={reports.first} className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50">{t("common.previous")}</button>
                    <span className="text-sm text-gray-500">{t("common.page")} {reports.number + 1} {t("common.of")} {reports.totalPages}</span>
                    <button onClick={() => setReportPage((p) => p + 1)} disabled={reports.last} className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50">{t("common.next")}</button>
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
              <div>
                <p className="mb-3 text-sm font-medium text-gray-700">{t("spot.analytics.heatmapTitle")}</p>
                <div className="overflow-x-auto">
                  <div className="min-w-[600px]">
                    <div className="mb-1 flex">
                      <div className="w-16 shrink-0" />
                      {Array.from({ length: 24 }, (_, i) => (
                        <div key={i} className="flex-1 text-center text-[9px] text-gray-400">
                          {i % 3 === 0 ? `${String(i).padStart(2, "0")}` : ""}
                        </div>
                      ))}
                    </div>
                    {analytics.days.map((day) => {
                      const hoursMap = new Map(day.hours.map(h => [h.hour, h]));
                      return (
                        <div key={day.dayOfWeek} className="mb-0.5 flex items-center">
                          <div className="w-16 shrink-0 pr-2 text-right text-xs font-medium text-gray-600">{day.dayOfWeek.slice(0, 3)}</div>
                          {Array.from({ length: 24 }, (_, i) => {
                            const hour = hoursMap.get(i);
                            const rate = hour?.availabilityRate ?? 0;
                            const count = hour?.reportCount ?? 0;
                            const bg = count === 0 ? "bg-gray-100" : rate >= 0.8 ? "bg-green-500" : rate >= 0.6 ? "bg-green-400" : rate >= 0.4 ? "bg-yellow-400" : rate >= 0.2 ? "bg-orange-400" : "bg-red-400";
                            const tooltip = count > 0 ? `${(rate * 100).toFixed(0)}% | ${hour?.avgPrice !== null && hour?.avgPrice !== undefined ? `R$${hour.avgPrice.toFixed(0)}` : "-"} | ${count} ${t("spot.analytics.reports").toLowerCase()}` : "";
                            return <div key={i} className={`mx-px flex-1 rounded-sm ${bg}`} style={{ height: "22px" }} title={tooltip} />;
                          })}
                        </div>
                      );
                    })}
                    <div className="mt-3 flex items-center justify-end gap-1 text-[10px] text-gray-500">
                      <span>{t("spot.analytics.low")}</span>
                      <div className="h-3 w-3 rounded-sm bg-red-400" />
                      <div className="h-3 w-3 rounded-sm bg-orange-400" />
                      <div className="h-3 w-3 rounded-sm bg-yellow-400" />
                      <div className="h-3 w-3 rounded-sm bg-green-400" />
                      <div className="h-3 w-3 rounded-sm bg-green-500" />
                      <span>{t("spot.analytics.high")}</span>
                      <div className="ml-2 h-3 w-3 rounded-sm bg-gray-100" />
                      <span>N/A</span>
                    </div>
                  </div>
                </div>
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

function SummaryCard({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-3 text-center">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`mt-1 text-lg font-semibold ${color || "text-gray-900"}`}>{value}</p>
    </div>
  );
}
