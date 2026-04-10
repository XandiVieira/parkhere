import { t } from "@/lib/i18n";
import type { SpotType, TrustLevel } from "@/types/api";

const ADDRESS_NOISE = [
  "região geográfica imediata",
  "região geográfica intermediária",
  "região metropolitana",
  "região sul",
  "região norte",
  "região sudeste",
  "região nordeste",
  "região centro-oeste",
];

export function cleanAddress(address: string): string {
  return address
    .split(",")
    .map(p => p.trim())
    .filter(p => !ADDRESS_NOISE.some(noise => p.toLowerCase().includes(noise)))
    .join(", ");
}

export function formatPrice(min: number, max: number): string {
  if (min === 0 && max === 0) return t("spot.free");
  if (min === max) return `R$${min.toFixed(0)}`;
  return `R$${min.toFixed(0)} - R$${max.toFixed(0)}`;
}

export function formatDate(isoString: string): string {
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return "just now";
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHour < 24) return `${diffHour}h ago`;
  if (diffDay === 1) return "yesterday";
  if (diffDay < 7) return `${diffDay}d ago`;
  return date.toLocaleDateString();
}

export function spotTypeLabel(type: SpotType): string {
  return t(`type.${type}` as any);
}

export function spotTypeIcon(type: SpotType): string {
  const icons: Record<SpotType, string> = {
    STREET: "P",
    PARKING_LOT: "L",
    MALL: "M",
    TERRAIN: "T",
    ZONA_AZUL: "Z",
  };
  return icons[type] ?? "?";
}

export function trustLevelColor(level: TrustLevel): string {
  const colors: Record<TrustLevel, string> = {
    HIGH: "bg-green-500",
    MEDIUM: "bg-yellow-500",
    LOW: "bg-orange-500",
    NO_DATA: "bg-gray-400",
  };
  return colors[level] ?? "bg-gray-400";
}

export function trustLevelTextColor(level: TrustLevel): string {
  const colors: Record<TrustLevel, string> = {
    HIGH: "text-green-700",
    MEDIUM: "text-yellow-700",
    LOW: "text-orange-700",
    NO_DATA: "text-gray-500",
  };
  return colors[level] ?? "text-gray-500";
}

export function trustLevelBorderColor(level: TrustLevel): string {
  const colors: Record<TrustLevel, string> = {
    HIGH: "border-green-500",
    MEDIUM: "border-yellow-500",
    LOW: "border-orange-500",
    NO_DATA: "border-gray-400",
  };
  return colors[level] ?? "border-gray-400";
}

export function trustLevelMarkerColor(level: TrustLevel): string {
  const colors: Record<TrustLevel, string> = {
    HIGH: "#22c55e",
    MEDIUM: "#eab308",
    LOW: "#f97316",
    NO_DATA: "#9ca3af",
  };
  return colors[level] ?? "#9ca3af";
}

export function apiBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
}

export function extractApiError(err: unknown): string | undefined {
  if (err && typeof err === "object" && "response" in err) {
    return (err as { response?: { data?: { message?: string } } }).response?.data?.message;
  }
  return undefined;
}
