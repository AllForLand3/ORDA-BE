-- 관악산 상세지점-등산로 구간 매칭 및 구간별 계단 지점 수

-- 대상 산: mountain_id = 2

-- 대상 좌표계: EPSG:5179

BEGIN;

-- 이 파일보다 01~05 시드를 먼저 실행해야 한다.
-- trail_edge 는 03 시드가 생성하며 edge_id 는 시퀀스 값이므로, 이 파일은 edge_id 를 고정하지 않고
-- 실행 시점의 geometry 로 가장 가까운 구간을 계산한다. 다시 실행해도 같은 행이 중복되지 않는다.
--
-- 매칭 원칙:
--   * 모든 상세지점을 가장 가까운 trail_edge 하나에 연결한다. 거리가 같으면 edge_id 가 작은 구간을 쓴다.
--   * 최대 거리 제한을 두지 않고 dist_m 에 실제 거리(m)를 저장한다. 먼 매칭의 불확실성은 dist_m 으로 판단한다.
--     (2026-11-01 로컬 기준 746건 중 708건 10m 이내, 731건 20m 이내, 최대 58.3m)
--   * pos_ratio 는 구간 geometry 시작점 기준 0~1 위치이다.
--   * 이미 매칭이 있는 지점(수동 보정 포함)은 건드리지 않는다.
--   * 검수 전이므로 match_type_cd, match_status_cd 는 AUTO 이다.

-- 03 네트워크 시드와 동시에 실행되지 않도록 같은 트랜잭션 잠금을 건다.
SELECT pg_advisory_xact_lock(5179, 2);

-- 선행 데이터가 없으면 아무것도 넣지 않고 중단한다.
DO $$
DECLARE
    edge_cnt bigint;
    spot_cnt bigint;
BEGIN
    SELECT count(*) INTO edge_cnt FROM public.trail_edge WHERE mountain_id = 2;
    SELECT count(*) INTO spot_cnt FROM public.trail_spot WHERE mountain_id = 2;
    IF edge_cnt = 0 OR spot_cnt = 0 THEN
        RAISE EXCEPTION '선행 시드 누락: trail_edge %, trail_spot %', edge_cnt, spot_cnt;
    END IF;
END
$$;

INSERT INTO public.spot_edge_match (
    spot_id, edge_id, raw_trail_id, dist_m, pos_ratio, match_type_cd, match_status_cd
)
SELECT
    spot.spot_id,
    nearest.edge_id,
    nearest.raw_trail_id,
    round(ST_Distance(spot.geom, nearest.geom)::numeric, 2),
    round(ST_LineLocatePoint(nearest.geom, spot.geom)::numeric, 6),
    'AUTO',
    'AUTO'
FROM public.trail_spot AS spot
CROSS JOIN LATERAL (
    SELECT edge.edge_id, edge.raw_trail_id, edge.geom
    FROM public.trail_edge AS edge
    WHERE edge.mountain_id = spot.mountain_id
    ORDER BY ST_Distance(spot.geom, edge.geom), edge.edge_id
    LIMIT 1
) AS nearest
WHERE spot.mountain_id = 2
  AND NOT EXISTS (
      SELECT 1
      FROM public.spot_edge_match AS existing
      WHERE existing.spot_id = spot.spot_id
  )
ON CONFLICT (spot_id, edge_id)
DO NOTHING;

-- 구간별 계단 지점 수 (trail_edge.stair_spot_cnt)
--   * 대상: STAIR 태그가 REJECTED 가 아니고 매칭이 REJECTED 가 아닌 지점
--   * 같은 계단이 여러 점으로 찍힌 경우를 한 번만 센다.
--     - '계단시작'·'계단끝' 표시 지점은 같은 구간에서 짝을 지어 max(시작 수, 끝 수)로 센다.
--     - 그 밖의 계단 지점은 같은 구간 안에서 10m 이내로 붙어 있으면 하나로 센다.
--   * 한 계단이 구간 경계를 넘으면 걸친 구간마다 1로 센다.
--   * 매번 전체를 다시 계산하므로, 값이 달라진 구간만 갱신한다. 그 밖의 trail_edge 컬럼은 바꾸지 않는다.
WITH stair_spot AS (
    SELECT
        match.edge_id,
        spot.geom,
        CASE
            WHEN spot.detail_txt ~ '계단시작' THEN 'START'
            WHEN spot.detail_txt ~ '계단끝' THEN 'END'
            ELSE 'POINT'
        END AS stair_kind
    FROM public.spot_edge_match AS match
    JOIN public.trail_spot AS spot ON spot.spot_id = match.spot_id
    JOIN public.spot_tag AS tag ON tag.spot_id = spot.spot_id
    WHERE spot.mountain_id = 2
      AND match.match_status_cd <> 'REJECTED'
      AND tag.tag_cd = 'STAIR'
      AND tag.verify_status_cd <> 'REJECTED'
),
point_cluster AS (
    SELECT
        edge_id,
        ST_ClusterDBSCAN(geom, eps := 10, minpoints := 1) OVER (PARTITION BY edge_id) AS cluster_no
    FROM stair_spot
    WHERE stair_kind = 'POINT'
),
edge_stair AS (
    SELECT edge_id, count(DISTINCT cluster_no) AS stair_cnt
    FROM point_cluster
    GROUP BY edge_id
    UNION ALL
    SELECT
        edge_id,
        greatest(count(*) FILTER (WHERE stair_kind = 'START'), count(*) FILTER (WHERE stair_kind = 'END'))
    FROM stair_spot
    WHERE stair_kind <> 'POINT'
    GROUP BY edge_id
),
edge_total AS (
    SELECT edge.edge_id, coalesce(sum(edge_stair.stair_cnt), 0)::integer AS stair_cnt
    FROM public.trail_edge AS edge
    LEFT JOIN edge_stair ON edge_stair.edge_id = edge.edge_id
    WHERE edge.mountain_id = 2
    GROUP BY edge.edge_id
)
UPDATE public.trail_edge AS edge
SET stair_spot_cnt = edge_total.stair_cnt
FROM edge_total
WHERE edge.edge_id = edge_total.edge_id
  AND edge.stair_spot_cnt IS DISTINCT FROM edge_total.stair_cnt;

-- 매칭되지 않은 상세지점이 있으면 조용히 넘어가지 않도록 실패시킨다.
DO $$
DECLARE
    unmatched_cnt bigint;
BEGIN
    SELECT count(*) INTO unmatched_cnt
    FROM public.trail_spot AS spot
    WHERE spot.mountain_id = 2
      AND NOT EXISTS (
          SELECT 1 FROM public.spot_edge_match AS match WHERE match.spot_id = spot.spot_id
      );
    IF unmatched_cnt > 0 THEN
        RAISE EXCEPTION '구간에 매칭되지 않은 상세지점 %건', unmatched_cnt;
    END IF;
END
$$;

COMMIT;
