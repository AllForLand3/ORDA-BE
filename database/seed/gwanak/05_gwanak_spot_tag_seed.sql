-- 관악산 상세지점 추가 및 지점 태그

-- 출처: 산림청 PMNTN_SPOT

-- 대상 좌표계: EPSG:5179

BEGIN;

-- 이 파일보다 01~04 시드를 먼저 실행해야 한다.
-- 116200201 상세지점 534건은 01_gwanak_seed.sql 에 이미 있으므로 여기서는 나머지 4개 산 코드만 추가한다.
-- dataset_id 는 dataset_nm 으로 조회하며, 다시 실행해도 같은 행이 중복되지 않는다.
--
-- 메타데이터 확인 상태:
--   * 제공기관·출처 URL·src_crs_cd 는 01 시드의 kfs:116200201:PMNTN_SPOT 을 따른다.
--   * 원천 SPOT 파일에는 기준일 필드가 없어 base_de 는 NULL 이다 (01 시드의 SPOT 데이터셋과 같음).
--   * collect_de 는 4개 산 코드 SHP 를 산림청 등산로 정보에서 내려받은 날(2026-10-31)이다.
--   * file_hash 는 원본 파일 해시가 아니라 데이터셋별 지점 INSERT 데이터의 SHA-256 이며,
--     data_ver 에는 그 앞부분을 사용했다 (02 시드와 같은 방식). file_nm·license_nm 은 NULL.
--   * 원천 좌표는 EPSG:5186 (TM 127°, 원점 38°, 200000/600000, GRS80) 이며 EPSG:5179 로 변환해 저장한다.
--     검수 전이므로 geom 과 src_geom 은 같은 위치다.
INSERT INTO public.source_dataset (
    dataset_nm, origin_cd, data_type_cd, provider_nm, source_url, base_de, collect_de,
    src_crs_cd, file_nm, file_hash, license_nm, data_ver, dataset_desc
)
SELECT seed.*
FROM (
    VALUES
    ('kfs:116200604:PMNTN_SPOT', 'PUBLIC', 'SPOT', '산림청', 'https://www.data.go.kr/data/3034022/fileData.do', NULL::date, DATE '2026-10-31', 'CUSTOM_WKT', NULL, '68f9e549968ad3e929b9410bd48edfae4aa294369ae9635fcdcf424753051c82', NULL, 'sha256:68f9e549968ad3e9', '산림청 PMNTN_SPOT 116200604. 제공기관·URL·src_crs_cd 는 kfs:116200201:PMNTN_SPOT 을 따름; collect_de 는 내려받은 날; file_hash/data_ver 는 이 시드의 지점 INSERT payload SHA-256 기반(원본 파일 해시 아님).'),
    ('kfs:116200704:PMNTN_SPOT', 'PUBLIC', 'SPOT', '산림청', 'https://www.data.go.kr/data/3034022/fileData.do', NULL::date, DATE '2026-10-31', 'CUSTOM_WKT', NULL, 'd9727715d430762bd938a53778b1f64a93e7e6e8af2975643a3df4179a3bde00', NULL, 'sha256:d9727715d430762b', '산림청 PMNTN_SPOT 116200704. 제공기관·URL·src_crs_cd 는 kfs:116200201:PMNTN_SPOT 을 따름; collect_de 는 내려받은 날; file_hash/data_ver 는 이 시드의 지점 INSERT payload SHA-256 기반(원본 파일 해시 아님).'),
    ('kfs:116200804:PMNTN_SPOT', 'PUBLIC', 'SPOT', '산림청', 'https://www.data.go.kr/data/3034022/fileData.do', NULL::date, DATE '2026-10-31', 'CUSTOM_WKT', NULL, '4c397cd4e8d803813109dd3faceaf6136eb4d795f3b120bd9e8c28896ef91322', NULL, 'sha256:4c397cd4e8d80381', '산림청 PMNTN_SPOT 116200804. 제공기관·URL·src_crs_cd 는 kfs:116200201:PMNTN_SPOT 을 따름; collect_de 는 내려받은 날; file_hash/data_ver 는 이 시드의 지점 INSERT payload SHA-256 기반(원본 파일 해시 아님).'),
    ('kfs:116200904:PMNTN_SPOT', 'PUBLIC', 'SPOT', '산림청', 'https://www.data.go.kr/data/3034022/fileData.do', NULL::date, DATE '2026-10-31', 'CUSTOM_WKT', NULL, 'd2c8b50fe2709b569a73492fabcc676ac017c70e7e3a75d34ee21dc178a60d01', NULL, 'sha256:d2c8b50fe2709b56', '산림청 PMNTN_SPOT 116200904. 제공기관·URL·src_crs_cd 는 kfs:116200201:PMNTN_SPOT 을 따름; collect_de 는 내려받은 날; file_hash/data_ver 는 이 시드의 지점 INSERT payload SHA-256 기반(원본 파일 해시 아님).')
) AS seed (
    dataset_nm, origin_cd, data_type_cd, provider_nm, source_url, base_de, collect_de,
    src_crs_cd, file_nm, file_hash, license_nm, data_ver, dataset_desc
)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.source_dataset AS existing
    WHERE existing.dataset_nm = seed.dataset_nm
);

