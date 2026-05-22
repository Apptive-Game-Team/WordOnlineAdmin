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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.admin.dto.quest.QuestRequestDto;
import com.wordonline.admin.service.QuestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/quests")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class QuestController {

    private final QuestService questService;

    @PostMapping
    public ResponseEntity<String> createQuest(
            @RequestBody QuestRequestDto questRequestDto,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        Long questId = questService.createQuest(questRequestDto, isSecondary(db));
        return ResponseEntity.created(
                URI.create(String.format("/api/admin/quests/%d", questId))
        ).build();
    }

    @PutMapping("/{questId}")
    public ResponseEntity<String> updateQuest(
            @PathVariable Long questId,
            @RequestBody QuestRequestDto questRequestDto,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        questService.updateQuest(questId, questRequestDto, isSecondary(db));
        return ResponseEntity.ok("Successfully updated");
    }

    @DeleteMapping("/{questId}")
    public ResponseEntity<String> deleteQuest(
            @PathVariable Long questId,
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        questService.deleteQuest(questId, isSecondary(db));
        return ResponseEntity.ok("Successfully removed");
    }

    @PostMapping("/sync-to-secondary")
    public ResponseEntity<String> syncToSecondary() {
        return ResponseEntity.ok(questService.syncToSecondary().toMessage("Quests: Prod -> Dev"));
    }

    @PostMapping("/sync-to-primary")
    public ResponseEntity<String> syncToPrimary() {
        return ResponseEntity.ok(questService.syncToPrimary().toMessage("Quests: Dev -> Prod"));
    }

    private boolean isSecondary(String db) {
        return "secondary".equalsIgnoreCase(db);
    }
}
