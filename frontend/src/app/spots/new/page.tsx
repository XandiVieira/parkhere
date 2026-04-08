"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import { spotsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { SpotType } from "@/types/api";

const LocationPicker = dynamic(() => import("@/components/map/LocationPicker"), {
  ssr: false,
  loading: () => (
    <div className="flex h-64 items-center justify-center rounded-md bg-gray-100">
      <span className="text-sm text-gray-500">Loading map...</span>
    </div>
  ),
});

const SPOT_TYPES: { value: SpotType; label: string }[] = [
  { value: "STREET", label: "Street" },
  { value: "PARKING_LOT", label: "Parking Lot" },
  { value: "MALL", label: "Mall" },
  { value: "TERRAIN", label: "Terrain" },
  { value: "ZONA_AZUL", label: "Zona Azul" },
];

export default function NewSpotPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

  const [name, setName] = useState("");
  const [type, setType] = useState<SpotType>("STREET");
  const [priceMin, setPriceMin] = useState("0");
  const [priceMax, setPriceMax] = useState("0");
  const [notes, setNotes] = useState("");
  const [requiresBooking, setRequiresBooking] = useState(false);
  const [estimatedSpots, setEstimatedSpots] = useState("");
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (!isAuthenticated) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <p className="text-gray-600">You must be logged in to add a spot.</p>
          <button
            onClick={() => router.push("/login")}
            className="mt-2 text-sm font-medium text-blue-600 hover:underline"
          >
            Sign In
          </button>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!latitude || !longitude) {
      setError("Please select a location on the map");
      return;
    }

    setLoading(true);
    try {
      const res = await spotsApi.create({
        name,
        type,
        latitude,
        longitude,
        priceMin: parseFloat(priceMin) || 0,
        priceMax: parseFloat(priceMax) || 0,
        notes: notes || null,
        requiresBooking,
        estimatedSpots: estimatedSpots ? parseInt(estimatedSpots) : null,
      });
      router.push(`/spots/${res.data.id}`);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || "Failed to create spot");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-6">
      <button onClick={() => router.back()} className="mb-3 text-sm text-blue-600 hover:underline">
        &larr; Back
      </button>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Add New Spot</h1>

      {error && (
        <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="name" className="mb-1 block text-sm font-medium text-gray-700">
            Name
          </label>
          <input
            id="name"
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder="e.g., Rua Augusta parking"
          />
        </div>

        <div>
          <label htmlFor="type" className="mb-1 block text-sm font-medium text-gray-700">
            Type
          </label>
          <select
            id="type"
            value={type}
            onChange={(e) => setType(e.target.value as SpotType)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
          >
            {SPOT_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="priceMin" className="mb-1 block text-sm font-medium text-gray-700">
              Min Price (R$)
            </label>
            <input
              id="priceMin"
              type="number"
              min="0"
              step="0.5"
              value={priceMin}
              onChange={(e) => setPriceMin(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label htmlFor="priceMax" className="mb-1 block text-sm font-medium text-gray-700">
              Max Price (R$)
            </label>
            <input
              id="priceMax"
              type="number"
              min="0"
              step="0.5"
              value={priceMax}
              onChange={(e) => setPriceMax(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        </div>

        <div>
          <label htmlFor="estimatedSpots" className="mb-1 block text-sm font-medium text-gray-700">
            Estimated Spots (optional)
          </label>
          <input
            id="estimatedSpots"
            type="number"
            min="1"
            value={estimatedSpots}
            onChange={(e) => setEstimatedSpots(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder="Number of parking spots"
          />
        </div>

        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={requiresBooking}
            onChange={(e) => setRequiresBooking(e.target.checked)}
            className="rounded text-blue-600"
          />
          <span className="text-sm text-gray-700">Requires booking</span>
        </label>

        <div>
          <label htmlFor="notes" className="mb-1 block text-sm font-medium text-gray-700">
            Notes (optional)
          </label>
          <textarea
            id="notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
            placeholder="Any useful information..."
          />
        </div>

        {/* Location Picker */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700">
            Location (click on map)
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
              Selected: {latitude.toFixed(6)}, {longitude.toFixed(6)}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? "Creating..." : "Create Spot"}
        </button>
      </form>
    </div>
  );
}
