-- 관악산 등산로 네트워크 최초 생성
-- 대상 산: mountain_id = 2
-- 대상 좌표계: EPSG:5179
--
-- 생성 원칙:
--   * 신규 빈 DB에서 01, 02 시드 다음에 한 번 실행한다.
--   * 기존 trail_edge 또는 trail_node가 있으면 참조 데이터와 장소 연결을 보호하기 위해 중단한다.
--   * use_yn = 'Y'인 모든 raw_trail을 대상으로 한다.
--   * raw trail을 모든 교차점에서 분할하고 분할 구간의 끝점을 노드 후보로 사용한다.
--   * 서로 1m 이내인 분할 구간 끝점을 하나의 노드로 묶는다.
--   * 분할 구간마다 trail_edge를 생성하고 원본 raw_trail_id를 보존한다.
--   * 원천 데이터만으로 진행 방향을 확정할 수 없으므로 방향·시간·고도 값은
--     UNKNOWN 또는 NULL로 두고 원천 값은 raw_trail에 보존한다.
--   * length_m은 EPSG:5179 geometry의 실제 길이로 계산한다.
--   * edge_cnt와 component_no는 생성된 전체 네트워크를 기준으로 다시 계산한다.

BEGIN;

-- 같은 산에 대한 네트워크 생성이 동시에 실행되지 않도록 트랜잭션 잠금을 건다.
SELECT pg_advisory_xact_lock(5179, 2);

-- 이 파일은 신규 빈 DB의 최초 네트워크 생성 전용이다.
-- 기존 네트워크를 삭제하면 routing_run, spot_edge_match의 FK 참조가 깨질 수 있고,
-- FK가 없는 route_candidate.edge_seq와 기존 trail_node.place_id도 유효하지 않게 된다.
-- 네트워크가 이미 있으면 어떤 데이터도 삭제하지 않고 전체 작업을 중단한다.
DO $$
DECLARE
    existing_edge_count bigint;
    existing_node_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO existing_edge_count
    FROM public.trail_edge
    WHERE mountain_id = 2;

    SELECT COUNT(*)
    INTO existing_node_count
    FROM public.trail_node
    WHERE mountain_id = 2;

    IF existing_edge_count > 0 OR existing_node_count > 0 THEN
        RAISE EXCEPTION
            '관악산 네트워크가 이미 존재합니다(edge %, node %). 기존 참조와 place_id 연결을 보호하기 위해 최초 생성 시드를 중단합니다.',
            existing_edge_count,
            existing_node_count;
    END IF;
END
$$;

-- 활성 raw trail이 없거나 빈 geometry가 있으면 전체 작업을 중단한다.
DO $$
DECLARE
    active_raw_trail_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO active_raw_trail_count
    FROM public.raw_trail
    WHERE mountain_id = 2
      AND use_yn = 'Y';

    IF active_raw_trail_count = 0 THEN
        RAISE EXCEPTION 'mountain_id 2의 활성 raw_trail이 없습니다.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.raw_trail
        WHERE mountain_id = 2
          AND use_yn = 'Y'
          AND ST_IsEmpty(geom)
    ) THEN
        RAISE EXCEPTION 'mountain_id 2의 활성 raw_trail에 빈 geometry가 있습니다.';
    END IF;
END
$$;

-- 전체 활성 raw trail을 이번 네트워크 생성 대상으로 고정한다.
CREATE TEMP TABLE tmp_gwanak_raw_trail
ON COMMIT DROP
AS
SELECT
    rt.raw_trail_id,
    rt.mountain_id,
    rt.geom
FROM public.raw_trail AS rt
WHERE rt.mountain_id = 2
  AND rt.use_yn = 'Y';

CREATE UNIQUE INDEX tmp_gwanak_raw_trail_pk
    ON tmp_gwanak_raw_trail (raw_trail_id);

