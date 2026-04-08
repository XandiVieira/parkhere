"use client";

import { Suspense, useState, useRef, useCallback, useEffect } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useAuthStore } from "@/stores/auth";
import axios from "axios";
import { t } from "@/lib/i18n";
import { usersApi } from "@/lib/api";
import type { TrustLevel, SpotType, SpotResponse } from "@/types/api";
import QuickReportModal from "@/components/reports/QuickReportModal";
import SpotListView from "@/components/spots/SpotListView";

const MapView = dynamic(() => import("@/components/map/MapView"), {
  ssr: false,
  loading: () => (
    <div className="flex h-[calc(100vh-3.5rem)] items-center justify-center bg-gray-100">
      <div className="text-gray-500">{t("map.loading")}</div>
    </div>
  ),
});

export interface MapFilters {
  trustLevels: Set<TrustLevel>;
  spotTypes: Set<SpotType>;
  freeOnly: boolean;
}

const ALL_TRUST: TrustLevel[] = ["HIGH", "MEDIUM", "LOW", "NO_DATA"];
const ALL_TYPES: SpotType[] = ["STREET", "PARKING_LOT", "MALL", "TERRAIN", "ZONA_AZUL"];

const TRUST_META: Record<TrustLevel, { label: string; color: string; dot: string }> = {
  HIGH: { label: "Alta", color: "text-green-600", dot: "bg-green-500" },
  MEDIUM: { label: "Média", color: "text-yellow-600", dot: "bg-yellow-500" },
  LOW: { label: "Baixa", color: "text-orange-600", dot: "bg-orange-500" },
  NO_DATA: { label: "Sem dados", color: "text-indigo-600", dot: "bg-indigo-500" },
};

const TYPE_META: Record<SpotType, { label: string; icon: string }> = {
  STREET: { label: "Rua", icon: "🛣" },
  PARKING_LOT: { label: "Estacionamento", icon: "🅿️" },
  MALL: { label: "Shopping", icon: "🏬" },
  TERRAIN: { label: "Terreno", icon: "🏗" },
  ZONA_AZUL: { label: "Zona Azul", icon: "🔵" },
};

export default function HomePage() {
  return (
    <Suspense fallback={<div className="flex h-[calc(100vh-3.5rem)] items-center justify-center"><span className="text-gray-500">{t("map.loading")}</span></div>}>
      <HomePageInner />
    </Suspense>
  );
}

