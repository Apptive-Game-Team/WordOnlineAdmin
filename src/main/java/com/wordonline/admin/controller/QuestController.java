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
            @RequestBody QuestRequestDto questRequestDto
    ) {
        Long questId = questService.createQuest(questRequestDto);
        return ResponseEntity.created(
                URI.create(String.format("/api/admin/quests/%d", questId))
        ).build();
    }

    @PutMapping("/{questId}")
    public ResponseEntity<String> updateQuest(
            @PathVariable Long questId,
            @RequestBody QuestRequestDto questRequestDto
    ) {
        questService.updateQuest(questId, questRequestDto);
        return ResponseEntity.ok("Successfully updated");
    }

    @DeleteMapping("/{questId}")
    public ResponseEntity<String> deleteQuest(
            @PathVariable Long questId
    ) {
        questService.deleteQuest(questId);
        return ResponseEntity.ok("Successfully removed");
    }
}
