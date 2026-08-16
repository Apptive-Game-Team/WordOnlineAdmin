package com.wordonline.admin.controller;

import com.wordonline.admin.dto.server.ServerSessionCountDto;
import com.wordonline.admin.dto.server.TargetBotSessionsUpdateDto;
import com.wordonline.admin.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** A body with {@code targetBotSessions} = {@code null} clears the override. */
    @PutMapping("/{id}/target-bot-sessions")
    public ResponseEntity<String> updateTargetBotSessions(
            @PathVariable long id,
            @RequestBody TargetBotSessionsUpdateDto request) {
        if (request.database() == null) {
            return ResponseEntity.badRequest().body("database is required");
        }
        if (request.targetBotSessions() != null && request.targetBotSessions() < 0) {
            return ResponseEntity.badRequest().body("targetBotSessions must not be negative");
        }
        serverService.updateTargetBotSessions(request.database(), id, request.targetBotSessions());
        return ResponseEntity.noContent().build();
    }
}
