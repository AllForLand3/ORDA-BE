# 계약 결정과 확인할 사항

## 근거와 상태

2026-10-15 초기 세팅 기준이다. BE `chore/#1-project-setup`과 FE `chore/#1-setup`의 코드·문서,
사용자가 제공한 CodeRabbit 검토 지침과 후속 브랜치·커밋·Issue·PR·폴더 규칙을 반영했다. API 초안과 테이블 정의서 원문, 실제 DB 스키마는 제공되지 않았다.
아래 초안 요약을 승인된 DB 정의로 취급하거나 그 내용을 근거로 테이블을 생성하지 않는다.

## 적용한 설정

| 항목 | 현재 적용 | 근거/범위 |
| --- | --- | --- |
| Tomcat | 외부 8.5.95, WAR 유지 | 사용자 필수 조건 |
| 브랜치 | default/integration `dev`, 릴리스 `dev` → `main`, 새 작업 `feature/*`·`refactor/*`·`fix/*` | 제공된 협업 규칙. 기존 setup 브랜치는 유지하며 혼용된 develop은 dev로 통일 |
| 커밋·PR | `type: 작업 내용`, `[타입] 제목 (#이슈번호)`, 팀원 최소 1명 리뷰·코멘트 반영 | AGENTS·Git 문서·템플릿·CodeRabbit에 반영. 원격 보호 규칙은 별도 설정 |
| 도메인 구조 | `com.allforland.orda.domain.trail/course/route/analysis`, 공통은 `common` | 제공된 BE 폴더 규칙. 기능 구현 시 필요한 패키지만 생성 |
| Mapper XML | `src/main/resources/sqlmap/<domain>/*.xml`, `classpath*:sqlmap/**/*.xml` | 문서와 실제 MyBatis 검색 설정 동기화 |
| FE 연동 경로 | `/api`, 로컬 Tomcat 루트 컨텍스트 권장 | 기존 FE 프록시 유지. IDE/서버 배포 설정 필요 |
| 오류 응답 | `status/code/message/details/traceId` | FE 필드 보존을 위한 임시 호환 구현. 최종 계약 합의 아님 |
| details | `{field, message}` 배열, 없으면 `[]` | 초기 구현. FE 타입 확장은 별도 반영 필요 |
| API 공간 데이터 | EPSG:4326 GeoJSON, 경도·위도, m/% | 기존 FE 규칙과 제공된 지침 |
| DB migration | 의존성과 규칙만, 자동 적용 비활성화 | 기존 스키마/baseline 미확인 |
| 헬스체크·테스트 | 헬스체크 API와 자동 테스트 코드·설정·전용 의존성 제거 | 사용자 요청. Maven package는 포맷·린트·컴파일·WAR 패키징을 수행하며 CI workflow는 저장소에 없음 |

## 기능 개발 전에 결정할 사항

| 항목 | 충돌·미확정 내용 | 확인할 근거 |
| --- | --- | --- |
| 오류 필드 | FE의 status/code/message vs 초안 code/message/details/traceId | status 유지, details 구조, FE 타입 반영 합의 |
| 형상 이력 | 정의서의 course.geom/rev_no 덮어쓰기 vs 불변 course_revision | 실제 migration와 버전·분석 참조 모델 |
| 추가 모델 | network_version, routing_run_condition, route_condition, processing_rule, import_job, comparison_snapshot/item, saved_course | 실제 도입 범위와 스키마 |
| edge_seq | 단일 bigint 메모 vs bigint[] | 순서·방향 저장 방식 |
| 코드 값 | IMPORT/DRAW/CANDIDATE_COPY, FROM_CANDIDATE, FROM_ROUTE_CANDIDATE; NORMAL/MODERATE·EXPERT; DONE/COMPLETED; 시설 유형 | DB와 DTO의 명시적 변환 여부 |
| 경로 입력 | FE의 노드 ID 뼈대·지도 좌표 목표 vs 초안 mountainId/startPlaceId/endPlaceId | 지점·좌표 선택과 스냅 책임, place/node 연결 |
| DB SRID | 제공 지침은 EPSG:5179 | 실제 geometry SRID·단위·타입과 데이터 |
| 인증 | 로그인, 비밀번호 저장, Refresh Token, PUBLIC/USER/ADMIN | 승인된 인증·인가 정책 |
| 저장 권한 | 비로그인 후보의 로그인 후 저장, 코스 공개 범위 | 사용자·자원 소유권 정책 |
| GPX 입력 | 기존 MVP 제외 vs API D-01 포함 | 이번 MVP 범위 |
| 장기 보존 | 코스·후보·네트워크·분석 계보와 삭제 | 보존 기간과 논리 삭제 정책 |
| 목록·ID | 페이지 필드·기본값, bigint 문자열 반환 | FE·BE DTO 합의 |
| DB 구성 원본 | 구조 예시의 compose.yaml·V20261012000000__init_spatial_schema.sql은 현재 저장소에 없음 | 기존 DB 전용 Compose·승인된 초기 스키마 원문 확인 후 반영 |

결정 시 날짜, 승인자/PR, 합의 내용, 영향을 받는 FE·BE·migration을 기록하고 관련 문서를 함께 갱신한다.
