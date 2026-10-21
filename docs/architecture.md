# Backend Architecture

## 목적과 범위

ORDA GIS 등산 코스 서비스의 Backend는 REST API, 비즈니스 규칙, 데이터 접근, 공간 질의와 경로 탐색을 담당하는 독립 Repository다. 이 문서는 기술 스택, 계층 경계, 시스템 경계, 처리 흐름을 정의한다.

## 기술 스택

- Java 17, Spring Boot 2.7.18, eGovFrame 4.1.0, Spring MVC
- MyBatis, Maven WAR
- 외부 Tomcat 8.5.95 배포 (필수)
- PostgreSQL JDBC 드라이버가 설정되어 있다. PostGIS·pgRouting과 실제 DB 버전/확장 상태는 운영 스키마와 대조한다.
- Flyway는 신규 빈 DB용 초기 Migration을 `src/main/resources/db/migration/`에서 실행한다. 기존 DB 전환은 baseline 전략 합의 후 별도로 진행한다.
- 현재 자동 테스트 코드와 테스트 전용 의존성은 없다.

실제 버전은 `pom.xml`을 기준으로 한다. 기술 스택과 외부 Tomcat 요구사항을 임의로 바꾸지 않는다.

## 시스템 경계

```text
Frontend          ── REST /api ──▶  외부 Tomcat 8.5.95 (WAR) ──▶ Spring MVC / MyBatis ──▶ PostgreSQL
GeoServer (선택, 별도) ── 고정 레이어 WMS 발행 ──▶ Frontend
```

- API, 권한 검증, 공간 연산, 경로 탐색과 분석은 Backend 책임이다. 구체적인 DEM 처리 방식과 원천 데이터 적재 담당은 미확정이다.
- Flyway는 스키마만 관리하고 원천 데이터를 넣지 않는다.

## 도메인

| 도메인 | 담당 | 범위 |
|---|---|---|
| `trail` | 공용 | 등산로·노드·시설 조회 |
| `course` | 1조 | 내 코스 등록·수정·삭제 |
| `route` | 2조 | 좌표 스냅, 경로 탐색, 후보 비교·저장·내보내기 |
| `analysis` | 3조 | 고도·경사·지점 분석 결과 조회 |
| `user` | 회원 | 회원가입·BCrypt 비밀번호 저장 |
| `auth` | 인증 | JWT 로그인·현재 사용자. 공통 보안 설정은 common/security |

## 계층과 의존 방향

```text
HTTP 요청
  → controller
  → service / service.impl
  → mapper (+ sqlmap XML)
  → PostgreSQL · PostGIS · pgRouting
```

- `controller`: HTTP 입력 형식 검증, Service 호출, DTO 응답 변환
- `service`: 비즈니스 계약과 트랜잭션 경계
- `service.impl`: 서비스 구현이 분리된 경우의 구현체 (분리 여부 미확정)
- `mapper`: MyBatis를 통한 영속성 접근. eGov의 DAO 역할
- `dto`: API 요청·응답 데이터
- `vo`: DB 조회 결과 표현

공통 기반은 `common/`에 둔다. 현재 공통 오류 처리는 `common/error/`, 요청 추적은 `common/logging/`에 있다. 패키지 예시는 [backend-convention.md](backend-convention.md)에 있으며 모든 폴더를 미리 만들지 않는다.

## 주요 흐름

1. Controller가 HTTP 요청을 받아 형식 검증을 수행한다.
2. Service가 도메인 규칙과 트랜잭션을 관리한다.
3. Mapper와 XML SQL이 데이터를 조회·변경한다.
4. 공간 질의와 경로 탐색은 PostGIS·pgRouting에서 수행하고 Service가 결과 의미를 구성한다.
5. Controller가 [api-convention.md](api-convention.md)에 맞는 응답을 반환한다.

경로 탐색의 개념 흐름 (`route`; 실제 알고리즘, API, 오류 코드는 승인된 계약을 따른다):

```text
RouteController → RouteServiceImpl
  1. 출발·도착 좌표를 유효한 네트워크 지점에 연결
  2. 조건에 맞는 비용식 선택
  3. pgRouting으로 후보 경로 탐색
  4. 겹치는 후보 제거 후 응답 DTO 구성
```

좌표·공간분석 기준은 [gis-convention.md](gis-convention.md), DB 객체와 Migration은 [database-convention.md](database-convention.md), 테스트 경계는 [testing-convention.md](testing-convention.md)를 따른다.
