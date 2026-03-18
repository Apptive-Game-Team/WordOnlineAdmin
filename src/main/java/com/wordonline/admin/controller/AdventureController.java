package com.wordonline.admin.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.admin.dto.adventure.AdventureRequestDto;
import com.wordonline.admin.dto.adventure.BulkCreateScenariosRequestDto;
import com.wordonline.admin.dto.adventure.BulkCreateStagesRequestDto;
import com.wordonline.admin.service.AdventureService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class AdventureController {

    private final AdventureService adventureService;

    @PostMapping("/adventures")
    public ResponseEntity<String> createAdventure(
            @RequestBody AdventureRequestDto requestDto
    ) {
        Long adventureId = adventureService.createAdventure(requestDto);
        return ResponseEntity.created(
                URI.create(String.format("/api/admin/adventures/%d", adventureId))
        ).build();
    }

    @PutMapping("/adventures/{adventureId}")
    public ResponseEntity<String> updateAdventure(
            @PathVariable Long adventureId,
            @RequestBody AdventureRequestDto requestDto
    ) {
        adventureService.updateAdventure(adventureId, requestDto);
        return ResponseEntity.ok("Successfully updated");
    }

    @DeleteMapping("/adventures/{adventureId}")
    public ResponseEntity<String> deleteAdventure(
            @PathVariable Long adventureId
    ) {
        adventureService.deleteAdventure(adventureId);
        return ResponseEntity.ok("Successfully removed");
    }

    @PostMapping("/adventures/{adventureId}/stages")
    public ResponseEntity<String> createStage(
            @PathVariable Long adventureId
    ) {
        Long stageId = adventureService.createStage(adventureId);
        return ResponseEntity.created(
                URI.create(String.format("/api/admin/stages/%d", stageId))
        ).build();
    }

    @DeleteMapping("/stages/{stageId}")
    public ResponseEntity<String> deleteStage(
            @PathVariable Long stageId
    ) {
        adventureService.deleteStage(stageId);
        return ResponseEntity.ok("Successfully removed");
    }

    @PostMapping("/stages/{stageId}/scenarios")
    public ResponseEntity<String> createScenario(
            @PathVariable Long stageId
    ) {
        Long scenarioId = adventureService.createScenario(stageId);
        return ResponseEntity.created(
                URI.create(String.format("/api/admin/scenarios/%d", scenarioId))
        ).build();
    }

    @DeleteMapping("/scenarios/{scenarioId}")
    public ResponseEntity<String> deleteScenario(
            @PathVariable Long scenarioId
    ) {
        adventureService.deleteScenario(scenarioId);
        return ResponseEntity.ok("Successfully removed");
    }

    @PostMapping("/stages/{stageId}/scenarios/bulk")
    public ResponseEntity<String> bulkCreateScenarios(
            @PathVariable Long stageId,
            @RequestBody BulkCreateScenariosRequestDto requestDto
    ) {
        adventureService.bulkCreateScenarios(stageId, requestDto.count());
        return ResponseEntity.ok("Successfully created");
    }

    @PostMapping("/adventures/{adventureId}/stages/bulk")
    public ResponseEntity<String> bulkCreateStages(
            @PathVariable Long adventureId,
            @RequestBody BulkCreateStagesRequestDto requestDto
    ) {
        adventureService.bulkCreateStages(adventureId, requestDto.stageCount(), requestDto.scenarioCount());
        return ResponseEntity.ok("Successfully created");
    }
}
