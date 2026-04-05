package com.wordonline.admin.controller;

import com.wordonline.admin.entity.deploy.DeployStatusValue;
import com.wordonline.admin.service.DeployStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class DeployStatusAdminController {

    private final DeployStatusService deployStatusService;

    @GetMapping("/admin/deploy-status")
    public String adminDeployStatus(Model model) {
        model.addAttribute("deployStatuses", deployStatusService.findAll());
        model.addAttribute("statusValues", DeployStatusValue.values());
        return "admin-deploy-status";
    }
}
