package com.wordonline.admin.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
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
    private final Optional<SecondaryAdminDataService> secondaryAdminDataService;

    public boolean hasSecondaryDatabase() {
        return secondaryAdminDataService.isPresent();
    }

    @Transactional(readOnly = true)
    public List<AdventureDto> findAllAdventures() {
        return findAllAdventures(false);
    }

    @Transactional(readOnly = true)
    public List<AdventureDto> findAllAdventures(boolean secondary) {
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().getAdventures();
        }

        return adventureRepository.findAll(Sort.by("id")).stream()
                .map(adventure -> {
                    List<StageDto> stages = stageRepository.findByAdventureIdOrderByIdAsc(adventure.getId())
                            .stream()
                            .map(stage -> {
                                List<ScenarioDto> scenarios = scenarioRepository.findByStageIdOrderByIdAsc(stage.getId())
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
        return createAdventure(requestDto, false);
    }

    public Long createAdventure(AdventureRequestDto requestDto, boolean secondary) {
        String accessType = requestDto.accessType() != null ? requestDto.accessType() : "FREE";
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().createAdventure(requestDto.name(), accessType);
        }
        Adventure adventure = new Adventure(null, requestDto.name(), accessType);
        return adventureRepository.save(adventure).getId();
    }

    public void updateAdventure(Long adventureId, AdventureRequestDto requestDto) {
        updateAdventure(adventureId, requestDto, false);
    }

    public void updateAdventure(Long adventureId, AdventureRequestDto requestDto, boolean secondary) {
        if (secondary) {
            String accessType = requestDto.accessType() != null ? requestDto.accessType() : "FREE";
            secondaryAdminDataService.orElseThrow().updateAdventure(adventureId, requestDto.name(), accessType);
            return;
        }
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Adventure not found"));
        String accessType = requestDto.accessType() != null ? requestDto.accessType() : adventure.getAccessType();
        Adventure updated = new Adventure(adventure.getId(), requestDto.name(), accessType);
        adventureRepository.save(updated);
    }

    public void deleteAdventure(Long adventureId) {
        deleteAdventure(adventureId, false);
    }

    public void deleteAdventure(Long adventureId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().deleteAdventure(adventureId);
            return;
        }
        adventureRepository.deleteById(adventureId);
    }

    public Long createStage(Long adventureId) {
        return createStage(adventureId, false);
    }

    public Long createStage(Long adventureId, boolean secondary) {
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().createStage(adventureId);
        }
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Adventure not found"));
        Stage stage = new Stage(null, adventure);
        return stageRepository.save(stage).getId();
    }

    public void deleteStage(Long stageId) {
        deleteStage(stageId, false);
    }

    public void deleteStage(Long stageId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().deleteStage(stageId);
            return;
        }
        stageRepository.deleteById(stageId);
    }

    public Long createScenario(Long stageId) {
        return createScenario(stageId, false);
    }

    public Long createScenario(Long stageId, boolean secondary) {
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().createScenario(stageId);
        }
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
        Scenario scenario = new Scenario(null, stage);
        return scenarioRepository.save(scenario).getId();
    }

    public void deleteScenario(Long scenarioId) {
        deleteScenario(scenarioId, false);
    }

    public void deleteScenario(Long scenarioId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().deleteScenario(scenarioId);
            return;
        }
        scenarioRepository.deleteById(scenarioId);
    }

    public void bulkCreateScenarios(Long stageId, int count) {
        bulkCreateScenarios(stageId, count, false);
    }

    public void bulkCreateScenarios(Long stageId, int count, boolean secondary) {
        if (secondary) {
            for (int i = 0; i < count; i++) {
                secondaryAdminDataService.orElseThrow().createScenario(stageId);
            }
            return;
        }
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
        for (int i = 0; i < count; i++) {
            scenarioRepository.save(new Scenario(null, stage));
        }
    }

    public void bulkCreateStages(Long adventureId, int stageCount, int scenarioCount) {
        bulkCreateStages(adventureId, stageCount, scenarioCount, false);
    }

    public void bulkCreateStages(Long adventureId, int stageCount, int scenarioCount, boolean secondary) {
        if (secondary) {
            for (int i = 0; i < stageCount; i++) {
                Long stageId = secondaryAdminDataService.orElseThrow().createStage(adventureId);
                for (int j = 0; j < scenarioCount; j++) {
                    secondaryAdminDataService.orElseThrow().createScenario(stageId);
                }
            }
            return;
        }
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Adventure not found"));
        for (int i = 0; i < stageCount; i++) {
            Stage stage = stageRepository.save(new Stage(null, adventure));
            for (int j = 0; j < scenarioCount; j++) {
                scenarioRepository.save(new Scenario(null, stage));
            }
        }
    }

    public SyncResult syncToSecondary() {
        return secondaryAdminDataService.orElseThrow().syncAdventuresToSecondary(findAllAdventures(false));
    }

    public SyncResult syncToPrimary() {
        List<AdventureDto> adventures = secondaryAdminDataService.orElseThrow().getAdventures();
        Map<Long, Adventure> adventuresById = adventureRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Adventure::getId, adventure -> adventure));
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        List<String> changed = new java.util.ArrayList<>();

        for (AdventureDto adventureDto : adventures) {
            Adventure existing = adventuresById.get(adventureDto.id());
            if (existing == null) {
                adventureRepository.save(new Adventure(adventureDto.id(), adventureDto.name(), adventureDto.accessType()));
                created++;
                changed.add("adventure#" + adventureDto.id());
            } else if (!existing.getName().equals(adventureDto.name())
                    || !existing.getAccessType().equals(adventureDto.accessType())) {
                adventureRepository.save(new Adventure(adventureDto.id(), adventureDto.name(), adventureDto.accessType()));
                updated++;
                changed.add("adventure#" + adventureDto.id());
            } else {
                unchanged++;
            }

            Adventure adventure = adventureRepository.findById(adventureDto.id()).orElseThrow();
            for (StageDto stageDto : adventureDto.stages()) {
                Stage stage = stageRepository.findById(stageDto.id())
                        .orElseGet(() -> stageRepository.save(new Stage(stageDto.id(), adventure)));
                for (ScenarioDto scenarioDto : stageDto.scenarios()) {
                    if (scenarioRepository.findById(scenarioDto.id()).isEmpty()) {
                        scenarioRepository.save(new Scenario(scenarioDto.id(), stage));
                    }
                }
            }
        }

        return new SyncResult(created, updated, unchanged, changed);
    }
}
