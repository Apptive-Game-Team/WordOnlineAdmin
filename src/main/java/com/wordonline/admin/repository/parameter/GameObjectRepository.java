package com.wordonline.admin.repository.parameter;

import com.wordonline.admin.entity.parameter.GameObject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameObjectRepository extends JpaRepository<GameObject, Long> {
    Optional<GameObject> findByName(String name);

    List<GameObject> findAllByNameIn(Collection<String> names);
}
