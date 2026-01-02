package com.wordonline.admin.dto.quest;

import java.util.List;
import java.util.stream.Collectors;

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

    public String getRewardParamsString() {
        if (rewardParams == null || rewardParams.isEmpty()) {
            return "";
        }
        return rewardParams.stream()
                .map(param -> param.name() + ":" + param.value())
                .collect(Collectors.joining(","));
    }
}
