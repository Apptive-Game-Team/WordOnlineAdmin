# 2026-08-16 — 성능 페이지에 Frame 외 시스템 합계 추세 추가

- Date: 2026-08-16
- GitHub Issue: #82
- Status: Draft

## Goal

`/admin/statistics/performance`에서 개별 `GameSystem` 이름뿐 아니라, **`Frame`을 제외한 모든 이름을
합친 값**의 분포와 추세를 볼 수 있게 한다. 합계는 한 프레임에서 시스템들이 통틀어 쓴 CPU 시간이며,
50ms 예산 대비 여유가 얼마나 남았는지를 직접 보여준다.

## Non-goals

- 게임 서버 쪽 변경 없음. `FrameIntervalAspect`(게임 서버 #384)를 `deploy`로 승격하는 일은 이 작업과
  분리된 운영 판단이다.
- 게임 상세 페이지(`/games/{id}`)는 이번 범위 밖. 한 게임 안의 합계는 이미 표의 값들을 눈으로 더하면
  된다.
- 알림/임계값 같은 회귀 감지 자동화는 범위 밖.

## Context / Constraints

- 데이터 원본은 `statistic_update_time` (게임당 이름당 한 행: `min/max/mean_interval_ns`).
- `Frame`은 프레임 **간격**이라 루프가 남는 예산을 자는 동안 50ms에 붙박여 있다
  (`GameLoop.runLoop`). 그래서 기존 추세는 median이 아니라 **p95** 비교다. 합계 행도 같은 규칙을
  따라야 한 표 안에서 비교가 성립한다.
- 게임별 합계 = `SUM(mean_interval_ns) WHERE name <> 'Frame'`. 각 이름의 mean은 서로 다른(그리고
  기록되지 않은) 프레임 수의 평균이지만, `GameSystem.update`는 프레임마다 한 번씩 돌므로 같은 게임
  안에서는 표본 수가 사실상 같다. 그래서 "평균들의 합"을 "프레임당 합계의 평균"으로 읽어도 된다.
- min/max는 이름별 **단일 프레임** 값이다. 이것들을 더한 값은 실제로 관측된 적 없는 상한/하한이므로
  합계 행에서는 표시하지 않는다 → `SystemTimingDto`의 min/max를 nullable로 바꾼다.
- 조회는 `JdbcTemplate` 네이티브 SQL. 보조 데이터베이스(`DEV_DATABASE_URL`)도 같은 코드로 돈다.

## Approach (Checklist)

- [ ] **Step 0: Recon**
  - `StatisticUpdateTimeRepository`(FIND_SYSTEM_TIMINGS, findTimeSeries),
    `StatisticPerformanceService`(selectName/frameTiming), `StatisticPerformanceController`,
    `admin-statistics-performance.html`, 기존 테스트 3종.
- [ ] **Step 1: Implementation**
  - `SystemTimingDto`: `minIntervalNs`/`maxIntervalNs`를 `Long`으로. `hasIntervalBounds()` 추가.
  - `StatisticUpdateTimeRepository`
    - `COMBINED_SYSTEMS_NAME` 상수 추가(표시용 이름, 실제 `name` 값과 충돌하지 않아야 한다).
    - `findCombinedSystemTiming(...)`: 게임별 합계 CTE → median/p95/전·후반 p95/게임 수.
    - `findTimeSeries(...)`: 합계 이름이면 합계 CTE 기반 시계열로 분기.
  - `StatisticPerformanceService`: 합계 행을 이름별 목록 뒤에 붙이고 p95 내림차순 정렬 유지.
    `selectName`은 `Frame` 기본값 유지, 없으면 합계, 그다음 첫 행.
  - 템플릿: min/max 없는 행은 `-`, 합계 행 설명 문구, Plot 링크 동작.
- [ ] **Step 2: Tests**
  - 서비스: 합계 행 병합/정렬, `selectName` 폴백 순서.
  - 렌더: 합계 행이 표와 차트 데이터에 나오고 min/max 자리가 `-`인지, 추세 배지가 붙는지.
  - 컨트롤러: `name=<합계 이름>`으로 요청했을 때 시계열이 합계로 조회되는지.
- [ ] **Step 3: Rollout / Rollback**
  - 마이그레이션 없음. 조회 전용. 배포는 기존 admin 파이프라인.

## Validation

- **Commands to run:**
  - `./gradlew test`
  - `./gradlew clean build`
- **Expected output:** 전부 통과. 신규 테스트가 합계 행/시계열/`-` 표시를 덮는다.
- **Not verified:** 실제 운영 데이터 대상 화면 확인. 로컬에 DB 접속 정보가 없다.

## Risks & Rollback

- **Risks:**
  - 합계 SQL이 게임 수가 많을 때 느려질 수 있다. 기존 집계와 같은 조인/필터를 쓰되 게임 단위로 한 번
    더 그룹핑하므로 비용이 는다.
  - min/max nullable 전환이 기존 표/차트 표시를 건드린다. 렌더 테스트로 막는다.
  - 합계 이름이 언젠가 실제 측정 이름과 겹치면 시계열 분기가 잘못 탄다. 겹치지 않는 라벨을 고른다.
- **Rollback steps:** `git revert` 한 커밋. 스키마 변경이 없어 되돌리기가 자유롭다.

## Open Questions

- 합계에서 `Frame`만 빼면 되는가? 현재 `Frame`이 유일한 비-시스템 이름이라 그렇게 둔다. 게임 서버가
  다른 종류의 이름을 기록하기 시작하면 제외 목록을 다시 봐야 한다.
