package com.wordonline.admin.repository;

import com.wordonline.admin.entity.GameObject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameObjectRepository extends JpaRepository<GameObject, Long> { }

