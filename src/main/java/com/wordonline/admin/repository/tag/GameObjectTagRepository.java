package com.wordonline.admin.repository.tag;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.tag.GameObjectTag;

public interface GameObjectTagRepository extends JpaRepository<GameObjectTag, Long> {

    Optional<GameObjectTag> findByGameObjectIdAndTagId(Long gameObjectId, Long tagId);
    void deleteByGameObjectIdAndTagId(Long gameObjectId, Long tagId);
}
