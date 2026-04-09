"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import { spotsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import type { SpotType } from "@/types/api";

const LocationPicker = dynamic(() => import("@/components/map/LocationPicker"), {
  ssr: false,
  loading: () => (
    <div className="flex h-64 items-center justify-center rounded-md bg-gray-100">
      <span className="text-sm text-gray-500">{t("map.loading")}</span>
    </div>
  ),
});

const SPOT_TYPES: SpotType[] = ["STREET", "PARKING_LOT", "MALL", "TERRAIN", "ZONA_AZUL"];

export default function NewSpotPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

  const [name, setName] = useState("");
  const [type, setType] = useState<SpotType>("STREET");
  const [priceMín, setPriceMín] = useState("0");
  const [priceMáx, setPriceMáx] = useState("0");
  const [notes, setNotes] = useState("");
  const [requiresBooking, setRequiresBooking] = useState(false);
  const [estimatedSpots, setEstimatedSpots] = useState("");
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [informalFrequency, setInformalFrequency] = useState("UNKNOWN");
  const [loading, setLoading] = useState(false);

  if (!isAuthenticated) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <p className="text-gray-600">{t("auth.loginRequired")}</p>
          <button
            onClick={() => router.push("/login")}
            className="mt-2 text-sm font-medium text-blue-600 hover:underline"
          >
            {t("auth.signIn")}
          </button>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!latitude || !longitude) {
      setError(t("newSpot.selectLocation"));
      return;
    }

    setLoading(true);
    try {
      const res = await spotsApi.create({
        name,
        type,
        latitude,
        longitude,
        priceMín: parseFloat(priceMín) || 0,
        priceMáx: parseFloat(priceMáx) || 0,
        notes: notes || null,
        requiresBooking,
        estimatedSpots: estimatedSpots ? parseInt(estimatedSpots) : null,
        informalChargeFrequency: informalFrequency,
      });
      router.push(`/spots/${res.data.id}`);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || t("newSpot.createFailed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-6">
      <button onClick={() => router.back()} className="mb-3 text-sm text-blue-600 hover:underline">
        &larr; {t("common.back")}
      </button>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{t("newSpot.title")}</h1>

      {error && (
        <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="name" className="mb-1 block text-sm font-medium text-gray-700">
            {t("newSpot.name")}
          </label>
          <input
            id="name"
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder={t("newSpot.namePlaceholder")}
          />
        </div>

        <div>
          <label htmlFor="type" className="mb-1 block text-sm font-medium text-gray-700">
            {t("newSpot.type")}
          </label>
          <select
            id="type"
            value={type}
            onChange={(e) => setType(e.target.value as SpotType)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
          >
            {SPOT_TYPES.map((st) => (
              <option key={st} value={st}>
                {t(`type.${st}` as any)}
              </option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="priceMín" className="mb-1 block text-sm font-medium text-gray-700">
              {t("newSpot.minPrice")}
            </label>
            <input
              id="priceMín"
              type="number"
              min="0"
              step="0.5"
              value={priceMín}
              onChange={(e) => setPriceMín(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label htmlFor="priceMáx" className="mb-1 block text-sm font-medium text-gray-700">
              {t("newSpot.maxPrice")}
            </label>
            <input
              id="priceMáx"
              type="number"
              min="0"
              step="0.5"
              value={priceMáx}
              onChange={(e) => setPriceMáx(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        </div>

        <div>
          <label htmlFor="estimatedSpots" className="mb-1 block text-sm font-medium text-gray-700">
            {t("newSpot.estimated")}
          </label>
          <input
            id="estimatedSpots"
            type="number"
            min="1"
            value={estimatedSpots}
            onChange={(e) => setEstimatedSpots(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder={t("newSpot.estimatedPlaceholder")}
          />
        </div>

        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={requiresBooking}
            onChange={(e) => setRequiresBooking(e.target.checked)}
            className="rounded text-blue-600"
          />
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
          <label htmlFor="notes" className="mb-1 block text-sm font-medium text-gray-700">
            {t("newSpot.notes")}
          </label>
          <textarea
            id="notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder={t("newSpot.notesPlaceholder")}
          />
        </div>

        {/* Location Picker */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700">
            {t("newSpot.location")}
          </label>
          <div className="h-64 overflow-hidden rounded-md border border-gray-300">
            <LocationPicker
              onLocationSelect={(lat, lng) => {
                setLatitude(lat);
                setLongitude(lng);
              }}
            />
          </div>
          {latitude && longitude && (
            <p className="mt-1 text-xs text-gray-500">
              {t("common.selected")}: {latitude.toFixed(6)}, {longitude.toFixed(6)}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? t("newSpot.creating") : t("newSpot.create")}
        </button>
      </form>
    </div>
  );
}
