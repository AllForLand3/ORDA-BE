-- =====================================================
-- Update source_dataset constraints
-- =====================================================

-- 1. 데이터 출처 구분 제약조건 변경
ALTER TABLE public.source_dataset
DROP CONSTRAINT source_dataset_origin_cd_check;

ALTER TABLE public.source_dataset
  ADD CONSTRAINT source_dataset_origin_cd_check
    CHECK (
      origin_cd IN (
                    'EXTERNAL',
                    'TEAM',
                    'PUBLIC',
                    'DERIVED'
        )
      );

-- 2. 데이터 유형 제약조건 변경
ALTER TABLE public.source_dataset
DROP CONSTRAINT source_dataset_data_type_cd_check;

ALTER TABLE public.source_dataset
  ADD CONSTRAINT source_dataset_data_type_cd_check
    CHECK (
      data_type_cd IN (
                       'TRAIL',
                       'SPOT',
                       'DEM',
                       'FACILITY',
                       'AREA',
                       'PLACE',
                       'SERVICE_AREA'
        )
      );
