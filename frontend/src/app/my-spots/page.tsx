"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { spotsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import type { SpotResponse, Page } from "@/types/api";
import SpotCard from "@/components/spots/SpotCard";

export default function MySpotsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [spots, setSpots] = useState<Page<SpotResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const fetchSpots = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const res = await spotsApi.getMine(p);
      setSpots(res.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    fetchSpots(page);
  }, [isAuthenticated, page, router, fetchSpots]);

  if (!isAuthenticated) return null;

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{t("mySpots.title")}</h1>

      {loading ? (
        <div className="text-center text-sm text-gray-500">{t("common.loading")}</div>
      ) : spots && spots.content.length > 0 ? (
        <>
          <div className="space-y-3">
            {spots.content.map((spot) => (
              <SpotCard key={spot.id} spot={spot} />
            ))}
          </div>

          {spots.totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={spots.first}
                className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
              >
                {t("common.previous")}
              </button>
              <span className="text-sm text-gray-500">
                {t("common.page")} {spots.number + 1} {t("common.of")} {spots.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={spots.last}
                className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
              >
                {t("common.next")}
              </button>
            </div>
          )}
        </>
      ) : (
        <p className="text-center text-sm text-gray-500">
          {t("mySpots.empty")}
        </p>
      )}
    </div>
  );
}
