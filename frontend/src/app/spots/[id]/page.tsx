"use client";

import { use, useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { spotsApi, reportsApi, removalApi, usersApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type {
  SpotResponse,
  SpotSummaryResponse,
  ReportResponse,
  SpotAnalyticsResponse,
  Page,
} from "@/types/api";
import TrustBadge from "@/components/spots/TrustBadge";
import ReportForm from "@/components/reports/ReportForm";
import { formatPrice, formatDate, spotTypeLabel } from "@/lib/utils";

type Tab = "summary" | "reports" | "analytics";

export default function SpotDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

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
      setError("Failed to load spot details");
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
            setIsFavorite(true); // already favorited
          } else throw err;
        }
      }
    } catch {
      // silent
    }
  };

  const requestRemoval = async () => {
    if (!confirm("Tem certeza que deseja solicitar remoção desta vaga?")) return;
    setRemovalLoading(true);
    try {
      await removalApi.request(id, "Spot no longer exists or is inaccurate");
      alert("Solicitação de remoção enviada");
    } catch {
      alert("Falha ao solicitar remoção");
    } finally {
      setRemovalLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  if (error || !spot) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <p className="text-red-600">{error || "Spot not found"}</p>
          <button onClick={() => router.back()} className="mt-2 text-sm text-blue-600 hover:underline">
            Go back
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
            &larr; Voltar
          </button>
          <a href={`/?lat=${spot.latitude}&lng=${spot.longitude}`} className="text-sm text-green-600 hover:underline">
            📍 Ver no mapa
          </a>
          <a
            href={`https://www.google.com/maps/dir/?api=1&destination=${spot.latitude},${spot.longitude}`}
            target="_blank" rel="noopener noreferrer"
            className="flex items-center gap-1 rounded bg-green-600 px-3 py-1 text-sm font-medium text-white hover:bg-green-700"
          >
            🧭 Navegar
          </a>
        </div>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{spot.name}</h1>
            <p className="mt-1 text-sm text-gray-500">{spotTypeLabel(spot.type)}</p>
            {spot.address && <p className="mt-1 text-sm text-gray-600">{spot.address}</p>}
          </div>
          <TrustBadge level={spot.trustLevel} className="mt-1" />
        </div>

        <div className="mt-4 flex flex-wrap gap-4 text-sm text-gray-700">
          <span>
            <strong>Preço:</strong> {formatPrice(spot.priceMin, spot.priceMax)}
          </span>
          <span>
            <strong>Confiança:</strong> {(spot.trustScore * 100).toFixed(0)}%
          </span>
          <span>
            <strong>Confirmações:</strong> {spot.totalConfirmations}
          </span>
          {spot.estimatedSpots !== null && (
            <span>
              <strong>Vagas estimadas:</strong> {spot.estimatedSpots}
            </span>
          )}
          {spot.requiresBooking && (
            <span className="rounded bg-yellow-100 px-2 py-0.5 text-xs font-medium text-yellow-800">
              Reserva necessária
            </span>
          )}
        </div>

        {/* Schedules */}
        {spot.schedules.length > 0 && (
          <div className="mt-4">
            <h3 className="mb-2 text-sm font-semibold text-gray-700">Horários</h3>
            <div className="grid grid-cols-1 gap-1 sm:grid-cols-2">
              {spot.schedules.map((s) => (
                <div key={s.id} className="flex justify-between rounded bg-gray-50 px-3 py-1.5 text-xs">
                  <span className="font-medium text-gray-700">{s.dayOfWeek}</span>
                  <span className="text-gray-500">
                    {s.openTime} - {s.closeTime}
                    {s.paidOnly && (
                      <span className="ml-1 text-yellow-600">(paid)</span>
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Actions */}
        {isAuthenticated && (
          <div className="mt-4 flex flex-wrap gap-2">
            <button
              onClick={toggleFavorite}
              className={`flex items-center gap-1 rounded-md px-3 py-1.5 text-sm font-medium ${
                isFavorite
                  ? "bg-red-50 text-red-600 hover:bg-red-100"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              {isFavorite ? "❤️" : "🤍"} {isFavorite ? "Favoritado" : "Favoritar"}
            </button>
            <button
              onClick={() => setShowReportForm(!showReportForm)}
              className="rounded-md bg-blue-100 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-200"
            >
              {showReportForm ? "Cancelar Relato" : "Enviar Relato"}
            </button>
            <button
              onClick={requestRemoval}
              disabled={removalLoading}
              className="rounded-md bg-orange-100 px-3 py-1.5 text-sm font-medium text-orange-700 hover:bg-orange-200 disabled:opacity-50"
            >
              Solicitar Remoção
            </button>
          </div>
        )}
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
              className={`border-b-2 pb-3 text-sm font-medium capitalize transition ${
                activeTab === tab
                  ? "border-blue-600 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              {tab}
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
                label="Disponibilidade"
                value={summary.dominantAvailability.replace("_", " ")}
                color={
                  summary.dominantAvailability === "AVAILABLE"
                    ? "text-green-700"
                    : summary.dominantAvailability === "UNAVAILABLE"
                      ? "text-red-700"
                      : "text-gray-500"
                }
              />
              <SummaryCard
                label="Preço Médio"
                value={summary.avgEstimatedPrice !== null ? `R$${summary.avgEstimatedPrice.toFixed(0)}` : "N/A"}
              />
              <SummaryCard
                label="Segurança"
                value={summary.avgSafetyRating !== null ? `${summary.avgSafetyRating.toFixed(1)}/5` : "N/A"}
              />
              <SummaryCard
                label="Cobrança Informal"
                value={`${summary.informalChargePercentage.toFixed(0)}%`}
                color={summary.informalChargeReportedRecently ? "text-red-600" : undefined}
              />
            </div>
            {summary.lastReportAt && (
              <p className="text-sm text-gray-500">
                Último relato: {formatDate(summary.lastReportAt)}
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
                          {report.availabilityStatus}
                        </span>
                        <span className="text-xs text-gray-400">{formatDate(report.createdAt)}</span>
                      </div>
                      <div className="mt-2 flex flex-wrap gap-3 text-sm text-gray-600">
                        {report.estimatedPrice !== null && (
                          <span>Preço: R${report.estimatedPrice}</span>
                        )}
                        {report.safetyRating !== null && (
                          <span>Segurança: {"★".repeat(report.safetyRating)}{"☆".repeat(5 - report.safetyRating)}</span>
                        )}
                        {report.informalChargeReported && (
                          <span className="text-red-600">Cobrança informal</span>
                        )}
                      </div>
                      {report.note && (
                        <p className="mt-2 text-sm text-gray-700">{report.note}</p>
                      )}
                      {report.images.length > 0 && (
                        <div className="mt-2 flex gap-2">
                          {report.images.map((img) => (
                            <span key={img.filename} className="text-xs text-blue-600">
                              {img.originalFilename}
                            </span>
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
                      Anterior
                    </button>
                    <span className="text-sm text-gray-500">
                      Page {reports.number + 1} of {reports.totalPages}
                    </span>
                    <button
                      onClick={() => setReportPage((p) => p + 1)}
                      disabled={reports.last}
                      className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
                    >
                      Próximo
                    </button>
                  </div>
                )}
              </>
            ) : (
              <p className="text-center text-sm text-gray-500">Nenhum relato ainda.</p>
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
                      <th className="pb-2 pr-4 font-medium text-gray-700">Dia</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">Hora</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">Disponibilidade</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">Avg Price</th>
                      <th className="pb-2 pr-4 font-medium text-gray-700">Segurança</th>
                      <th className="pb-2 font-medium text-gray-700">Relatos</th>
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
                            {hour.avgSafetyRating !== null ? `${hour.avgSafetyRating.toFixed(1)}/5` : "-"}
                          </td>
                          <td className="py-2 text-gray-600">{hour.reportCount}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-center text-sm text-gray-500">No analytics data available yet.</p>
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
