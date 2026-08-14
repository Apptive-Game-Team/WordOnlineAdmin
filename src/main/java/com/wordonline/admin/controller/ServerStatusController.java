package com.wordonline.admin.controller;

import com.wordonline.admin.dto.server.ServerSessionCountDto;
import com.wordonline.admin.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/servers")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
@RequiredArgsConstructor
public class ServerStatusController {

    private final ServerService serverService;

    @GetMapping("/session-counts")
    public ResponseEntity<List<ServerSessionCountDto>> getSessionCounts() {
        return ResponseEntity.ok(serverService.getGameServerSessionCounts());
    }
}
