-- ParkHere - Complete initial schema

CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================
-- Users
-- ============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    reputation_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================
-- Parking Spots
-- ============================================
CREATE TABLE parking_spots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    price_min DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    price_max DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    trust_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_confirmations INTEGER NOT NULL DEFAULT 0,
    requires_booking BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_spots INTEGER,
    notes VARCHAR(1000),
    address VARCHAR(500),
    last_confirmed_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parking_spots_location ON parking_spots USING GIST (location);
CREATE INDEX idx_parking_spots_type ON parking_spots(type);
CREATE INDEX idx_parking_spots_created_by ON parking_spots(created_by);
CREATE INDEX idx_parking_spots_active ON parking_spots(active);

-- ============================================
-- Parking Spot Schedules
-- ============================================
CREATE TABLE parking_spot_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_spot_id UUID NOT NULL REFERENCES parking_spots(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    paid_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_spot_schedules_spot ON parking_spot_schedules(parking_spot_id);

-- ============================================
-- Parking Reports
-- ============================================
CREATE TABLE parking_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_spot_id UUID NOT NULL REFERENCES parking_spots(id),
    user_id UUID NOT NULL REFERENCES users(id),
    availability_status VARCHAR(20) NOT NULL,
    estimated_price DOUBLE PRECISION,
    safety_rating INTEGER CHECK (safety_rating BETWEEN 1 AND 5),
    informal_charge_reported BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(500),
    gps_distance_meters DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parking_reports_spot ON parking_reports(parking_spot_id);
CREATE INDEX idx_parking_reports_user ON parking_reports(user_id);
CREATE INDEX idx_parking_reports_created_at ON parking_reports(created_at DESC);

-- ============================================
-- Report Images
-- ============================================
CREATE TABLE report_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES parking_reports(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_images_report ON report_images(report_id);

-- ============================================
-- Spot Removal Requests
-- ============================================
CREATE TABLE spot_removal_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_spot_id UUID NOT NULL REFERENCES parking_spots(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmations_needed INTEGER NOT NULL DEFAULT 3,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE spot_removal_confirmations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    removal_request_id UUID NOT NULL REFERENCES spot_removal_requests(id),
    confirmed_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(removal_request_id, confirmed_by)
);

CREATE INDEX idx_removal_requests_spot ON spot_removal_requests(parking_spot_id);
CREATE INDEX idx_removal_requests_status ON spot_removal_requests(status);
CREATE INDEX idx_removal_confirmations_request ON spot_removal_confirmations(removal_request_id);

-- ============================================
-- User Favorites
-- ============================================
CREATE TABLE user_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    parking_spot_id UUID NOT NULL REFERENCES parking_spots(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, parking_spot_id)
);

CREATE INDEX idx_user_favorites_user ON user_favorites(user_id);

-- ============================================
-- Password Reset Tokens
-- ============================================
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token UUID NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);

-- ============================================
-- Gamification
-- ============================================
CREATE TABLE user_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    total_points INTEGER NOT NULL DEFAULT 0,
    weekly_points INTEGER NOT NULL DEFAULT 0,
    monthly_points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    badge_type VARCHAR(30) NOT NULL,
    earned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, badge_type)
);

CREATE TABLE user_streaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_report_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_points_user ON user_points(user_id);
CREATE INDEX idx_user_badges_user ON user_badges(user_id);
CREATE INDEX idx_user_streaks_user ON user_streaks(user_id);

-- ============================================
-- Spot Analytics
-- ============================================
CREATE TABLE spot_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_spot_id UUID NOT NULL REFERENCES parking_spots(id),
    day_of_week VARCHAR(10) NOT NULL,
    hour_bucket INTEGER NOT NULL CHECK (hour_bucket BETWEEN 0 AND 23),
    avg_availability_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    avg_price DOUBLE PRECISION,
    avg_safety_rating DOUBLE PRECISION,
    informal_charge_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    report_count INTEGER NOT NULL DEFAULT 0,
    computed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_spot_analytics_composite ON spot_analytics(parking_spot_id, day_of_week, hour_bucket);
CREATE INDEX idx_spot_analytics_spot ON spot_analytics(parking_spot_id);

-- ============================================
-- Leaderboards
-- ============================================
CREATE TABLE leaderboard_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    period VARCHAR(20) NOT NULL,
    period_key VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    rank INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_leaderboard_unique ON leaderboard_entries(period, period_key, category, user_id);
CREATE INDEX idx_leaderboard_lookup ON leaderboard_entries(period, period_key, category, rank);
