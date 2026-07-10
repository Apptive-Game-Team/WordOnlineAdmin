package com.wordonline.admin.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
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
    private final Optional<SecondaryAdminDataService> secondaryAdminDataService;

    public boolean hasSecondaryDatabase() {
        return secondaryAdminDataService.isPresent();
    }

    @Transactional(readOnly = true)
    public List<QuestDto> findAllQuests() {
        return findAllQuests(false);
    }

    @Transactional(readOnly = true)
    public List<QuestDto> findAllQuests(boolean secondary) {
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().getQuests();
        }

        return questRepository.findAll(Sort.by("id")).stream()
                .map(quest -> {
                    List<RewardParamDto> rewardParams = rewardParamRepository.findByQuestIdOrderByIdAsc(quest.getId())
                            .stream()
                            .map(RewardParamDto::new)
                            .collect(Collectors.toList());
                    return new QuestDto(quest, rewardParams);
                })
                .collect(Collectors.toList());
    }

    public Long createQuest(QuestRequestDto requestDto) {
        return createQuest(requestDto, false);
    }

    public Long createQuest(QuestRequestDto requestDto, boolean secondary) {
        if (secondary) {
            Long questId = secondaryAdminDataService.orElseThrow().createQuest(
                    requestDto.progressChecker(),
                    requestDto.requireValue(),
                    requestDto.rewardGiver()
            );
            if (requestDto.rewardParams() != null) {
                for (RewardParamDto paramDto : requestDto.rewardParams()) {
                    secondaryAdminDataService.orElseThrow().createRewardParam(questId, paramDto.name(), paramDto.value());
                }
            }
            return questId;
        }

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
        updateQuest(questId, requestDto, false);
    }

    public void updateQuest(Long questId, QuestRequestDto requestDto, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().updateQuest(
                    questId,
                    requestDto.progressChecker(),
                    requestDto.requireValue(),
                    requestDto.rewardGiver()
            );
            if (requestDto.rewardParams() != null) {
                for (RewardParamDto paramDto : requestDto.rewardParams()) {
                    secondaryAdminDataService.orElseThrow().createRewardParam(questId, paramDto.name(), paramDto.value());
                }
            }
            return;
        }

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
        deleteQuest(questId, false);
    }

    public void deleteQuest(Long questId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().deleteQuest(questId);
            return;
        }
        questRepository.deleteById(questId);
    }

    public SyncResult syncToSecondary() {
        return secondaryAdminDataService.orElseThrow().syncQuestsToSecondary(findAllQuests(false));
    }

    public SyncResult syncToPrimary() {
        List<QuestDto> quests = secondaryAdminDataService.orElseThrow().getQuests();
        Map<Long, Quest> questsById = questRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Quest::getId, quest -> quest));
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        List<String> changed = new java.util.ArrayList<>();

        for (QuestDto questDto : quests) {
            Quest existing = questsById.get(questDto.id());
            Quest quest = new Quest(
                    questDto.id(),
                    questDto.progressChecker(),
                    questDto.requireValue(),
                    questDto.rewardGiver()
            );

            if (existing == null) {
                created++;
                changed.add("quest#" + questDto.id());
            } else if (!java.util.Objects.equals(existing.getProgressChecker(), questDto.progressChecker())
                    || !java.util.Objects.equals(existing.getRequireValue(), questDto.requireValue())
                    || !java.util.Objects.equals(existing.getRewardGiver(), questDto.rewardGiver())) {
                updated++;
                changed.add("quest#" + questDto.id());
            } else {
                unchanged++;
            }

            questRepository.save(quest);
            rewardParamRepository.deleteByQuestId(questDto.id());
            for (RewardParamDto rewardParam : questDto.rewardParams()) {
                rewardParamRepository.save(new RewardParam(
                        rewardParam.id(),
                        quest,
                        rewardParam.name(),
                        rewardParam.value()
                ));
            }
        }

        return new SyncResult(created, updated, unchanged, changed);
    }
}
