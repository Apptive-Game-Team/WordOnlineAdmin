package com.wordonline.admin.repository.deploy;

import com.wordonline.admin.entity.deploy.DeployStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeployStatusRepository extends JpaRepository<DeployStatus, Long> {
}
