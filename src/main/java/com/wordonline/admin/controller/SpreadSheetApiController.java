package com.wordonline.admin.controller;

import com.wordonline.admin.service.SpreadSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/spread-sheets")
public class SpreadSheetApiController {

    private final SpreadSheetService spreadSheetService;

    public record ParameterUpdateDto(Long gameObjectId, String parameterName, Double value) {}

    @PostMapping("/game-objects")
    public ResponseEntity<String> updateGameObjects(
            @RequestBody List<ParameterUpdateDto> updates
    ) {
        spreadSheetService.batchUpdateParameters(updates);
        return ResponseEntity.ok("Changes saved successfully!");
    }
}
