# ORDA API

ORDA 서비스의 백엔드 API 프로젝트입니다. Maven으로 빌드하고 외부 Tomcat에 WAR 파일로 배포합니다.

## 기술 구성

- Java 17
- Spring Boot 2.7.18
- eGovFrame 4.1.0
- Tomcat 8.5.95
- MyBatis (MyBatis Spring Boot Starter 2.3.2)

애플리케이션 패키지는 `com.allforland.orda`이며, 프로젝트와 WAR 이름은 `orda-api`입니다.

## 빌드

Java 17과 Maven을 설치한 뒤 프로젝트 루트에서 실행합니다.

```bash
mvn clean package
```

빌드 결과물은 `target/orda-api.war`입니다.

## 배포

외부 Tomcat 8.5.95에서 WAR로 실행합니다. `target/orda-api.war`를 Tomcat의 `webapps/` 디렉터리에 복사한 뒤 Tomcat을 시작하거나 재시작합니다.

기본 컨텍스트 경로는 `/orda-api`입니다. 데이터베이스 연결 정보 등 실행 환경에 필요한 설정은 배포 환경에서 별도로 구성합니다.
## AWS DB 로컬 개발 설정

`application.yml`은 사용자 폴더의 `.orda/application-local.yml`을 자동으로 읽습니다.
Windows에서는 `C:\Users\사용자명\.orda\application-local.yml`입니다.
외부 Tomcat으로 실행할 때도 동일하며 IntelliJ 환경 변수나 프로필 선택은 필요하지 않습니다.
기존 `SPRING_DATASOURCE_*` 환경 변수가 있다면 파일보다 우선하므로 파일 설정을 사용하려면 삭제합니다.

처음 한 번 `application-local.yml.example`을 위 위치에 복사하고 비밀번호를 입력하세요.
실제 설정 파일은 저장소와 WAR 밖에 보관하며 `src/main/resources`에 넣지 않습니다.
예시 파일은 커밋하고 실제 설정 파일과 SSH 키는 커밋하지 않습니다.

AWS DB는 서버의 `127.0.0.1:55432`에 연결되어 있습니다.
Tomcat 실행 전에 별도 터미널에서 SSH 터널을 엽니다.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-db-tunnel.ps1 -KeyPath "<pem 파일 경로>"
```

터널을 실행한 터미널은 열어둡니다. 로컬 포트 15432에 터널이 이미 실행 중이면 중복 실행하지 않습니다.
처음 접속하는 서버라면 `ssh -i "<pem 파일 경로>" ubuntu@43.203.221.58`로 먼저 접속하여
서버 호스트 키를 확인한 뒤 등록합니다.

그다음 `mvn clean package`로 빌드하고 IntelliJ Tomcat에서 `target/orda-api.war`를 다시 배포합니다.
컴퓨터를 재시작하면 SSH 터널도 다시 열어야 합니다. DB 테이블 자동 생성은 비활성화되어 있습니다.
애플리케이션 시작만으로 DB 인증까지 검증되는 것은 아니며 실제 DB 조회로 확인해야 합니다.
## AWS DB 로컬 개발 설정

`application.yml`은 사용자 폴더의 `.orda/application-local.yml`을 자동으로 읽습니다.
Windows에서는 `C:\Users\사용자명\.orda\application-local.yml`입니다.
외부 Tomcat으로 실행할 때도 동일하며 IntelliJ 환경 변수나 프로필 선택은 필요하지 않습니다.
기존 `SPRING_DATASOURCE_*` 환경 변수가 있다면 파일보다 우선하므로 파일 설정을 사용하려면 삭제합니다.

처음 한 번 `application-local.yml.example`을 위 위치에 복사하고 비밀번호를 입력하세요.
실제 설정 파일은 저장소와 WAR 밖에 보관하며 `src/main/resources`에 넣지 않습니다.
예시 파일은 커밋하고 실제 설정 파일과 SSH 키는 커밋하지 않습니다.

AWS DB는 서버의 `127.0.0.1:55432`에 연결되어 있습니다.
Tomcat 실행 전에 별도 터미널에서 SSH 터널을 엽니다.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-db-tunnel.ps1 -KeyPath "<pem 파일 경로>"
```

터널을 실행한 터미널은 열어둡니다. 로컬 포트 15432에 터널이 이미 실행 중이면 중복 실행하지 않습니다.
처음 접속하는 서버라면 `ssh -i "<pem 파일 경로>" ubuntu@43.203.221.58`로 먼저 접속하여
서버 호스트 키를 확인한 뒤 등록합니다.

그다음 `mvn clean package`로 빌드하고 IntelliJ Tomcat에서 `target/orda-api.war`를 다시 배포합니다.
컴퓨터를 재시작하면 SSH 터널도 다시 열어야 합니다. DB 테이블 자동 생성은 비활성화되어 있습니다.
애플리케이션 시작만으로 DB 인증까지 검증되는 것은 아니며 실제 DB 조회로 확인해야 합니다.

## 코드 포맷 및 린트

Java 코드는 공백 4칸을 사용합니다. Spotless의 google-java-format AOSP 스타일로
들여쓰기, 공백, import, 줄바꿈을 정리합니다. 줄바꿈은 LF, 인코딩은 UTF-8입니다.
