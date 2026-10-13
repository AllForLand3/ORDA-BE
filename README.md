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
