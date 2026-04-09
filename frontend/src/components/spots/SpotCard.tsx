"use client";

import Link from "next/link";
import type { SpotResponse } from "@/types/api";
import TrustBadge from "./TrustBadge";
import { formatPrice, spotTypeIcon, cleanAddress } from "@/lib/utils";
import { t } from "@/lib/i18n";

interface SpotCardProps {
  spot: SpotResponse;
  actions?: React.ReactNode;
  distanceLabel?: string;
}

export default function SpotCard({ spot, actions, distanceLabel }: SpotCardProps) {
  return (
    <Link
      href={`/spots/${spot.id}`}
      className="block rounded-lg border border-gray-200 bg-white p-4 shadow-sm transition hover:shadow-md"
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 text-sm font-bold text-blue-700">
            {spotTypeIcon(spot.type)}
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">{spot.name}</h3>
            <p className="text-sm text-gray-500">{t(`type.${spot.type}` as any)}</p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-1">
          <TrustBadge level={spot.trustLevel} />
          {distanceLabel && (
            <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[11px] font-medium text-blue-700">
              {distanceLabel}
            </span>
          )}
        </div>
      </div>

      {spot.address && (
        <p className="mt-2 text-sm text-gray-600 line-clamp-1">{cleanAddress(spot.address)}</p>
      )}

      {(spot.informalChargeFrequency === "OFTEN" || spot.informalChargeFrequency === "ALWAYS") && (
        <div className="mt-2 flex items-center gap-1 rounded bg-red-50 px-2 py-1 text-xs font-medium text-red-700">
          🚨 {t("informal.warning")}
        </div>
      )}

      <div className="mt-3 flex items-center justify-between text-sm">
        <span className="font-medium text-gray-800">
          {formatPrice(spot.priceMin, spot.priceMax)}
        </span>
        <span className="text-gray-500">
          {spot.totalConfirmations} {t("spot.confirmations")}
        </span>
      </div>

      {actions && (
        <div className="mt-3 border-t border-gray-100 pt-3" onClick={(e) => e.preventDefault()}>
          {actions}
        </div>
      )}
    </Link>
  );
}