-- 아래 하위 쿼리는 정확히 하나의 source_dataset 행이 조회되어야 한다.
-- 메타데이터가 없거나 중복된 경우 임의의 데이터셋에 지점을 연결하지 않고 실패한다.

-- 산 코드: 116200604, 데이터셋 키: kfs:116200604:PMNTN_SPOT, 건수: 34
INSERT INTO public.trail_spot (
    dataset_id, mountain_id, src_spot_id, src_old_spot_id, src_class_cd, src_class_nm,
    detail_txt, etc_txt, geom, src_geom, verify_status_cd
)
SELECT
    (SELECT dataset_id FROM public.source_dataset WHERE dataset_nm = 'kfs:116200604:PMNTN_SPOT'),
    2, seed.src_spot_id, seed.src_old_spot_id, seed.src_class_cd, seed.src_class_nm,
    seed.detail_txt, seed.etc_txt, seed.geom, seed.geom, 'UNVERIFIED'
FROM (
    VALUES
    ('7736', '1162006040001', '99', '기타', '계단끝', NULL, ST_SetSRID(ST_MakePoint(950608.059, 1939264.056), 5179)),
    ('7737', '1162006040002', '99', '기타', '계단끝', NULL, ST_SetSRID(ST_MakePoint(950377.758, 1939012.297), 5179)),
    ('7738', '1162006040003', '99', '기타', '다리', NULL, ST_SetSRID(ST_MakePoint(950821.974, 1939573.861), 5179)),
    ('7739', '1162006040004', '99', '기타', '계단시작', NULL, ST_SetSRID(ST_MakePoint(950273.259, 1938874.151), 5179)),
    ('7740', '1162006040005', '99', '기타', '119 K15', NULL, ST_SetSRID(ST_MakePoint(950273.255, 1938299.161), 5179)),
    ('7741', '1162006040006', '99', '기타', '제2광장', NULL, ST_SetSRID(ST_MakePoint(950904.326, 1939680.458), 5179)),
    ('7742', '1162006040007', '99', '기타', '계단시작', NULL, ST_SetSRID(ST_MakePoint(950339.664, 1938956.249), 5179)),
    ('7743', '1162006040008', '04', '화장실', '화장실', NULL, ST_SetSRID(ST_MakePoint(951162.973, 1940572.011), 5179)),
    ('7744', '1162006040009', '99', '기타', '테니스장', NULL, ST_SetSRID(ST_MakePoint(951156.574, 1940340.909), 5179)),
    ('7745', '1162006040010', '99', '기타', '제1광장', NULL, ST_SetSRID(ST_MakePoint(951090.861, 1940134.859), 5179)),
    ('7746', '1162006040011', '99', '기타', '다리', NULL, ST_SetSRID(ST_MakePoint(950231.928, 1938743.730), 5179)),
    ('7747', '1162006040012', '07', '시설물(운동기구 등)', '게이틀볼장', NULL, ST_SetSRID(ST_MakePoint(951022.317, 1939993.418), 5179)),
    ('7748', '1162006040013', '99', '기타', '계단끝', NULL, ST_SetSRID(ST_MakePoint(950308.229, 1938918.230), 5179)),
    ('7749', '1162006040014', '99', '기타', '계단시작', NULL, ST_SetSRID(ST_MakePoint(950432.657, 1939083.617), 5179)),
    ('7750', '1162006040015', '99', '기타', '계단시작', NULL, ST_SetSRID(ST_MakePoint(950723.168, 1939335.692), 5179)),
    ('7751', '1162006040016', '99', '기타', '계단끝', NULL, ST_SetSRID(ST_MakePoint(950235.043, 1938813.443), 5179)),
    ('7752', '1162006040017', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949853.709, 1939387.107), 5179)),
    ('7753', '1162006040018', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950325.878, 1939191.222), 5179)),
    ('7754', '1162006040019', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950364.953, 1939001.282), 5179)),
    ('7755', '1162006040020', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950377.517, 1939219.400), 5179)),
    ('7756', '1162006040021', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950426.835, 1939084.121), 5179)),
    ('7757', '1162006040022', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950437.600, 1939250.254), 5179)),
    ('7758', '1162006040023', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950512.927, 1937653.314), 5179)),
    ('7759', '1162006040024', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950520.898, 1937584.719), 5179)),
    ('7760', '1162006040025', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950783.756, 1939573.072), 5179)),
    ('7761', '1162006040026', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950835.223, 1939588.359), 5179)),
    ('7762', '1162006040027', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950837.800, 1938025.842), 5179)),
    ('7763', '1162006040028', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950910.394, 1939692.809), 5179)),
    ('7764', '1162006040029', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951096.603, 1938764.340), 5179)),
    ('7765', '1162006040030', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951167.843, 1938101.088), 5179)),
    ('7766', '1162006040031', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951183.236, 1940657.747), 5179)),
    ('95978', '1162006040032', '03', '정자', '정자, 벤치', NULL, ST_SetSRID(ST_MakePoint(950963.311, 1939811.348), 5179)),
    ('95979', '1162006040033', '05', '벤치', '벤치', NULL, ST_SetSRID(ST_MakePoint(950783.184, 1939390.141), 5179)),
    ('95980', '1162006040034', '11', '안내판또는지도', '안내판또는지도', NULL, ST_SetSRID(ST_MakePoint(950543.888, 1939183.278), 5179))
) AS seed (src_spot_id, src_old_spot_id, src_class_cd, src_class_nm, detail_txt, etc_txt, geom)
ON CONFLICT (dataset_id, src_spot_id)
DO NOTHING;