-- 전체 선형을 교차점에서 noding한 뒤 모든 교차점 후보를 추출한다.
-- 한 raw trail의 끝점이 다른 raw trail의 내부에 닿는 경우도 이 단계에서 노드 후보가 된다.
CREATE TEMP TABLE tmp_gwanak_noded_line
ON COMMIT DROP
AS
WITH linework AS (
    SELECT ST_UnaryUnion(ST_Collect(geom)) AS geom
    FROM tmp_gwanak_raw_trail
)
SELECT
    ROW_NUMBER() OVER (ORDER BY ST_AsEWKB(dumped.geom))::bigint AS noded_line_id,
    dumped.geom::geometry(LineString, 5179) AS geom
FROM linework
CROSS JOIN LATERAL ST_Dump(ST_CollectionExtract(linework.geom, 2)) AS dumped;

CREATE UNIQUE INDEX tmp_gwanak_noded_line_pk
    ON tmp_gwanak_noded_line (noded_line_id);

CREATE INDEX tmp_gwanak_noded_line_geom_gix
    ON tmp_gwanak_noded_line
    USING gist (geom);

CREATE TEMP TABLE tmp_gwanak_noding_point
ON COMMIT DROP
AS
SELECT ST_StartPoint(geom)::geometry(Point, 5179) AS geom
FROM tmp_gwanak_noded_line

UNION

SELECT ST_EndPoint(geom)::geometry(Point, 5179) AS geom
FROM tmp_gwanak_noded_line;

CREATE UNIQUE INDEX tmp_gwanak_noding_point_pk
    ON tmp_gwanak_noding_point (ST_AsEWKB(geom));

CREATE INDEX tmp_gwanak_noding_point_geom_gix
    ON tmp_gwanak_noding_point
    USING gist (geom);

-- 각 raw trail을 자신 위의 모든 noding point에서 분할한다.
-- ST_ClosestPoint로 1mm 이내의 수치 오차를 선 위에 투영하고,
-- ST_Snap으로 교차를 보장한 뒤 ST_Split에 전달한다.
CREATE TEMP TABLE tmp_gwanak_segment
ON COMMIT DROP
AS
WITH split_parts AS (
    SELECT
        raw.raw_trail_id,
        raw.mountain_id,
        raw.geom AS raw_geom,
        dumped.geom::geometry(LineString, 5179) AS geom
    FROM tmp_gwanak_raw_trail AS raw
    LEFT JOIN LATERAL (
        SELECT ST_Collect(projected.geom) AS geom
        FROM (
            SELECT ST_ClosestPoint(raw.geom, point.geom)::geometry(Point, 5179) AS geom
            FROM tmp_gwanak_noding_point AS point
            WHERE ST_DWithin(raw.geom, point.geom, 0.001)
            GROUP BY ST_ClosestPoint(raw.geom, point.geom)
        ) AS projected
    ) AS blade
      ON TRUE
    CROSS JOIN LATERAL ST_Dump(
        CASE
            WHEN blade.geom IS NULL THEN raw.geom
            ELSE ST_Split(
                ST_Snap(raw.geom, blade.geom, 0.001),
                blade.geom
            )
        END
    ) AS dumped
    WHERE GeometryType(dumped.geom) = 'LINESTRING'
      AND ST_Length(dumped.geom) > 0.001
),
oriented_parts AS (
    SELECT
        raw_trail_id,
        mountain_id,
        CASE
            WHEN ST_LineLocatePoint(raw_geom, ST_StartPoint(geom))
               <= ST_LineLocatePoint(raw_geom, ST_EndPoint(geom))
                THEN geom
            ELSE ST_Reverse(geom)::geometry(LineString, 5179)
        END AS geom,
        LEAST(
            ST_LineLocatePoint(raw_geom, ST_StartPoint(geom)),
            ST_LineLocatePoint(raw_geom, ST_EndPoint(geom))
        ) AS start_fraction
    FROM split_parts
)
SELECT
    ROW_NUMBER() OVER (
        ORDER BY raw_trail_id, start_fraction, ST_AsEWKB(geom)
    )::bigint AS segment_id,
    raw_trail_id,
    mountain_id,
    geom
FROM oriented_parts;

CREATE UNIQUE INDEX tmp_gwanak_segment_pk
    ON tmp_gwanak_segment (segment_id);

CREATE INDEX tmp_gwanak_segment_raw_trail_idx
    ON tmp_gwanak_segment (raw_trail_id);

