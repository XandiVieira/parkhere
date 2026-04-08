"use client";

import type { SpotResponse } from "@/types/api";
import SpotCard from "./SpotCard";
import { formatPrice } from "@/lib/utils";

interface SpotListViewProps {
  spots: SpotResponse[];
  userLat: number;
  userLng: number;
}

function calcDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function formatDistance(meters: number): string {
  return meters < 1000 ? `${Math.round(meters)}m` : `${(meters / 1000).toFixed(1)}km`;
}

export default function SpotListView({ spots, userLat, userLng }: SpotListViewProps) {
  const sorted = [...spots].sort((a, b) => {
    const dA = calcDistance(userLat, userLng, a.latitude, a.longitude);
    const dB = calcDistance(userLat, userLng, b.latitude, b.longitude);
    return dA - dB;
  });

  if (sorted.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-gray-500">Nenhuma vaga encontrada nesta região</p>
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto bg-gray-50 px-4 py-4">
      <p className="mb-3 text-sm text-gray-500">{sorted.length} vagas encontradas</p>
      <div className="space-y-3">
        {sorted.map((spot) => {
          const dist = calcDistance(userLat, userLng, spot.latitude, spot.longitude);
          return (
            <div key={spot.id} className="relative">
              <SpotCard spot={spot} />
              <span className="absolute top-3 right-3 rounded-full bg-blue-100 px-2 py-0.5 text-[11px] font-medium text-blue-700">
                {formatDistance(dist)}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
