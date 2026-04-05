package com.wordonline.admin.service;

import com.wordonline.admin.dto.deploy.DeployStatusDto;
import com.wordonline.admin.dto.deploy.DeployStatusRequestDto;
import com.wordonline.admin.entity.deploy.DeployStatus;
import com.wordonline.admin.repository.deploy.DeployStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeployStatusService {

    private final DeployStatusRepository deployStatusRepository;

    @Transactional(readOnly = true)
    public List<DeployStatusDto> findAll() {
        return deployStatusRepository.findAll().stream()
                .map(DeployStatusDto::new)
                .collect(Collectors.toList());
    }

    public Long create(DeployStatusRequestDto requestDto) {
        DeployStatus deployStatus = new DeployStatus(null, requestDto.deployType(), requestDto.status());
        return deployStatusRepository.save(deployStatus).getId();
    }

    public void update(Long id, DeployStatusRequestDto requestDto) {
        DeployStatus deployStatus = deployStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DeployStatus not found"));
        DeployStatus updated = new DeployStatus(deployStatus.getId(), requestDto.deployType(), requestDto.status());
        deployStatusRepository.save(updated);
    }

    public void delete(Long id) {
        deployStatusRepository.deleteById(id);
    }
}
