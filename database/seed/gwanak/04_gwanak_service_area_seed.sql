-- 관악산 서비스 가능 영역 생성
--
-- 03_gwanak_network_seed.sql 실행 후 생성된 통행 가능 trail_edge를 기준으로 한다.
-- EPSG:5179의 거리 단위를 사용해 등산로 양쪽 200m를 서비스 가능 영역으로 잡고,
-- 버퍼 결합 과정에서 생긴 내부 구멍은 외곽 링만 다시 폴리곤으로 만들어 제거한다.
-- mountain_id와 dataset_id는 환경마다 달라질 수 있으므로 이름으로 조회한다.
-- 기존 관악산 service_area가 있으면 갱신하고, 없으면 새로 생성한다.

BEGIN;

-- 산과 파생 데이터셋이 정확히 하나씩 존재하고 네트워크가 준비됐는지 확인한다.
DO $$
DECLARE
    mountain_count bigint;
    dataset_count bigint;
    edge_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO mountain_count
    FROM public.mountain
    WHERE mountain_nm = '관악산'
      AND use_yn = 'Y';

    IF mountain_count <> 1 THEN
        RAISE EXCEPTION
            '사용 중인 관악산 데이터가 정확히 1건이어야 합니다. 실제: %건',
            mountain_count;
    END IF;

    SELECT COUNT(*)
    INTO dataset_count
    FROM public.source_dataset
    WHERE dataset_nm = 'derived:gwanak:service_area';

    IF dataset_count <> 1 THEN
        RAISE EXCEPTION
            '서비스 영역 데이터셋이 정확히 1건이어야 합니다. 실제: %건',
            dataset_count;
    END IF;

    SELECT COUNT(*)
    INTO edge_count
    FROM public.trail_edge AS edge
    JOIN public.mountain AS mountain
      ON mountain.mountain_id = edge.mountain_id
    WHERE mountain.mountain_nm = '관악산'
      AND mountain.use_yn = 'Y'
      AND edge.pass_yn = 'Y'
      AND edge.geom IS NOT NULL;

    IF edge_count = 0 THEN
        RAISE EXCEPTION
            '관악산 통행 가능 trail_edge가 없습니다. 03_gwanak_network_seed.sql을 먼저 실행하세요.';
    END IF;
END
$$;

-- 01 시드의 고정 폴리곤 메타데이터를 현재 파생 규칙에 맞게 갱신한다.
-- 결과 형상은 현재 trail_edge에서 매번 계산되므로 고정 파일 해시는 사용하지 않는다.
UPDATE public.source_dataset
SET file_nm = '04_gwanak_service_area_seed.sql',
    file_hash = NULL,
    data_ver = 'network-buffer-200m-v1',
    dataset_desc = '관악산 통행 가능 trail_edge에 200m 버퍼를 적용하고 내부 구멍을 제거한 서비스 가능 영역.'
WHERE dataset_nm = 'derived:gwanak:service_area';

WITH target_mountain AS (
    SELECT mountain_id
    FROM public.mountain
    WHERE mountain_nm = '관악산'
      AND use_yn = 'Y'
),
target_dataset AS (
    SELECT dataset_id
    FROM public.source_dataset
    WHERE dataset_nm = 'derived:gwanak:service_area'
),
buffered_area AS (
    SELECT
        mountain.mountain_id,
        ST_Union(ST_Buffer(edge.geom, 200.0)) AS geom
    FROM target_mountain AS mountain
    JOIN public.trail_edge AS edge
      ON edge.mountain_id = mountain.mountain_id
    WHERE edge.pass_yn = 'Y'
      AND edge.geom IS NOT NULL
    GROUP BY mountain.mountain_id
),
polygon_parts AS (
    SELECT
        area.mountain_id,
        (ST_Dump(ST_CollectionExtract(area.geom, 3))).geom AS geom
    FROM buffered_area AS area
),
filled_area AS (
    SELECT
        part.mountain_id,
        ST_Multi(
            ST_Union(
                ST_MakePolygon(ST_ExteriorRing(part.geom))
            )
        )::geometry(MultiPolygon, 5179) AS geom
    FROM polygon_parts AS part
    GROUP BY part.mountain_id
),
updated AS (
    UPDATE public.service_area AS service_area
    SET dataset_id = dataset.dataset_id,
        area_nm = '관악산 서비스 가능 영역',
        geom = area.geom,
        use_yn = 'Y'
    FROM filled_area AS area
    CROSS JOIN target_dataset AS dataset
    WHERE service_area.mountain_id = area.mountain_id
    RETURNING service_area.area_id
)
INSERT INTO public.service_area (
    mountain_id,
    dataset_id,
    area_nm,
    geom,
    use_yn
)
SELECT
    area.mountain_id,
    dataset.dataset_id,
    '관악산 서비스 가능 영역',
    area.geom,
    'Y'
FROM filled_area AS area
CROSS JOIN target_dataset AS dataset
WHERE NOT EXISTS (
    SELECT 1
    FROM updated
);

-- 생성 결과와 전체 통행 가능 edge 포함 여부를 검증한다.
DO $$
DECLARE
    service_area_count bigint;
    invalid_area_count bigint;
    outside_edge_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO service_area_count
    FROM public.service_area AS service_area
    JOIN public.mountain AS mountain
      ON mountain.mountain_id = service_area.mountain_id
    WHERE mountain.mountain_nm = '관악산'
      AND mountain.use_yn = 'Y'
      AND service_area.use_yn = 'Y';

    IF service_area_count <> 1 THEN
        RAISE EXCEPTION
            '사용 중인 관악산 서비스 영역이 정확히 1건이어야 합니다. 실제: %건',
            service_area_count;
    END IF;

    SELECT COUNT(*)
    INTO invalid_area_count
    FROM public.service_area AS service_area
    JOIN public.mountain AS mountain
      ON mountain.mountain_id = service_area.mountain_id
    WHERE mountain.mountain_nm = '관악산'
      AND mountain.use_yn = 'Y'
      AND service_area.use_yn = 'Y'
      AND (
          service_area.geom IS NULL
          OR ST_IsEmpty(service_area.geom)
          OR NOT ST_IsValid(service_area.geom)
          OR ST_SRID(service_area.geom) <> 5179
          OR GeometryType(service_area.geom) <> 'MULTIPOLYGON'
      );

    IF invalid_area_count > 0 THEN
        RAISE EXCEPTION
            '관악산 서비스 영역의 형상 유형, SRID 또는 유효성이 올바르지 않습니다.';
    END IF;

    SELECT COUNT(*)
    INTO outside_edge_count
    FROM public.trail_edge AS edge
    JOIN public.mountain AS mountain
      ON mountain.mountain_id = edge.mountain_id
    WHERE mountain.mountain_nm = '관악산'
      AND mountain.use_yn = 'Y'
      AND edge.pass_yn = 'Y'
      AND edge.geom IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM public.service_area AS service_area
          WHERE service_area.mountain_id = edge.mountain_id
            AND service_area.use_yn = 'Y'
            AND ST_Covers(service_area.geom, edge.geom)
      );

    IF outside_edge_count > 0 THEN
        RAISE EXCEPTION
            '서비스 영역에 포함되지 않은 관악산 trail_edge가 %건 있습니다.',
            outside_edge_count;
    END IF;
END
$$;

COMMIT;
