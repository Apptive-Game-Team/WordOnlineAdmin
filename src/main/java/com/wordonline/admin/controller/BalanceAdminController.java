package com.wordonline.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wordonline.admin.service.BalanceProposalService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/balance/proposals")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class BalanceAdminController {

    private final BalanceProposalService balanceProposalService;

    @GetMapping
    public String proposals(@RequestParam(required = false) Long proposalId, Model model) {
        var proposals = balanceProposalService.getAllProposals();
        model.addAttribute("proposals", proposals);
        if (proposalId != null) {
            model.addAttribute("selectedProposal", balanceProposalService.getProposal(proposalId));
        } else if (!proposals.isEmpty()) {
            model.addAttribute("selectedProposal", proposals.getFirst());
        }
        return "admin-balance-proposals";
    }
}
