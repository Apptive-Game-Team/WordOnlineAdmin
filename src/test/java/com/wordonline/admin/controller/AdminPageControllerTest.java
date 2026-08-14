package com.wordonline.admin.controller;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.dto.server.ServerDto;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import com.wordonline.admin.repository.tag.TagRepository;
import com.wordonline.admin.service.ParameterService;
import com.wordonline.admin.service.ServerService;
import com.wordonline.admin.service.SpreadSheetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServerService serverService;
    @MockitoBean
    private ParameterService parameterService;
    @MockitoBean
    private SpreadSheetService spreadSheetService;
    @MockitoBean
    private GameObjectRepository gameObjectRepository;
    @MockitoBean
    private ParameterRepository parameterRepository;
    @MockitoBean
    private TagRepository tagRepository;
    @MockitoBean
    private GameServerClient gameServerClient;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void indexRendersASessionBadgeAndThePollingScriptForGameServers() throws Exception {
        when(serverService.getPrimaryServers()).thenReturn(List.of(
                new ServerDto(1L, "http", "game", 8080, ServerType.GAME, ServerState.ACTIVE),
                new ServerDto(2L, "http", "lobby", 8080, ServerType.LOBBY, ServerState.ACTIVE)
        ));
        when(serverService.getSecondaryServers()).thenReturn(List.of());
        when(parameterService.hasSecondaryDatabase()).thenReturn(false);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-server-id=\"1\" data-server-database=\"PRIMARY\"")))
                // The layout dialect only keeps markup inside layout:fragment, so the poller must live there.
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/admin/servers/session-counts")));
    }

    @Test
    void indexRendersTheGrantDefaultsScript() throws Exception {
        when(serverService.getPrimaryServers()).thenReturn(List.of());
        when(serverService.getSecondaryServers()).thenReturn(List.of());
        when(parameterService.hasSecondaryDatabase()).thenReturn(false);

        // Scripts outside layout:fragment are dropped by the layout dialect, which left the
        // Grant Defaults buttons without their click handlers.
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "btn-grant-default-contents-primary')")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/admin/grant-default-contents")));
    }

    @Test
    void indexRendersDevServersAndDefaultsToActiveFilter() throws Exception {
        when(serverService.getPrimaryServers()).thenReturn(List.of());
        when(serverService.getSecondaryServers()).thenReturn(List.of(
                new ServerDto(1L, "https", "dev-game", 8443, ServerType.GAME, ServerState.INACTIVE)
        ));
        when(parameterService.hasSecondaryDatabase()).thenReturn(true);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dev (Dev DB)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-server-id=\"1\" data-server-database=\"SECONDARY\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<option value=\"ACTIVE\" selected>ACTIVE only</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "selectedState === 'ALL' || column.dataset.serverState === selectedState")));
    }
}
