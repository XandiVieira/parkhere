CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE parking_spots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    price_min DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    price_max DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    trust_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_confirmations INTEGER NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parking_spots_location ON parking_spots USING GIST (location);
CREATE INDEX idx_parking_spots_type ON parking_spots(type);
CREATE INDEX idx_parking_spots_created_by ON parking_spots(created_by);
