// === Enums ===
export type SpotType = "STREET" | "PARKING_LOT" | "MALL" | "TERRAIN" | "ZONA_AZUL";
export type AvailabilityStatus = "AVAILABLE" | "UNAVAILABLE" | "UNKNOWN";
export type TrustLevel = "HIGH" | "MEDIUM" | "LOW" | "NO_DATA";
export type Role = "USER" | "ADMIN";
export type RemovalStatus = "PENDING" | "CONFIRMED" | "REJECTED";
export type BadgeType =
  | "FIRST_STEPS" | "REGULAR" | "VETERAN" | "CENTURION"
  | "SPOT_DISCOVERER" | "CARTOGRAPHER" | "RELIABLE"
  | "NIGHT_OWL" | "EARLY_BIRD" | "COMMUNITY_GUARDIAN";
export type LeaderboardPeriod = "WEEKLY" | "MONTHLY";
export type LeaderboardCategory = "MOST_POINTS" | "MOST_REPORTS" | "LONGEST_STREAK";

// === Auth ===
export interface UserResponse {
  id: string;
  name: string;
  nickname: string | null;
  email: string;
  role: Role;
  reputationScore: number;
  profilePicUrl: string | null;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

// === Spots ===
export interface ScheduleResponse {
  id: string;
  dayOfWeek: string;
  openTime: string;
  closeTime: string;
  paidOnly: boolean;
}

export interface SpotResponse {
  id: string;
  name: string;
  type: SpotType;
  latitude: number;
  longitude: number;
  priceMin: number;
  priceMax: number;
  requiresBooking: boolean;
  estimatedSpots: number | null;
  notes: string | null;
  trustScore: number;
  trustLevel: TrustLevel;
  totalConfirmations: number;
  lastConfirmedAt: string | null;
  address: string | null;
  informalChargeFrequency: string;
  schedules: ScheduleResponse[];
  createdBy: string;
  createdAt: string;
}

export interface SpotSummaryResponse {
  spotId: string;
  name: string;
  type: SpotType;
  latitude: number;
  longitude: number;
  priceMin: number;
  priceMax: number;
  requiresBooking: boolean;
  address: string | null;
  trustScore: number;
  trustLevel: TrustLevel;
  totalConfirmations: number;
  lastConfirmedAt: string | null;
  dominantAvailability: AvailabilityStatus;
  avgEstimatedPrice: number | null;
  avgSafetyRating: number | null;
  informalChargePercentage: number;
  informalChargeReportedRecently: boolean;
  lastReportAt: string | null;
}

// === Reports ===
export interface ReportImageResponse {
  filename: string;
  originalFilename: string;
  contentType: string;
}

export interface ReportResponse {
  id: string;
  spotId: string;
  userId: string;
  availabilityStatus: AvailabilityStatus;
  estimatedPrice: number | null;
  safetyRating: number | null;
  informalChargeReported: boolean;
  informalChargeType: string | null;
  informalChargeAmount: number | null;
  informalChargeAggressiveness: number | null;
  informalChargeNote: string | null;
  note: string | null;
  gpsDistanceMeters: number;
  images: ReportImageResponse[];
  createdAt: string;
}

// === Gamification ===
export interface GamificationResponse {
  totalPoints: number;
  weeklyPoints: number;
  monthlyPoints: number;
  badges: { type: BadgeType; earnedAt: string }[];
  streak: { currentStreak: number; longestStreak: number; lastReportDate: string | null };
}

// === Leaderboard ===
export interface LeaderboardEntryResponse {
  rank: number;
  userName: string;
  score: number;
}

export interface LeaderboardResponse {
  period: LeaderboardPeriod;
  periodKey: string;
  category: LeaderboardCategory;
  entries: LeaderboardEntryResponse[];
}

// === Analytics ===
export interface HourAnalytics {
  hour: number;
  availabilityRate: number;
  avgPrice: number | null;
  avgSafetyRating: number | null;
  informalChargeRate: number;
  reportCount: number;
}

export interface DayAnalytics {
  dayOfWeek: string;
  hours: HourAnalytics[];
}

export interface SpotAnalyticsResponse {
  spotId: string;
  days: DayAnalytics[];
}

// === Removal ===
export interface RemovalRequestResponse {
  id: string;
  spotId: string;
  requestedBy: string;
  reason: string | null;
  status: RemovalStatus;
  confirmationCount: number;
  confirmationsNeeded: number;
  createdAt: string;
}

// === Pagination ===
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
