package com.wordonline.admin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.admin.dto.tag.TagDto;
import com.wordonline.admin.dto.tag.TagRequestDto;
import com.wordonline.admin.service.TagService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tags")
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<String> createTag(
            @RequestBody TagRequestDto tagRequestDto
    ) {
        long tagId = tagService.addTag(tagRequestDto);
        return ResponseEntity.created(
                URI.create(String.format("/api/admin/tags/%d", tagId))
        ).build();
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<String> putTag(
            @PathVariable long tagId,
            @RequestBody TagRequestDto tagRequestDto
    ) {
        tagService.putTag(new TagDto(tagId, tagRequestDto.name()));
        return ResponseEntity.ok(
                "Successfully updated"
        );
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<String> deleteTag(
            @PathVariable long tagId
    ) {
        tagService.removeTag(tagId);
        return ResponseEntity.ok(
                "Successfully removed"
        );
    }
}
