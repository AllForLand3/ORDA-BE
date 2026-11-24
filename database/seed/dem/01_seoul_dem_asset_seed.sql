-- 서울권 공용 Copernicus GLO-30 DSM 등록
--
-- 실행 전 다음 파일이 컨테이너의 /data/dem 아래에 있어야 한다.
--   /data/dem/n37e126_dem.tif
--   /data/dem/n37e127_dem.tif
--   /data/dem/seoul_copernicus_glo30.tif
--
-- 관악산 시드로 등록된 N37E126 자산의 경로를 새 파일명으로 변경하고,
-- N37E127 원본 타일과 두 타일을 병합한 서울권 공용 DEM을 등록한다.
-- 원본 타일은 출처 추적용으로 비활성화하고 공용 DEM만 신규 분석에 사용한다.

BEGIN;

SELECT pg_advisory_xact_lock(5179, hashtext('derived:copernicus:glo30:seoul-mosaic'));

UPDATE public.source_dataset
SET file_nm = 'n37e126_dem.tif'
WHERE dataset_nm = 'copernicus:glo30:N37E126';

UPDATE public.dem_asset AS dem
SET dem_nm = 'Copernicus GLO-30 N37E126 DSM',
    file_path = '/data/dem/n37e126_dem.tif'
FROM public.source_dataset AS dataset
WHERE dataset.dataset_id = dem.dataset_id
  AND dataset.dataset_nm = 'copernicus:glo30:N37E126';

INSERT INTO public.source_dataset (
    dataset_nm, origin_cd, data_type_cd, provider_nm, source_url,
    base_de, collect_de, src_crs_cd, file_nm, file_hash,
    license_nm, data_ver, dataset_desc
)
VALUES (
    'copernicus:glo30:N37E127',
    'PUBLIC',
    'DEM',
    'Copernicus Programme / ESA',
    'https://copernicus-dem-30m.s3.amazonaws.com/Copernicus_DSM_COG_10_N37_00_E127_00_DEM/Copernicus_DSM_COG_10_N37_00_E127_00_DEM.tif',
    NULL,
    DATE '2026-11-24',
    'EPSG:4326',
    'n37e127_dem.tif',
    '4f8fef79da8c18ed9a9a0fb97b3d2695c4681181e013e039d87044e5479f7df6',
    'Copernicus DEM License',
    'n37e127-v1',
    'Copernicus GLO-30 N37E127 원본 DSM 타일. 범위는 EPSG:4326 [127, 37, 128, 38]이다.'
)
ON CONFLICT (dataset_nm, data_ver)
DO UPDATE SET
    provider_nm = EXCLUDED.provider_nm,
    source_url = EXCLUDED.source_url,
    collect_de = EXCLUDED.collect_de,
    src_crs_cd = EXCLUDED.src_crs_cd,
    file_nm = EXCLUDED.file_nm,
    file_hash = EXCLUDED.file_hash,
    license_nm = EXCLUDED.license_nm,
    dataset_desc = EXCLUDED.dataset_desc;

INSERT INTO public.dem_asset (
    dataset_id, dem_nm, file_path, crs_cd, res_m,
    surface_type_cd, nodata_val, extent_geom, prep_status_cd, use_yn
)
SELECT
    dataset.dataset_id,
    'Copernicus GLO-30 N37E127 DSM',
    '/data/dem/n37e127_dem.tif',
    'EPSG:4326',
    30.00,
    'DSM',
    NULL,
    ST_Transform(ST_MakeEnvelope(127, 37, 128, 38, 4326), 5179),
    'READY',
    'N'
FROM public.source_dataset AS dataset
WHERE dataset.dataset_nm = 'copernicus:glo30:N37E127'
  AND dataset.data_ver = 'n37e127-v1'
  AND NOT EXISTS (
      SELECT 1
      FROM public.dem_asset AS existing
      WHERE existing.dataset_id = dataset.dataset_id
        AND existing.file_path = '/data/dem/n37e127_dem.tif'
  );

INSERT INTO public.source_dataset (
    dataset_nm, origin_cd, data_type_cd, provider_nm, source_url,
    base_de, collect_de, src_crs_cd, file_nm, file_hash,
    license_nm, data_ver, dataset_desc
)
VALUES (
    'derived:copernicus:glo30:seoul-mosaic',
    'DERIVED',
    'DEM',
    'ORDA',
    'https://copernicus-dem-30m.s3.amazonaws.com/readme.html',
    NULL,
    DATE '2026-11-24',
    'EPSG:4326',
    'seoul_copernicus_glo30.tif',
    'a3e545058390babe4abe266d13d0cb4e9aef51e349a8893005f649cf7868ce9d',
    'Copernicus DEM License',
    'n37e126-e127-v1',
    'Copernicus GLO-30 N37E126 및 N37E127 원본 타일을 병합한 서울권 공용 DSM. 범위는 EPSG:4326 [126, 37, 128, 38]이다.'
)
ON CONFLICT (dataset_nm, data_ver)
DO UPDATE SET
    provider_nm = EXCLUDED.provider_nm,
    source_url = EXCLUDED.source_url,
    collect_de = EXCLUDED.collect_de,
    src_crs_cd = EXCLUDED.src_crs_cd,
    file_nm = EXCLUDED.file_nm,
    file_hash = EXCLUDED.file_hash,
    license_nm = EXCLUDED.license_nm,
    dataset_desc = EXCLUDED.dataset_desc;

