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
