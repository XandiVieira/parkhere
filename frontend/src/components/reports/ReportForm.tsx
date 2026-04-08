"use client";

import { useState } from "react";
import { reportsApi } from "@/lib/api";
import { t } from "@/lib/i18n";
import type { AvailabilityStatus } from "@/types/api";

interface ReportFormProps {
  spotId: string;
  onSuccess: () => void;
  onCancel: () => void;
}

export default function ReportForm({ spotId, onSuccess, onCancel }: ReportFormProps) {
  const [availability, setAvailability] = useState<AvailabilityStatus>("AVAILABLE");
  const [estimatedPrice, setEstimatedPrice] = useState("");
  const [safetyRating, setSafetyRating] = useState(3);
  const [informalCharge, setInformalCharge] = useState(false);
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    let latitude = -22.9068;
    let longitude = -43.1729;

    try {
      const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 });
      });
      latitude = pos.coords.latitude;
      longitude = pos.coords.longitude;
    } catch {
      // Use default location if geolocation fails
    }

    try {
      await reportsApi.submit(spotId, {
        availabilityStatus: availability,
        estimatedPrice: estimatedPrice ? parseFloat(estimatedPrice) : null,
        safetyRating,
        informalChargeReported: informalCharge,
        note: note || null,
        latitude,
        longitude,
      });
      setSuccess(true);
      setTimeout(onSuccess, 1500);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || t("report.submitFailed"));
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="rounded-lg border border-green-200 bg-green-50 p-6 text-center">
        <p className="font-medium text-green-700">{t("report.success")}</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border border-gray-200 bg-white p-6">
      <h3 className="text-lg font-semibold text-gray-900">{t("report.title")}</h3>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      {/* Disponibilidade */}
      <div>
        <label className="mb-2 block text-sm font-medium text-gray-700">{t("spot.availability")}</label>
        <div className="flex gap-3">
          {(["AVAILABLE", "UNAVAILABLE", "UNKNOWN"] as AvailabilityStatus[]).map((status) => (
            <label key={status} className="flex items-center gap-1.5">
              <input
                type="radio"
                name="availability"
                value={status}
                checked={availability === status}
                onChange={() => setAvailability(status)}
                className="text-blue-600"
              />
              <span className="text-sm text-gray-700">
                {status === "AVAILABLE" ? t("report.available") : status === "UNAVAILABLE" ? t("report.unavailable") : t("report.unknown")}
              </span>
            </label>
          ))}
        </div>
      </div>

      {/* Estimated Price */}
      <div>
        <label htmlFor="estimatedPrice" className="mb-1 block text-sm font-medium text-gray-700">
          {t("report.price")}
        </label>
        <input
          id="estimatedPrice"
          type="number"
          min="0"
          step="0.5"
          value={estimatedPrice}
          onChange={(e) => setEstimatedPrice(e.target.value)}
          placeholder={t("report.priceFree")}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      {/* Safety Rating */}
      <div>
        <label className="mb-2 block text-sm font-medium text-gray-700">{t("report.safety")}</label>
        <div className="flex gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => setSafetyRating(star)}
              className={`text-2xl transition ${star <= safetyRating ? "text-yellow-400" : "text-gray-300"}`}
            >
              ★
            </button>
          ))}
        </div>
      </div>

      {/* Informal Charge */}
      <label className="flex items-center gap-2">
        <input
          type="checkbox"
          checked={informalCharge}
          onChange={(e) => setInformalCharge(e.target.checked)}
          className="rounded text-blue-600"
        />
        <span className="text-sm text-gray-700">{t("report.informalCharge")}</span>
      </label>

      {/* Note */}
      <div>
        <label htmlFor="note" className="mb-1 block text-sm font-medium text-gray-700">
          {t("report.note")}
        </label>
        <textarea
          id="note"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          rows={3}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
          placeholder={t("report.additionalNotes")}
        />
      </div>

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={loading}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? t("report.submitting") : t("report.submit")}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
        >
          {t("common.cancel")}
        </button>
      </div>
    </form>
  );
}