CREATE INDEX tmp_gwanak_segment_geom_gix
    ON tmp_gwanak_segment
    USING gist (geom);

-- 분할 과정에서 raw trail이 누락되거나 교차점이 분할되지 않았으면 중단한다.
DO $$
DECLARE
    invalid_segment_source_count bigint;
    unsplit_noding_point_count bigint;
BEGIN
    WITH segment_summary AS (
        SELECT
            raw_trail_id,
            COUNT(*) AS segment_count,
            SUM(ST_Length(geom)) AS total_length_m
        FROM tmp_gwanak_segment
        GROUP BY raw_trail_id
    )
    SELECT COUNT(*)
    INTO invalid_segment_source_count
    FROM tmp_gwanak_raw_trail AS raw
    LEFT JOIN segment_summary AS summary
      ON summary.raw_trail_id = raw.raw_trail_id
    WHERE summary.segment_count IS NULL
       OR ABS(ST_Length(raw.geom) - summary.total_length_m) > 0.01;

    IF invalid_segment_source_count > 0 THEN
        RAISE EXCEPTION
            '분할 구간이 누락됐거나 원본 길이와 일치하지 않는 raw_trail이 %건 있습니다.',
            invalid_segment_source_count;
    END IF;

    SELECT COUNT(*)
    INTO unsplit_noding_point_count
    FROM tmp_gwanak_raw_trail AS raw
    JOIN tmp_gwanak_noding_point AS point
      ON ST_DWithin(raw.geom, point.geom, 0.001)
    CROSS JOIN LATERAL (
        SELECT ST_ClosestPoint(raw.geom, point.geom)::geometry(Point, 5179) AS geom
    ) AS projected
    WHERE ST_LineLocatePoint(raw.geom, projected.geom) > 0.000000001
      AND ST_LineLocatePoint(raw.geom, projected.geom) < 0.999999999
      AND NOT EXISTS (
          SELECT 1
          FROM tmp_gwanak_segment AS segment
          WHERE segment.raw_trail_id = raw.raw_trail_id
            AND (
                ST_DWithin(ST_StartPoint(segment.geom), projected.geom, 0.001)
                OR ST_DWithin(ST_EndPoint(segment.geom), projected.geom, 0.001)
            )
      );

    IF unsplit_noding_point_count > 0 THEN
        RAISE EXCEPTION
            'raw_trail 내부의 noding point 중 분할 구간 끝점으로 반영되지 않은 지점이 %건 있습니다.',
            unsplit_noding_point_count;
    END IF;
END
$$;

-- 분할된 각 segment에서 시작점과 끝점을 추출한다.
CREATE TEMP TABLE tmp_gwanak_endpoint
ON COMMIT DROP
AS
SELECT
    ROW_NUMBER() OVER (
        ORDER BY endpoint.segment_id, endpoint.endpoint_type
    )::bigint AS endpoint_id,
    endpoint.segment_id,
    endpoint.raw_trail_id,
    endpoint.endpoint_type,
    endpoint.geom
FROM (
    SELECT
        segment_id,
        raw_trail_id,
        'SOURCE'::varchar AS endpoint_type,
        ST_StartPoint(geom)::geometry(Point, 5179) AS geom
    FROM tmp_gwanak_segment

    UNION ALL

    SELECT
        segment_id,
        raw_trail_id,
        'TARGET'::varchar AS endpoint_type,
        ST_EndPoint(geom)::geometry(Point, 5179) AS geom
    FROM tmp_gwanak_segment
) AS endpoint;

CREATE UNIQUE INDEX tmp_gwanak_endpoint_pk
    ON tmp_gwanak_endpoint (segment_id, endpoint_type);

CREATE UNIQUE INDEX tmp_gwanak_endpoint_id_uq
    ON tmp_gwanak_endpoint (endpoint_id);

CREATE INDEX tmp_gwanak_endpoint_geom_gix
    ON tmp_gwanak_endpoint
    USING gist (geom);

