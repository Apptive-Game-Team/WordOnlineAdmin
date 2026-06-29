package com.wordonline.admin.controller;

import com.wordonline.admin.service.SpreadSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/spread-sheets")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class SpreadSheetApiController {

    private final SpreadSheetService spreadSheetService;

    public record ParameterUpdateDto(Long gameObjectId, String parameterName, Double value) {}

    public record NamedParameterUpdateDto(
            String gameObjectName,
            String parameterName,
            Double value,
            String db
    ) {}

    public record BatchParameterUpdateRequest(
            List<ParameterUpdateDto> updates,
            Boolean syncSecondary
    ) {}

    @PostMapping("/game-objects")
    public ResponseEntity<String> updateGameObjects(
            @RequestBody BatchParameterUpdateRequest request,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        spreadSheetService.batchUpdateParameters(
                request.updates(),
                "secondary".equalsIgnoreCase(db),
                Boolean.TRUE.equals(request.syncSecondary())
        );
        return ResponseEntity.ok("Changes saved successfully!");
    }

    @PostMapping("/game-objects/by-name")
    public ResponseEntity<String> updateGameObjectsByName(
            @RequestBody List<NamedParameterUpdateDto> updates
    ) {
        spreadSheetService.batchUpdateParametersByName(updates);
        return ResponseEntity.ok("Changes saved successfully!");
    }

    @PostMapping("/sync-to-secondary")
    public ResponseEntity<String> syncToSecondary() {
        return ResponseEntity.ok(spreadSheetService.syncToSecondary());
    }

    @PostMapping("/sync-to-primary")
    public ResponseEntity<String> syncToPrimary() {
        return ResponseEntity.ok(spreadSheetService.syncToPrimary());
    }
}
