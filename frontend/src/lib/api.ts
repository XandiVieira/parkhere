import axios from "axios";
import type {
  AuthResponse,
  GamificationResponse,
  LeaderboardCategory,
  LeaderboardPeriod,
  LeaderboardResponse,
  Page,
  RemovalRequestResponse,
  ReportResponse,
  SpotAnalyticsResponse,
  SpotResponse,
  SpotSummaryResponse,
} from "@/types/api";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// === Auth ===
export const authApi = {
  register: (name: string, email: string, password: string) =>
    api.post<AuthResponse>("/auth/register", { name, email, password }),

  login: (email: string, password: string) =>
    api.post<AuthResponse>("/auth/login", { email, password }),

  forgotPassword: (email: string) =>
    api.post("/auth/forgot-password", { email }),

  resetPassword: (token: string, newPassword: string) =>
    api.post("/auth/reset-password", { token, newPassword }),
};

// === Spots ===
export const spotsApi = {
  search: (lat: number, lng: number, radius = 800, params?: Record<string, string>) =>
    api.get<Page<SpotResponse>>("/spots", { params: { lat, lng, radius, ...params } }),

  getById: (id: string) =>
    api.get<SpotResponse>(`/spots/${id}`),

  create: (data: Record<string, unknown>) =>
    api.post<SpotResponse>("/spots", data),

  update: (id: string, data: Record<string, unknown>) =>
    api.put<SpotResponse>(`/spots/${id}`, data),

  getMine: (page = 0, size = 20) =>
    api.get<Page<SpotResponse>>("/spots/mine", { params: { page, size } }),

  getSummary: (spotId: string) =>
    api.get<SpotSummaryResponse>(`/spots/${spotId}/summary`),

  getAnalytics: (spotId: string) =>
    api.get<SpotAnalyticsResponse>(`/spots/${spotId}/analytics`),

  addFavorite: (spotId: string) =>
    api.post(`/spots/${spotId}/favorite`),

  removeFavorite: (spotId: string) =>
    api.delete(`/spots/${spotId}/favorite`),
};

// === Reports ===
export const reportsApi = {
  submit: (spotId: string, data: Record<string, unknown>) =>
    api.post<ReportResponse>(`/spots/${spotId}/reports`, data),

  getForSpot: (spotId: string, page = 0, size = 20) =>
    api.get<Page<ReportResponse>>(`/spots/${spotId}/reports`, { params: { page, size } }),
};

// === Removal ===
export const removalApi = {
  request: (spotId: string, reason?: string) =>
    api.post<RemovalRequestResponse>(`/spots/${spotId}/removal-requests`, { reason }),

  confirm: (spotId: string, requestId: string) =>
    api.post<RemovalRequestResponse>(`/spots/${spotId}/removal-requests/${requestId}/confirm`),

  getPending: (spotId: string) =>
    api.get<RemovalRequestResponse[]>(`/spots/${spotId}/removal-requests`),
};

// === Users ===
export const usersApi = {
  getProfile: () =>
    api.get("/users/me"),

  updateProfile: (name: string, nickname?: string | null) =>
    api.put("/users/me", { name, nickname }),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.put("/users/me/password", { currentPassword, newPassword }),

  updateProfilePic: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.put("/users/me/profile-pic", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  getFavorites: (page = 0, size = 20) =>
    api.get<Page<SpotResponse>>("/users/me/favorites", { params: { page, size } }),

  getPreferences: () =>
    api.get("/users/me/preferences"),

  updatePreferences: (data: { defaultSpotTypes: string[]; defaultTrustLevels: string[]; freeOnly: boolean }) =>
    api.put("/users/me/preferences", data),

  getGamification: () =>
    api.get<GamificationResponse>("/users/me/gamification"),

  getUserGamification: (userId: string) =>
    api.get<GamificationResponse>(`/users/${userId}/gamification`),
};

// === Leaderboards ===
export const leaderboardApi = {
  get: (period: LeaderboardPeriod, category: LeaderboardCategory, periodKey?: string) =>
    api.get<LeaderboardResponse>("/leaderboards", { params: { period, category, periodKey } }),
};

export default api;
