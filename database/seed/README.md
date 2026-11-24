# 산 데이터 시드 실행

이 디렉터리의 SQL은 Flyway migration이 아니다. Flyway로 최신 스키마를 적용한 뒤 개발·운영 DB에 명시적으로 실행한다. 모든 명령은 저장소 루트에서 실행한다.

## 사전 조건

- 대상 DB에 최신 Flyway migration이 적용되어 있어야 한다.
- PostgreSQL 클라이언트의 `psql` 명령을 사용할 수 있어야 한다.
- 대상 DB에 PostGIS와 pgRouting이 설치되어 있어야 한다.
- 운영 실행 전 DB 백업, 접속 대상, 실행할 산과 SQL 파일을 확인한다.
- 비밀번호를 명령어나 Git에 기록하지 않는다. 운영 환경에서는 `~/.pgpass` 등 별도 비밀 관리 수단 사용을 권장한다.

## DB 연결 설정

다음 중 한 가지 방식으로 연결 정보를 설정한다.

### 개별 환경변수

Git Bash 예시:

```bash
export PGHOST=localhost
export PGPORT=5432
export PGUSER=orda
export PGDATABASE=orda
```

비밀번호는 `PGPASSWORD` 또는 `~/.pgpass`로 제공할 수 있다.

### DATABASE_URL

```bash
export DATABASE_URL='postgresql://사용자@호스트:5432/DB이름'
```

연결 문자열에 비밀번호를 직접 넣으면 셸 기록에 남을 수 있으므로 운영 환경에서는 주의한다.

## 전체 초기 적재

데이터가 전혀 없는 신규 DB에 관악산과 단일 파일로 관리하는 모든 산 시드를 실행한다.

```bash
bash ./database/seed/run_seed.sh all
```

`all` 모드는 `public.flyway_schema_history`와 PostGIS의 `public.spatial_ref_sys`를 제외한 `public` 테이블이 모두 비어 있을 때만 시작한다. 관악산의 `NN_*.sql`을 번호순으로 먼저 실행하고, `database/seed/*_seed.sql`을 파일명순으로 실행한다.

관악산의 `01_gwanak_seed.sql`에는 산 데이터 외에 개발·시연 계정과 코스도 포함되어 있다. `all`은 이 초기 상태 전체가 필요한 빈 DB에서만 사용한다. 운영 DB에 산 데이터만 추가하려면 `all`을 사용하지 말고 아래의 산별 개별 실행을 사용한다. 관악산을 운영 DB에 시연 데이터 없이 적재하려면 기존 관악산 시드에서 운영용 산 데이터만 분리하는 별도 작업이 필요하다.

각 SQL 파일은 자체 트랜잭션을 사용한다. 중간 파일에서 실패하면 그 파일은 롤백되지만 앞서 완료된 파일은 이미 커밋되어 유지된다. 전체 적재 실패 후에는 운영자가 실패 원인을 확인하고, 신규 DB를 다시 준비하거나 아직 실행되지 않은 산을 개별 실행해야 한다.

## 산별 개별 실행

산 이름을 지정하면 해당 산의 폴더 또는 `<산 이름>_seed.sql`을 실행한다.

```bash
bash ./database/seed/run_seed.sh gwanak
bash ./database/seed/run_seed.sh bukhan
bash ./database/seed/run_seed.sh dobong
bash ./database/seed/run_seed.sh cheonggye
bash ./database/seed/run_seed.sh surak
bash ./database/seed/run_seed.sh buram
bash ./database/seed/run_seed.sh acha
bash ./database/seed/run_seed.sh inwang
bash ./database/seed/run_seed.sh bugak
bash ./database/seed/run_seed.sh yongma
```

관악산은 `database/seed/gwanak/NN_*.sql`을 번호순으로 실행한다. 나머지 산은 `database/seed/<산 이름>_seed.sql` 단일 파일을 실행한다.

단일 파일명을 직접 지정해도 된다.

```bash
bash ./database/seed/run_seed.sh inwang_seed.sql
```

산별 SQL은 최초 적재 전용이다. 같은 산의 활성 데이터나 네트워크가 이미 있으면 파일 내부 검증에서 중단한다. 기존 데이터를 교체하거나 재적재하려면 참조 데이터 영향과 복구 방법을 별도로 검토해야 한다.

## 서울권 공용 DEM 등록

Copernicus GLO-30 원본 타일 2개와 두 타일을 병합한 파일을 다음 경로에 배치한다.

| 용도 | 호스트 경로 | 컨테이너 경로 |
|---|---|---|
| 서쪽 원본 타일 | `database/dem/n37e126_dem.tif` | `/data/dem/n37e126_dem.tif` |
| 동쪽 원본 타일 | `database/dem/n37e127_dem.tif` | `/data/dem/n37e127_dem.tif` |
| 서울권 공용 DEM | `database/dem/seoul_copernicus_glo30.tif` | `/data/dem/seoul_copernicus_glo30.tif` |

관악산 시드를 먼저 실행해야 한다. 공용 DEM은 EPSG:4326, 7200 x 3600 픽셀, 경도 126~128·위도 37~38 범위여야 한다. 파일 배치와 컨테이너의 `/data/dem` 읽기 전용 마운트를 확인한 뒤 별도 DEM 시드를 실행한다.

```bash
bash ./database/seed/run_seed.sh dem
```

DEM 시드는 기존 `N37E126` 자산의 파일명을 `n37e126_dem.tif`로 변경하고 `N37E127` 자산과 서울권 공용 자산을 등록한다. 원본 타일 2개는 출처 추적을 위해 유지하되 `use_yn = 'N'`으로 두고, 공용 DEM만 `use_yn = 'Y'`로 활성화한다. 실행 결과에 출력된 공용 `dem_id`를 모든 산의 신규 고도 분석 요청에 사용한다. 기존 분석 행이 참조하는 `dem_id`는 바뀌지 않는다.

GeoTIFF 파일은 Git에 커밋하지 않는다. 다음 명령으로 SHA-256을 확인하고 시드에 기록된 값과 일치하는지 검증한다.

```bash
sha256sum database/dem/n37e127_dem.tif
sha256sum database/dem/seoul_copernicus_glo30.tif
```

현재 시드에 기록된 해시는 다음과 같다.

```sql
N37E127: 4f8fef79da8c18ed9a9a0fb97b3d2695c4681181e013e039d87044e5479f7df6
서울권 공용: a3e545058390babe4abe266d13d0cb4e9aef51e349a8893005f649cf7868ce9d
```

`run_seed.sh all`은 서버별 DEM 파일 배치 여부를 판단할 수 없으므로 DEM 시드를 자동 실행하지 않는다.

## 실행 확인

스크립트는 실행 전에 접속한 DB, 사용자, 서버와 실행할 SQL 목록을 출력하고 확인을 요청한다. 표시된 대상이 의도한 DB와 다르면 `N`을 입력해 취소한다.

자동화된 비대화형 환경에서는 확인 메시지 대신 다음 값을 명시해야 한다.

```bash
CONFIRM_SEED_RUN=yes bash ./database/seed/run_seed.sh inwang
```

`ON_ERROR_STOP=1`을 사용하므로 SQL 오류가 발생하면 즉시 중단된다. 성공 메시지가 출력된 뒤 해당 산의 `mountain`, `raw_trail`, `trail_spot`, `trail_node`, `trail_edge`, `service_area` 및 관련 매핑 데이터가 생성됐는지 확인한다.
