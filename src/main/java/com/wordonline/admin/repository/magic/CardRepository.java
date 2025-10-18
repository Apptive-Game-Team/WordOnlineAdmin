package com.wordonline.admin.repository.magic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.magic.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
    
}
