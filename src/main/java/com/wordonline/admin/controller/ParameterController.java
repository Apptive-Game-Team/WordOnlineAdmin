package com.wordonline.admin.controller;

import com.wordonline.admin.dto.ParameterRequestDto;
import com.wordonline.admin.service.ParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/parameters")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class ParameterController {

    private final ParameterService parameterService;

    @PostMapping
    public ResponseEntity<String> saveParameter(
            @RequestBody ParameterRequestDto dto,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        parameterService.createParameter(dto.name(), isSecondary(db), isSyncRequested(dto.syncSecondary()));
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PostMapping("/by-name")
    public ResponseEntity<String> createParameterByName(
            @RequestBody ParameterRequestDto dto,
            @RequestParam(name = "db") String db
    ) {
        parameterService.createParameterInDatabase(dto.name(), isSecondary(db));
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/by-name/{parameterName}")
    public ResponseEntity<String> updateParameterByName(
            @PathVariable String parameterName,
            @RequestBody ParameterRequestDto dto,
            @RequestParam(name = "db") String db
    ) {
        parameterService.updateParameter(parameterName, dto.name(), isSecondary(db));
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/by-name/{parameterName}")
    public ResponseEntity<String> deleteParameterByName(
            @PathVariable String parameterName,
            @RequestParam(name = "db") String db
    ) {
        parameterService.deleteParameter(parameterName, isSecondary(db));
        return ResponseEntity.ok("Successfully Deleted");
    }

    @PatchMapping("/{parameterId}")
    public ResponseEntity<String> updateParameter(
            @PathVariable Long parameterId,
            @RequestBody ParameterRequestDto dto,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        parameterService.updateParameter(parameterId, dto.name(), isSecondary(db), isSyncRequested(dto.syncSecondary()));
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/{parameterId}")
    public ResponseEntity<String> deleteParameter(
            @PathVariable Long parameterId,
            @RequestParam(name = "syncSecondary", defaultValue = "false") boolean syncSecondary,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        parameterService.deleteParameter(parameterId, isSecondary(db), syncSecondary);
        return ResponseEntity.ok("Successfully Deleted");
    }

    @PostMapping("/sync-to-secondary")
    public ResponseEntity<String> syncToSecondary() {
        return ResponseEntity.ok(parameterService.syncParametersToSecondary().toMessage("Parameters: Deploy -> Dev"));
    }

    @PostMapping("/sync-to-primary")
    public ResponseEntity<String> syncToPrimary() {
        return ResponseEntity.ok(parameterService.syncParametersToPrimary().toMessage("Parameters: Dev -> Deploy"));
    }

    private boolean isSyncRequested(Boolean syncSecondary) {
        return Boolean.TRUE.equals(syncSecondary);
    }

    private boolean isSecondary(String db) {
        return "secondary".equalsIgnoreCase(db);
    }
}