function HomePageInner() {
  const { isAuthenticated } = useAuthStore();
  const searchParams = useSearchParams();
  const [searchValue, setSearchValue] = useState("");
  const [searching, setSearching] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [viewMode, setViewMode] = useState<"map" | "list">("map");
  const [filters, setFilters] = useState<MapFilters>({
    trustLevels: new Set(ALL_TRUST),
    spotTypes: new Set(ALL_TYPES),
    freeOnly: false,
  });
  const flyToRef = useRef<((lat: number, lng: number) => void) | null>(null);
  const [nearbySpots, setNearbySpots] = useState<SpotResponse[]>([]);
  const [userPos, setUserPos] = useState<{ lat: number; lng: number } | null>(null);
  const [quickReportSpot, setQuickReportSpot] = useState<SpotResponse | null>(null);

  // Load user preferences as default filters
  useEffect(() => {
    if (!isAuthenticated) return;
    usersApi.getPreferences().then(res => {
      const d = res.data;
      const types = d.defaultSpotTypes?.length ? new Set(d.defaultSpotTypes as SpotType[]) : new Set(ALL_TYPES);
      const trust = d.defaultTrustLevels?.length ? new Set(d.defaultTrustLevels as TrustLevel[]) : new Set(ALL_TRUST);
      setFilters({ trustLevels: trust, spotTypes: types, freeOnly: d.freeOnly || false });
    }).catch(() => {});
  }, [isAuthenticated]);

  // Fly to coordinates from URL params — retry until flyToRef is ready
  const pendingFlyTo = useRef<{lat: number; lng: number} | null>(null);
  useEffect(() => {
    const lat = searchParams.get("lat");
    const lng = searchParams.get("lng");
    if (lat && lng) {
      pendingFlyTo.current = { lat: parseFloat(lat), lng: parseFloat(lng) };
      if (flyToRef.current) {
        flyToRef.current(pendingFlyTo.current.lat, pendingFlyTo.current.lng);
        pendingFlyTo.current = null;
      }
    }
  }, [searchParams]);

  const handleSearch = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchValue.trim() || !flyToRef.current) return;
    setSearching(true);
    try {
      const query = searchValue.toLowerCase().includes("porto alegre")
        ? searchValue
        : `${searchValue}, Porto Alegre, RS, Brasil`;
      const res = await axios.get("https://nominatim.openstreetmap.org/search", {
        params: { q: query, format: "json", limit: 1, countrycodes: "br" },
        headers: { "User-Agent": "ParkHere/1.0" },
      });
      if (res.data.length > 0) {
        const { lat, lon } = res.data[0];
        flyToRef.current(parseFloat(lat), parseFloat(lon));
      }
    } catch { /* silently fail */ } finally { setSearching(false); }
  }, [searchValue]);

  const toggleTrust = (level: TrustLevel) => {
    setFilters(prev => {
      const next = new Set(prev.trustLevels);
      next.has(level) ? next.delete(level) : next.add(level);
      return { ...prev, trustLevels: next };
    });
  };

  const toggleType = (type: SpotType) => {
    setFilters(prev => {
      const next = new Set(prev.spotTypes);
      next.has(type) ? next.delete(type) : next.add(type);
      return { ...prev, spotTypes: next };
    });
  };

  const activeFilterCount =
    (ALL_TRUST.length - filters.trustLevels.size) +
    (ALL_TYPES.length - filters.spotTypes.size) +
    (filters.freeOnly ? 1 : 0);

  return (
    <div className="relative flex flex-1 flex-col">
      {/* Search bar */}
      <div className="absolute top-4 left-1/2 z-[1000] w-full max-w-md -translate-x-1/2 px-4">
        <form onSubmit={handleSearch} className="flex overflow-hidden rounded-lg bg-white shadow-lg">
          <input type="text" value={searchValue} onChange={(e) => setSearchValue(e.target.value)}
            placeholder={t("map.search")} className="flex-1 px-4 py-3 text-sm focus:outline-none" />
          <button type="submit" disabled={searching}
            className="bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
            {searching ? "..." : t("map.searchBtn")}
          </button>
        </form>
      </div>

      {/* View toggle + Filter button */}
      <div className="absolute top-[4.5rem] right-4 z-[1000] flex overflow-hidden rounded-lg bg-white shadow-lg">
        <button onClick={() => setViewMode("map")}
          className={`px-3 py-2 text-sm font-medium ${viewMode === "map" ? "bg-blue-600 text-white" : "text-gray-700 hover:bg-gray-50"}`}>
          🗺 Mapa
        </button>
        <button onClick={() => setViewMode("list")}
          className={`px-3 py-2 text-sm font-medium ${viewMode === "list" ? "bg-blue-600 text-white" : "text-gray-700 hover:bg-gray-50"}`}>
          📋 Lista
        </button>
      </div>

      <button
        onClick={() => setFiltersOpen(!filtersOpen)}
        className="absolute top-[4.5rem] left-4 z-[1000] flex items-center gap-1.5 rounded-lg bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-lg hover:bg-gray-50"
      >
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
        </svg>
        Filtros
        {activeFilterCount > 0 && (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-[10px] text-white">
            {activeFilterCount}
          </span>
        )}
      </button>

      {/* Filter panel */}
      {filtersOpen && (
        <div className="absolute top-[7rem] left-4 z-[1000] w-72 rounded-lg bg-white p-4 shadow-xl">
          {/* Trust Level */}
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Confiança</p>
          <div className="mb-4 space-y-1.5">
            {ALL_TRUST.map(level => (
              <label key={level} className="flex cursor-pointer items-center gap-2 text-sm">
                <input type="checkbox" checked={filters.trustLevels.has(level)}
                  onChange={() => toggleTrust(level)} className="rounded" />
                <span className={`h-3 w-3 rounded-full ${TRUST_META[level].dot}`} />
                <span className={TRUST_META[level].color}>{TRUST_META[level].label}</span>
              </label>
            ))}
          </div>

          {/* Spot Type */}
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Tipo de vaga</p>
          <div className="mb-4 space-y-1.5">
            {ALL_TYPES.map(type => (
              <label key={type} className="flex cursor-pointer items-center gap-2 text-sm">
                <input type="checkbox" checked={filters.spotTypes.has(type)}
                  onChange={() => toggleType(type)} className="rounded" />
                <span>{TYPE_META[type].icon}</span>
                <span>{TYPE_META[type].label}</span>
              </label>
            ))}
          </div>

          {/* Free only */}
          <label className="flex cursor-pointer items-center gap-2 border-t border-gray-100 pt-3 text-sm">
            <input type="checkbox" checked={filters.freeOnly}
              onChange={() => setFilters(prev => ({ ...prev, freeOnly: !prev.freeOnly }))} className="rounded" />
            <span>Somente grátis</span>
          </label>

          <button onClick={() => setFiltersOpen(false)}
            className="mt-3 w-full rounded-md bg-blue-600 py-1.5 text-sm font-medium text-white hover:bg-blue-700">
            Aplicar
          </button>
        </div>
      )}

      {/* Legend */}
      <div className="absolute bottom-6 left-4 z-[1000] rounded-lg bg-white/90 px-3 py-2 text-[11px] shadow backdrop-blur">
        <div className="flex items-center gap-3">
          {ALL_TRUST.map(level => (
            <span key={level} className="flex items-center gap-1">
              <span className={`inline-block h-2.5 w-2.5 rounded-full ${TRUST_META[level].dot}`} />
              {TRUST_META[level].label}
            </span>
          ))}
        </div>
      </div>

      {/* Map or List */}
      <div className="h-[calc(100vh-3.5rem)]">
        {viewMode === "map" ? (
          <MapView filters={filters} onFlyToReady={(fn) => {
            flyToRef.current = fn;
            if (pendingFlyTo.current) {
              fn(pendingFlyTo.current.lat, pendingFlyTo.current.lng);
              pendingFlyTo.current = null;
            }
          }} onSpotsLoaded={(spots, lat, lng) => {
            setNearbySpots(spots);
            setUserPos({ lat, lng });
          }} />
        ) : (
          <SpotListView
            spots={nearbySpots.filter(spot => {
              if (!filters.trustLevels.has(spot.trustLevel)) return false;
              if (!filters.spotTypes.has(spot.type)) return false;
              if (filters.freeOnly && spot.priceMax > 0) return false;
              return true;
            })}
            userLat={userPos?.lat || -30.03}
            userLng={userPos?.lng || -51.22}
          />
        )}
      </div>

      {/* Floating action buttons */}
      {isAuthenticated && (
        <div className="fixed right-6 bottom-6 z-[1000] flex flex-col gap-3">
          <button
            onClick={() => {
              if (nearbySpots.length === 0) {
                alert("Nenhuma vaga próxima encontrada");
                return;
              }
              // Find nearest spot
              const sorted = [...nearbySpots].sort((a, b) => {
                const distA = Math.hypot(a.latitude - (userPos?.lat || 0), a.longitude - (userPos?.lng || 0));
                const distB = Math.hypot(b.latitude - (userPos?.lat || 0), b.longitude - (userPos?.lng || 0));
                return distA - distB;
              });
              setQuickReportSpot(sorted[0]);
            }}
            className="flex h-12 w-12 items-center justify-center rounded-full bg-green-600 text-lg shadow-lg transition hover:bg-green-700"
            title="Relatar vaga próxima"
          >
            📍
          </button>
          <Link href="/spots/new"
            className="flex h-14 w-14 items-center justify-center rounded-full bg-blue-600 text-2xl font-bold text-white shadow-lg transition hover:bg-blue-700"
            title="Adicionar vaga">
            +
          </Link>
        </div>
      )}

      {/* Quick Report Modal */}
      {quickReportSpot && userPos && (
        <QuickReportModal
          spot={quickReportSpot}
          userLat={userPos.lat}
          userLng={userPos.lng}
          onClose={() => setQuickReportSpot(null)}
          onSuccess={() => {
            setQuickReportSpot(null);
            alert("Relato enviado! +5 pontos 🎉");
          }}
        />
      )}
    </div>
  );
}
