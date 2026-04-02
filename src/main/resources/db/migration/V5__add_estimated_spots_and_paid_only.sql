ALTER TABLE parking_spots ADD COLUMN estimated_spots INTEGER;

ALTER TABLE parking_spot_schedules ADD COLUMN paid_only BOOLEAN NOT NULL DEFAULT FALSE;
