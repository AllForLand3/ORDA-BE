-- =====================================================
-- Update Gwanaksan DEM file path
-- =====================================================
-- Copernicus GLO-30 DSM
-- Docker container path: /data/dem/gwanak_dem.tif

UPDATE public.dem_asset
SET file_path = '/data/dem/gwanak_dem.tif'
WHERE dem_id = 1
  AND file_path = '/workspace/Copernicus_DSM_COG_10_N37_00_E126_00_DEM (5).tif';
