# ORDA API

GIS 기반 개인 맞춤형 등산 코스 탐색·분석 서비스의 백엔드입니다.
Java 17, Spring Boot 2.7.18, eGovFrame 4.1.0, MyBatis를 사용하고 **외부 Tomcat 8.5.95에 WAR로 배포**합니다.
Tomcat 8.5.95는 프로젝트 필수 조건입니다.

## 빌드와 검증

JDK 17이 필요합니다. Maven 3.9.16은 Wrapper가 다운로드합니다.

```powershell
.\mvnw.cmd clean package
```

macOS/Linux에서는 `bash ./mvnw clean package`를 사용합니다.
Spotless, Checkstyle, Java 컴파일과 WAR 패키징을 실행합니다.
현재 자동 테스트 코드는 없으며 빌드에 Docker는 필요하지 않습니다.

```powershell
.\mvnw.cmd spotless:apply   # Java 자동 포맷
.\mvnw.cmd validate         # 포맷·린트 검사
```

빌드 결과는 `target/orda-api.war`입니다. 애플리케이션 패키지는 `com.allforland.orda`입니다.

## 로컬 DB와 Tomcat 실행

1. `application-local.yml.example`을 사용자 홈의 `.orda/application-local.yml`로 복사해 실제 DB 접속 정보를 입력합니다.
   Windows 예: `C:\Users\사용자명\.orda\application-local.yml`.
   저장소의 `src/main/resources/application-local.yml`은 외부 파일을 읽는 공개 설정이며 비밀번호를 넣지 않습니다.
2. SSH 터널을 별도 터미널에서 실행합니다.
3. IntelliJ Tomcat 실행 설정의 VM options에 `-Dspring.profiles.active=local`을 지정합니다.
4. 외부 Tomcat **8.5.95**, JDK 17을 선택하고 `target/orda-api.war`를 배포합니다.
5. FE와 함께 개발할 때 Deployment의 **Application context를 `/`**로 지정합니다.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-db-tunnel.ps1 -KeyPath "<pem 파일 경로>"
```

터널은 로컬 `127.0.0.1:15432`를 AWS 서버의 `127.0.0.1:55432`에 연결합니다.
터미널을 열어두고, 같은 포트에 터널이 이미 실행 중이면 중복 실행하지 않습니다.
최초 접속 시 `ssh -i "<pem 파일 경로>" ubuntu@43.203.221.58`로 서버 호스트 키를 확인하고 등록합니다.
`SPRING_DATASOURCE_*` 환경 변수는 파일보다 우선하므로 로컬 파일과 중복 설정하지 않습니다.
기존 설정과 달리 이제 **local 프로필을 명시해야 개인 파일을 읽습니다.**

## FE 연동과 API 문서

루트 컨텍스트로 배포하면 다음 주소를 사용합니다.

| 용도 | 주소 |
| --- | --- |
| Swagger UI (local만) | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON (local만) | `http://localhost:8080/v3/api-docs` |

FE의 `/api → localhost:8080` 프록시는 그대로 사용합니다. `webapps/orda-api.war`로 기본 배포하면 컨텍스트가 `/orda-api`이므로 위 주소에 `/orda-api`가 붙습니다.
직접 배포 시 루트 컨텍스트가 필요하면 전용 Tomcat에 `ROOT.war`로 배포하거나 Tomcat Context를 별도로 구성합니다. 기존 ROOT 애플리케이션을 임의로 덮어쓰지 않습니다.
외부 Tomcat의 컨텍스트는 `server.servlet.context-path`만으로 바뀌지 않습니다.

## 운영 설정

`SPRING_PROFILES_ACTIVE=prod`와 다음 환경 변수를 Tomcat 프로세스에 제공합니다.

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

운영에는 개인 홈의 `.orda` 파일이 필요하지 않습니다. API 문서 노출은 기본 비활성화입니다.
운영 프록시의 `/api` 전달 경로는 실제 Tomcat 컨텍스트와 맞춰야 합니다.
인증 방식은 미확정입니다. 이 초기 세팅은 사용자·관리자 권한이 필요한 API의 공개 배포 준비 완료를 의미하지 않습니다.

## DB 변경 관리

Flyway 의존성과 안전 설정을 준비했지만 자동 migration은 기본 비활성화입니다.
기존 DB를 조회하거나 변경하지 않았으며 가상의 테이블 생성 SQL도 추가하지 않았습니다.
실제 스키마·DB/확장 버전·baseline을 확인한 후 [DB 규칙](docs/database-convention.md)에 따라 활성화합니다.

## 협업 문서와 CodeRabbit

기본 브랜치는 `dev`이며 새 작업은 `feature/{domainName}-{detail}`, `refactor/{domainName}-{detail}`, `fix/{domainName}-{detail}`에서 진행합니다.
작업 PR은 `dev`, 릴리스 PR은 `dev` → `main`으로 생성하며 `main` 직접 push는 금지합니다.
커밋은 `type: 작업 내용`, PR 제목은 `[타입] 제목 (#이슈번호)`를 사용합니다.
기능 패키지는 `com.allforland.orda.domain` 아래, Mapper XML은 `src/main/resources/sqlmap/<domain>/`에 둡니다.

- [구조](docs/architecture.md), [백엔드 규칙](docs/backend-convention.md)
- [API 규칙](docs/api-convention.md), [GIS 규칙](docs/gis-convention.md)
- [검증·향후 테스트 규칙](docs/testing-convention.md), [Git·리뷰](docs/git-convention.md)
- [계약 결정과 미확정 사항](docs/contract-decisions.md)
- [AI 작업 지침](AGENTS.md)

`.coderabbit.yaml`은 한국어 리뷰, `dev`/`main` 대상 자동 리뷰, 초안 PR 제외, 도메인별 검토 기준을 정의합니다.
GitHub의 CodeRabbit App을 이 저장소에 연결해야 실제 리뷰가 동작합니다. YAML 추가는 App 설치나 원격 저장소 설정 변경을 수행하지 않습니다.
CodeRabbit 외에 팀원 최소 1명의 리뷰가 필요합니다. 이를 강제하려면 GitHub branch protection/ruleset도 별도로 설정합니다.
