ALTER TABLE parking_spots ADD COLUMN requires_booking BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE parking_spots ADD COLUMN notes VARCHAR(1000);
ALTER TABLE parking_spots ADD COLUMN last_confirmed_at TIMESTAMP;

CREATE TABLE parking_spot_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_spot_id UUID NOT NULL REFERENCES parking_spots(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_spot_schedules_spot ON parking_spot_schedules(parking_spot_id);
