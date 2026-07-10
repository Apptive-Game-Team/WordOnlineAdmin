package com.wordonline.admin.dto.deploy;

import com.wordonline.admin.entity.deploy.DeployStatusValue;

public record DeployStatusRequestDto(
        String deployType,
        DeployStatusValue status
) {
}
