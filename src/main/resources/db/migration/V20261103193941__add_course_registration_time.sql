ALTER TABLE course ADD COLUMN frst_regist_dt TIMESTAMPTZ;

-- 이후 등록되는 코스에만 최초 저장 시각을 기록한다.
ALTER TABLE course ALTER COLUMN frst_regist_dt SET DEFAULT CURRENT_TIMESTAMP;
