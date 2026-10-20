# Backend Convention

현재 애플리케이션은 Java 17, Spring Boot 2.7.18, eGovFrame 4.1.0, MyBatis 기반이며 외부 Tomcat 8.5.95에 WAR로 배포한다. 의존성과 버전은 `pom.xml`을 기준으로 한다.

## 패키지 구조

기능은 `com.allforland.orda.domain` 아래 도메인별로 배치한다.

```text
com.allforland.orda/
├── common/
│   ├── error/       공통 오류 응답과 예외 처리
│   └── logging/     요청 추적
└── domain/
    └── <domain>/   trail, course, route, analysis
        ├── controller/
        ├── service/
        ├── mapper/
        └── dto/
```

필요한 패키지만 만든다. Service 인터페이스 분리 여부와 구현체 배치는 기존 코드와 복잡도에 따른다. 다른 조가 담당하는 도메인을 바꿀 때는 영향 범위를 먼저 확인한다.

MyBatis XML은 `src/main/resources/sqlmap/<domain>/*.xml`에 두며 `classpath*:sqlmap/**/*.xml`에서 읽는다. namespace는 Mapper 인터페이스의 전체 이름, statement id는 메서드 이름과 맞춘다. 실제 설정과 코드를 먼저 확인한다.

## Controller

- HTTP 매핑과 입력 형식 검증, Service 호출, 응답 변환을 담당한다.
- 요청·응답 DTO를 사용하고 DB 행이나 VO를 외부에 그대로 노출하지 않는다.
- 비즈니스 규칙과 SQL을 Controller에 두지 않는다.
- 상태 코드와 오류 응답은 [api-convention.md](api-convention.md)를 따른다.

## Service

- 도메인 규칙, 여러 Mapper 호출의 조합, 비즈니스 단위 트랜잭션을 담당한다.
- 생성자 주입을 사용하고 순환 의존성을 만들지 않는다.
- 원자성이 필요한 변경 범위를 `@Transactional`로 묶는다.
- 의미 없는 전달 메서드와 불필요한 인터페이스를 만들지 않는다.

## MyBatis

- Mapper interface와 XML을 기본으로 사용한다.
- namespace, statement id, 파라미터와 결과 매핑을 일치시킨다.
- 값은 `#{...}`로 바인딩한다. `${...}` 식별자는 서버 허용 목록으로 제한한다.
- 여러 파라미터 이름은 `@Param`으로 명시한다.
- 동적 SQL은 조건 누락으로 전체 조회·수정·삭제가 발생하지 않도록 한다.
- SQL 규칙은 [database-convention.md](database-convention.md)를 따른다.

## DTO, 검증, 보안

- 요청 DTO와 응답 DTO는 역할이 다르면 분리한다. DTO는 API 모양, VO는 DB 조회 결과를 표현한다.
- 형식 검증과 도메인 검증을 구분한다. 데이터 없음, 빈 목록, `null`, 잘못된 요청을 같은 상태로 처리하지 않는다.
- 인증·인가는 서버에서 검증한다. 인증 방식은 합의 전까지 임의로 도입하지 않는다.
- CORS 허용 출처는 설정으로 관리하고 운영에서 `*`를 사용하지 않는다.
- 비밀번호, 토큰, 개인정보, 원문 요청 본문을 로그에 남기지 않는다. 내부 예외와 SQL을 응답에 노출하지 않는다.

구조 경계는 [architecture.md](architecture.md), 좌표 처리는 [gis-convention.md](gis-convention.md), 검증 범위는 [testing-convention.md](testing-convention.md)를 따른다.