-- 서로 1m 이내인 끝점의 인접 관계를 만든 뒤 재귀 탐색으로 연결 군집을 구한다.
-- minpoints가 1인 DBSCAN과 같은 연결 결과를 내며 PostGIS 버전별 윈도 함수
-- 구문 차이에 의존하지 않는다.
CREATE TEMP TABLE tmp_gwanak_endpoint_clustered
ON COMMIT DROP
AS
WITH RECURSIVE adjacency AS (
    SELECT
        endpoint_a.endpoint_id AS from_endpoint_id,
        endpoint_b.endpoint_id AS to_endpoint_id
    FROM tmp_gwanak_endpoint AS endpoint_a
    JOIN tmp_gwanak_endpoint AS endpoint_b
      ON ST_DWithin(endpoint_a.geom, endpoint_b.geom, 1.0)
),
reachable AS (
    SELECT
        endpoint_id AS start_endpoint_id,
        endpoint_id AS reached_endpoint_id
    FROM tmp_gwanak_endpoint

    UNION

    SELECT
        reachable.start_endpoint_id,
        adjacency.to_endpoint_id
    FROM reachable
    JOIN adjacency
      ON adjacency.from_endpoint_id = reachable.reached_endpoint_id
),
cluster_membership AS (
    SELECT
        reached_endpoint_id AS endpoint_id,
        MIN(start_endpoint_id)::bigint AS cluster_no
    FROM reachable
    GROUP BY reached_endpoint_id
)
SELECT
    endpoint.segment_id,
    endpoint.raw_trail_id,
    endpoint.endpoint_type,
    endpoint.geom,
    membership.cluster_no
FROM tmp_gwanak_endpoint AS endpoint
JOIN cluster_membership AS membership
  ON membership.endpoint_id = endpoint.endpoint_id;

CREATE UNIQUE INDEX tmp_gwanak_endpoint_clustered_pk
    ON tmp_gwanak_endpoint_clustered (segment_id, endpoint_type);

CREATE INDEX tmp_gwanak_endpoint_clustered_cluster_idx
    ON tmp_gwanak_endpoint_clustered (cluster_no);

-- 각 군집에서 정렬상 첫 번째 끝점을 대표 노드 좌표로 사용한다.
-- 평균 좌표를 사용하지 않으므로 노드는 항상 실제 segment 끝점과 일치한다.
CREATE TEMP TABLE tmp_gwanak_node_candidate
ON COMMIT DROP
AS
SELECT DISTINCT ON (cluster_no)
    cluster_no,
    geom
FROM tmp_gwanak_endpoint_clustered
ORDER BY cluster_no, segment_id, endpoint_type;

CREATE UNIQUE INDEX tmp_gwanak_node_candidate_cluster_uq
    ON tmp_gwanak_node_candidate (cluster_no);

INSERT INTO public.trail_node (
    mountain_id,
    place_id,
    component_no,
    edge_cnt,
    geom
)
SELECT
    2,
    NULL,
    NULL,
    0,
    candidate.geom
FROM tmp_gwanak_node_candidate AS candidate
ORDER BY candidate.cluster_no;

-- 자동 생성된 node_id를 끝점 군집 번호와 연결한다.
CREATE TEMP TABLE tmp_gwanak_node_map
ON COMMIT DROP
AS
SELECT
    candidate.cluster_no,
    node.node_id
FROM tmp_gwanak_node_candidate AS candidate
JOIN public.trail_node AS node
  ON node.mountain_id = 2
 AND ST_Equals(node.geom, candidate.geom);

CREATE UNIQUE INDEX tmp_gwanak_node_map_cluster_uq
    ON tmp_gwanak_node_map (cluster_no);

CREATE UNIQUE INDEX tmp_gwanak_node_map_node_uq
    ON tmp_gwanak_node_map (node_id);

-- 각 분할 segment의 시작 노드와 끝 노드를 결정한다.
CREATE TEMP TABLE tmp_gwanak_edge_node_map
ON COMMIT DROP
AS
SELECT
    source_endpoint.segment_id,
    source_endpoint.raw_trail_id,
    source_node.node_id AS source_node_id,
    target_node.node_id AS target_node_id
FROM tmp_gwanak_endpoint_clustered AS source_endpoint
JOIN tmp_gwanak_node_map AS source_node
  ON source_node.cluster_no = source_endpoint.cluster_no
