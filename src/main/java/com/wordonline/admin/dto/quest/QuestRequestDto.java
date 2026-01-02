package com.wordonline.admin.dto.quest;

import java.util.List;

public record QuestRequestDto(
        String progressChecker,
        Integer requireValue,
        String rewardGiver,
        List<RewardParamDto> rewardParams
) {
}
