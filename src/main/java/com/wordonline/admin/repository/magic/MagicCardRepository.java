package com.wordonline.admin.repository.magic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.magic.MagicCard;

import java.util.List;

public interface MagicCardRepository extends JpaRepository<MagicCard, Long> {
    List<MagicCard> findByMagicId(Long magicId);

    void deleteByMagicIdAndCardId(Long magicId, Long cardId);
}