-- 산 코드: 116200704, 데이터셋 키: kfs:116200704:PMNTN_SPOT, 건수: 42
INSERT INTO public.trail_spot (
    dataset_id, mountain_id, src_spot_id, src_old_spot_id, src_class_cd, src_class_nm,
    detail_txt, etc_txt, geom, src_geom, verify_status_cd
)
SELECT
    (SELECT dataset_id FROM public.source_dataset WHERE dataset_nm = 'kfs:116200704:PMNTN_SPOT'),
    2, seed.src_spot_id, seed.src_old_spot_id, seed.src_class_cd, seed.src_class_nm,
    seed.detail_txt, seed.etc_txt, seed.geom, seed.geom, 'UNVERIFIED'
FROM (
    VALUES
    ('7767', '1162007040001', '06', '음수대', '상봉약수터', NULL, ST_SetSRID(ST_MakePoint(953049.346, 1940368.858), 5179)),
    ('7768', '1162007040002', '99', '기타', '계단', NULL, ST_SetSRID(ST_MakePoint(952561.588, 1941168.589), 5179)),
    ('7769', '1162007040003', '99', '기타', '119 K30', NULL, ST_SetSRID(ST_MakePoint(953045.310, 1940437.298), 5179)),
    ('7770', '1162007040004', '99', '기타', '천지약수', NULL, ST_SetSRID(ST_MakePoint(952518.171, 1940791.617), 5179)),
    ('7771', '1162007040005', '99', '기타', '인현고등학교', NULL, ST_SetSRID(ST_MakePoint(953120.330, 1941457.586), 5179)),
    ('7772', '1162007040006', '99', '기타', '사당초등학교', NULL, ST_SetSRID(ST_MakePoint(953476.847, 1941695.971), 5179)),
    ('7773', '1162007040007', '99', '기타', '관악산입구', NULL, ST_SetSRID(ST_MakePoint(953259.568, 1941582.543), 5179)),
    ('7774', '1162007040008', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(952178.209, 1940792.712), 5179)),
    ('7775', '1162007040009', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(952522.883, 1941618.145), 5179)),
    ('7776', '1162007040010', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(952642.376, 1940861.177), 5179)),
    ('7777', '1162007040011', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(952790.066, 1941177.833), 5179)),
    ('7778', '1162007040012', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(952975.393, 1940518.277), 5179)),
    ('7779', '1162007040013', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953019.572, 1940577.976), 5179)),
    ('7780', '1162007040014', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953048.227, 1940431.734), 5179)),
    ('7781', '1162007040015', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(953086.960, 1941492.078), 5179)),
    ('7782', '1162007040016', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(953094.500, 1941308.291), 5179)),
    ('7783', '1162007040017', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953206.526, 1941480.683), 5179)),
    ('7784', '1162007040018', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(953265.694, 1940572.893), 5179)),
    ('7785', '1162007040019', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953391.976, 1940837.388), 5179)),
    ('7786', '1162007040020', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953425.926, 1940888.991), 5179)),
    ('7787', '1162007040021', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953507.962, 1940711.266), 5179)),
    ('7788', '1162007040022', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953547.731, 1940885.302), 5179)),
    ('7789', '1162007040023', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953575.121, 1940984.611), 5179)),
    ('7790', '1162007040024', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(953610.337, 1941662.952), 5179)),
    ('7791', '1162007040025', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953646.572, 1941474.980), 5179)),
    ('7792', '1162007040026', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953668.637, 1941467.461), 5179)),
    ('7793', '1162007040027', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953679.745, 1940814.663), 5179)),
    ('7794', '1162007040028', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953707.647, 1941136.504), 5179)),
    ('7795', '1162007040029', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953729.545, 1941430.862), 5179)),
    ('7796', '1162007040030', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953760.723, 1941038.634), 5179)),
    ('7797', '1162007040031', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953775.121, 1941407.638), 5179)),
    ('7798', '1162007040032', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953778.963, 1941382.986), 5179)),
    ('7799', '1162007040033', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953792.853, 1941430.818), 5179)),
    ('7800', '1162007040034', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953833.539, 1940906.264), 5179)),
    ('7801', '1162007040035', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(953856.931, 1941015.336), 5179)),
    ('7802', '1162007040036', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953863.048, 1940938.553), 5179)),
    ('7803', '1162007040037', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953873.621, 1941224.089), 5179)),
    ('7804', '1162007040038', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(953876.811, 1941622.768), 5179)),
    ('7805', '1162007040039', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(953888.051, 1940902.264), 5179)),
    ('95981', '1162007040040', '15', '훼손지', '훼손지', NULL, ST_SetSRID(ST_MakePoint(952568.316, 1941573.508), 5179)),
    ('95982', '1162007040041', '14', '조망점', '조망점', NULL, ST_SetSRID(ST_MakePoint(952636.827, 1940922.229), 5179)),
    ('95983', '1162007040042', '12', '위험지역', '위험지역', NULL, ST_SetSRID(ST_MakePoint(953671.661, 1940814.798), 5179))
) AS seed (src_spot_id, src_old_spot_id, src_class_cd, src_class_nm, detail_txt, etc_txt, geom)
ON CONFLICT (dataset_id, src_spot_id)
DO NOTHING;

