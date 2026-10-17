# Git Convention

이 문서는 Backend와 Frontend Repository에 같은 내용으로 둔다. 수정할 때는 두 Repository를 함께 고친다.

## 브랜치

- `main`: 배포·시연 가능한 안정 브랜치
- `dev`: 기능 통합 브랜치
- `feature/{domainName}-{detail}`: 기능 개발
- `fix/{domainName}-{detail}`: 버그 수정
- `refactor/{domainName}-{detail}`: 동작 변경 없는 구조 개선

작업 브랜치는 `dev`를 기준으로 만들고 Pull Request도 원칙적으로 `dev`를 대상으로 한다. 초기 뼈대 문서의 `develop`, `feat/` 표기는 이 규칙(`dev`, `feature/`)으로 맞춘다.

하나의 기능이 Frontend와 Backend에 걸치면 두 Repository에서 같은 브랜치 이름을 쓰고, 각 PR 본문에 상대 PR 링크를 적는다.

## 커밋

커밋은 하나의 논리적 변경을 담는다. 메시지 형식은 `type: 작업 내용`이며 허용 타입은 `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `design`, `comment`, `rename`, `remove`, `!HOTFIX`다.

## Pull Request

- 기능 Issue는 `[FEAT] 설명`, 버그 Issue는 `[BUG] 설명`으로 작성한다.
- PR 제목은 `[타입] 제목 (#이슈번호)` 형식으로 쓰고, 관련 이슈·작업 내용·검증 결과·리뷰 포인트·체크리스트를 포함한다.
- 목적, 핵심 변경, 검증 결과, 영향 범위를 작성한다.
- API 계약, SQL, Migration, GIS 계산이 바뀌면 호환성과 배포 영향, 상대 Repository 영향을 명시한다.
- 작성자는 diff를 확인한 뒤 리뷰를 요청한다.
- 최소 한 명의 팀원 리뷰와 리뷰 의견 반영 후 병합한다. CodeRabbit은 사람 리뷰를 대체하지 않는다.
- 리뷰 의견은 반영 여부와 이유를 명확히 답한다.
- 병합 방식(squash 등)은 미확정이다.

## AI 작업 제한

AI는 사용자의 명시적 요청 없이는 커밋하거나 원격에 푸시하지 않는다. 강제 푸시, 이력 재작성, 브랜치 삭제도 임의로 수행하지 않는다. 그 밖의 AI 행동 규칙은 [../AGENTS.md](../AGENTS.md)를 따른다.
