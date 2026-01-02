package com.wordonline.admin.dto.quest;

import java.util.List;

import com.wordonline.admin.entity.quest.Quest;

public record QuestDto(
        Long id,
        String progressChecker,
        Integer requireValue,
        String rewardGiver,
        List<RewardParamDto> rewardParams
) {

    public QuestDto(Quest quest, List<RewardParamDto> rewardParams) {
        this(quest.getId(), quest.getProgressChecker(), quest.getRequireValue(), 
             quest.getRewardGiver(), rewardParams);
    }
}