INSERT INTO public.dem_asset (
    dataset_id, dem_nm, file_path, crs_cd, res_m,
    surface_type_cd, nodata_val, extent_geom, prep_status_cd, use_yn
)
SELECT
    dataset.dataset_id,
    '서울권 Copernicus GLO-30 DSM',
    '/data/dem/seoul_copernicus_glo30.tif',
    'EPSG:4326',
    30.00,
    'DSM',
    NULL,
    ST_Transform(ST_MakeEnvelope(126, 37, 128, 38, 4326), 5179),
    'READY',
    'Y'
FROM public.source_dataset AS dataset
WHERE dataset.dataset_nm = 'derived:copernicus:glo30:seoul-mosaic'
  AND dataset.data_ver = 'n37e126-e127-v1'
  AND NOT EXISTS (
      SELECT 1
      FROM public.dem_asset AS existing
      WHERE existing.dataset_id = dataset.dataset_id
        AND existing.file_path = '/data/dem/seoul_copernicus_glo30.tif'
  );

-- 재실행 시에도 메타데이터와 활성 상태를 동일하게 맞춘다.
UPDATE public.dem_asset AS dem
SET dem_nm = 'Copernicus GLO-30 N37E127 DSM',
    crs_cd = 'EPSG:4326',
    res_m = 30.00,
    surface_type_cd = 'DSM',
    extent_geom = ST_Transform(ST_MakeEnvelope(127, 37, 128, 38, 4326), 5179),
    prep_status_cd = 'READY',
    use_yn = 'N'
FROM public.source_dataset AS dataset
WHERE dataset.dataset_id = dem.dataset_id
  AND dataset.dataset_nm = 'copernicus:glo30:N37E127'
  AND dataset.data_ver = 'n37e127-v1'
  AND dem.file_path = '/data/dem/n37e127_dem.tif';

UPDATE public.dem_asset AS dem
SET dem_nm = '서울권 Copernicus GLO-30 DSM',
    crs_cd = 'EPSG:4326',
    res_m = 30.00,
    surface_type_cd = 'DSM',
    extent_geom = ST_Transform(ST_MakeEnvelope(126, 37, 128, 38, 4326), 5179),
    prep_status_cd = 'READY',
    use_yn = 'Y'
FROM public.source_dataset AS dataset
WHERE dataset.dataset_id = dem.dataset_id
  AND dataset.dataset_nm = 'derived:copernicus:glo30:seoul-mosaic'
  AND dataset.data_ver = 'n37e126-e127-v1'
  AND dem.file_path = '/data/dem/seoul_copernicus_glo30.tif';

UPDATE public.dem_asset AS dem
SET use_yn = 'N'
FROM public.source_dataset AS dataset
WHERE dataset.dataset_id = dem.dataset_id
  AND dataset.dataset_nm IN (
      'copernicus:glo30:N37E126',
      'copernicus:glo30:N37E127'
  );

DO $$
DECLARE
    west_tile_count bigint;
    east_tile_count bigint;
    mosaic_count bigint;
BEGIN
    SELECT COUNT(*)
    INTO west_tile_count
    FROM public.dem_asset AS dem
    JOIN public.source_dataset AS dataset ON dataset.dataset_id = dem.dataset_id
    WHERE dataset.dataset_nm = 'copernicus:glo30:N37E126'
      AND dataset.file_nm = 'n37e126_dem.tif'
      AND dem.file_path = '/data/dem/n37e126_dem.tif'
      AND dem.use_yn = 'N';

    SELECT COUNT(*)
    INTO east_tile_count
    FROM public.dem_asset AS dem
    JOIN public.source_dataset AS dataset ON dataset.dataset_id = dem.dataset_id
    WHERE dataset.dataset_nm = 'copernicus:glo30:N37E127'
      AND dataset.data_ver = 'n37e127-v1'
      AND dem.file_path = '/data/dem/n37e127_dem.tif'
      AND dem.use_yn = 'N';

    SELECT COUNT(*)
    INTO mosaic_count
    FROM public.dem_asset AS dem
    JOIN public.source_dataset AS dataset ON dataset.dataset_id = dem.dataset_id
    WHERE dataset.dataset_nm = 'derived:copernicus:glo30:seoul-mosaic'
      AND dataset.data_ver = 'n37e126-e127-v1'
      AND dem.file_path = '/data/dem/seoul_copernicus_glo30.tif'
      AND dem.crs_cd = 'EPSG:4326'
      AND dem.surface_type_cd = 'DSM'
      AND dem.prep_status_cd = 'READY'
      AND dem.use_yn = 'Y'
      AND ST_Covers(
          dem.extent_geom,
          ST_Transform(ST_MakeEnvelope(126, 37, 128, 38, 4326), 5179)
      );

    IF west_tile_count <> 1 OR east_tile_count <> 1 OR mosaic_count <> 1 THEN
        RAISE EXCEPTION
            '서울권 DEM 등록 검증 실패(N37E126 %, N37E127 %, mosaic %). 관악산 시드 선행 여부와 중복 자산을 확인하세요.',
            west_tile_count,
            east_tile_count,
            mosaic_count;
    END IF;
END
$$;

SELECT
    dem.dem_id,
    dem.dem_nm,
    dem.file_path,
    dem.crs_cd,
    dem.res_m,
    dem.prep_status_cd,
    dem.use_yn
FROM public.dem_asset AS dem
JOIN public.source_dataset AS dataset ON dataset.dataset_id = dem.dataset_id
WHERE dataset.dataset_nm = 'derived:copernicus:glo30:seoul-mosaic'
  AND dataset.data_ver = 'n37e126-e127-v1';

COMMIT;
