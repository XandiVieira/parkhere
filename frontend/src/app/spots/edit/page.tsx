"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { spotsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import { apiBaseUrl, extractApiError } from "@/lib/utils";
import type { SpotResponse } from "@/types/api";

function EditSpotForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const spotId = searchParams.get("id") || "";
  const { isAuthenticated, user } = useAuthStore();

  const [spot, setSpot] = useState<SpotResponse | null>(null);
  const [name, setName] = useState("");
  const [priceMin, setPriceMin] = useState("0");
  const [priceMax, setPriceMax] = useState("0");
  const [notes, setNotes] = useState("");
  const [requiresBooking, setRequiresBooking] = useState(false);
  const [estimatedSpots, setEstimatedSpots] = useState("");
  const [informalFrequency, setInformalFrequency] = useState("UNKNOWN");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [coverPreview, setCoverPreview] = useState<string | null>(null);
  const [coverUploading, setCoverUploading] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) { router.push("/login"); return; }
    if (!spotId) { router.push("/"); return; }
    spotsApi.getById(spotId).then(res => {
      const s = res.data;
      if (s.createdBy !== user?.id) { router.push(`/spots/${spotId}`); return; }
      setSpot(s);
      setName(s.name);
      setPriceMin(String(s.priceMin));
      setPriceMax(String(s.priceMax));
      setNotes(s.notes || "");
      setRequiresBooking(s.requiresBooking);
      setEstimatedSpots(s.estimatedSpots !== null ? String(s.estimatedSpots) : "");
      setInformalFrequency(s.informalChargeFrequency || "UNKNOWN");
      if (s.coverImageUrl) setCoverPreview(`${apiBaseUrl()}${s.coverImageUrl}`);
    }).catch(() => {
      router.push("/");
    }).finally(() => setPageLoading(false));
  }, [spotId, isAuthenticated, user, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await spotsApi.update(spotId, {
        name,
        priceMin: parseFloat(priceMin) || 0,
        priceMax: parseFloat(priceMax) || 0,
        notes: notes || null,
        requiresBooking,
        estimatedSpots: estimatedSpots ? parseInt(estimatedSpots) : null,
        informalChargeFrequency: informalFrequency,
      });
      setSuccess(true);
      setTimeout(() => router.push(`/spots/${spotId}`), 1500);
    } catch (err: unknown) {
      setError(extractApiError(err) || t("editSpot.updateFailed"));
    } finally {
      setLoading(false);
    }
  };

  if (pageLoading) {
    return <div className="flex flex-1 items-center justify-center"><span className="text-gray-500">{t("common.loading")}</span></div>;
  }
  if (!spot) return null;

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-6">
      <button onClick={() => router.back()} className="mb-3 text-sm text-blue-600 hover:underline">
        &larr; {t("common.back")}
      </button>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{t("editSpot.title")}</h1>

      {success && (
        <div className="mb-4 rounded-md bg-green-50 p-3 text-sm font-medium text-green-700">{t("editSpot.updated")}</div>
      )}
      {error && (
        <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="name" className="mb-1 block text-sm font-medium text-gray-700">{t("newSpot.name")}</label>
          <input id="name" type="text" required value={name} onChange={(e) => setName(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none" />
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t("newSpot.type")}</label>
          <p className="text-sm text-gray-500">{t(`type.${spot.type}` as any)}</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="priceMin" className="mb-1 block text-sm font-medium text-gray-700">{t("newSpot.minPrice")}</label>
            <input id="priceMin" type="number" min="0" step="0.5" value={priceMin} onChange={(e) => setPriceMin(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none" />
          </div>
          <div>
            <label htmlFor="priceMax" className="mb-1 block text-sm font-medium text-gray-700">{t("newSpot.maxPrice")}</label>
            <input id="priceMax" type="number" min="0" step="0.5" value={priceMax} onChange={(e) => setPriceMax(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none" />
          </div>
        </div>

        <div>
          <label htmlFor="estimatedSpots" className="mb-1 block text-sm font-medium text-gray-700">{t("newSpot.estimated")}</label>
          <input id="estimatedSpots" type="number" min="1" value={estimatedSpots} onChange={(e) => setEstimatedSpots(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder={t("newSpot.estimatedPlaceholder")} />
        </div>

        <label className="flex items-center gap-2">
          <input type="checkbox" checked={requiresBooking} onChange={(e) => setRequiresBooking(e.target.checked)} className="rounded text-blue-600" />
          <span className="text-sm text-gray-700">{t("newSpot.booking")}</span>
        </label>

        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t("informal.frequency")}</label>
          <div className="flex flex-wrap gap-2">
            {["UNKNOWN", "NEVER", "SOMETIMES", "OFTEN", "ALWAYS"].map(freq => (
              <button key={freq} type="button" onClick={() => setInformalFrequency(freq)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
                  informalFrequency === freq
                    ? freq === "OFTEN" || freq === "ALWAYS" ? "bg-red-600 text-white" : freq === "SOMETIMES" ? "bg-orange-500 text-white" : "bg-blue-600 text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}>
                {t(`informal.${freq}` as any)}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label htmlFor="notes" className="mb-1 block text-sm font-medium text-gray-700">{t("newSpot.notes")}</label>
          <textarea id="notes" value={notes} onChange={(e) => setNotes(e.target.value)} rows={3}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder={t("newSpot.notesPlaceholder")} />
        </div>

        {/* Cover Image Upload */}
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">{t("editSpot.coverImage")}</label>
          <p className="mb-2 text-xs text-gray-500">{t("editSpot.coverImageHint")}</p>
          {coverPreview && (
            <img src={coverPreview} alt="" className="mb-2 h-36 w-full rounded-md object-cover" />
          )}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            disabled={coverUploading}
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              setCoverUploading(true);
              try {
                const res = await spotsApi.updateCoverImage(spotId, file);
                const url = res.data.coverImageUrl;
                setCoverPreview(url ? `${apiBaseUrl()}${url}` : null);
              } catch {
                setError(t("editSpot.updateFailed"));
              } finally {
                setCoverUploading(false);
              }
            }}
            className="text-sm text-gray-500 file:mr-3 file:rounded-md file:border-0 file:bg-blue-50 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-blue-700 hover:file:bg-blue-100"
          />
          {coverUploading && <p className="mt-1 text-xs text-blue-600">{t("editSpot.uploadingCover")}</p>}
        </div>

        <button type="submit" disabled={loading || success}
          className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {loading ? t("editSpot.saving") : t("editSpot.save")}
        </button>
      </form>
    </div>
  );
}

export default function EditSpotPage() {
  return (
    <Suspense fallback={<div className="flex flex-1 items-center justify-center"><span className="text-gray-500">{t("common.loading")}</span></div>}>
      <EditSpotForm />
    </Suspense>
  );
}
