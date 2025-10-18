package com.wordonline.admin.repository.magic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.magic.MagicCard;

public interface MagicCardRepository extends JpaRepository<MagicCard, Long> {

}
