package com.wordonline.admin.controller;

import com.wordonline.admin.dto.ParameterDto;
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
            @RequestBody ParameterDto dto
    ) {
        parameterService.createParameter(dto.name());
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/{parameterId}")
    public ResponseEntity<String> updateParameter(
            @PathVariable Long parameterId,
            @RequestBody ParameterDto dto
    ) {
        parameterService.updateParameter(parameterId, dto.name());
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/{parameterId}")
    public ResponseEntity<String> deleteParameter(
            @PathVariable Long parameterId
    ) {
        parameterService.deleteParameter(parameterId);
        return ResponseEntity.ok("Successfully Deleted");
    }
}