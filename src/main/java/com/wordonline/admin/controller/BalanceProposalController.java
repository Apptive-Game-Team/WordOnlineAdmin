package com.wordonline.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.admin.dto.balance.BalanceRejectRequestDto;
import com.wordonline.admin.entity.balance.BalanceProposal;
import com.wordonline.admin.service.BalanceProposalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/balance/proposals")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class BalanceProposalController {

    private final BalanceProposalService balanceProposalService;

    @PostMapping("/generate")
    public ResponseEntity<BalanceProposal> generate() {
        return ResponseEntity.ok(balanceProposalService.generateProposal());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BalanceProposal> getProposal(@PathVariable Long id) {
        return ResponseEntity.ok(balanceProposalService.getProposal(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<BalanceProposal> approve(@PathVariable Long id) {
        return ResponseEntity.ok(balanceProposalService.approve(id));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<BalanceProposal> apply(@PathVariable Long id) {
        return ResponseEntity.ok(balanceProposalService.apply(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<BalanceProposal> reject(@PathVariable Long id, @RequestBody(required = false) BalanceRejectRequestDto requestDto) {
        String reason = requestDto == null ? "" : requestDto.reason();
        return ResponseEntity.ok(balanceProposalService.reject(id, reason));
    }
}
