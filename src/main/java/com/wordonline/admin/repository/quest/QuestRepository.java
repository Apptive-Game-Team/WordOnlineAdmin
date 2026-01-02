package com.wordonline.admin.repository.quest;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.quest.Quest;

public interface QuestRepository extends JpaRepository<Quest, Long> {

}
