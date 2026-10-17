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
   `REPLACE_WITH_DB_NAME`, `REPLACE_WITH_DB_USERNAME`, `REPLACE_WITH_DB_PASSWORD`를 팀 내부에서 공유받은 값으로 바꿉니다.
   저장소의 `src/main/resources/application-local.yml`은 외부 파일을 읽는 공개 설정이며 비밀번호를 넣지 않습니다.
2. SSH 터널을 별도 터미널에서 실행합니다.
3. IntelliJ Tomcat 실행 설정의 VM options에 `-Dspring.profiles.active=local`을 지정합니다.
4. 외부 Tomcat **8.5.95**, JDK 17을 선택하고 `target/orda-api.war`를 배포합니다.
5. FE와 함께 개발할 때 Deployment의 **Application context를 `/`**로 지정합니다.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-db-tunnel.ps1 `
    -KeyPath "<pem 파일 경로>" `
    -ServerHost "<서버 주소>" `
    -SshUser "<SSH 계정>" `
    -RemoteDbPort "<서버 측 DB 포트>"
```

서버 주소·SSH 계정·서버 측 DB 포트는 필수 인자이며 실제 값은 팀 내부에서 공유받습니다.
터널은 로컬 `127.0.0.1:15432`를 SSH 서버의 `127.0.0.1:<서버 측 DB 포트>`에 연결합니다.
터미널을 열어두고, 같은 포트에 터널이 이미 실행 중이면 중복 실행하지 않습니다.
최초 접속 시 `ssh -i "<pem 파일 경로>" "<SSH 계정>@<서버 주소>"`로 접속하고, 표시된 호스트 키 지문을 서버 담당자에게 확인한 뒤 등록합니다.
실제 접속 정보와 개인키는 저장소 밖에서 관리하며 README나 스크립트에 직접 적어 커밋하지 않습니다.

매번 인자를 입력하지 않으려면 개인용 실행 파일을 `%USERPROFILE%\.orda\start-db-tunnel.ps1`에 만들 수 있습니다. 아래 예시를 복사한 뒤 경로와 접속 정보를 본인 환경에 맞게 바꾸세요. 이 파일은 저장소 밖에 두고 공유 저장소에 커밋하지 않습니다.

```powershell
$repoPath = 'C:\path\to\ORDA-BE'

& (Join-Path $repoPath 'scripts\start-db-tunnel.ps1') `
    -KeyPath 'C:\path\to\team-key.pem' `
    -ServerHost 'team-server.example.com' `
    -SshUser 'team-user' `
    -RemoteDbPort 12345
```

`C:\path\to\...`, `team-server.example.com`, `team-user`, `12345`는 예시이므로 팀에서 전달받은 값으로 바꿉니다. 개인용 파일에는 `.pem` 경로만 지정하고 키 내용이나 DB 비밀번호를 넣지 않습니다. 저장한 파일은 다음과 같이 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\.orda\start-db-tunnel.ps1"
```

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

## Docker로 EC2에서 수동 실행

이 구성은 CI/CD 없이 EC2에서 WAR와 Docker 이미지를 직접 빌드해 실행하는 초기 확인용입니다. DB 컨테이너는 만들지 않고, EC2에 설치된 PostgreSQL에 연결합니다.

1. EC2에 Docker Engine과 Docker Compose 플러그인을 설치하고 저장소를 준비합니다.
2. 저장소 루트에서 `bash ./mvnw clean package`를 실행해 `target/orda-api.war`를 만듭니다. Windows에서 `.\mvnw.cmd clean package`로 빌드한다면, 먼저 EC2 저장소 루트에서 `mkdir -p target`을 실행한 뒤 Windows의 `target/orda-api.war`를 EC2의 해당 디렉터리에 복사합니다.
3. `.env.example`을 `.env`로 복사하고 운영 DB URL, 사용자명, 비밀번호를 입력합니다. `.env`는 Git에서 제외됩니다.
4. EC2에서 `docker compose up --build -d`로 이미지를 만들고 컨테이너를 시작합니다.

```bash
cp .env.example .env
# .env의 DB 사용자명과 비밀번호를 실제 값으로 설정한 뒤:
docker compose up --build -d
docker compose logs -f backend
```

WAR는 `orda-api.war` 이름으로 배포되므로 애플리케이션 경로는 `/orda-api`입니다. Compose는 컨테이너의 8080 포트를 EC2의 `127.0.0.1:8080`에만 연결하므로 외부에서 직접 접근할 수 없습니다. 개발 PC에서 확인하려면 SSH 터널을 엽니다.

```powershell
ssh -i "<pem 파일 경로>" -N -L 18080:127.0.0.1:8080 "<SSH 계정>@<EC2 공인 IP>"
```

`<SSH 계정>`은 서버 담당자가 안내한 실제 로그인 계정으로 바꿉니다. 예를 들어 계정이 `ubuntu`이고 개인키가 `C:\keys\orda.pem`, EC2 공인 IP가 `203.0.113.10`이라면 다음과 같이 실행합니다. 아래 경로와 IP는 설명용 예시이므로 실제 값으로 바꿉니다.

```powershell
ssh -i "C:\keys\orda.pem" -N -L 18080:127.0.0.1:8080 "ubuntu@203.0.113.10"
```

터널을 켜 둔 상태에서 `http://localhost:18080/orda-api`로 접속합니다. 이 주소는 Tomcat 배포 확인용이며, 실제 API 경로는 기능 구현 후 확인합니다.

컨테이너에서 `host.docker.internal`은 EC2 호스트를 가리킵니다. EC2의 PostgreSQL이 Docker 브리지 네트워크에서 접속 가능한 주소와 포트로 수신하고, 해당 컨테이너 네트워크를 `pg_hba.conf`에서 허용해야 합니다. DB 포트를 인터넷에 공개하지 않습니다. 이 DB 네트워크 설정은 현재 Compose가 자동 변경하지 않습니다.

```bash
docker compose down
```

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
