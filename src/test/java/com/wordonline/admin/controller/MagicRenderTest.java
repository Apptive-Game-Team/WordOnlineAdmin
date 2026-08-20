package com.wordonline.admin.controller;

import com.wordonline.admin.config.WebSecurityConfig;
import com.wordonline.admin.dto.MagicComparisonDto;
import com.wordonline.admin.security.JwtAuthenticationFilter;
import com.wordonline.admin.service.MagicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 템플릿을 실제로 렌더한다. Thymeleaf 표현식 오류는 뷰 이름만 보는 테스트를 그대로 통과한다.
 */
@WebMvcTest(controllers = MagicController.class)
@Import({WebSecurityConfig.class, JwtAuthenticationFilter.class})
class MagicRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MagicService magicService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(authorities = "WORDONLINE_ADMIN")
    void pageOffersTheFiveCastTypesAndShowsBothValuesPerDatabase() throws Exception {
        when(magicService.hasSecondaryDatabase()).thenReturn(true);
        when(magicService.getCardNames(anyBoolean())).thenReturn(List.of("fire-card"));
        when(magicService.getMagicComparisons()).thenReturn(List.of(new MagicComparisonDto(
                "fireball", true, true,
                "shoot", "explode",
                "DEFAULT", "DEFAULT",
                List.of()
        )));

        String html = mockMvc.perform(get("/admin/magic"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("name=\"castType\"", "name=\"accessType\"");
        assertThat(html).contains("spawn", "drop", "explode", "build", "shoot");
        // 배포/개발의 cast_type이 다르면 그것이 보여야 한다.
        assertThat(html).contains("cast: shoot", "cast: explode", "Deploy / Dev 값이 다르다");
        assertThat(html).contains("access: DEFAULT");
        assertThat(markup(html)).doesNotContain("${", "th:text");
    }

    @Test
    @WithMockUser(authorities = "WORDONLINE_ADMIN")
    void aRejectedCastTypeComesBackAsAFlashMessageInsteadOfAnErrorPage() throws Exception {
        doThrow(new IllegalArgumentException(
                "Magic cast type must be one of spawn, drop, explode, build, shoot; got: summon"))
                .when(magicService).createMagic(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(post("/admin/magic/by-name/create")
                        .param("name", "fireball")
                        .param("castType", "summon")
                        .param("accessType", "DEFAULT")
                        .param("db", "primary")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/magic"))
                .andExpect(flash().attribute("error",
                        "Magic cast type must be one of spawn, drop, explode, build, shoot; got: summon"));
    }

    @Test
    @WithMockUser(authorities = "PLAYER")
    void anOrdinaryUserCannotReachTheMagicPage() throws Exception {
        mockMvc.perform(get("/admin/magic")).andExpect(status().is4xxClientError());
    }

    /** 렌더되지 않은 Thymeleaf 표현식을 찾기 위해 페이지 스크립트 앞까지만 본다. */
    private String markup(String html) {
        return html.substring(0, html.indexOf("<script"));
    }
}
