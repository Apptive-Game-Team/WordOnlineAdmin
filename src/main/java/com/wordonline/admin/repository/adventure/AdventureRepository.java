package com.wordonline.admin.repository.adventure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.adventure.Adventure;

public interface AdventureRepository extends JpaRepository<Adventure, Long> {
}
