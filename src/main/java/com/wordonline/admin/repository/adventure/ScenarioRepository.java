package com.wordonline.admin.repository.adventure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.adventure.Scenario;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findByStageId(Long stageId);
}
