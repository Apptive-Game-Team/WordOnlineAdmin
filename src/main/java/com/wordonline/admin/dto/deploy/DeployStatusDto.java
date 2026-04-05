package com.wordonline.admin.dto.deploy;

import com.wordonline.admin.entity.deploy.DeployStatus;
import com.wordonline.admin.entity.deploy.DeployStatusValue;

public record DeployStatusDto(
        Long id,
        String deployType,
        DeployStatusValue status
) {
    public DeployStatusDto(DeployStatus deployStatus) {
        this(deployStatus.getId(), deployStatus.getDeployType(), deployStatus.getStatus());
    }
}
