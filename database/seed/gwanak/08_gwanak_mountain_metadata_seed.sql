-- 관악산 위치 및 상세 메타데이터
--
-- 대표 좌표는 일반 지도에서 관악산 산봉우리로 표시되는 대표 핀을 사용한다.
-- OpenStreetMap natural=peak node 442878166: 37.44514, 126.96424.
-- EPSG:4326 좌표를 DB 저장 좌표계인 EPSG:5179로 변환해 저장한다.
-- 해발고도 632.2m는 산림청 100대 명산 관악산 정보를 기준으로 한다.

BEGIN;

DO $$
DECLARE
    mountain_count bigint;
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
END
$$;

UPDATE public.mountain
SET province_nm = '서울특별시',
    district_nm = '관악구',
    address_label = '서울특별시 관악구',
    elevation_m = 632.20,
    representative_geom = ST_Transform(
        ST_SetSRID(ST_MakePoint(126.96424, 37.44514), 4326),
        5179
    )
WHERE mountain_nm = '관악산'
  AND use_yn = 'Y';

DO $$
DECLARE
    invalid_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO invalid_count
    FROM public.mountain
    WHERE mountain_nm = '관악산'
      AND use_yn = 'Y'
      AND (
          province_nm IS DISTINCT FROM '서울특별시'
          OR district_nm IS DISTINCT FROM '관악구'
          OR address_label IS DISTINCT FROM '서울특별시 관악구'
          OR elevation_m IS DISTINCT FROM 632.20
          OR representative_geom IS NULL
          OR GeometryType(representative_geom) <> 'POINT'
          OR ST_SRID(representative_geom) <> 5179
          OR NOT ST_DWithin(
              representative_geom,
              ST_Transform(
                  ST_SetSRID(ST_MakePoint(126.96424, 37.44514), 4326),
                  5179
              ),
              0.01
          )
      );

    IF invalid_count <> 0 THEN
        RAISE EXCEPTION '관악산 위치 및 상세 메타데이터 검증에 실패했습니다.';
    END IF;
END
$$;

COMMIT;
