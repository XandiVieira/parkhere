ALTER TABLE parking_reports ADD COLUMN informal_charge_type VARCHAR(30);
ALTER TABLE parking_reports ADD COLUMN informal_charge_amount DOUBLE PRECISION;
ALTER TABLE parking_reports ADD COLUMN informal_charge_aggressiveness INTEGER;
ALTER TABLE parking_reports ADD COLUMN informal_charge_note VARCHAR(500);