-- 산 코드: 116200804, 데이터셋 키: kfs:116200804:PMNTN_SPOT, 건수: 87
INSERT INTO public.trail_spot (
    dataset_id, mountain_id, src_spot_id, src_old_spot_id, src_class_cd, src_class_nm,
    detail_txt, etc_txt, geom, src_geom, verify_status_cd
)
SELECT
    (SELECT dataset_id FROM public.source_dataset WHERE dataset_nm = 'kfs:116200804:PMNTN_SPOT'),
    2, seed.src_spot_id, seed.src_old_spot_id, seed.src_class_cd, seed.src_class_nm,
    seed.detail_txt, seed.etc_txt, seed.geom, seed.geom, 'UNVERIFIED'
FROM (
    VALUES
    ('7806', '1162008040001', '12', '위험지역', '위험', NULL, ST_SetSRID(ST_MakePoint(950134.821, 1939437.134), 5179)),
    ('7807', '1162008040002', '99', '기타', '119 K20', NULL, ST_SetSRID(ST_MakePoint(950076.472, 1939332.447), 5179)),
    ('7808', '1162008040003', '99', '기타', '경고문', NULL, ST_SetSRID(ST_MakePoint(950024.067, 1939268.556), 5179)),
    ('7809', '1162008040004', '19', '야영장', '제1야영장', NULL, ST_SetSRID(ST_MakePoint(949701.726, 1938978.098), 5179)),
    ('7810', '1162008040005', '99', '기타', '119 K17', NULL, ST_SetSRID(ST_MakePoint(950316.478, 1937798.907), 5179)),
    ('7811', '1162008040006', '99', '기타', '배드민턴장', NULL, ST_SetSRID(ST_MakePoint(950627.052, 1940281.161), 5179)),
    ('7812', '1162008040007', '99', '기타', '119 K72', NULL, ST_SetSRID(ST_MakePoint(950601.655, 1940223.988), 5179)),
    ('7813', '1162008040008', '99', '기타', '119 K39', NULL, ST_SetSRID(ST_MakePoint(949679.675, 1938641.112), 5179)),
    ('7814', '1162008040009', '99', '기타', '다리', NULL, ST_SetSRID(ST_MakePoint(951131.692, 1940622.121), 5179)),
    ('7815', '1162008040010', '99', '기타', '119 K19', NULL, ST_SetSRID(ST_MakePoint(950203.255, 1939722.144), 5179)),
    ('7816', '1162008040011', '12', '위험지역', '위험', NULL, ST_SetSRID(ST_MakePoint(949966.095, 1939199.670), 5179)),
    ('7817', '1162008040012', '99', '기타', '배드민턴장', NULL, ST_SetSRID(ST_MakePoint(951029.557, 1940548.756), 5179)),
    ('7818', '1162008040013', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(948019.528, 1939102.486), 5179)),
    ('7819', '1162008040014', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(948825.100, 1939446.985), 5179)),
    ('7820', '1162008040015', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(948837.580, 1938536.604), 5179)),
    ('7821', '1162008040016', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(948936.391, 1938679.048), 5179)),
    ('7822', '1162008040017', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949111.429, 1939837.464), 5179)),
    ('7823', '1162008040018', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949203.391, 1939870.378), 5179)),
    ('7824', '1162008040019', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949242.017, 1939951.972), 5179)),
    ('7825', '1162008040020', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949246.505, 1939412.972), 5179)),
    ('7826', '1162008040021', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949271.643, 1939960.273), 5179)),
    ('7827', '1162008040022', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949308.285, 1939002.247), 5179)),
    ('7828', '1162008040023', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949318.560, 1940280.966), 5179)),
    ('7829', '1162008040024', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949360.190, 1939629.264), 5179)),
    ('7830', '1162008040025', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949375.188, 1940150.914), 5179)),
    ('7831', '1162008040026', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949464.230, 1939706.602), 5179)),
    ('7832', '1162008040027', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949468.393, 1939137.353), 5179)),
    ('7833', '1162008040028', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949489.733, 1939152.106), 5179)),
    ('7834', '1162008040029', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949530.115, 1939263.320), 5179)),
    ('7835', '1162008040030', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949545.889, 1939109.332), 5179)),
    ('7836', '1162008040031', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949563.616, 1939926.952), 5179)),
    ('7837', '1162008040032', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949593.631, 1939771.444), 5179)),
    ('7838', '1162008040033', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949641.837, 1938703.403), 5179)),
    ('7839', '1162008040034', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949646.673, 1939279.257), 5179)),
    ('7840', '1162008040035', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949689.333, 1939264.206), 5179)),
    ('7841', '1162008040036', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949691.528, 1938969.385), 5179)),
    ('7842', '1162008040037', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949700.545, 1938963.967), 5179)),
    ('7843', '1162008040038', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949722.926, 1939212.228), 5179)),
    ('7844', '1162008040039', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949758.846, 1940613.045), 5179)),
    ('7845', '1162008040040', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949773.056, 1940436.045), 5179)),
    ('7846', '1162008040041', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(949864.807, 1940080.945), 5179)),
    ('7847', '1162008040042', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949906.633, 1939853.663), 5179)),
    ('7848', '1162008040043', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(949927.622, 1938479.705), 5179)),
    ('7849', '1162008040044', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950101.940, 1940049.002), 5179)),
    ('7850', '1162008040045', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950130.522, 1940115.199), 5179)),
    ('7851', '1162008040046', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950137.212, 1939956.784), 5179)),
    ('7852', '1162008040047', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950145.304, 1940076.881), 5179)),
    ('7853', '1162008040048', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950159.636, 1938762.346), 5179)),
    ('7854', '1162008040049', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950171.401, 1940157.046), 5179)),
    ('7855', '1162008040050', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950203.239, 1940740.447), 5179)),
    ('7856', '1162008040051', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950206.886, 1940307.991), 5179)),
    ('7857', '1162008040052', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950241.380, 1940392.234), 5179)),
    ('7858', '1162008040053', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950270.445, 1940898.793), 5179)),
    ('7859', '1162008040054', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950271.158, 1940105.747), 5179)),
    ('7860', '1162008040055', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950277.873, 1939810.462), 5179)),
    ('7861', '1162008040056', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950288.184, 1940299.031), 5179)),
    ('7862', '1162008040057', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950315.095, 1940712.308), 5179)),
    ('7863', '1162008040058', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950314.138, 1940329.686), 5179)),
    ('7864', '1162008040059', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950321.721, 1940245.993), 5179)),
    ('7865', '1162008040060', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950338.740, 1940380.878), 5179)),
    ('7866', '1162008040061', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950348.879, 1940804.307), 5179)),
    ('7867', '1162008040062', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950345.731, 1939977.622), 5179)),
    ('7868', '1162008040063', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950357.303, 1940601.469), 5179)),
    ('7869', '1162008040064', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950367.011, 1940672.870), 5179)),
    ('7870', '1162008040065', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950394.393, 1940026.233), 5179)),
    ('7871', '1162008040066', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950398.086, 1940244.709), 5179)),
    ('7872', '1162008040067', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950420.333, 1940828.603), 5179)),
    ('7873', '1162008040068', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950455.641, 1940425.576), 5179)),
    ('7874', '1162008040069', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950549.493, 1940368.164), 5179)),
    ('7875', '1162008040070', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950554.144, 1940203.473), 5179)),
    ('7876', '1162008040071', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950626.314, 1940279.960), 5179)),
    ('7877', '1162008040072', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950649.733, 1939879.767), 5179)),
    ('7878', '1162008040073', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950723.547, 1940147.453), 5179)),
    ('7879', '1162008040074', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950735.612, 1940193.609), 5179)),
    ('7880', '1162008040075', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950804.532, 1939939.906), 5179)),
    ('7881', '1162008040076', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950901.196, 1939934.834), 5179)),
    ('7882', '1162008040077', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(950950.424, 1939978.171), 5179)),
    ('7883', '1162008040078', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950973.509, 1939790.947), 5179)),
    ('7884', '1162008040079', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951079.282, 1940053.943), 5179)),
    ('7885', '1162008040080', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951164.433, 1940244.813), 5179)),
    ('95984', '1162008040081', '10', '이정표', '이정표', NULL, ST_SetSRID(ST_MakePoint(950168.761, 1939531.569), 5179)),
    ('95985', '1162008040082', '10', '이정표', '이정표', NULL, ST_SetSRID(ST_MakePoint(949644.862, 1938735.627), 5179)),
    ('95986', '1162008040083', '10', '이정표', '이정표', NULL, ST_SetSRID(ST_MakePoint(949669.422, 1938896.352), 5179)),
    ('95987', '1162008040084', '04', '화장실', '화장실', NULL, ST_SetSRID(ST_MakePoint(949786.586, 1939012.140), 5179)),
    ('95988', '1162008040085', '05', '벤치', '벤치', NULL, ST_SetSRID(ST_MakePoint(951096.085, 1940612.817), 5179)),
    ('95989', '1162008040086', '05', '벤치', '벤치', NULL, ST_SetSRID(ST_MakePoint(950657.024, 1940364.193), 5179)),
    ('95990', '1162008040087', '10', '이정표', '이정표', NULL, ST_SetSRID(ST_MakePoint(949914.667, 1938491.767), 5179))
) AS seed (src_spot_id, src_old_spot_id, src_class_cd, src_class_nm, detail_txt, etc_txt, geom)
ON CONFLICT (dataset_id, src_spot_id)
DO NOTHING;

