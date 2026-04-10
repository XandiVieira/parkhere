"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import L from "leaflet";
import "@/lib/leaflet-fix";
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import { spotsApi } from "@/lib/api";
import type { SpotResponse, TrustLevel } from "@/types/api";
import { formatPrice, trustLevelMarkerColor } from "@/lib/utils";
import TrustBadge from "@/components/spots/TrustBadge";
import Link from "next/link";
import { t } from "@/lib/i18n";

const DEFAULT_CENTER: [number, number] = [-30.0346, -51.2177]; // Porto Alegre
const DEFAULT_ZOOM = 13;

function createPinIcon(color: string, priceLabel?: string, hasInformalCharge?: boolean): L.DivIcon {
  const label = priceLabel
    ? `<div style="position:absolute;top:42px;left:50%;transform:translateX(-50%);white-space:nowrap;font-size:10px;font-weight:600;color:#333;background:#fff;padding:1px 4px;border-radius:3px;box-shadow:0 1px 3px rgba(0,0,0,.3)">${priceLabel}</div>`
    : "";
  const warning = hasInformalCharge
    ? `<div style="position:absolute;top:-6px;right:-6px;width:18px;height:18px;background:#dc2626;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;font-size:10px;line-height:1">⚠</div>`
    : "";
  return L.divIcon({
    className: "",
    iconSize: [30, 55],
    iconAnchor: [15, 40],
    popupAnchor: [0, -40],
    html: `<div style="position:relative"><svg width="30" height="40" viewBox="0 0 30 40" xmlns="http://www.w3.org/2000/svg">
      <path d="M15 0C6.7 0 0 6.7 0 15c0 10.5 15 25 15 25s15-14.5 15-25C30 6.7 23.3 0 15 0z" fill="${color}" stroke="#fff" stroke-width="2"/>
      <circle cx="15" cy="14" r="6" fill="#fff"/>
    </svg>${warning}${label}</div>`,
  });
}

const TRUST_COLORS: Record<TrustLevel, string> = {
  HIGH: "#22c55e",
  MEDIUM: "#eab308",
  LOW: "#f97316",
  NO_DATA: "#6366f1",
};

function getSpotIcon(spot: SpotResponse): L.DivIcon {
  const color = TRUST_COLORS[spot.trustLevel];
  const price = spot.priceMax > 0 ? `R$${spot.priceMin}-${spot.priceMax}` : t("spot.free");
  const hasInformal = spot.informalChargeFrequency === "OFTEN" || spot.informalChargeFrequency === "ALWAYS";
  return createPinIcon(color, price, hasInformal);
}

import type { MapFilters } from "@/app/page";

interface MapViewProps {
  filters?: MapFilters;
  onFlyToReady?: (flyTo: (lat: number, lng: number) => void) => void;
  onSpotsLoaded?: (spots: SpotResponse[], userLat: number, userLng: number) => void;
  onSpotSelect?: (spot: SpotResponse) => void;
}

function MapEventHandler({ onMoveEnd }: { onMoveEnd: (lat: number, lng: number, map: L.Map) => void }) {
  const map = useMapEvents({
    moveend: () => {
      const center = map.getCenter();
      onMoveEnd(center.lat, center.lng, map);
    },
  });
  return null;
}

export default function MapView({ filters, onFlyToReady, onSpotsLoaded, onSpotSelect }: MapViewProps) {
  const [spots, setSpots] = useState<SpotResponse[]>([]);
  const [center, setCenter] = useState<[number, number]>(DEFAULT_CENTER);
  const [loading, setLoading] = useState(false);
  const [located, setLocated] = useState(false);
  const mapRef = useRef<L.Map | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const newCenter: [number, number] = [pos.coords.latitude, pos.coords.longitude];
        setCenter(newCenter);
        setLocated(true);
        if (mapRef.current) {
          mapRef.current.setView(newCenter, DEFAULT_ZOOM);
        }
      },
      () => {
        setLocated(true);
      },
      { timeout: 5000 }
    );
  }, []);

  const fetchSpots = useCallback(
    async (lat: number, lng: number, map?: L.Map) => {
      setLoading(true);
      try {
        let radius = 20000; // 20km default
        if (map) {
          const bounds = map.getBounds();
          const c = bounds.getCenter();
          const corner = bounds.getNorthEast();
          radius = Math.max(c.distanceTo(corner), 5000);
        }
        const res = await spotsApi.search(lat, lng, radius);
        setSpots(res.data.content);
        if (onSpotsLoaded) onSpotsLoaded(res.data.content, lat, lng);
      } catch {
        // silently fail
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (onFlyToReady && mapRef.current) {
      onFlyToReady((lat: number, lng: number) => {
        mapRef.current?.flyTo([lat, lng], 15);
      });
    }
  }, [onFlyToReady, located]);

  useEffect(() => {
    fetchSpots(center[0], center[1]);
  }, [center, fetchSpots]);

  const handleMoveEnd = useCallback(
    (lat: number, lng: number, map: L.Map) => {
      fetchSpots(lat, lng, map);
    },
    [fetchSpots]
  );

  if (!located) {
    return (
      <div className="flex h-full items-center justify-center bg-gray-100">
        <div className="text-gray-500">{t("map.locating")}</div>
      </div>
    );
  }

  return (
    <div className="relative h-full w-full">
      {loading && (
        <div className="absolute top-2 right-2 z-[1000] rounded-md bg-white px-3 py-1 text-xs text-gray-500 shadow">
          {t("map.loadingSpots")}
        </div>
      )}
      <MapContainer
        center={center}
        zoom={DEFAULT_ZOOM}
        className="h-full w-full"
        ref={mapRef}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapEventHandler onMoveEnd={handleMoveEnd} />
        {spots.filter(spot => {
          if (!filters) return true;
          if (!filters.trustLevels.has(spot.trustLevel)) return false;
          if (!filters.spotTypes.has(spot.type)) return false;
          if (filters.freeOnly && spot.priceMax > 0) return false;
          if (filters.noInformalCharge && (spot.informalChargeFrequency === "OFTEN" || spot.informalChargeFrequency === "ALWAYS")) return false;
          return true;
        }).map((spot) => (
          <Marker
            key={spot.id}
            position={[spot.latitude, spot.longitude]}
            icon={getSpotIcon(spot)}
            eventHandlers={onSpotSelect ? { click: () => onSpotSelect(spot) } : undefined}
          >
            <Popup>
              <div className="min-w-[180px]">
                <h3 className="font-semibold text-gray-900">{spot.name}</h3>
                <p className="text-xs text-gray-500">{t(`type.${spot.type}` as any)}</p>
                <div className="mt-1">
                  <TrustBadge level={spot.trustLevel} />
                </div>
                <p className="mt-1 text-sm text-gray-700">
                  {formatPrice(spot.priceMin, spot.priceMax)}
                </p>
                <div className="mt-2 flex items-center gap-3">
                  <Link
                    href={`/spots/${spot.id}`}
                    className="text-sm font-medium text-blue-600 hover:underline"
                  >
                    {t("map.details")}
                  </Link>
                  <a
                    href={`https://www.google.com/maps/dir/?api=1&destination=${spot.latitude},${spot.longitude}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 rounded bg-green-600 px-2 py-1 text-xs font-medium text-white hover:bg-green-700"
                  >
                    🧭 Navegar
                  </a>
                </div>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}
