# Database Convention

## 범위

이 문서는 PostgreSQL 및 공간 확장(PostGIS·pgRouting)을 사용하는 스키마·SQL·MyBatis SQL·Flyway Migration 규칙을 정의한다. 실제 DB 버전, 확장 설치 상태, 스키마명, 테이블명, 컬럼명, SRID와 네트워크 모델은 Repository와 데이터 사전을 확인하며 여기서 임의로 확정하지 않는다. 저장 SRID 기준은 [gis-convention.md](gis-convention.md)를 따른다.

현재 저장소에는 승인된 migration SQL이 없다. Flyway 의존성과 기본 설정은 준비되어 있으나 `spring.flyway.enabled=false`다. Migration 위치는 `classpath:db/migration`, 적용 이력 검증은 `validate-on-migrate=true`이며, 기존 DB와 baseline을 확인하고 합의하기 전까지 migration을 실행하거나 초기 스키마를 만들지 않는다.

## SQL

- 필요한 컬럼을 명시하고 `SELECT *`를 사용하지 않는다.
- 테이블과 컬럼 별칭은 일관되고 의미 있게 작성한다.
- 대량 조회는 페이지 또는 공간 범위 등 명시적인 제한을 둔다.
- 사용자 값은 MyBatis `#{...}`로 바인딩한다.
- 정렬 컬럼 등 식별자 동적 선택은 서버가 정의한 허용 목록으로 제한한다.
- 수정·삭제는 의도한 조건과 영향 행 수를 확인한다.
- 복잡하거나 성능 민감한 SQL은 실제 데이터 분포와 실행 계획을 검토한다.
- pgRouting 함수에 넘기는 내부 SQL 문자열은 고정 문자열로 두고, 사용자 값은 바깥 파라미터로만 바인딩한다.
- 공간 SQL은 [gis-convention.md](gis-convention.md)를 따른다.

## 스키마와 정합성

- PK, FK, UNIQUE, NOT NULL, CHECK 등 DB 제약조건으로 가능한 정합성을 보장한다.
- Geometry 컬럼은 `geometry(<유형>, <SRID>)`로 유형과 SRID를 고정한다.
- 인덱스는 실제 조회 조건과 쓰기 비용을 근거로 추가한다. Geometry 컬럼에는 GiST 인덱스를 둔다.
- Geometry 인덱스의 연산자와 함수가 실제로 인덱스를 활용하는지 확인한다.
- 애플리케이션 검증만으로 동시성·정합성이 보장된다고 가정하지 않는다.
- 트랜잭션 범위는 Service의 비즈니스 단위와 일치시킨다.

## MyBatis XML

Mapper XML은 기본적으로 다음 위치에서 도메인별로 관리한다.

```text
src/main/resources/sqlmap/<domain>/
```

Repository 설정이 다른 경로를 사용하면 실제 설정을 우선한다. namespace는 Mapper interface와, statement id는 메서드와 일치시킨다. 재사용할 가치가 있는 SQL 조각만 `<sql>`과 `<include>`로 분리한다.

## Flyway

Migration은 다음 위치를 기본으로 한다.

```text
src/main/resources/db/migration/
```

파일명은 반드시 다음 형식을 사용한다.

```text
VyyyyMMddHHmmss__description.sql
```

예: `V20261012143000__add_course_spatial_index.sql`

- 새 스키마 변경은 새 Migration으로 추가한다.
- 이미 공유되거나 적용된 Migration의 내용과 버전을 수정하지 않는다.
- 동일 timestamp를 중복 사용하지 않고 description은 변경 목적이 드러나는 영문 snake_case로 작성한다.
- `dev`를 받은 뒤 다른 사람의 Migration과 순서가 꼬이지 않았는지 timestamp를 다시 확인한다.
- 실행 순서, 잠금 시간, 기존 데이터, 롤백 또는 복구 방법, 애플리케이션 호환성을 검토한다.
- 파괴적 변경은 개발자 승인과 배포 순서 없이 수행하지 않는다.
- PostGIS·pgRouting 확장 생성과 권한 변경은 배포 환경 권한을 확인한 뒤 Migration 정책에 맞춘다.
- 수동 DDL로 Flyway 이력을 우회하지 않는다.

## 원천 데이터

- Flyway는 스키마만 관리한다. 산림청 등산로, 상세지점, DEM 분석 결과 같은 원천 데이터를 Migration에 넣지 않는다.
- 원천 데이터 적재 담당과 방법은 미확정이다. 확정 전에는 원천 데이터 테이블에 직접 쓰는 코드를 만들지 않는다.
- 원천 파일(SHP, GeoTIFF)과 DB 덤프는 Git에 올리지 않는다.

Migration 또는 Mapper SQL 변경의 검증은 [testing-convention.md](testing-convention.md), 작업 리뷰 절차는 [git-convention.md](git-convention.md)를 따른다.