-- 산 코드: 116200904, 데이터셋 키: kfs:116200904:PMNTN_SPOT, 건수: 49
INSERT INTO public.trail_spot (
    dataset_id, mountain_id, src_spot_id, src_old_spot_id, src_class_cd, src_class_nm,
    detail_txt, etc_txt, geom, src_geom, verify_status_cd
)
SELECT
    (SELECT dataset_id FROM public.source_dataset WHERE dataset_nm = 'kfs:116200904:PMNTN_SPOT'),
    2, seed.src_spot_id, seed.src_old_spot_id, seed.src_class_cd, seed.src_class_nm,
    seed.detail_txt, seed.etc_txt, seed.geom, seed.geom, 'UNVERIFIED'
FROM (
    VALUES
    ('7886', '1162009040001', '99', '기타', 'KBS기지국', NULL, ST_SetSRID(ST_MakePoint(952544.142, 1938104.712), 5179)),
    ('7887', '1162009040002', '99', '기타', '119 K44', NULL, ST_SetSRID(ST_MakePoint(952145.761, 1938053.365), 5179)),
    ('7888', '1162009040003', '99', '기타', '119 K45', NULL, ST_SetSRID(ST_MakePoint(952423.077, 1938075.816), 5179)),
    ('7889', '1162009040004', '11', '안내판또는지도', '안내도', NULL, ST_SetSRID(ST_MakePoint(951144.583, 1938222.740), 5179)),
    ('7890', '1162009040005', '99', '기타', '119 K64', NULL, ST_SetSRID(ST_MakePoint(951243.946, 1937890.321), 5179)),
    ('7891', '1162009040006', '99', '기타', '119 K43', NULL, ST_SetSRID(ST_MakePoint(951110.981, 1938599.310), 5179)),
    ('7892', '1162009040007', '11', '안내판또는지도', '안내판', NULL, ST_SetSRID(ST_MakePoint(951135.100, 1938438.295), 5179)),
    ('7893', '1162009040008', '99', '기타', '다리필요', NULL, ST_SetSRID(ST_MakePoint(951140.239, 1938308.827), 5179)),
    ('7894', '1162009040009', '99', '기타', '119 K46', NULL, ST_SetSRID(ST_MakePoint(951829.405, 1937909.107), 5179)),
    ('7895', '1162009040010', '99', '기타', '119 K10', NULL, ST_SetSRID(ST_MakePoint(951166.162, 1938109.843), 5179)),
    ('7896', '1162009040011', '99', '기타', '119 K75', NULL, ST_SetSRID(ST_MakePoint(951585.540, 1938823.980), 5179)),
    ('7897', '1162009040012', '99', '기타', '버스정류장', NULL, ST_SetSRID(ST_MakePoint(951510.810, 1938915.223), 5179)),
    ('7898', '1162009040013', '99', '기타', '119 K75', NULL, ST_SetSRID(ST_MakePoint(951585.506, 1938824.042), 5179)),
    ('7899', '1162009040014', '99', '기타', '버스정류장', NULL, ST_SetSRID(ST_MakePoint(951525.741, 1938940.883), 5179)),
    ('7900', '1162009040015', '99', '기타', '계단', NULL, ST_SetSRID(ST_MakePoint(951538.450, 1938844.655), 5179)),
    ('7901', '1162009040016', '99', '기타', '119 K75', NULL, ST_SetSRID(ST_MakePoint(951770.386, 1938513.049), 5179)),
    ('7902', '1162009040017', '99', '기타', '버스정류장', NULL, ST_SetSRID(ST_MakePoint(951710.613, 1938629.905), 5179)),
    ('7903', '1162009040018', '99', '기타', '계단', NULL, ST_SetSRID(ST_MakePoint(951723.323, 1938533.665), 5179)),
    ('7904', '1162009040019', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950341.381, 1938102.749), 5179)),
    ('7905', '1162009040020', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(950909.707, 1939693.535), 5179)),
    ('7906', '1162009040021', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951090.554, 1938842.409), 5179)),
    ('7907', '1162009040022', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951117.856, 1938615.547), 5179)),
    ('7908', '1162009040023', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951137.378, 1939581.035), 5179)),
    ('7909', '1162009040024', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951213.599, 1938508.034), 5179)),
    ('7910', '1162009040025', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951306.967, 1939339.657), 5179)),
    ('7911', '1162009040026', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951312.274, 1938641.606), 5179)),
    ('7912', '1162009040027', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951549.093, 1938894.459), 5179)),
    ('7913', '1162009040028', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951579.683, 1938831.315), 5179)),
    ('7914', '1162009040029', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951608.789, 1938729.995), 5179)),
    ('7915', '1162009040030', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951637.054, 1939454.853), 5179)),
    ('7916', '1162009040031', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951659.537, 1938666.702), 5179)),
    ('7917', '1162009040032', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951676.674, 1938268.718), 5179)),
    ('7918', '1162009040033', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951697.614, 1938320.372), 5179)),
    ('7919', '1162009040034', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951728.377, 1937924.251), 5179)),
    ('7920', '1162009040035', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951784.732, 1939107.589), 5179)),
    ('7921', '1162009040036', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(951815.213, 1938361.143), 5179)),
    ('7922', '1162009040037', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(951842.631, 1937901.634), 5179)),
    ('7923', '1162009040038', '02', '분기점', '분기점', NULL, ST_SetSRID(ST_MakePoint(952519.120, 1938112.251), 5179)),
    ('7924', '1162009040039', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(952573.350, 1938070.580), 5179)),
    ('7925', '1162009040040', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(952602.461, 1938248.610), 5179)),
    ('7926', '1162009040041', '01', '시종점', '시종점', NULL, ST_SetSRID(ST_MakePoint(952624.412, 1938590.438), 5179)),
    ('95991', '1162009040042', '15', '훼손지', '훼손지', NULL, ST_SetSRID(ST_MakePoint(951072.561, 1939588.811), 5179)),
    ('95992', '1162009040043', '05', '벤치', '벤치', NULL, ST_SetSRID(ST_MakePoint(951166.599, 1939536.485), 5179)),
    ('95993', '1162009040044', '15', '훼손지', '훼손지', NULL, ST_SetSRID(ST_MakePoint(951141.767, 1939575.461), 5179)),
    ('95994', '1162009040045', '14', '조망점', '조망점', NULL, ST_SetSRID(ST_MakePoint(951713.111, 1937941.215), 5179)),
    ('95995', '1162009040046', '14', '조망점', '조망점', NULL, ST_SetSRID(ST_MakePoint(951518.654, 1937970.080), 5179)),
    ('95996', '1162009040047', '14', '조망점', '조망점', NULL, ST_SetSRID(ST_MakePoint(952117.440, 1937999.903), 5179)),
    ('95997', '1162009040048', '12', '위험지역', '위험지역', NULL, ST_SetSRID(ST_MakePoint(951766.092, 1937924.266), 5179)),
    ('95998', '1162009040049', '06', '음수대', '음수대', NULL, ST_SetSRID(ST_MakePoint(951675.076, 1938501.716), 5179))
) AS seed (src_spot_id, src_old_spot_id, src_class_cd, src_class_nm, detail_txt, etc_txt, geom)
ON CONFLICT (dataset_id, src_spot_id)
DO NOTHING;

