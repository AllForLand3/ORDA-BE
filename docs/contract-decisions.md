# 계약 결정과 확인할 사항

## 근거와 상태

2026-10-15 초기 세팅 기준이며, 2026-10-18 사용자가 제공·승인한 ORDA 테이블 정의서와 신규 빈 DB 운영 결정을 반영했다. BE `chore/#1-project-setup`과 FE `chore/#1-setup`의 코드·문서,
사용자가 제공한 CodeRabbit 검토 지침과 후속 브랜치·커밋·Issue·PR·폴더 규칙을 반영했다. 이후 스키마 변경은 적용된 Migration을 수정하지 않고 새 Migration으로 추가한다.

## 적용한 설정

| 항목 | 현재 적용 | 근거/범위 |
| --- | --- | --- |
| Tomcat | 외부 8.5.95, WAR 유지 | 사용자 필수 조건 |
| 브랜치 | default/integration `dev`, 릴리스 `dev` → `main`, 새 작업 `feature/*`·`refactor/*`·`fix/*` | 제공된 협업 규칙. 기존 setup 브랜치는 유지하며 혼용된 develop은 dev로 통일 |
| 커밋·PR | `type: 작업 내용`, `[타입] 제목 (#이슈번호)`, 팀원 최소 1명 리뷰·코멘트 반영 | AGENTS·Git 문서·템플릿·CodeRabbit에 반영. 원격 보호 규칙은 별도 설정 |
| 도메인 구조 | `com.allforland.orda.domain.trail/course/route/analysis/user/auth`, 공통은 `common` | 기능 구현 시 필요한 패키지만 생성 |
| Mapper XML | `src/main/resources/sqlmap/<domain>/*.xml`, `classpath*:sqlmap/**/*.xml` | 문서와 실제 MyBatis 검색 설정 동기화 |
| FE 연동 경로 | `/api`, 로컬 Tomcat 루트 컨텍스트 권장 | 기존 FE 프록시 유지. IDE/서버 배포 설정 필요 |
| 오류 응답 | `status/code/message/details/traceId` | FE 필드 보존을 위한 임시 호환 구현. 최종 계약 합의 아님 |
| details | `{field, message}` 배열, 없으면 `[]` | 초기 구현. FE 타입 확장은 별도 반영 필요 |
| API 공간 데이터 | EPSG:4326 GeoJSON, 경도·위도, m/% | 기존 FE 규칙과 제공된 지침 |
| DB migration | 신규 빈 DB용 초기 Migration, 자동 적용 활성화 | 사용자 승인 테이블 정의서, 기존 DB 전환은 baseline 별도 합의 |
| 헬스체크·테스트 | 기존 Actuator·CI/CD 구성 유지, 자동 테스트 코드·의존성 추가 안 함 | Maven package는 포맷·린트·컴파일·WAR 패키징을 수행하며 빌드 성공을 테스트 통과로 간주하지 않음 |

## 기능 개발 전에 결정할 사항

| 항목 | 충돌·미확정 내용 | 확인할 근거 |
| --- | --- | --- |
| 오류 필드 | FE의 status/code/message vs 초안 code/message/details/traceId | status 유지, details 구조, FE 타입 반영 합의 |
| 형상 이력 | 정의서의 course.geom/rev_no 덮어쓰기 vs 불변 course_revision | 실제 migration와 버전·분석 참조 모델 |
| 추가 모델 | network_version, routing_run_condition, route_condition, processing_rule, import_job, comparison_snapshot/item, saved_course | 실제 도입 범위와 스키마 |
| edge_seq | 단일 bigint 메모 vs bigint[] | 순서·방향 저장 방식 |
| 코드 값 | IMPORT/DRAW/CANDIDATE_COPY, FROM_CANDIDATE, FROM_ROUTE_CANDIDATE; NORMAL/MODERATE·EXPERT; DONE/COMPLETED; 시설 유형 | DB와 DTO의 명시적 변환 여부 |
| 경로 입력 | FE의 노드 ID 뼈대·지도 좌표 목표 vs 초안 mountainId/startPlaceId/endPlaceId | 지점·좌표 선택과 스냅 책임, place/node 연결 |
| DB SRID | EPSG:5179 | 사용자 승인 및 초기 Migration Geometry 컬럼에 반영 |
| 인증 확장 | Refresh Token·서버 로그아웃, 역할 전용 API | 기본 회원가입·JWT 인증은 아래 2026-10-21 결정 적용 |
| 저장 권한 확장 | 비로그인 후보의 로그인 후 저장, 기존 시연 코스 공개·이전 | 개인 코스 소유권은 아래 2026-10-21 결정 적용 |
| GPX 입력 | 기존 MVP 제외 vs API D-01 포함 | 이번 MVP 범위 |
| 장기 보존 | 코스·후보·네트워크·분석 계보와 삭제 | 보존 기간과 논리 삭제 정책 |
| 목록·ID | 페이지 필드·기본값, bigint 문자열 반환 | FE·BE DTO 합의 |
| 기존 DB 전환 | 신규 빈 DB용 초기 스키마와 별개로 baseline 전략 필요 | 기존 DB의 스키마·적용 이력·배포 권한 확인 |

