"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { usersApi, spotsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { SpotResponse, Page } from "@/types/api";
import SpotCard from "@/components/spots/SpotCard";

export default function FavoritesPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [favorites, setFavorites] = useState<Page<SpotResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const fetchFavorites = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const res = await usersApi.getFavorites(p);
      setFavorites(res.data);
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
    fetchFavorites(page);
  }, [isAuthenticated, page, router, fetchFavorites]);

  const handleRemoveFavorite = async (spotId: string) => {
    try {
      await spotsApi.removeFavorite(spotId);
      fetchFavorites(page);
    } catch {
      // silent
    }
  };

  if (!isAuthenticated) return null;

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Favorites</h1>

      {loading ? (
        <div className="text-center text-sm text-gray-500">Loading...</div>
      ) : favorites && favorites.content.length > 0 ? (
        <>
          <div className="space-y-3">
            {favorites.content.map((spot) => (
              <SpotCard
                key={spot.id}
                spot={spot}
                actions={
                  <button
                    onClick={() => handleRemoveFavorite(spot.id)}
                    className="text-sm text-red-600 hover:underline"
                  >
                    Remove from favorites
                  </button>
                }
              />
            ))}
          </div>

          {favorites.totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={favorites.first}
                className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
              >
                Previous
              </button>
              <span className="text-sm text-gray-500">
                Page {favorites.number + 1} of {favorites.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={favorites.last}
                className="rounded-md bg-gray-100 px-3 py-1 text-sm disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      ) : (
        <p className="text-center text-sm text-gray-500">
          No favorites yet. Browse the map and favorite spots you like!
        </p>
      )}
    </div>
  );
}
