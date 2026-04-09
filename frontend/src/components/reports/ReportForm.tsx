"use client";

import { useRef, useState } from "react";
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
  const [informalType, setInformalType] = useState("FLANELINHA");
  const [informalAmount, setInformalAmount] = useState("");
  const [informalAggro, setInformalAggro] = useState(1);
  const [informalNote, setInformalNote] = useState("");
  const [note, setNote] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    setImages(prev => [...prev, ...files].slice(0, 3));
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const removeImage = (index: number) => {
    setImages(prev => prev.filter((_, i) => i !== index));
  };

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
      const reportData = {
        availabilityStatus: availability,
        estimatedPrice: estimatedPrice ? parseFloat(estimatedPrice) : null,
        safetyRating,
        informalChargeReported: informalCharge,
        informalChargeType: informalCharge ? informalType : null,
        informalChargeAmount: informalCharge && informalAmount ? parseFloat(informalAmount) : null,
        informalChargeAggressiveness: informalCharge ? informalAggro : null,
        informalChargeNote: informalCharge && informalNote ? informalNote : null,
        note: note || null,
        userLatitude: latitude,
        userLongitude: longitude,
      };

      await reportsApi.submit(spotId, reportData, images.length > 0 ? images : undefined);
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

      {/* Availability */}
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
      <div className="space-y-3">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={informalCharge}
            onChange={(e) => setInformalCharge(e.target.checked)}
            className="rounded text-blue-600"
          />
          <span className="text-sm font-medium text-gray-700">{t("report.informalCharge")}</span>
        </label>

        {informalCharge && (
          <div className="ml-6 space-y-3 rounded-md border border-orange-200 bg-orange-50 p-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">{t("report.informalType")}</label>
              <select value={informalType} onChange={(e) => setInformalType(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none">
                <option value="FLANELINHA">{t("report.informalTypeFlanelinha")}</option>
                <option value="IRREGULAR">{t("report.informalTypeIrregular")}</option>
                <option value="ABUSIVE">{t("report.informalTypeAbusive")}</option>
                <option value="OTHER">{t("report.informalTypeOther")}</option>
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">{t("report.informalAmount")}</label>
              <input type="number" min="0" step="1" value={informalAmount} onChange={(e) => setInformalAmount(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">{t("report.informalAggressiveness")}</label>
              <div className="flex gap-1">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button key={n} type="button" onClick={() => setInformalAggro(n)}
                    className={`rounded px-3 py-1 text-sm font-medium transition ${n <= informalAggro ? "bg-red-500 text-white" : "bg-gray-200 text-gray-600"}`}>
                    {n}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">{t("report.informalNote")}</label>
              <textarea value={informalNote} onChange={(e) => setInformalNote(e.target.value)} rows={2}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                placeholder={t("report.informalNotePlaceholder")} />
            </div>
          </div>
        )}
      </div>

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

      {/* Image Upload */}
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">{t("report.addImages")}</label>
        {images.length > 0 && (
          <div className="mb-2 flex flex-wrap gap-2">
            {images.map((file, i) => (
              <div key={i} className="relative">
                <img src={URL.createObjectURL(file)} alt="" className="h-16 w-16 rounded-md object-cover" />
                <button type="button" onClick={() => removeImage(i)}
                  className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] text-white">&times;</button>
              </div>
            ))}
          </div>
        )}
        {images.length < 3 && (
          <button type="button" onClick={() => fileInputRef.current?.click()}
            className="rounded-md border border-dashed border-gray-300 px-3 py-2 text-sm text-gray-500 hover:border-blue-400 hover:text-blue-600">
            + {t("report.addImages")}
          </button>
        )}
        <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handleImageChange} />
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
