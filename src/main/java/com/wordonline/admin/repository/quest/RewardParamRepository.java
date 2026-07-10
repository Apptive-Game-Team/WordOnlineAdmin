package com.wordonline.admin.repository.quest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.quest.RewardParam;

public interface RewardParamRepository extends JpaRepository<RewardParam, Long> {

    List<RewardParam> findByQuestId(Long questId);

    List<RewardParam> findByQuestIdOrderByIdAsc(Long questId);
    
    void deleteByQuestId(Long questId);
}
