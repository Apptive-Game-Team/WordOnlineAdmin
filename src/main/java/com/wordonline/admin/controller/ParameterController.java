package com.wordonline.admin.controller;

import com.wordonline.admin.service.ParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/parameters")
@RequiredArgsConstructor
public class ParameterController {

    private final ParameterService parameterService;

    @PostMapping
    public ResponseEntity<String> saveParameter(
            @RequestBody String name
    ) {
        parameterService.createParameter(name);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/{parameterId}")
    public ResponseEntity<String> updateParameter(
            @PathVariable Long parameterId,
            @RequestBody String name
    ) {
        parameterService.updateParameter(parameterId, name);
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
