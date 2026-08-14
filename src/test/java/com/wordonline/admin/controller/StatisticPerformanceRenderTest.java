package com.wordonline.admin.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wordonline.admin.config.WebSecurityConfig;
import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.GameTimingDetailDto;
import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.TimeSeriesPointDto;
import com.wordonline.admin.security.JwtAuthenticationFilter;
import com.wordonline.admin.service.StatisticPerformanceService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 템플릿을 실제로 렌더한다. 렌더 시점에 터지는 Thymeleaf 표현식도 컴파일은 되고 뷰 이름만 확인하는
 * 테스트는 통과하므로, 여기서는 만들어진 HTML을 직접 확인한다.
 * <p>
 * 권한 검사도 함께 본다. 통계 화면이 권한 없는 사용자에게 조용히 열리는 것이 가장 그럴듯한 실패
 * 모드다.
 */
@WebMvcTest(controllers = StatisticPerformanceController.class)
@Import({WebSecurityConfig.class, JwtAuthenticationFilter.class})
class StatisticPerformanceRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticPerformanceService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final SystemTimingDto frame =
            new SystemTimingDto("Frame", 41_000_000L, 96_000_000L, 51_200_000.0, 63_400_000.0, 42);
    private final SystemTimingDto physics =
            new SystemTimingDto("PhysicSystem", 400_000L, 5_100_000L, 1_200_000.0, 2_400_000.0, 42);

    private void stubPopulated() {
        when(service.findSystemTimings(any(), any())).thenReturn(List.of(frame, physics));
        when(service.selectName(any(), any())).thenReturn("Frame");
        when(service.frameTiming(any())).thenReturn(frame);
        when(service.findTimeSeries(any(), any(), any())).thenReturn(List.of(
                new TimeSeriesPointDto(1L, LocalDateTime.of(2026, 8, 1, 10, 0), 47_000_000.0),
                new TimeSeriesPointDto(2L, LocalDateTime.of(2026, 8, 2, 10, 0), 62_900_000.0)));
        when(service.findRecentGames(any(), any(), anyInt(), anyInt())).thenReturn(List.of(
                new GameFrameSummaryDto(2L, LocalDateTime.of(2026, 8, 2, 10, 0), "PVP", 302L,
                        62_900_000.0, 96_000_000L),
                // 프레임 행이 없는 게임도 목록에 남아야 한다.
                new GameFrameSummaryDto(3L, LocalDateTime.of(2026, 8, 1, 10, 0), "Practice", 60L,
                        null, null)));
        when(service.countRecentGames(any(), any())).thenReturn(45L);
        when(service.totalPages(anyLong(), anyInt())).thenReturn(3);
    }

    private String render(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @WithMockUser(authorities = "WORDONLINE_ADMIN")
    void performancePageRendersFiltersSummaryChartsAndTable() throws Exception {
        stubPopulated();

        String html = render("/admin/statistics/performance?gameType=PVP&days=7");

        // 레이아웃 데코레이터와 nav 프래그먼트가 실제로 적용됐는지.
        assertThat(html).contains("Word Online Admin", "/admin/statistics/performance");
        // 51.2ms와 63.4ms는 예산 50ms를 넘으므로 빨갛게 표시된다.
        assertThat(html).contains("51.20 ms", "63.40 ms", "text-danger");
        // 느린 순 정렬이므로 Frame이 PhysicSystem보다 앞에 온다.
        assertThat(html.indexOf(">Frame<")).isLessThan(html.indexOf(">PhysicSystem<"));
        assertThat(html).contains("id=\"timingChart\"", "id=\"seriesChart\"");
        // 차트 데이터는 밀리초로 변환되어 페이지에 박혀 있다.
        assertThat(html).contains("\"median\":51.2", "\"p95\":63.4");
        // 최근 게임 표와 상세 링크, 그리고 프레임 데이터가 없는 게임의 표시.
        assertThat(html).contains("/admin/statistics/performance/games/2");
        assertThat(html).contains("Page 1 of 3");
        // 렌더되지 않은 Thymeleaf 표현식이 출력에 남지 않았는지.
        assertThat(html).doesNotContain("${", "th:text");
    }

    @Test
    @WithMockUser(authorities = "WORDONLINE_ADMIN")
    void performancePageRendersWithNoDataAtAll() throws Exception {
        when(service.findSystemTimings(any(), any())).thenReturn(List.of());
        when(service.selectName(any(), any())).thenReturn(null);
        when(service.findRecentGames(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(service.countRecentGames(any(), any())).thenReturn(0L);
        when(service.totalPages(anyLong(), anyInt())).thenReturn(0);

        String html = render("/admin/statistics/performance");

        assertThat(html).contains("기록된 타이밍 통계가 없다.", "해당 기간에 게임이 없다.");
        assertThat(html).doesNotContain("${");
    }

    @Test
    @WithMockUser(authorities = "WORDONLINE_ADMIN")
    void gameDetailPageRendersEveryMeasuredName() throws Exception {
        when(service.findGame(anyLong())).thenReturn(Optional.of(new GameFrameSummaryDto(
                2L, LocalDateTime.of(2026, 8, 2, 10, 0), "PVP", 302L, 62_900_000.0, 96_000_000L)));
        when(service.findGameTimings(anyLong())).thenReturn(List.of(
                new GameTimingDetailDto("Frame", 41_000_000L, 96_000_000L, 62_900_000.0),
                new GameTimingDetailDto("PhysicSystem", 400_000L, 5_100_000L, 1_200_000.0)));

        String html = render("/admin/statistics/performance/games/2");

        assertThat(html).contains("Frame", "PhysicSystem", "62.90 ms", "302 s");
        assertThat(html).doesNotContain("${");
    }

    @Test
    @WithMockUser(authorities = "WORDONLINE_ADMIN")
    void gameDetailPageRendersAMessageForAnUnknownGame() throws Exception {
        when(service.findGame(anyLong())).thenReturn(Optional.empty());

        String html = render("/admin/statistics/performance/games/404");

        assertThat(html).contains("Game not found");
        assertThat(html).doesNotContain("${");
    }

    @Test
    @WithMockUser(authorities = "PLAYER")
    void anOrdinaryUserCannotReachThePerformancePages() throws Exception {
        mockMvc.perform(get("/admin/statistics/performance")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/admin/statistics/performance/games/1")).andExpect(status().is4xxClientError());
    }

    @Test
    @WithAnonymousUser
    void unauthenticatedCallersAreSentToLogin() throws Exception {
        mockMvc.perform(get("/admin/statistics/performance")).andExpect(status().is3xxRedirection());
    }
}
