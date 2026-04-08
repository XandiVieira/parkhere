"use client";

import { useState } from "react";
import { reportsApi } from "@/lib/api";
import type { SpotResponse, AvailabilityStatus } from "@/types/api";
import { t } from "@/lib/i18n";

interface QuickReportModalProps {
  spot: SpotResponse;
  userLat: number;
  userLng: number;
  onClose: () => void;
  onSuccess: () => void;
}

export default function QuickReportModal({ spot, userLat, userLng, onClose, onSuccess }: QuickReportModalProps) {
  const [status, setStatus] = useState<AvailabilityStatus>("AVAILABLE");
  const [safety, setSafety] = useState(3);
  const [informal, setInformal] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    setLoading(true);
    setError("");
    try {
      await reportsApi.submit(spot.id, {
        availabilityStatus: status,
        safetyRating: safety,
        informalChargeReported: informal,
        userLatitude: userLat,
        userLongitude: userLng,
      });
      onSuccess();
    } catch (err: any) {
      if (err?.response?.status === 429) {
        setError(t("report.rateLimited"));
      } else {
        setError(t("report.submitFailed"));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[2000] flex items-end justify-center sm:items-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-t-2xl bg-white p-5 shadow-xl sm:rounded-2xl">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-gray-900">{t("report.title")}</h3>
            <p className="text-sm text-gray-500">{spot.name}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        {error && <p className="mb-3 rounded bg-red-50 p-2 text-sm text-red-600">{error}</p>}

        {/* Availability */}
        <p className="mb-2 text-sm font-medium text-gray-700">{t("spot.availability")}</p>
        <div className="mb-4 grid grid-cols-3 gap-2">
          {(["AVAILABLE", "UNAVAILABLE", "UNKNOWN"] as AvailabilityStatus[]).map((s) => (
            <button key={s} onClick={() => setStatus(s)}
              className={`rounded-lg py-3 text-center text-sm font-medium transition ${
                status === s
                  ? s === "AVAILABLE" ? "bg-green-600 text-white" : s === "UNAVAILABLE" ? "bg-red-600 text-white" : "bg-gray-600 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}>
              {s === "AVAILABLE" ? `✅ ${t("report.available")}` : s === "UNAVAILABLE" ? `❌ ${t("report.full")}` : `❓ ${t("report.unknown")}`}
            </button>
          ))}
        </div>

        {/* Safety */}
        <p className="mb-2 text-sm font-medium text-gray-700">{t("report.safety")}</p>
        <div className="mb-4 flex gap-1">
          {[1, 2, 3, 4, 5].map((n) => (
            <button key={n} onClick={() => setSafety(n)}
              className={`text-2xl transition ${n <= safety ? "text-yellow-400" : "text-gray-300"}`}>
              ★
            </button>
          ))}
        </div>

        {/* Informal charge */}
        <label className="mb-5 flex items-center gap-2 text-sm">
          <input type="checkbox" checked={informal} onChange={() => setInformal(!informal)} className="rounded" />
          <span className="text-gray-700">🚨 {t("report.informalCharge")}</span>
        </label>

        {/* Submit */}
        <button onClick={handleSubmit} disabled={loading}
          className="w-full rounded-lg bg-blue-600 py-3 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50">
          {loading ? t("report.submitting") : t("report.submit")}
        </button>
      </div>
    </div>
  );
}
