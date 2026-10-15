# Testing Convention

## 원칙

현재 저장소에는 자동 테스트 코드, 테스트 전용 의존성, Docker 기반 검증 구성이 없다. 이를 별도 요청 없이 다시 추가하지 않는다.

## 도구와 명령

| 목적 | Windows | macOS/Linux |
|---|---|---|
| 포맷·린트·컴파일·WAR 패키징 | `./mvnw.cmd clean package` | `bash ./mvnw clean package` |

성공한 빌드는 테스트 통과나 실제 Tomcat 배포·DB 연결 검증을 의미하지 않는다. 테스트를 새로 도입할 때는 별도 요청과 격리 DB 전략이 필요하다.

## 향후 테스트 도입 시 기준

- Service 단위 테스트: 도메인 규칙, 분기, 예외, 트랜잭션 단위의 의도
- Web 테스트: 경로, 입력 검증, 상태 코드, 직렬화와 오류 계약
- Mapper 통합 테스트: Mapper XML 매핑, SQL 결과, `null`과 빈 결과, 변경 행 수
- DB 통합 테스트: PostgreSQL 제약조건, Flyway 적용, 트랜잭션
- GIS 테스트: SRID, 축 순서, 공간 경계, 거리 단위, 무효 Geometry, 경로 없음·단절·스냅 실패
- 회귀 테스트: 재현 가능한 버그 조건과 기대 결과

## 작성 규칙

- 테스트 이름은 조건과 기대 결과를 드러낸다.
- 외부 시스템은 테스트 경계에서 통제하되 검증 대상 SQL까지 모킹하지 않는다.
- 순서와 공유 상태에 의존하지 않도록 독립적으로 구성한다.
- 테스트 데이터는 의도를 읽을 수 있을 만큼 작고 명시적으로 유지한다. 경로 테스트는 노드 3~5개짜리 작은 네트워크로 만든다.
- 실제 PostgreSQL/PostGIS 동작이 중요한 쿼리를 H2 등 다른 DB의 동작만으로 검증 완료 처리하지 않는다.

## 완료 기준

공유 AWS DB를 사용하지 않는다. 공간 SQL은 운영과 호환되는 PostgreSQL·PostGIS·pgRouting 환경에서 좌표·단위·방향·결측을 검증한다. 실행하지 않은 검증은 통과했다고 보고하지 않는다.

계층 경계는 [architecture.md](architecture.md), SQL 규칙은 [database-convention.md](database-convention.md), 공간 의미는 [gis-convention.md](gis-convention.md)를 따른다.
