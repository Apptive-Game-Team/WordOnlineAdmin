package com.wordonline.admin.controller;

import com.wordonline.admin.service.DefaultContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class DefaultContentController {

    private final DefaultContentService defaultContentService;

    @PostMapping("/grant-default-contents")
    public ResponseEntity<String> grantDefaultContents(
            @RequestParam(name = "db", defaultValue = "primary") String db
    ) {
        defaultContentService.grantDefaultContentsToAllUsers("secondary".equalsIgnoreCase(db));
        return ResponseEntity.ok("Successfully granted default adventures and magics to all users");
    }
}