JOIN tmp_gwanak_endpoint_clustered AS target_endpoint
  ON target_endpoint.segment_id = source_endpoint.segment_id
 AND target_endpoint.endpoint_type = 'TARGET'
JOIN tmp_gwanak_node_map AS target_node
  ON target_node.cluster_no = target_endpoint.cluster_no
WHERE source_endpoint.endpoint_type = 'SOURCE';

CREATE UNIQUE INDEX tmp_gwanak_edge_node_map_pk
    ON tmp_gwanak_edge_node_map (segment_id);

-- 분할 segment 하나당 edge 하나를 생성하며 원본 raw_trail_id를 보존한다.
INSERT INTO public.trail_edge (
    mountain_id,
    source_node_id,
    target_node_id,
    raw_trail_id,
    geom,
    length_m,
    fwd_uphill_yn,
    direction_basis_cd,
    fwd_time_min,
    rev_time_min,
    fwd_ascent_m,
    fwd_descent_m,
    stair_spot_cnt,
    pass_yn
)
SELECT
    segment.mountain_id,
    edge_node.source_node_id,
    edge_node.target_node_id,
    segment.raw_trail_id,
    segment.geom,
    ST_Length(segment.geom),
    NULL,
    'UNKNOWN',
    NULL,
    NULL,
    NULL,
    NULL,
    0,
    'Y'
FROM tmp_gwanak_segment AS segment
JOIN tmp_gwanak_edge_node_map AS edge_node
  ON edge_node.segment_id = segment.segment_id
ORDER BY segment.segment_id;

-- 각 노드에 접속한 edge 수를 계산한다. 자기 루프는 차수 2로 계산된다.
WITH incident_edge AS (
    SELECT source_node_id AS node_id
    FROM public.trail_edge
    WHERE mountain_id = 2

    UNION ALL

    SELECT target_node_id AS node_id
    FROM public.trail_edge
    WHERE mountain_id = 2
),
edge_count AS (
    SELECT node_id, COUNT(*)::integer AS edge_cnt
    FROM incident_edge
    GROUP BY node_id
)
UPDATE public.trail_node AS node
SET edge_cnt = edge_count.edge_cnt
FROM edge_count
WHERE node.mountain_id = 2
  AND node.node_id = edge_count.node_id;

-- pgRouting으로 연결요소를 계산하고 1부터 연속된 component_no를 부여한다.
WITH connected AS (
    SELECT component, node
    FROM pgr_connectedComponents(
        'SELECT edge_id AS id,
                source_node_id AS source,
                target_node_id AS target,
                length_m::float8 AS cost
         FROM public.trail_edge
         WHERE mountain_id = 2'
    )
),
ranked AS (
    SELECT
        node,
        DENSE_RANK() OVER (ORDER BY component)::integer AS component_no
    FROM connected
)
UPDATE public.trail_node AS trail_node
SET component_no = ranked.component_no
FROM ranked
WHERE trail_node.mountain_id = 2
  AND trail_node.node_id = ranked.node;

-- 생성 결과가 분할 segment와 일치하지 않으면 트랜잭션을 중단한다.
DO $$
DECLARE
    expected_edge_count bigint;
    actual_edge_count bigint;
    missing_node_metadata_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO expected_edge_count
    FROM tmp_gwanak_segment;

    SELECT COUNT(*)
    INTO actual_edge_count
    FROM public.trail_edge
    WHERE mountain_id = 2;

    IF actual_edge_count <> expected_edge_count THEN
        RAISE EXCEPTION
            'trail_edge 건수 불일치: 예상 %, 실제 %',
            expected_edge_count,
            actual_edge_count;
    END IF;

    SELECT COUNT(*)
    INTO missing_node_metadata_count
    FROM public.trail_node
    WHERE mountain_id = 2
      AND (
          component_no IS NULL
          OR edge_cnt IS NULL
          OR edge_cnt = 0
      );

    IF missing_node_metadata_count > 0 THEN
        RAISE EXCEPTION
            'component_no 또는 edge_cnt가 계산되지 않은 노드가 %개 있습니다.',
            missing_node_metadata_count;
    END IF;
END
$$;

COMMIT;
