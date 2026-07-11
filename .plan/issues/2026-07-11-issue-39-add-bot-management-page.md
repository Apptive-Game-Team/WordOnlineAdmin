# 2026-07-11 — 봇 사용자·페르소나·덱 관리 페이지 추가

- Date: 2026-07-11
- GitHub Issue: #39
- Status: Draft

## Goal

Admin에서 음수 bot user, persona, owned decks/cards를 한 화면 흐름으로 생성·조회·수정·삭제하고 selected deck을 구성한다.

## Non-goals

- Account member 생성
- Game AI 알고리즘 변경
- 실제 사용자 관리 화면 확장

## Context / Constraints

- 생성은 user/persona/초기 deck 전체가 단일 DB transaction이어야 한다.
- 음수 ID 할당은 동시 요청에도 충돌하면 안 된다.
- 이름/AI 값/enabled는 persona, MMR/status/selected deck은 user가 소유한다.
- database #8과 game #288 계약에 의존한다.

## Approach (Checklist)

- [ ] **Step 0: Recon** (Admin datasource/security/controller/template/repository patterns 및 deck/card schema 확인)
- [ ] **Step 1: Persistence** (BotUser/Persona/Deck projections와 repositories, 소유권 검증 query)
- [ ] **Step 2: Service** (ID 할당 호출, create/update/delete transaction, selected deck/card validation)
- [ ] **Step 3: Web** (목록/상세/form/delete-confirm controller와 Thymeleaf template/nav)
- [ ] **Step 4: Validation** (persona 범위, 이름, deck card count/소유권, delete active/enabled 정책)
- [ ] **Step 5: Tests** (service transaction, repository, MVC/security, create/edit/delete/deck flows)
- [ ] **Step 6: Rollout / Rollback** (database #8 → game/lobby → admin 순서, 기능 노출 전 schema readiness)

## Validation

- **Commands to run:** `./gradlew test`; `./gradlew clean build`; 브라우저에서 CRUD/deck form 확인
- **Expected output:** 반복 submit/동시 create에도 중복 bot 없음; 모든 수정이 올바른 소유 row에만 반영; 실패 시 전체 rollback

## Risks & Rollback

- **Risks:** user-selected_deck/deck-user 순환 참조로 삭제 순서 오류; 실행 중 bot 삭제; secondary DB 비교 기능과 repository wiring 혼동; 카드 수량 규칙 미확정
- **Rollback steps:** Admin app 이전 버전 배포. 생성된 bot 데이터는 자동 삭제하지 않고 검증된 관리 절차로 비활성화 후 제거.

## Open Questions

- 삭제는 hard delete보다 `enabled=false` 우선 후 별도 영구 삭제가 안전한가? 권장: 기본 UI는 비활성화, 명시적 확인 후 hard delete.
- deck 유효성 규칙(카드 총량/중복/보유 카드 제한)을 기존 lobby 규칙과 동일하게 적용할지 확인 필요.
