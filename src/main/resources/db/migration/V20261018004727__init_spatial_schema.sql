CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgrouting;

CREATE TABLE app_user (
    user_id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    user_nm VARCHAR(50) NOT NULL,
    role_cd VARCHAR(10) NOT NULL DEFAULT 'USER' CHECK (role_cd IN ('USER', 'ADMIN')),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE source_dataset (
    dataset_id BIGSERIAL PRIMARY KEY,
    dataset_nm VARCHAR(100) NOT NULL,
    origin_cd VARCHAR(10) NOT NULL CHECK (origin_cd IN ('EXTERNAL', 'TEAM')),
    data_type_cd VARCHAR(20) NOT NULL CHECK (data_type_cd IN ('TRAIL', 'SPOT', 'DEM', 'FACILITY', 'AREA', 'PLACE')),
    provider_nm VARCHAR(100) NOT NULL,
    source_url VARCHAR(300),
    base_de DATE,
    collect_de DATE NOT NULL,
    src_crs_cd VARCHAR(20),
    file_nm VARCHAR(200),
    file_hash CHAR(64),
    license_nm VARCHAR(100),
    data_ver VARCHAR(30) NOT NULL,
    dataset_desc VARCHAR(1000),
    CONSTRAINT uq_source_dataset_name_version UNIQUE (dataset_nm, data_ver)
);

CREATE TABLE mountain (
    mountain_id BIGSERIAL PRIMARY KEY,
    mountain_nm VARCHAR(50) NOT NULL,
    src_mntn_cd VARCHAR(9),
    src_old_mntn_cd VARCHAR(10),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE service_area (
    area_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    dataset_id BIGINT NOT NULL REFERENCES source_dataset (dataset_id),
    area_nm VARCHAR(100) NOT NULL,
    geom geometry(MultiPolygon, 5179) NOT NULL,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE course (
    course_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    owner_user_id BIGINT NOT NULL REFERENCES app_user (user_id),
    course_nm VARCHAR(100) NOT NULL,
    memo VARCHAR(1000),
    create_type_cd VARCHAR(20) NOT NULL CHECK (create_type_cd IN ('IMPORT', 'DRAW', 'CANDIDATE_COPY')),
    rev_no INTEGER NOT NULL DEFAULT 1 CHECK (rev_no >= 1),
    geom geometry(LineString, 5179) NOT NULL,
    length_m NUMERIC(9, 1),
    est_time_min NUMERIC(6, 1),
    calc_difficulty_cd VARCHAR(10) CHECK (calc_difficulty_cd IN ('EASY', 'NORMAL', 'HARD')),
    src_difficulty_mix_json JSONB,
    del_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (del_yn IN ('Y', 'N'))
);

CREATE TABLE raw_trail (
    raw_trail_id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES source_dataset (dataset_id),
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    src_trail_id VARCHAR(20) NOT NULL,
    trail_nm VARCHAR(100),
    src_length_m NUMERIC(9, 1),
    src_difficulty_nm VARCHAR(10),
    src_uphill_min INTEGER,
    src_downhill_min INTEGER,
    src_surface_nm VARCHAR(50),
    geom geometry(LineString, 5179) NOT NULL,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N')),
    exclude_rsn VARCHAR(200),
    src_attr_json JSONB,
    CONSTRAINT uq_raw_trail_dataset_source UNIQUE (dataset_id, src_trail_id),
    CONSTRAINT uq_raw_trail_mountain_trail UNIQUE (mountain_id, raw_trail_id)
);

CREATE TABLE key_place (
    place_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    dataset_id BIGINT NOT NULL REFERENCES source_dataset (dataset_id),
    place_nm VARCHAR(100) NOT NULL,
    place_type_cd VARCHAR(20) NOT NULL CHECK (place_type_cd IN ('ENTRANCE', 'SUMMIT', 'JUNCTION', 'VIEW', 'WATER', 'ETC')),
    geom geometry(Point, 5179) NOT NULL,
    verify_status_cd VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' CHECK (verify_status_cd IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE trail_node (
    node_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    place_id BIGINT REFERENCES key_place (place_id),
    component_no INTEGER,
    edge_cnt INTEGER,
    geom geometry(Point, 5179) NOT NULL,
    CONSTRAINT uq_trail_node_mountain_node UNIQUE (mountain_id, node_id)
);

CREATE TABLE trail_edge (
    edge_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    raw_trail_id BIGINT NOT NULL,
    geom geometry(LineString, 5179) NOT NULL,
    length_m NUMERIC(9, 1) NOT NULL,
    fwd_uphill_yn CHAR(1) CHECK (fwd_uphill_yn IN ('Y', 'N')),
    direction_basis_cd VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' CHECK (direction_basis_cd IN ('DEM', 'MANUAL', 'UNKNOWN')),
    fwd_time_min NUMERIC(6, 1),
    rev_time_min NUMERIC(6, 1),
    fwd_ascent_m NUMERIC(7, 1),
    fwd_descent_m NUMERIC(7, 1),
    stair_spot_cnt INTEGER NOT NULL DEFAULT 0,
    pass_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (pass_yn IN ('Y', 'N')),
    CONSTRAINT fk_trail_edge_source_node_mountain
        FOREIGN KEY (mountain_id, source_node_id)
        REFERENCES trail_node (mountain_id, node_id),
    CONSTRAINT fk_trail_edge_target_node_mountain
        FOREIGN KEY (mountain_id, target_node_id)
        REFERENCES trail_node (mountain_id, node_id),
    CONSTRAINT fk_trail_edge_raw_trail_mountain
        FOREIGN KEY (mountain_id, raw_trail_id)
        REFERENCES raw_trail (mountain_id, raw_trail_id)
);

CREATE TABLE routing_run (
    run_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_user (user_id),
    start_node_id BIGINT NOT NULL REFERENCES trail_node (node_id),
    end_node_id BIGINT NOT NULL REFERENCES trail_node (node_id),
    exp_level_cd VARCHAR(20) CHECK (exp_level_cd IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    dist_first_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (dist_first_yn IN ('Y', 'N')),
    gentle_slope_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (gentle_slope_yn IN ('Y', 'N')),
    less_stair_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (less_stair_yn IN ('Y', 'N')),
    result_cd VARCHAR(20) NOT NULL CHECK (result_cd IN ('OK', 'NO_PATH', 'DISCONNECTED', 'SAME_POINT', 'NO_MATCH')),
    cand_cnt SMALLINT NOT NULL DEFAULT 0 CHECK (cand_cnt BETWEEN 0 AND 3)
);

CREATE TABLE route_candidate (
    cand_id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES routing_run (run_id),
    rank_no SMALLINT NOT NULL CHECK (rank_no BETWEEN 1 AND 3),
    priority_cond_cd VARCHAR(20) NOT NULL CHECK (priority_cond_cd IN ('DIST_FIRST', 'GENTLE_SLOPE', 'LESS_STAIR')),
    edge_seq BIGINT[] NOT NULL,
    path_hash CHAR(32) NOT NULL,
    geom geometry(LineString, 5179) NOT NULL,
    length_m NUMERIC(9, 1) NOT NULL,
    est_time_min NUMERIC(6, 1),
    ascent_m NUMERIC(7, 1),
    descent_m NUMERIC(7, 1),
    stair_spot_cnt INTEGER NOT NULL DEFAULT 0,
    src_difficulty_mix_json JSONB,
    calc_difficulty_cd VARCHAR(10) CHECK (calc_difficulty_cd IN ('EASY', 'NORMAL', 'HARD')),
    total_cost NUMERIC(12, 2) NOT NULL,
    cond_met_yn CHAR(1) NOT NULL CHECK (cond_met_yn IN ('Y', 'N')),
    CONSTRAINT uq_route_candidate_run_rank UNIQUE (run_id, rank_no),
    CONSTRAINT uq_route_candidate_run_path_hash UNIQUE (run_id, path_hash)
);

CREATE TABLE trail_spot (
    spot_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    dataset_id BIGINT NOT NULL REFERENCES source_dataset (dataset_id),
    src_spot_id VARCHAR(20),
    src_old_spot_id VARCHAR(14),
    src_class_cd VARCHAR(2),
    src_class_nm VARCHAR(50),
    detail_txt VARCHAR(200),
    etc_txt VARCHAR(200),
    geom geometry(Point, 5179) NOT NULL,
    src_geom geometry(Point, 5179) NOT NULL,
    dup_of_spot_id BIGINT REFERENCES trail_spot (spot_id),
    verify_status_cd VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' CHECK (verify_status_cd IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    verify_user_id BIGINT REFERENCES app_user (user_id),
    verify_dt TIMESTAMP,
    CONSTRAINT uq_trail_spot_dataset_source UNIQUE (dataset_id, src_spot_id)
);

CREATE TABLE spot_tag (
    spot_id BIGINT NOT NULL REFERENCES trail_spot (spot_id),
    tag_cd VARCHAR(20) NOT NULL CHECK (tag_cd IN ('STAIR', 'STEEP', 'ROPE', 'DANGER', 'WATER', 'REST', 'PEAK', 'VIEW', 'TOILET', 'PARKING', 'HELIPAD')),
    extract_type_cd VARCHAR(10) NOT NULL CHECK (extract_type_cd IN ('KEYWORD', 'MANUAL')),
    verify_status_cd VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' CHECK (verify_status_cd IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    verify_user_id BIGINT REFERENCES app_user (user_id),
    verify_dt TIMESTAMP,
    PRIMARY KEY (spot_id, tag_cd)
);

CREATE TABLE spot_edge_match (
    match_id BIGSERIAL PRIMARY KEY,
    spot_id BIGINT NOT NULL REFERENCES trail_spot (spot_id),
    edge_id BIGINT NOT NULL REFERENCES trail_edge (edge_id),
    raw_trail_id BIGINT NOT NULL REFERENCES raw_trail (raw_trail_id),
    dist_m NUMERIC(7, 2) NOT NULL CHECK (dist_m >= 0),
    pos_ratio NUMERIC(7, 6) CHECK (pos_ratio BETWEEN 0 AND 1),
    match_type_cd VARCHAR(10) NOT NULL DEFAULT 'AUTO' CHECK (match_type_cd IN ('AUTO', 'MANUAL')),
    match_status_cd VARCHAR(20) NOT NULL DEFAULT 'AUTO' CHECK (match_status_cd IN ('AUTO', 'CONFIRMED', 'CORRECTED', 'REJECTED')),
    verify_user_id BIGINT REFERENCES app_user (user_id),
    verify_dt TIMESTAMP,
    CONSTRAINT uq_spot_edge_match_spot_edge UNIQUE (spot_id, edge_id)
);

CREATE TABLE facility (
    facility_id BIGSERIAL PRIMARY KEY,
    mountain_id BIGINT NOT NULL REFERENCES mountain (mountain_id),
    dataset_id BIGINT NOT NULL REFERENCES source_dataset (dataset_id),
    src_fac_id VARCHAR(50),
    fac_type_cd VARCHAR(20) NOT NULL CHECK (fac_type_cd IN ('TOILET', 'PARKING')),
    fac_nm VARCHAR(100),
    addr VARCHAR(200),
    geom geometry(Point, 5179) NOT NULL,
    loc_basis_cd VARCHAR(20) NOT NULL CHECK (loc_basis_cd IN ('SOURCE_COORD', 'GEOCODE', 'FIELD')),
    dup_of_facility_id BIGINT REFERENCES facility (facility_id),
    verify_status_cd VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' CHECK (verify_status_cd IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    verify_user_id BIGINT REFERENCES app_user (user_id),
    verify_dt TIMESTAMP,
    src_attr_json JSONB,
    CONSTRAINT uq_facility_dataset_source UNIQUE (dataset_id, src_fac_id)
);

CREATE TABLE dem_asset (
    dem_id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES source_dataset (dataset_id),
    dem_nm VARCHAR(100) NOT NULL,
    file_path VARCHAR(300) NOT NULL,
    crs_cd VARCHAR(20) NOT NULL,
    res_m NUMERIC(6, 2) NOT NULL,
    surface_type_cd VARCHAR(10) NOT NULL CHECK (surface_type_cd IN ('DTM', 'DSM')),
    nodata_val NUMERIC(12, 3),
    extent_geom geometry(Polygon, 5179) NOT NULL,
    prep_status_cd VARCHAR(20) NOT NULL DEFAULT 'RAW' CHECK (prep_status_cd IN ('RAW', 'READY', 'FAILED')),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE terrain_analysis (
    analysis_id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES course (course_id),
    rev_no INTEGER NOT NULL,
    dem_id BIGINT NOT NULL REFERENCES dem_asset (dem_id),
    sample_itv_m NUMERIC(6, 1) NOT NULL,
    status_cd VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' CHECK (status_cd IN ('REQUESTED', 'RUNNING', 'DONE', 'FAILED')),
    err_msg VARCHAR(2000),
    ascent_m NUMERIC(7, 1),
    descent_m NUMERIC(7, 1),
    max_elev_m NUMERIC(7, 2),
    min_elev_m NUMERIC(7, 2),
    sample_cnt INTEGER,
    missing_cnt INTEGER,
    done_dt TIMESTAMP,
    CONSTRAINT uq_terrain_analysis_course_revision_dem_interval UNIQUE (course_id, rev_no, dem_id, sample_itv_m)
);

CREATE TABLE elevation_sample (
    analysis_id BIGINT NOT NULL REFERENCES terrain_analysis (analysis_id),
    seq_no INTEGER NOT NULL,
    dist_m NUMERIC(9, 1) NOT NULL,
    elev_m NUMERIC(7, 2),
    PRIMARY KEY (analysis_id, seq_no)
);

CREATE INDEX idx_service_area_mountain_id ON service_area (mountain_id);
CREATE INDEX idx_service_area_dataset_id ON service_area (dataset_id);
CREATE INDEX idx_service_area_geom ON service_area USING GIST (geom);
CREATE INDEX idx_course_mountain_id ON course (mountain_id);
CREATE INDEX idx_course_owner_user_id ON course (owner_user_id);
CREATE INDEX idx_course_geom ON course USING GIST (geom);
CREATE INDEX idx_raw_trail_mountain_id ON raw_trail (mountain_id);
CREATE INDEX idx_raw_trail_geom ON raw_trail USING GIST (geom);
CREATE INDEX idx_key_place_mountain_id ON key_place (mountain_id);
CREATE INDEX idx_key_place_dataset_id ON key_place (dataset_id);
CREATE INDEX idx_key_place_geom ON key_place USING GIST (geom);
CREATE INDEX idx_trail_node_mountain_id ON trail_node (mountain_id);
CREATE INDEX idx_trail_node_place_id ON trail_node (place_id);
CREATE INDEX idx_trail_node_geom ON trail_node USING GIST (geom);
CREATE INDEX idx_trail_edge_mountain_id ON trail_edge (mountain_id);
CREATE INDEX idx_trail_edge_source_node_id ON trail_edge (source_node_id);
CREATE INDEX idx_trail_edge_target_node_id ON trail_edge (target_node_id);
CREATE INDEX idx_trail_edge_raw_trail_id ON trail_edge (raw_trail_id);
CREATE INDEX idx_trail_edge_geom ON trail_edge USING GIST (geom);
CREATE INDEX idx_routing_run_user_id ON routing_run (user_id);
CREATE INDEX idx_routing_run_start_node_id ON routing_run (start_node_id);
CREATE INDEX idx_routing_run_end_node_id ON routing_run (end_node_id);
CREATE INDEX idx_route_candidate_geom ON route_candidate USING GIST (geom);
CREATE INDEX idx_trail_spot_mountain_id ON trail_spot (mountain_id);
CREATE INDEX idx_trail_spot_dup_of_spot_id ON trail_spot (dup_of_spot_id);
CREATE INDEX idx_trail_spot_verify_user_id ON trail_spot (verify_user_id);
CREATE INDEX idx_trail_spot_geom ON trail_spot USING GIST (geom);
CREATE INDEX idx_trail_spot_src_geom ON trail_spot USING GIST (src_geom);
CREATE INDEX idx_spot_tag_verify_user_id ON spot_tag (verify_user_id);
CREATE INDEX idx_spot_edge_match_edge_id ON spot_edge_match (edge_id);
CREATE INDEX idx_spot_edge_match_raw_trail_id ON spot_edge_match (raw_trail_id);
CREATE INDEX idx_spot_edge_match_verify_user_id ON spot_edge_match (verify_user_id);
CREATE INDEX idx_facility_mountain_id ON facility (mountain_id);
CREATE INDEX idx_facility_dup_of_facility_id ON facility (dup_of_facility_id);
CREATE INDEX idx_facility_verify_user_id ON facility (verify_user_id);
CREATE INDEX idx_facility_geom ON facility USING GIST (geom);
CREATE INDEX idx_dem_asset_extent_geom ON dem_asset USING GIST (extent_geom);
CREATE INDEX idx_terrain_analysis_dem_id ON terrain_analysis (dem_id);
