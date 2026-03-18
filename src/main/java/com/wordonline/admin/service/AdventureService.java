package com.wordonline.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.dto.adventure.AdventureDto;
import com.wordonline.admin.dto.adventure.AdventureRequestDto;
import com.wordonline.admin.dto.adventure.ScenarioDto;
import com.wordonline.admin.dto.adventure.StageDto;
import com.wordonline.admin.entity.adventure.Adventure;
import com.wordonline.admin.entity.adventure.Scenario;
import com.wordonline.admin.entity.adventure.Stage;
import com.wordonline.admin.repository.adventure.AdventureRepository;
import com.wordonline.admin.repository.adventure.ScenarioRepository;
import com.wordonline.admin.repository.adventure.StageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdventureService {

    private final AdventureRepository adventureRepository;
    private final StageRepository stageRepository;
    private final ScenarioRepository scenarioRepository;

    @Transactional(readOnly = true)
    public List<AdventureDto> findAllAdventures() {
        return adventureRepository.findAll().stream()
                .map(adventure -> {
                    List<StageDto> stages = stageRepository.findByAdventureId(adventure.getId())
                            .stream()
                            .map(stage -> {
                                List<ScenarioDto> scenarios = scenarioRepository.findByStageId(stage.getId())
                                        .stream()
                                        .map(ScenarioDto::new)
                                        .collect(Collectors.toList());
                                return new StageDto(stage, scenarios);
                            })
                            .collect(Collectors.toList());
                    return new AdventureDto(adventure, stages);
                })
                .collect(Collectors.toList());
    }

    public Long createAdventure(AdventureRequestDto requestDto) {
        String accessType = requestDto.accessType() != null ? requestDto.accessType() : "FREE";
        Adventure adventure = new Adventure(null, requestDto.name(), accessType);
        return adventureRepository.save(adventure).getId();
    }

    public void updateAdventure(Long adventureId, AdventureRequestDto requestDto) {
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Adventure not found"));
        String accessType = requestDto.accessType() != null ? requestDto.accessType() : adventure.getAccessType();
        Adventure updated = new Adventure(adventure.getId(), requestDto.name(), accessType);
        adventureRepository.save(updated);
    }

    public void deleteAdventure(Long adventureId) {
        adventureRepository.deleteById(adventureId);
    }

    public Long createStage(Long adventureId) {
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Adventure not found"));
        Stage stage = new Stage(null, adventure);
        return stageRepository.save(stage).getId();
    }

    public void deleteStage(Long stageId) {
        stageRepository.deleteById(stageId);
    }

    public Long createScenario(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
        Scenario scenario = new Scenario(null, stage);
        return scenarioRepository.save(scenario).getId();
    }

    public void deleteScenario(Long scenarioId) {
        scenarioRepository.deleteById(scenarioId);
    }

    public void bulkCreateScenarios(Long stageId, int count) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
        for (int i = 0; i < count; i++) {
            scenarioRepository.save(new Scenario(null, stage));
        }
    }

    public void bulkCreateStages(Long adventureId, int stageCount, int scenarioCount) {
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Adventure not found"));
        for (int i = 0; i < stageCount; i++) {
            Stage stage = stageRepository.save(new Stage(null, adventure));
            for (int j = 0; j < scenarioCount; j++) {
                scenarioRepository.save(new Scenario(null, stage));
            }
        }
    }
}
