package com.wordonline.admin.controller;

import com.wordonline.admin.dto.deploy.DeployStatusRequestDto;
import com.wordonline.admin.service.DeployStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/deploy-status")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
@RequiredArgsConstructor
public class DeployStatusController {

    private final DeployStatusService deployStatusService;

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody DeployStatusRequestDto requestDto) {
        Long id = deployStatusService.create(requestDto);
        return ResponseEntity.ok(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody DeployStatusRequestDto requestDto) {
        deployStatusService.update(id, requestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deployStatusService.delete(id);
        return ResponseEntity.ok().build();
    }
}
