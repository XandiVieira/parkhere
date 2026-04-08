"use client";

import { useEffect, useState, useCallback } from "react";
import { leaderboardApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { LeaderboardPeriod, LeaderboardCategory, LeaderboardResponse } from "@/types/api";

export default function LeaderboardsPage() {
  const { user } = useAuthStore();
  const [period, setPeriod] = useState<LeaderboardPeriod>("WEEKLY");
  const [category, setCategory] = useState<LeaderboardCategory>("MOST_POINTS");
  const [data, setData] = useState<LeaderboardResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchLeaderboard = useCallback(async () => {
    setLoading(true);
    try {
      const res = await leaderboardApi.get(period, category);
      setData(res.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  }, [period, category]);

  useEffect(() => {
    fetchLeaderboard();
  }, [fetchLeaderboard]);

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Leaderboards</h1>

      {/* Controls */}
      <div className="mb-6 flex flex-wrap gap-4">
        <div className="flex rounded-md bg-gray-100">
          {(["WEEKLY", "MONTHLY"] as LeaderboardPeriod[]).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`rounded-md px-4 py-2 text-sm font-medium transition ${
                period === p ? "bg-blue-600 text-white" : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              {p === "WEEKLY" ? "Weekly" : "Monthly"}
            </button>
          ))}
        </div>
        <div className="flex rounded-md bg-gray-100">
          {(["MOST_POINTS", "LONGEST_STREAK"] as LeaderboardCategory[]).map((c) => (
            <button
              key={c}
              onClick={() => setCategory(c)}
              className={`rounded-md px-4 py-2 text-sm font-medium transition ${
                category === c ? "bg-blue-600 text-white" : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              {c === "MOST_POINTS" ? "Most Points" : "Longest Streak"}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="text-center text-sm text-gray-500">Loading...</div>
      ) : data && data.entries.length > 0 ? (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="px-4 py-3 font-medium text-gray-700">Rank</th>
                <th className="px-4 py-3 font-medium text-gray-700">Name</th>
                <th className="px-4 py-3 text-right font-medium text-gray-700">Score</th>
              </tr>
            </thead>
            <tbody>
              {data.entries.map((entry) => {
                const isCurrentUser = user && entry.userName === user.name;
                return (
                  <tr
                    key={entry.rank}
                    className={`border-b border-gray-100 ${isCurrentUser ? "bg-blue-50" : ""}`}
                  >
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                          entry.rank === 1
                            ? "bg-yellow-400 text-white"
                            : entry.rank === 2
                              ? "bg-gray-300 text-white"
                              : entry.rank === 3
                                ? "bg-amber-600 text-white"
                                : "bg-gray-100 text-gray-600"
                        }`}
                      >
                        {entry.rank}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={isCurrentUser ? "font-semibold text-blue-700" : "text-gray-900"}>
                        {entry.userName}
                        {isCurrentUser && (
                          <span className="ml-2 text-xs text-blue-500">(you)</span>
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-medium text-gray-700">
                      {entry.score.toLocaleString()}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-center text-sm text-gray-500">No leaderboard data available yet.</p>
      )}
    </div>
  );
}
