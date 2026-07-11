# 2026-07-11 — 봇 사용자·페르소나·덱 관리 페이지 추가

- Date: 2026-07-11
- GitHub Issue: #39
- Status: Draft

## Goal

Admin에서 Deploy(primary)와 Dev(secondary)의 음수 bot user, persona, owned decks/cards를 나란히 조회하고 각 DB 또는 양쪽을 생성·수정·삭제하며 selected deck을 구성·동기화한다.

## Non-goals

- Account member 생성
- Game AI 알고리즘 변경
- 실제 사용자 관리 화면 확장

## Context / Constraints

- 생성은 user/persona/초기 deck 전체가 단일 DB transaction이어야 한다.
- 음수 ID 할당은 동시 요청에도 충돌하면 안 된다.
- 이름/AI 값/enabled는 persona, MMR/status/selected deck은 user가 소유한다.
- database #8과 game #288 계약에 의존한다.
- Secondary DB는 optional이며 설정되지 않으면 Deploy 전용 기능이 유지되어야 한다.
- DB별 surrogate deck ID는 다를 수 있으므로 동기화는 bot user ID와 deck/card snapshot을 사용한다.
- Deploy와 Dev를 함께 수정하는 작업은 분산 transaction이 아니므로 부분 실패를 명시적으로 보고해야 한다.

## Approach (Checklist)

- [ ] **Step 0: Recon** (Admin datasource/security/controller/template/repository patterns 및 deck/card schema 확인)
- [ ] **Step 1: Persistence** (BotUser/Persona/Deck projections와 repositories, 소유권 검증 query)
- [ ] **Step 2: Service** (ID 할당 호출, create/update/delete transaction, selected deck/card validation)
- [ ] **Step 3: Web** (목록/상세/form/delete-confirm controller와 Thymeleaf template/nav)
- [ ] **Step 4: Validation** (persona 범위, 이름, deck card count/소유권, delete active/enabled 정책)
- [ ] **Step 5: Tests** (service transaction, repository, MVC/security, create/edit/delete/deck flows)
- [ ] **Step 6: Deploy / Dev 비교** (user ID 기준 side-by-side projection, DB별 present 상태와 값 표시)
- [ ] **Step 7: DB별/동시 수정** (`db=primary|secondary|both`, optional secondary 처리, 부분 실패 보고)
- [ ] **Step 8: 양방향 동기화** (Deploy → Dev / Dev → Deploy snapshot upsert, deck ID 비의존)
- [ ] **Step 9: Rollout / Rollback** (database #8 → game/lobby → admin 순서, 기능 노출 전 양쪽 schema readiness)

## Validation

- **Commands to run:** `./gradlew test`; `./gradlew clean build`; 브라우저에서 Deploy/Dev 비교, DB별/동시 CRUD, deck form, 양방향 sync 확인
- **Expected output:** 각 DB 값이 독립 표시·수정됨; `both`가 양쪽에 반영됨; sync 후 bot/persona/deck/card snapshot이 자연키 기준 일치함

## Risks & Rollback

- **Risks:** user-selected_deck/deck-user 순환 참조로 삭제 순서 오류; 실행 중 bot 삭제; secondary DB schema 미적용; 양쪽 동시 수정의 부분 실패; DB별 deck ID를 잘못 동일시할 위험
- **Rollback steps:** Admin app 이전 버전 배포. 생성된 bot 데이터는 자동 삭제하지 않고 검증된 관리 절차로 비활성화 후 제거.

## Open Questions

- 삭제는 hard delete보다 `enabled=false` 우선 후 별도 영구 삭제가 안전한가? 권장: 기본 UI는 비활성화, 명시적 확인 후 hard delete.
- deck 유효성 규칙(카드 총량/중복/보유 카드 제한)을 기존 lobby 규칙과 동일하게 적용할지 확인 필요.
