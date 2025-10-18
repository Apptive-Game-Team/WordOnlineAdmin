package com.wordonline.admin.repository.magic;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.magic.Magic;

public interface MagicRepository extends JpaRepository<Magic, Long> {

    @EntityGraph(attributePaths = {
            "magicCards",
            "magicCards.card"
    })
    List<Magic> findAllBy();
}