결정 시 날짜, 승인자/PR, 합의 내용, 영향을 받는 FE·BE·migration을 기록하고 관련 문서를 함께 갱신한다.

## 2026-10-17 공용 시연 작업공간 유지 결정

사용자는 CodeRabbit PR #5 리뷰 검토 후 당분간 공용 시연 방식을 유지하기로 재확인했다. 이번 코스 CRUD에는 로그인·회원가입·개인별 소유권 검증을 추가하지 않는다. 모든 접속자가 서버 설정의 활성 계정에 속한 코스를 공유하며, 감사 식별자도 실제 접속자가 아닌 설정 계정의 ID다.

`.env.example`의 임시 계정 ID `3`은 유지한다. 실제 DB에서 해당 계정이 시연 전용인지 확인한 결과를 뜻하지 않는다. 계정의 활성 여부와 조회·수정·삭제의 작업공간 범위 검증은 유지한다. 다른 DB에 연결할 때는 해당 환경의 계정 용도와 데이터 범위를 확인해야 한다.

## 2026-10-21 회원가입·JWT·개인 코스 소유권 전환 결정

2026-10-17 공용 시연 작업공간 결정은 이력으로 보존하며, 현재 인증·코스 정책은 아래와 같이 적용한다.

- `domain.user`는 회원가입, `domain.auth`는 로그인·현재 사용자 조회를 담당한다. 신규 회원은 BCrypt cost 12 해시와 USER/Y로 저장한다.
- 새 migration으로 nullable `app_user.password_hash VARCHAR(255)`를 추가한다. 기존 migration·seed·계정·코스 소유자 FK는 유지하며, 해시가 없는 레거시 계정은 로그인할 수 없다.
- JWT는 HS256, 외부 Base64 키(최소 32바이트), `user_id` subject를 사용한다. 기본 유효시간은 3600초(1시간)이며 TTL 변경은 새 토큰에만 적용한다.
- 매 Bearer 요청에서 토큰 서명·issuer·시간을 검증하고 DB의 회원 존재·활성 여부·유효한 BCrypt 해시·현재 USER/ADMIN 역할을 확인한다. 토큰의 역할 claim은 권한 근거로 사용하지 않는다.
- 코스 소유자는 인증된 사용자 ID로 정하며 공용 소유자 환경변수는 사용하지 않는다. 다른 사용자 코스는 ADMIN을 포함해 404로 처리하고, 기존 코스를 신규 회원에게 자동 배정하지 않는다. ETag·If-Match·412·논리 삭제·공간 검증은 유지한다.
- `nickname`은 `user_nm`에 매핑하고 중복을 허용한다. 아이디 중복은 DB UNIQUE 기준 409로 처리하며 식별자는 양의 bigint 문자열로 반환한다. 비밀번호 최소 길이·조합 규칙은 없고 빈 값·잘못된 Unicode·제어문자를 거부하며 UTF-8 72바이트로 제한한다. 상세 입력·응답·오류 계약은 [auth-api.md](auth-api.md)를 따른다.
- CORS는 `ORDA_AUTH_CORS_ALLOWED_ORIGINS`의 정확한 HTTP(S) Origin만 허용한다. 기본은 빈 값이며 로컬 예시는 `http://localhost:5173`이다. 와일드카드·쿠키 credentials는 허용하지 않는다. Authorization·If-Match 요청과 ETag·Location·X-Trace-Id 응답 헤더를 지원한다.
- 가입·로그인 POST와 활성화된 Swagger/OpenAPI·기존 health GET/HEAD만 공개하고, 나머지는 기본 인증을 요구한다. 새 공개 API·관리자 API는 별도 접근 정책을 지정한다.
- 코스 좌표 개수·JSON 본문 크기 상한은 두지 않는다. 좌표 형식·범위·최소 2개 점·비퇴화 형상·서비스 영역·DB 길이 검증은 유지한다.

남은 결정은 해시 NULL 허용 종료 시점과 레거시 계정 전환·비밀번호 복구 절차, 기존 시연 코스의 공개·이전 정책,
브라우저 새로고침 후 토큰 유지, 키 교체 절차, Refresh Token·서버 로그아웃, 역할 전용 API·감사 모델이다.
별도 합의 전 관련 API·테이블이나 데이터 변경을 추가하지 않는다.

격리 로컬 PostgreSQL·Tomcat에서 가입·로그인·/me·CORS·기본 접근 정책을 일부 확인했다.
코스 전체 CRUD·GIS·기존 시연 데이터 보존 검증은 미실행이며, 공유·운영 DB에는 접속하지 않았다.
