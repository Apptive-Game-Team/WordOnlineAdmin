package com.wordonline.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.dto.quest.QuestDto;
import com.wordonline.admin.dto.quest.QuestRequestDto;
import com.wordonline.admin.dto.quest.RewardParamDto;
import com.wordonline.admin.entity.quest.Quest;
import com.wordonline.admin.entity.quest.RewardParam;
import com.wordonline.admin.repository.quest.QuestRepository;
import com.wordonline.admin.repository.quest.RewardParamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestService {

    private final QuestRepository questRepository;
    private final RewardParamRepository rewardParamRepository;

    @Transactional(readOnly = true)
    public List<QuestDto> findAllQuests() {
        return questRepository.findAll().stream()
                .map(quest -> {
                    List<RewardParamDto> rewardParams = rewardParamRepository.findByQuestId(quest.getId())
                            .stream()
                            .map(RewardParamDto::new)
                            .collect(Collectors.toList());
                    return new QuestDto(quest, rewardParams);
                })
                .collect(Collectors.toList());
    }

    public Long createQuest(QuestRequestDto requestDto) {
        Quest quest = new Quest(
                null,
                requestDto.progressChecker(),
                requestDto.requireValue(),
                requestDto.rewardGiver()
        );
        Quest savedQuest = questRepository.save(quest);

        if (requestDto.rewardParams() != null) {
            for (RewardParamDto paramDto : requestDto.rewardParams()) {
                RewardParam rewardParam = new RewardParam(
                        null,
                        savedQuest,
                        paramDto.name(),
                        paramDto.value()
                );
                rewardParamRepository.save(rewardParam);
            }
        }

        return savedQuest.getId();
    }

    public void updateQuest(Long questId, QuestRequestDto requestDto) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found"));

        Quest updatedQuest = new Quest(
                quest.getId(),
                requestDto.progressChecker(),
                requestDto.requireValue(),
                requestDto.rewardGiver()
        );
        questRepository.save(updatedQuest);

        // Delete existing reward params and create new ones
        rewardParamRepository.deleteByQuestId(questId);

        if (requestDto.rewardParams() != null) {
            for (RewardParamDto paramDto : requestDto.rewardParams()) {
                RewardParam rewardParam = new RewardParam(
                        null,
                        updatedQuest,
                        paramDto.name(),
                        paramDto.value()
                );
                rewardParamRepository.save(rewardParam);
            }
        }
    }

    public void deleteQuest(Long questId) {
        questRepository.deleteById(questId);
    }
}
