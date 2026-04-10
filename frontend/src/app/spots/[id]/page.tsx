import type { Metadata } from "next";
import { notFound } from "next/navigation";
import SpotDetailClient from "@/components/spots/SpotDetailClient";
import type { SpotResponse, SpotSummaryResponse } from "@/types/api";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

async function fetchSpot(id: string): Promise<SpotResponse | null> {
  try {
    const res = await fetch(`${API_URL}/spots/${id}`, { next: { revalidate: 60 } });
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

async function fetchSummary(id: string): Promise<SpotSummaryResponse | null> {
  try {
    const res = await fetch(`${API_URL}/spots/${id}/summary`, { next: { revalidate: 60 } });
    if (!res.ok) return null;
    return res.json();
  } catch {
    return null;
  }
}

const TYPE_LABELS: Record<string, string> = {
  STREET: "Rua",
  PARKING_LOT: "Estacionamento",
  MALL: "Shopping",
  TERRAIN: "Terreno",
  ZONA_AZUL: "Zona Azul",
};

function formatPriceServer(min: number, max: number): string {
  if (max === 0) return "Gratis";
  if (min === max) return `R$${min}`;
  return `R$${min}-${max}`;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const spot = await fetchSpot(id);

  if (!spot) {
    return { title: "Vaga nao encontrada — ParkHere" };
  }

  const typeLabel = TYPE_LABELS[spot.type] || spot.type;
  const price = formatPriceServer(spot.priceMin, spot.priceMax);
  const description = [
    typeLabel,
    price,
    spot.address,
  ].filter(Boolean).join(" — ");

  const ogImages = spot.coverImageUrl
    ? [{ url: `${API_BASE}${spot.coverImageUrl}`, width: 800, height: 400 }]
    : [];

  return {
    title: `${spot.name} — ParkHere`,
    description,
    openGraph: {
      title: `${spot.name} — ParkHere`,
      description,
      type: "website",
      images: ogImages,
    },
    twitter: {
      card: spot.coverImageUrl ? "summary_large_image" : "summary",
      title: `${spot.name} — ParkHere`,
      description,
    },
  };
}

export default async function SpotDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  const [spot, summary] = await Promise.all([
    fetchSpot(id),
    fetchSummary(id),
  ]);

  if (!spot) {
    notFound();
  }

  return <SpotDetailClient initialSpot={spot} initialSummary={summary} />;
}
