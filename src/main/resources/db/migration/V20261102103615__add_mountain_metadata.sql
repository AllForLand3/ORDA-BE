ALTER TABLE mountain
    ADD COLUMN province_nm VARCHAR(100),
    ADD COLUMN district_nm VARCHAR(100),
    ADD COLUMN address_label VARCHAR(200),
    ADD COLUMN elevation_m NUMERIC(8, 2),
    ADD COLUMN representative_geom geometry(Point, 5179),
    ADD CONSTRAINT chk_mountain_elevation_non_negative
        CHECK (elevation_m IS NULL OR elevation_m >= 0);
