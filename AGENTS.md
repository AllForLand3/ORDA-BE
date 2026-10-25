# ORDA Backend AI 작업 지침

이 문서는 AI 도구가 이 저장소에서 작업할 때 따르는 규칙이다. 실제 코드·설정과 이 문서가 다르면 차이를 숨기지 말고 사용자에게 보고한다.

## 프로젝트와 기술 기준

- Java 17, Spring Boot 2.7.18, eGovFrame 4.1.0, Maven WAR, MyBatis를 사용한다. 실제 버전은 `pom.xml`을 확인한다.
- 외부 Tomcat 8.5.95 배포는 필수다. WAR와 `javax` Servlet/Validation API 구성을 유지한다.
- API, 권한 검증, 공간 연산, 경로 탐색, 분석은 Backend 책임이며 Frontend는 표현을 담당한다.
- JPA/Hibernate를 임의 도입하거나 기술 버전을 임의 변경하지 않는다.

## 도메인과 파일 배치

- 기능은 `com.allforland.orda.domain` 아래에 둔다: `trail`(공용 등산로·노드·시설), `course`(코스 CRUD), `route`(탐색·비교), `analysis`(고도·경사), `user`(회원), `auth`(로그인·현재 사용자).
- 도메인별 필요한 controller/service/mapper/dto만 만든다. 불필요한 전달 계층·인터페이스와 빈 패키지를 만들지 않는다.
- 공통 예외·오류 응답·설정·로깅은 `common`에 둔다.
- Mapper XML은 `src/main/resources/sqlmap/<domain>/*.xml`, 검색 설정은 `classpath*:sqlmap/**/*.xml`이다.
- 관련 코드와 `docs/contract-decisions.md`를 먼저 읽는다. API 초안과 테이블 정의서 원문은 저장소에 없으며 CodeRabbit 메모를 승인된 스키마로 취급하지 않는다.

## API, GIS, DB

- API 좌표는 EPSG:4326 GeoJSON `[longitude, latitude]`, 화면은 EPSG:3857이다. DB 저장·거리 연산은 EPSG:5179이며 Geometry 컬럼은 유형과 SRID를 고정한다.
- 좌표계를 임의 변환하거나 결측값을 0으로 채우지 않는다. SRID, 테이블·컬럼, 인증 방식 등 미확정 계약을 추측해 확정하지 않는다.
- SQL 값은 `#{}`로 바인딩한다. 동적 식별자는 서버 허용 목록으로 제한한다.
- 원자적 변경의 트랜잭션은 Service가 관리한다.
- 신규 빈 DB용 초기 Migration은 `src/main/resources/db/migration/`에서 관리한다. 적용된 Migration을 수정하거나 삭제하지 않으며, 기존 DB를 Flyway 대상으로 전환할 때는 별도의 baseline 전략을 합의한다.
- 실 DB와 공유 AWS DB를 테스트에 사용하지 않는다. 적용된 migration을 수정하거나 baseline을 자동 생성하지 않는다.

## 보안과 오류

- 요청·응답 본문, 비밀번호, 토큰, 개인정보를 로그에 넣지 않는다.
- 오류 응답에 내부 예외·SQL·경로를 노출하지 않는다.
- 인증은 사용자 승인(2026-10-21)에 따라 BCrypt·JWT Bearer로 구현한다. 토큰 서명·만료 검증 후 매 요청 app_user의 존재·활성·해시·현재 권한을 확인한다. 외부 JWT 키가 부적합하면 기동하지 않는다.
- 코스 CRUD는 인증된 사용자 user_id만 소유자로 사용한다. 다른 사용자의 코스는 404이며 기존 시연 계정·코스는 임의 변경하지 않는다. 2026-10-17 공용 작업공간 결정 이력은 docs/contract-decisions.md에 보존한다.

## Git과 변경 범위

- 기본 개발 브랜치는 `dev`다. 새 브랜치는 최신 `dev`에서 `feature/{domainName}-{detail}`, `refactor/{domainName}-{detail}`, `fix/{domainName}-{detail}` 형식으로 만든다. 기존 작업 브랜치를 임의로 바꾸거나 이름을 바꾸지 않는다.
- 기능 PR은 `dev`를 대상으로 하고 릴리스는 `dev`에서 `main`으로 진행한다. `main`에 직접 push하지 않는다.
- 명시적 요청 없이 commit/push/merge, 이력 재작성, 브랜치 삭제를 하지 않는다. 사용자 변경을 보존한다.
- 커밋 메시지는 `type: 작업 내용` 형식이다. 허용 타입은 `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `design`, `comment`, `rename`, `remove`, `!HOTFIX`다.
- PR 제목은 `[타입] 제목 (#이슈번호)` 형식이다. 본문은 `.github/PULL_REQUEST_TEMPLATE.md`를 따른다.

## 검증과 보고

- Windows 빌드 명령은 `./mvnw.cmd clean package`, macOS/Linux는 `bash ./mvnw clean package`다.
- 현재 사용자 요청으로 헬스체크와 자동 테스트 코드를 제거했다. 테스트 의존성·Docker 기반 검증도 없으므로, 별도 요청 없이 테스트나 헬스체크를 추가하지 않는다.
- 빌드 성공을 테스트 통과로 보고하지 않는다. 검증하지 않은 항목은 미실행으로 보고한다.
- 요청 범위 밖의 기술 교체, 대량 포맷팅, 미확정 기능 구현을 하지 않는다.
- 완료 시 변경 내용, 실행한 검증과 결과, 미실행 검증 및 남은 결정을 한국어로 보고한다.

세부 문서: [구조](docs/architecture.md), [Backend](docs/backend-convention.md), [API](docs/api-convention.md), [GIS](docs/gis-convention.md), [DB](docs/database-convention.md), [테스트](docs/testing-convention.md), [Git](docs/git-convention.md).
