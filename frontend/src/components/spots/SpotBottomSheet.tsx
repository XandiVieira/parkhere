"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { spotsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import type { SpotResponse, SpotSummaryResponse } from "@/types/api";
import TrustBadge from "@/components/spots/TrustBadge";
import { formatPrice, spotTypeLabel, cleanAddress } from "@/lib/utils";

interface SpotBottomSheetProps {
  spot: SpotResponse;
  onClose: () => void;
}

export default function SpotBottomSheet({ spot, onClose }: SpotBottomSheetProps) {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const [summary, setSummary] = useState<SpotSummaryResponse | null>(null);

  const fetchSummary = useCallback(async () => {
    try {
      const res = await spotsApi.getSummary(spot.id);
      setSummary(res.data);
    } catch {
      // silent
    }
  }, [spot.id]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  return (
    <div className="fixed inset-0 z-[2000] flex items-end md:hidden" onClick={onClose}>
      <div className="absolute inset-0 bg-black/30" />
      <div
        className="relative w-full animate-slide-up rounded-t-2xl bg-white px-4 pb-6 pt-3 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Drag handle */}
        <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-gray-300" />

        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <h2 className="text-lg font-bold text-gray-900">{spot.name}</h2>
            <p className="text-xs text-gray-500">{spotTypeLabel(spot.type)}</p>
            {spot.address && <p className="mt-0.5 text-xs text-gray-500">{cleanAddress(spot.address)}</p>}
          </div>
          <TrustBadge level={spot.trustLevel} />
        </div>

        {/* Key info */}
        <div className="mt-3 flex flex-wrap gap-3 text-sm">
          <span className="rounded-md bg-gray-100 px-2 py-1 font-medium text-gray-700">
            {formatPrice(spot.priceMin, spot.priceMax)}
          </span>
          {summary && (
            <span className={`rounded-md px-2 py-1 font-medium ${
              summary.availabilityRate >= 0.6 ? "bg-green-100 text-green-700" :
              summary.availabilityRate >= 0.3 ? "bg-yellow-100 text-yellow-700" :
              "bg-red-100 text-red-700"
            }`}>
              {(summary.availabilityRate * 100).toFixed(0)}% {t("spot.availability").toLowerCase()}
            </span>
          )}
          {spot.informalChargeFrequency && spot.informalChargeFrequency !== "UNKNOWN" && spot.informalChargeFrequency !== "NEVER" && (
            <span className="rounded-md bg-red-100 px-2 py-1 text-xs font-medium text-red-700">
              {t("informal.label")}
            </span>
          )}
        </div>

        {/* Summary stats */}
        {summary && (
          <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
            <div className="rounded-md bg-gray-50 px-2 py-2">
              <p className="text-gray-400">{t("spot.avgSafety")}</p>
              <p className="mt-0.5 font-semibold text-gray-700">
                {summary.avgSafetyRating !== null ? `${Number.isInteger(summary.avgSafetyRating) ? summary.avgSafetyRating : summary.avgSafetyRating.toFixed(1)}/5` : "N/A"}
              </p>
            </div>
            <div className="rounded-md bg-gray-50 px-2 py-2">
              <p className="text-gray-400">{t("spot.confirmations")}</p>
              <p className="mt-0.5 font-semibold text-gray-700">{spot.totalConfirmations}</p>
            </div>
            <div className="rounded-md bg-gray-50 px-2 py-2">
              <p className="text-gray-400">{t("spot.trustScore")}</p>
              <p className="mt-0.5 font-semibold text-gray-700">{(spot.trustScore * 100).toFixed(0)}%</p>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="mt-4 flex gap-2">
          <a
            href={`https://www.google.com/maps/dir/?api=1&destination=${spot.latitude},${spot.longitude}`}
            target="_blank" rel="noopener noreferrer"
            className="flex flex-1 items-center justify-center gap-1 rounded-lg bg-green-600 py-3 text-sm font-medium text-white"
          >
            {t("map.navigate")}
          </a>
          <button
            onClick={() => {
              if (!isAuthenticated) { router.push("/register"); return; }
              onClose();
              router.push(`/spots/${spot.id}`);
            }}
            className="flex flex-1 items-center justify-center gap-1 rounded-lg bg-blue-600 py-3 text-sm font-medium text-white"
          >
            {t("report.submit")}
          </button>
        </div>

        {/* Full page link */}
        <Link
          href={`/spots/${spot.id}`}
          className="mt-3 block text-center text-sm font-medium text-blue-600 hover:underline"
          onClick={onClose}
        >
          {t("map.viewFullPage")}
        </Link>
      </div>
    </div>
  );
}
