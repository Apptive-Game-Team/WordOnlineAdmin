package com.wordonline.admin.service;

import com.wordonline.admin.dto.*;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.Parameter;
import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import com.wordonline.admin.repository.parameter.ParameterValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParameterService {
    private final GameObjectRepository gameObjectRepository;
    private final ParameterValueRepository parameterValueRepository;
    private final ParameterRepository parameterRepository;
    private final Optional<SecondaryParameterSyncService> secondaryParameterSyncService;
    @Qualifier("jdbcTemplate")
    private final JdbcTemplate primaryJdbcTemplate;

    public boolean hasSecondaryDatabase() {
        return secondaryParameterSyncService.isPresent();
    }

    public void createParameter(String name, boolean syncSecondary) {
        Parameter parameter = new Parameter(name);
        parameterRepository.save(parameter);
        syncSecondary(syncSecondary, service -> service.createParameter(name));
    }

    public void createParameter(String name, boolean secondary, boolean syncOther) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().createParameter(name);
            if (syncOther) {
                createParameter(name, false);
            }
            return;
        }

        createParameter(name, syncOther);
    }

    public void updateParameter(Long parameterId, String name, boolean syncSecondary) {
        Parameter parameter = parameterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter") );
        String currentName = parameter.getName();
        parameter.setName(name);
        syncSecondary(syncSecondary, service -> service.updateParameter(currentName, name));
    }

    public void updateParameter(Long parameterId, String name, boolean secondary, boolean syncOther) {
        if (secondary) {
            String currentName = syncOther ? findSecondaryParameterName(parameterId) : null;
            secondaryParameterSyncService.orElseThrow().updateParameter(parameterId, name);
            if (syncOther) {
                updateParameter(currentName, name, false);
            }
            return;
        }

        updateParameter(parameterId, name, syncOther);
    }

    public void deleteParameter(Long parameterId, boolean syncSecondary) {
        Parameter parameter = parameterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter"));
        parameterRepository.delete(parameter);
        syncSecondary(syncSecondary, service -> service.deleteParameter(parameter.getName()));
    }

    public void deleteParameter(Long parameterId, boolean secondary, boolean syncOther) {
        if (secondary) {
            String name = syncOther ? findSecondaryParameterName(parameterId) : null;
            secondaryParameterSyncService.orElseThrow().deleteParameter(parameterId);
            if (syncOther) {
                deleteParameter(name, false);
            }
            return;
        }

        deleteParameter(parameterId, syncOther);
    }

    public void createParameterInDatabase(String name, boolean secondary) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().createParameter(name);
            return;
        }
        parameterRepository.save(new Parameter(name));
    }

    public void updateParameter(String currentName, String newName, boolean secondary) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().updateParameter(currentName, newName);
            return;
        }

        Parameter parameter = parameterRepository.findByName(currentName)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter: " + currentName));
        parameter.setName(newName);
    }

    public void deleteParameter(String name, boolean secondary) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().deleteParameter(name);
            return;
        }

        Parameter parameter = parameterRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter: " + name));
        parameterRepository.delete(parameter);
    }

    public void createGameObject(String name) {
        GameObject gameObject = new GameObject(name);
        gameObjectRepository.save(gameObject);
    }

    public void updateGameObject(Long gameObjectId, String name) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject"));
        gameObject.setName(name);
    }

    public void deleteGameObject(Long gameObjectId) {
        gameObjectRepository.deleteById(gameObjectId);
    }

    public void createGameObject(String name, boolean secondary) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().createGameObject(name);
            return;
        }
        createGameObject(name);
    }

    public void updateGameObject(String currentName, String newName, boolean secondary) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().updateGameObject(currentName, newName);
            return;
        }

        GameObject gameObject = gameObjectRepository.findByName(currentName)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject: " + currentName));
        gameObject.setName(newName);
    }

    public void deleteGameObject(String name, boolean secondary) {
        if (secondary) {
            secondaryParameterSyncService.orElseThrow().deleteGameObject(name);
            return;
        }

        GameObject gameObject = gameObjectRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject: " + name));
        gameObjectRepository.delete(gameObject);
    }

    @Transactional(readOnly = true)
    public List<com.wordonline.admin.dto.GameObjectComparisonDto> getGameObjectComparisons() {
        Set<String> primaryNames = gameObjectRepository.findAll().stream()
                .map(GameObject::getName)
                .collect(Collectors.toSet());
        Set<String> secondaryNames = secondaryParameterSyncService
                .map(service -> service.getGameObjects().stream()
                        .map(SecondaryParameterSyncService.GameObjectRow::name)
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
        Set<String> allNames = new TreeSet<>(primaryNames);
        allNames.addAll(secondaryNames);

        return allNames.stream()
                .map(name -> new com.wordonline.admin.dto.GameObjectComparisonDto(
                        name,
                        primaryNames.contains(name),
                        secondaryNames.contains(name)
                ))
                .toList();
    }
    public void createParameterValue(Long gameObjectId, Long parameterId, Double value, boolean syncSecondary) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject"));
        Parameter parameter = parameterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter"));

        // PostgreSQL atomic upsert: INSERT ... ON CONFLICT DO UPDATE
        // JPA/Hibernate 1차 캐시 및 동시성 레이스 컨디션을 DB 레벨에서 완전히 방어
        primaryJdbcTemplate.update(
                """
                INSERT INTO parameter_values (game_object_id, parameter_id, value)
                VALUES (?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_parameter_game_object
                DO UPDATE SET value = EXCLUDED.value
                """,
                gameObjectId, parameterId, value
        );

        syncSecondary(syncSecondary, service -> service.upsertParameterValue(gameObject.getName(), parameter.getName(), value));
    }

    public void createParameterValue(Long gameObjectId, Long parameterId, Double value, boolean secondary, boolean syncOther) {
        if (secondary) {
            GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject"));
            Parameter parameter = parameterRepository.findById(parameterId)
                    .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter"));
            secondaryParameterSyncService.orElseThrow().upsertParameterValue(gameObject.getName(), parameter.getName(), value);
            if (syncOther) {
                createParameterValue(gameObjectId, parameterId, value, false);
            }
            return;
        }

        createParameterValue(gameObjectId, parameterId, value, syncOther);
    }

    public void updateParameterValue(Long parameterValueId, Double value, boolean syncSecondary) {
        ParameterValue parameterValue = parameterValueRepository.findById(parameterValueId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter Value"));

        parameterValue.setValue(value);
        syncSecondary(syncSecondary, service -> service.upsertParameterValue(
                parameterValue.getGameObject().getName(),
                parameterValue.getParameter().getName(),
                value
        ));
    }

    public void updateParameterValue(Long parameterValueId, Double value, boolean secondary, boolean syncOther) {
        if (secondary) {
            ParameterValue parameterValue = parameterValueRepository.findById(parameterValueId)
                    .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter Value"));
            secondaryParameterSyncService.orElseThrow().upsertParameterValue(
                    parameterValue.getGameObject().getName(),
                    parameterValue.getParameter().getName(),
                    value
            );
            if (syncOther) {
                updateParameterValue(parameterValueId, value, false);
            }
            return;
        }

        updateParameterValue(parameterValueId, value, syncOther);
    }

    public void deleteParameterValue(Long parameterValueId, boolean syncSecondary) {
        ParameterValue parameterValue = parameterValueRepository.findById(parameterValueId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter Value"));
        parameterValue.getGameObject().removeParameterValue(parameterValue);
        parameterValueRepository.delete(parameterValue);
        syncSecondary(syncSecondary, service -> service.deleteParameterValue(
                parameterValue.getGameObject().getName(),
                parameterValue.getParameter().getName()
        ));
    }

    public void deleteParameterValue(Long parameterValueId, boolean secondary, boolean syncOther) {
        if (secondary) {
            ParameterValue parameterValue = parameterValueRepository.findById(parameterValueId)
                    .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter Value"));
            secondaryParameterSyncService.orElseThrow().deleteParameterValue(
                    parameterValue.getGameObject().getName(),
                    parameterValue.getParameter().getName()
            );
            if (syncOther) {
                deleteParameterValue(parameterValueId, false);
            }
            return;
        }

        deleteParameterValue(parameterValueId, syncOther);
    }

    public GameObjectsDto getGameObjects() {
        return new GameObjectsDto(

        gameObjectRepository.findAll(Sort.by("id"))
                .stream().map(
                        gameObject -> {
                            List<ParameterValueDto> parameterValueDtos = gameObject.getParameterValues()
                                    .stream()
                                    .sorted(Comparator.comparing(ParameterValue::getId))
                                    .map(
                                            parameterValue ->
                                                    new ParameterValueDto(
                                                            parameterValue.getId(),
                                                            parameterValue.getParameter().getId(),
                                                            parameterValue.getValue()
                                                    )
                                    ).toList();

                            return new GameObjectDto(
                                    gameObject.getId(),
                                    gameObject.getName(),
                                    parameterValueDtos
                            );
                        }

                ).toList()
        );
    }

    public void updateParameterValue(Long gameObjectId, String parameterName, Double value, boolean syncSecondary) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject"));
        Parameter parameter = parameterRepository.findByName(parameterName)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter name: " + parameterName));

        Optional<ParameterValue> existingValue = gameObject.getParameterValue(parameterName);
        if (existingValue.isEmpty()) {
            existingValue = parameterValueRepository.findByGameObjectIdAndParameterId(gameObject.getId(), parameter.getId());
        }

        if (existingValue.isPresent()) {
            existingValue.get().setValue(value);
        } else {
            ParameterValue newValue = new ParameterValue(value, gameObject, parameter);
            parameterValueRepository.save(newValue);
        }
        syncSecondary(syncSecondary, service -> service.upsertParameterValue(gameObject.getName(), parameter.getName(), value));
    }

    public void upsertParameterValue(
            String gameObjectName,
            String parameterName,
            Double value,
            boolean secondary
    ) {
        log.info("[ParameterService.upsertParameterValue] START: gameObjectName='{}', parameterName='{}', value={}, secondary={}",
                gameObjectName, parameterName, value, secondary);
        if (secondary) {
            log.info("  -> Delegating to secondaryParameterSyncService");
            try {
                secondaryParameterSyncService.orElseThrow()
                        .upsertParameterValue(gameObjectName, parameterName, value);
                log.info("  -> Secondary upsert completed successfully");
            } catch (Exception e) {
                log.error("  -> Secondary upsert FAILED: {}", e.getMessage(), e);
                throw e;
            }
            return;
        }

        log.info("  -> Fetching GameObject & Parameter from Primary Repository");
        Optional<GameObject> gameObject = gameObjectRepository.findByName(gameObjectName);
        Optional<Parameter> parameter = parameterRepository.findByName(parameterName);
        log.info("  -> Repository lookup complete: gameObjectPresent={}, parameterPresent={}",
                gameObject.isPresent(), parameter.isPresent());

        if (value == null) {
            log.info("  -> Value is null. Performing DELETE if exists.");
            if (gameObject.isEmpty() || parameter.isEmpty()) {
                log.info("  -> GameObject or Parameter absent. Nothing to delete.");
                return;
            }
            int deleted = primaryJdbcTemplate.update(
                    "DELETE FROM parameter_values WHERE game_object_id = ? AND parameter_id = ?",
                    gameObject.get().getId(), parameter.get().getId()
            );
            log.info("  -> DELETE completed: {} row(s) affected", deleted);
            return;
        }

        log.info("  -> Preparing target GameObject: currentPresent={}", gameObject.isPresent());
        GameObject targetGameObject = gameObject.orElseGet(
                () -> {
                    GameObject newGo = gameObjectRepository.saveAndFlush(new GameObject(gameObjectName));
                    log.info("    -> Created new GameObject in Primary DB: id={}, name='{}'", newGo.getId(), newGo.getName());
                    return newGo;
                }
        );
        log.info("  -> Preparing target Parameter: currentPresent={}", parameter.isPresent());
        Parameter targetParameter = parameter.orElseGet(
                () -> {
                    Parameter newParam = parameterRepository.saveAndFlush(new Parameter(parameterName));
                    log.info("    -> Created new Parameter in Primary DB: id={}, name='{}'", newParam.getId(), newParam.getName());
                    return newParam;
                }
        );

        log.info("  -> Upserting ParameterValue in Primary DB via JDBC");
        int rows = primaryJdbcTemplate.update(
                """
                INSERT INTO parameter_values (game_object_id, parameter_id, value)
                VALUES (?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_parameter_game_object
                DO UPDATE SET value = EXCLUDED.value
                """,
                targetGameObject.getId(), targetParameter.getId(), value
        );
        log.info("  -> Primary DB UPSERT COMPLETE: {} row(s) affected", rows);
    }

    public ParametersDto getParameters() {
        return new ParametersDto(
                parameterRepository.findAll(Sort.by("id"))
                        .stream().map(
                            parameter ->
                                    new ParameterDto(
                                            parameter.getId(),
                                            parameter.getName()
                                    )
                        ).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<ParameterDto> getParameterDtos(boolean secondary) {
        if (secondary) {
            return secondaryParameterSyncService.orElseThrow().getParameters()
                    .stream()
                    .map(parameter -> new ParameterDto(parameter.id(), parameter.name()))
                    .toList();
        }

        return parameterRepository.findAll(Sort.by("id"))
                .stream()
                .map(parameter -> new ParameterDto(parameter.getId(), parameter.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.wordonline.admin.dto.ParameterComparisonDto> getParameterComparisons() {
        Set<String> primaryNames = parameterRepository.findAll().stream()
                .map(Parameter::getName)
                .collect(Collectors.toSet());
        Set<String> secondaryNames = secondaryParameterSyncService
                .map(service -> service.getParameters().stream()
                        .map(SecondaryParameterSyncService.ParameterRow::name)
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
        Set<String> allNames = new TreeSet<>(primaryNames);
        allNames.addAll(secondaryNames);

        return allNames.stream()
                .map(name -> new com.wordonline.admin.dto.ParameterComparisonDto(
                        name,
                        primaryNames.contains(name),
                        secondaryNames.contains(name)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GameObjectDto getGameObjectDto(Long gameObjectId, boolean secondary) {
        if (secondary) {
            SecondaryParameterSyncService.GameObjectRow gameObject = secondaryParameterSyncService.orElseThrow()
                    .getGameObject(gameObjectId);
            return new GameObjectDto(
                    gameObject.id(),
                    gameObject.name(),
                    gameObject.parameterValues()
                            .stream()
                            .map(value -> new ParameterValueDto(
                                    value.id(),
                                    value.parameterId(),
                                    value.parameterName(),
                                    value.value()
                            ))
                            .toList()
            );
        }

        GameObject gameObject = gameObjectRepository.findById(gameObjectId).orElseThrow();
        return new GameObjectDto(
                gameObject.getId(),
                gameObject.getName(),
                gameObject.getParameterValues()
                        .stream()
                        .sorted(Comparator.comparing(ParameterValue::getId))
                        .map(value -> new ParameterValueDto(
                                value.getId(),
                                value.getParameter().getId(),
                                value.getParameter().getName(),
                                value.getValue()
                        ))
                        .toList()
        );
    }

    public SyncResult syncParametersToSecondary() {
        List<String> parameterNames = parameterRepository.findAll(Sort.by("id"))
                .stream()
                .map(Parameter::getName)
                .toList();
        return secondaryParameterSyncService.orElseThrow().replaceParameters(parameterNames);
    }

    public SyncResult syncParametersToPrimary() {
        List<String> parameterNames = secondaryParameterSyncService.orElseThrow().getParameters()
                .stream()
                .map(SecondaryParameterSyncService.ParameterRow::name)
                .toList();
        Map<String, Parameter> existingParametersByName = parameterRepository.findAllByNameIn(parameterNames)
                .stream()
                .collect(Collectors.toMap(Parameter::getName, Function.identity()));
        List<String> createdItems = new java.util.ArrayList<>();

        for (String parameterName : parameterNames) {
            if (!existingParametersByName.containsKey(parameterName)) {
                parameterRepository.save(new Parameter(parameterName));
                createdItems.add(parameterName);
            }
        }

        return new SyncResult(createdItems.size(), 0, parameterNames.size() - createdItems.size(), createdItems);
    }

    public SyncResult syncParameterValuesToSecondary() {
        return secondaryParameterSyncService.orElseThrow().replaceParameterValues(getPrimaryParameterValueSnapshots());
    }

    public SyncResult syncParameterValuesToPrimary() {
        List<SecondaryParameterSyncService.ParameterValueSnapshot> snapshots = secondaryParameterSyncService.orElseThrow()
                .getParameterValueSnapshots();
        if (snapshots.isEmpty()) {
            return SyncResult.empty();
        }

        Map<String, GameObject> gameObjectsByName = loadGameObjectsByName(snapshots);
        Map<String, Parameter> parametersByName = loadParametersByName(snapshots);
        Map<ParameterValueKey, ParameterValue> parameterValuesByKey = parameterValueRepository.findAllWithGameObjectAndParameter()
                .stream()
                .collect(Collectors.toMap(
                        value -> new ParameterValueKey(
                                value.getGameObject().getName(),
                                value.getParameter().getName()
                        ),
                        Function.identity(),
                        (current, replacement) -> replacement
                ));
        List<ParameterValue> changedValues = new java.util.ArrayList<>();
        List<String> changedItems = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (SecondaryParameterSyncService.ParameterValueSnapshot snapshot : snapshots) {
            GameObject gameObject = gameObjectsByName.get(snapshot.gameObjectName());
            Parameter parameter = parametersByName.get(snapshot.parameterName());
            ParameterValueKey key = new ParameterValueKey(snapshot.gameObjectName(), snapshot.parameterName());
            ParameterValue parameterValue = parameterValuesByKey.get(key);
            String changedItem = snapshot.gameObjectName() + "." + snapshot.parameterName();

            if (parameterValue == null) {
                parameterValue = new ParameterValue(snapshot.value(), gameObject, parameter);
                parameterValuesByKey.put(key, parameterValue);
                changedValues.add(parameterValue);
                changedItems.add(changedItem);
                created++;
                continue;
            }

            if (java.util.Objects.equals(parameterValue.getValue(), snapshot.value())) {
                unchanged++;
                continue;
            }

            parameterValue.setValue(snapshot.value());
            changedValues.add(parameterValue);
            changedItems.add(changedItem);
            updated++;
        }

        parameterValueRepository.saveAll(changedValues);
        return new SyncResult(created, updated, unchanged, changedItems);
    }

    private String findSecondaryParameterName(Long parameterId) {
        return secondaryParameterSyncService.orElseThrow().getParameters()
                .stream()
                .filter(parameter -> parameter.id().equals(parameterId))
                .findFirst()
                .map(SecondaryParameterSyncService.ParameterRow::name)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter in secondary database: " + parameterId));
    }

    private void syncSecondary(boolean enabled, SecondarySyncAction action) {
        if (!enabled) {
            return;
        }

        secondaryParameterSyncService.ifPresent(action::sync);
    }

    @FunctionalInterface
    private interface SecondarySyncAction {
        void sync(SecondaryParameterSyncService service);
    }

    private List<SecondaryParameterSyncService.ParameterValueSnapshot> getPrimaryParameterValueSnapshots() {
        return parameterValueRepository.findAll(Sort.by("id"))
                .stream()
                .map(value -> new SecondaryParameterSyncService.ParameterValueSnapshot(
                        value.getGameObject().getName(),
                        value.getParameter().getName(),
                        value.getValue()
                ))
                .toList();
    }

    private Map<String, GameObject> loadGameObjectsByName(
            List<SecondaryParameterSyncService.ParameterValueSnapshot> snapshots
    ) {
        List<String> names = snapshots.stream()
                .map(SecondaryParameterSyncService.ParameterValueSnapshot::gameObjectName)
                .distinct()
                .toList();
        Map<String, GameObject> gameObjectsByName = new HashMap<>(gameObjectRepository.findAllByNameIn(names)
                .stream()
                .collect(Collectors.toMap(GameObject::getName, Function.identity())));

        for (String name : names) {
            gameObjectsByName.computeIfAbsent(name, value -> gameObjectRepository.save(new GameObject(value)));
        }

        return gameObjectsByName;
    }

    private Map<String, Parameter> loadParametersByName(
            List<SecondaryParameterSyncService.ParameterValueSnapshot> snapshots
    ) {
        List<String> names = snapshots.stream()
                .map(SecondaryParameterSyncService.ParameterValueSnapshot::parameterName)
                .distinct()
                .toList();
        Map<String, Parameter> parametersByName = new HashMap<>(parameterRepository.findAllByNameIn(names)
                .stream()
                .collect(Collectors.toMap(Parameter::getName, Function.identity())));

        for (String name : names) {
            parametersByName.computeIfAbsent(name, value -> parameterRepository.save(new Parameter(value)));
        }

        return parametersByName;
    }

    private record ParameterValueKey(String gameObjectName, String parameterName) {
    }
}
