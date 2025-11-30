package com.wordonline.admin.repository.parameter;

import com.wordonline.admin.entity.parameter.GameObject;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameObjectRepository extends JpaRepository<GameObject, Long> {

}

