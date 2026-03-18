package com.wordonline.admin.repository.adventure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.adventure.Stage;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findByAdventureId(Long adventureId);
}