-- 지점 태그: 5개 산 코드 전체 176건
-- 상세지점 메모(detail_txt, etc_txt)와 원천 분류의 키워드로 추출했다 (extract_type_cd = KEYWORD).
--   STAIR 계단 / REST 벤치·쉼터·평상 / VIEW 조망점·조망(철조망 제외)·360도
--   STEEP 깔딱고개·경사심함·직벽·바위오름길·로프설치요 ('경사' 단독은 '전경사진' 오탐이라 쓰지 않음)
--   ROPE 로프·밧줄 (경계로프 제외) / DANGER 위험지역·위험·낙석·붕괴·미끄러움·물끼·빙판·절벽
--   WATER·HELIPAD·TOILET·PARKING 은 확장용이며 검수하지 않았다.
-- 규칙만으로 판단할 수 없는 17개 지점은 메모와 DEM 고도·경사를 보고 데스크 검수했다 (현장 확인 아님).
--   VERIFIED/REJECTED 로 반영했고, 험한 암릉 로프 2곳에는 STEEP 을 MANUAL 로 추가했다.
--   검수자를 특정한 계정이 없으므로 verify_user_id·verify_dt 는 NULL 이다.
INSERT INTO public.spot_tag (spot_id, tag_cd, extract_type_cd, verify_status_cd)
SELECT spot.spot_id, seed.tag_cd, seed.extract_type_cd, seed.verify_status_cd
FROM (
    VALUES
    ('kfs:116200604:PMNTN_SPOT', '7736', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7737', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7739', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7742', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7743', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7748', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7749', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7750', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '7751', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '95978', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200604:PMNTN_SPOT', '95979', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200704:PMNTN_SPOT', '7767', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200704:PMNTN_SPOT', '7768', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200704:PMNTN_SPOT', '7770', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200704:PMNTN_SPOT', '95982', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200704:PMNTN_SPOT', '95983', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200804:PMNTN_SPOT', '7806', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200804:PMNTN_SPOT', '7816', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200804:PMNTN_SPOT', '95987', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200804:PMNTN_SPOT', '95988', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200804:PMNTN_SPOT', '95989', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '7900', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '7903', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '95992', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '95994', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '95995', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '95996', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '95997', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200904:PMNTN_SPOT', '95998', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7211', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7211', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7211', 'PARKING', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7212', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7218', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7221', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7222', 'REST', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7223', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7225', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7226', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7226', 'REST', 'KEYWORD', 'REJECTED'),
    ('kfs:116200201:PMNTN_SPOT', '7228', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7228', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7228', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7230', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7231', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7236', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7239', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7240', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7240', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7241', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7243', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7244', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7245', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7246', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7251', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7252', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7253', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7253', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7253', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7260', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7262', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7263', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7264', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7270', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7270', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7273', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7273', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7274', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7287', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7292', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7300', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7304', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7305', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7318', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7320', 'ROPE', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7324', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7325', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7326', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7327', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7331', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7335', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7346', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7348', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7352', 'STEEP', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7358', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7361', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7366', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7367', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7368', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7370', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7370', 'ROPE', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7382', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7387', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7399', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7409', 'ROPE', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7421', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7429', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7430', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7431', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7446', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7458', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7459', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7481', 'STEEP', 'KEYWORD', 'REJECTED'),
    ('kfs:116200201:PMNTN_SPOT', '7482', 'PARKING', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7483', 'PARKING', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7497', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7498', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7503', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7505', 'VIEW', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7511', 'PARKING', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7519', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7528', 'REST', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7542', 'VIEW', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7555', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7558', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7563', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7579', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7582', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7583', 'REST', 'KEYWORD', 'REJECTED'),
    ('kfs:116200201:PMNTN_SPOT', '7586', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7590', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7596', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7611', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7614', 'STEEP', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7615', 'VIEW', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7630', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7631', 'ROPE', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7633', 'ROPE', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7634', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7635', 'ROPE', 'KEYWORD', 'REJECTED'),
    ('kfs:116200201:PMNTN_SPOT', '7639', 'ROPE', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7639', 'STEEP', 'MANUAL', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7640', 'ROPE', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7640', 'STEEP', 'MANUAL', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7644', 'ROPE', 'KEYWORD', 'VERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7645', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7655', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7656', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7661', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7665', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7671', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7672', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7676', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7677', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7678', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7679', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7680', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7680', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7680', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7684', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7684', 'HELIPAD', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7685', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7686', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7687', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7688', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7693', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7693', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7700', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7701', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7701', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7702', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7702', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7703', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7703', 'DANGER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7708', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7708', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7713', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7714', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7717', 'VIEW', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7717', 'WATER', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7723', 'STAIR', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7726', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7727', 'REST', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7728', 'TOILET', 'KEYWORD', 'UNVERIFIED'),
    ('kfs:116200201:PMNTN_SPOT', '7729', 'ROPE', 'KEYWORD', 'REJECTED'),
    ('kfs:116200201:PMNTN_SPOT', '100649', 'DANGER', 'KEYWORD', 'UNVERIFIED')
) AS seed (dataset_nm, src_spot_id, tag_cd, extract_type_cd, verify_status_cd)
JOIN public.source_dataset AS dataset ON dataset.dataset_nm = seed.dataset_nm
JOIN public.trail_spot AS spot ON spot.dataset_id = dataset.dataset_id AND spot.src_spot_id = seed.src_spot_id
ON CONFLICT (spot_id, tag_cd)
DO NOTHING;

-- 연결할 지점을 찾지 못한 태그가 있으면 조용히 빠지지 않도록 실패시킨다.
DO $$
DECLARE
    expected_cnt integer := 176;
    actual_cnt integer;
BEGIN
    SELECT count(*) INTO actual_cnt
    FROM public.spot_tag AS tag
    JOIN public.trail_spot AS spot ON spot.spot_id = tag.spot_id
    JOIN public.source_dataset AS dataset ON dataset.dataset_id = spot.dataset_id
    WHERE dataset.dataset_nm LIKE 'kfs:%:PMNTN_SPOT';
    IF actual_cnt < expected_cnt THEN
        RAISE EXCEPTION 'spot_tag 적재 건수 부족: 기대 %, 실제 %', expected_cnt, actual_cnt;
    END IF;
END
$$;

COMMIT;
