package com.wordonline.admin.dto.quest;

import com.wordonline.admin.entity.quest.RewardParam;

public record RewardParamDto(
        Long id,
        String name,
        Integer value
) {

    public RewardParamDto(RewardParam rewardParam) {
        this(rewardParam.getId(), rewardParam.getName(), rewardParam.getValue());
    }
}
