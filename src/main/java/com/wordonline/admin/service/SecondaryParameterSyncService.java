package com.wordonline.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnBean(name = "secondaryJdbcTemplate")
@RequiredArgsConstructor
@Transactional(transactionManager = "secondaryTransactionManager")
public class SecondaryParameterSyncService {

    private final JdbcTemplate secondaryJdbcTemplate;

    public record ParameterRow(Long id, String name) {}

    public record ParameterValueRow(Long id, Long parameterId, String parameterName, Double value) {}

    public record GameObjectRow(Long id, String name, List<ParameterValueRow> parameterValues) {}

    public record ParameterValueSnapshot(String gameObjectName, String parameterName, Double value) {}

    private record ParameterValueKey(String gameObjectName, String parameterName) {}

    private record ExistingParameterValue(Long id, Double value) {}

    public List<ParameterRow> getParameters() {
        return secondaryJdbcTemplate.query(
                "select id, name from parameters order by id",
                (rs, rowNum) -> new ParameterRow(rs.getLong("id"), rs.getString("name"))
        );
    }

    public GameObjectRow getGameObject(Long gameObjectId) {
        Map<Long, List<ParameterValueRow>> valuesByGameObjectId = getParameterValuesByGameObjectId();

        return secondaryJdbcTemplate.query(
                "select id, name from game_objects where id = ?",
                (rs, rowNum) -> new GameObjectRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        valuesByGameObjectId.getOrDefault(rs.getLong("id"), List.of())
                ),
                gameObjectId
        ).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject in secondary database: " + gameObjectId));
    }

    public List<GameObjectRow> getGameObjects() {
        Map<Long, List<ParameterValueRow>> valuesByGameObjectId = getParameterValuesByGameObjectId();

        return secondaryJdbcTemplate.query(
                "select id, name from game_objects order by id",
                (rs, rowNum) -> new GameObjectRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        valuesByGameObjectId.getOrDefault(rs.getLong("id"), List.of())
                )
        );
    }

    public void createGameObject(String name) {
        secondaryJdbcTemplate.update("insert into game_objects (name) values (?)", name);
    }

    public void updateGameObject(String currentName, String newName) {
        int updated = secondaryJdbcTemplate.update(
                "update game_objects set name = ? where name = ?",
                newName,
                currentName
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Not Found GameObject in secondary database: " + currentName);
        }
    }

    public void deleteGameObject(String name) {
        int deleted = secondaryJdbcTemplate.update("delete from game_objects where name = ?", name);
        if (deleted == 0) {
            throw new IllegalArgumentException("Not Found GameObject in secondary database: " + name);
        }
    }

    public List<ParameterValueSnapshot> getParameterValueSnapshots() {
        return secondaryJdbcTemplate.query(
                """
                select go.name as game_object_name, p.name as parameter_name, pv.value
                from parameter_values pv
                join game_objects go on go.id = pv.game_object_id
                join parameters p on p.id = pv.parameter_id
                order by go.id, p.id
                """,
                (rs, rowNum) -> new ParameterValueSnapshot(
                        rs.getString("game_object_name"),
                        rs.getString("parameter_name"),
                        rs.getObject("value", Double.class)
                )
        );
    }

    public SyncResult replaceParameters(List<String> names) {
        Set<String> existingNames = getParameters().stream()
                .map(ParameterRow::name)
                .collect(Collectors.toSet());
        List<String> createdItems = new ArrayList<>();

        for (String name : names) {
            if (!existingNames.contains(name)) {
                createdItems.add(name);
            }
        }

        secondaryJdbcTemplate.batchUpdate(
                "insert into parameters (name) values (?)",
                createdItems.stream().map(name -> new Object[]{name}).toList()
        );

        return new SyncResult(createdItems.size(), 0, names.size() - createdItems.size(), createdItems);
    }

    public SyncResult replaceParameterValues(List<ParameterValueSnapshot> values) {
        if (values.isEmpty()) {
            return SyncResult.empty();
        }

        Map<String, Long> gameObjectIdsByName = ensureGameObjects(values);
        Map<String, Long> parameterIdsByName = ensureParameters(values);
        Map<ParameterValueKey, ExistingParameterValue> existingValuesByKey = getExistingParameterValuesByKey();
        List<Object[]> inserts = new ArrayList<>();
        List<Object[]> updates = new ArrayList<>();
        List<String> changedItems = new ArrayList<>();
        int unchanged = 0;

        for (ParameterValueSnapshot value : values) {
            ParameterValueKey key = new ParameterValueKey(value.gameObjectName(), value.parameterName());
            ExistingParameterValue existingValue = existingValuesByKey.get(key);
            String changedItem = value.gameObjectName() + "." + value.parameterName();

            if (existingValue == null) {
                inserts.add(new Object[]{
                        value.value(),
                        gameObjectIdsByName.get(value.gameObjectName()),
                        parameterIdsByName.get(value.parameterName())
                });
                changedItems.add(changedItem);
                continue;
            }

            if (Objects.equals(existingValue.value(), value.value())) {
                unchanged++;
                continue;
            }

            updates.add(new Object[]{value.value(), existingValue.id()});
            changedItems.add(changedItem);
        }

        secondaryJdbcTemplate.batchUpdate(
                "insert into parameter_values (value, game_object_id, parameter_id) values (?, ?, ?)",
                inserts
        );
        secondaryJdbcTemplate.batchUpdate(
                "update parameter_values set value = ? where id = ?",
                updates
        );

        return new SyncResult(inserts.size(), updates.size(), unchanged, changedItems);
    }

    public void createParameter(String name) {
        secondaryJdbcTemplate.update(
                "insert into parameters (name) values (?)",
                name
        );
    }

    public void updateParameter(Long parameterId, String name) {
        secondaryJdbcTemplate.update(
                "update parameters set name = ? where id = ?",
                name,
                parameterId
        );
    }

    public void updateParameter(String currentName, String newName) {
        int updated = secondaryJdbcTemplate.update(
                "update parameters set name = ? where name = ?",
                newName,
                currentName
        );

        if (updated == 0) {
            throw new IllegalArgumentException("Not Found Parameter in secondary database: " + currentName);
        }
    }

    public void deleteParameter(Long parameterId) {
        secondaryJdbcTemplate.update("delete from parameters where id = ?", parameterId);
    }

    public void deleteParameter(String name) {
        int deleted = secondaryJdbcTemplate.update(
                "delete from parameters where name = ?",
                name
        );

        if (deleted == 0) {
            throw new IllegalArgumentException("Not Found Parameter in secondary database: " + name);
        }
    }

    public void upsertParameterValue(String gameObjectName, String parameterName, Double value) {
        log.info("[SecondaryParameterSyncService.upsertParameterValue] START: gameObjectName='{}', parameterName='{}', value={}",
                gameObjectName, parameterName, value);
        if (value == null) {
            log.info("  -> Value is null. Delegating to deleteParameterValueIfPresent");
            deleteParameterValueIfPresent(gameObjectName, parameterName);
            return;
        }

        log.info("  -> Ensuring GameObject and Parameter exist in secondary DB");
        ensureGameObject(gameObjectName);
        ensureParameter(parameterName);

        Long gameObjectId = findGameObjectId(gameObjectName);
        Long parameterId = findParameterId(parameterName);
        log.info("  -> Resolved IDs in secondary DB: gameObjectId={}, parameterId={}", gameObjectId, parameterId);

        List<Long> existingIds = secondaryJdbcTemplate.query(
                "select id from parameter_values where game_object_id = ? and parameter_id = ?",
                (rs, rowNum) -> rs.getLong("id"),
                gameObjectId,
                parameterId
        );

        if (existingIds.isEmpty()) {
            log.info("  -> No existing ParameterValue found. Inserting new record in secondary DB");
            try {
                secondaryJdbcTemplate.update(
                        "insert into parameter_values (value, game_object_id, parameter_id) values (?, ?, ?)",
                        value,
                        gameObjectId,
                        parameterId
                );
                log.info("  -> Secondary DB INSERT successful");
            } catch (Exception e) {
                log.error("  -> Secondary DB INSERT failed: {}", e.getMessage(), e);
                throw e;
            }
            return;
        }

        secondaryJdbcTemplate.update(
                "update parameter_values set value = ? where id = ?",
                value,
                existingIds.getFirst()
        );
    }

    public void createParameterValue(Long gameObjectId, Long parameterId, Double value) {
        secondaryJdbcTemplate.update(
                "insert into parameter_values (value, game_object_id, parameter_id) values (?, ?, ?)",
                value,
                gameObjectId,
                parameterId
        );
    }

    public void updateParameterValue(Long parameterValueId, Double value) {
        secondaryJdbcTemplate.update(
                "update parameter_values set value = ? where id = ?",
                value,
                parameterValueId
        );
    }

    public void deleteParameterValue(Long parameterValueId) {
        secondaryJdbcTemplate.update("delete from parameter_values where id = ?", parameterValueId);
    }

    public void deleteParameterValue(String gameObjectName, String parameterName) {
        Long gameObjectId = findGameObjectId(gameObjectName);
        Long parameterId = findParameterId(parameterName);

        int deleted = secondaryJdbcTemplate.update(
                "delete from parameter_values where game_object_id = ? and parameter_id = ?",
                gameObjectId,
                parameterId
        );

        if (deleted == 0) {
            throw new IllegalArgumentException(
                    "Not Found Parameter Value in secondary database: " + gameObjectName + ", " + parameterName
            );
        }
    }

    private void deleteParameterValueIfPresent(String gameObjectName, String parameterName) {
        List<Long> gameObjectIds = secondaryJdbcTemplate.query(
                "select id from game_objects where name = ?",
                (rs, rowNum) -> rs.getLong("id"),
                gameObjectName
        );
        List<Long> parameterIds = secondaryJdbcTemplate.query(
                "select id from parameters where name = ?",
                (rs, rowNum) -> rs.getLong("id"),
                parameterName
        );

        if (gameObjectIds.isEmpty() || parameterIds.isEmpty()) {
            return;
        }

        secondaryJdbcTemplate.update(
                "delete from parameter_values where game_object_id = ? and parameter_id = ?",
                gameObjectIds.getFirst(),
                parameterIds.getFirst()
        );
    }

    private Long findGameObjectId(String gameObjectName) {
        return secondaryJdbcTemplate.query(
                "select id from game_objects where name = ?",
                (rs, rowNum) -> rs.getLong("id"),
                gameObjectName
        ).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not Found GameObject in secondary database: " + gameObjectName
                ));
    }

    private Long findParameterId(String parameterName) {
        return secondaryJdbcTemplate.query(
                "select id from parameters where name = ?",
                (rs, rowNum) -> rs.getLong("id"),
                parameterName
        ).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not Found Parameter in secondary database: " + parameterName
                ));
    }

    private void ensureGameObject(String gameObjectName) {
        List<Long> ids = secondaryJdbcTemplate.query(
                "select id from game_objects where name = ?",
                (rs, rowNum) -> rs.getLong("id"),
                gameObjectName
        );

        if (ids.isEmpty()) {
            secondaryJdbcTemplate.update("insert into game_objects (name) values (?)", gameObjectName);
        }
    }

    private void ensureParameter(String parameterName) {
        List<Long> ids = secondaryJdbcTemplate.query(
                "select id from parameters where name = ?",
                (rs, rowNum) -> rs.getLong("id"),
                parameterName
        );

        if (ids.isEmpty()) {
            createParameter(parameterName);
        }
    }

    private Map<Long, List<ParameterValueRow>> getParameterValuesByGameObjectId() {
        return secondaryJdbcTemplate.query(
                """
                select pv.id, pv.game_object_id, pv.parameter_id, p.name as parameter_name, pv.value
                from parameter_values pv
                join parameters p on p.id = pv.parameter_id
                order by pv.id
                """,
                (rs, rowNum) -> Map.entry(
                        rs.getLong("game_object_id"),
                        new ParameterValueRow(
                                rs.getLong("id"),
                                rs.getLong("parameter_id"),
                                rs.getString("parameter_name"),
                                rs.getObject("value", Double.class)
                        )
                )
        ).stream().collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
        ));
    }

    private Map<String, Long> ensureGameObjects(List<ParameterValueSnapshot> values) {
        List<String> names = values.stream()
                .map(ParameterValueSnapshot::gameObjectName)
                .distinct()
                .toList();
        Map<String, Long> idsByName = getGameObjectIdsByName();
        List<String> missingNames = names.stream()
                .filter(name -> !idsByName.containsKey(name))
                .toList();

        secondaryJdbcTemplate.batchUpdate(
                "insert into game_objects (name) values (?)",
                missingNames.stream().map(name -> new Object[]{name}).toList()
        );

        return missingNames.isEmpty() ? idsByName : getGameObjectIdsByName();
    }

    private Map<String, Long> ensureParameters(List<ParameterValueSnapshot> values) {
        List<String> names = values.stream()
                .map(ParameterValueSnapshot::parameterName)
                .distinct()
                .toList();
        Map<String, Long> idsByName = getParameterIdsByName();
        List<String> missingNames = names.stream()
                .filter(name -> !idsByName.containsKey(name))
                .toList();

        secondaryJdbcTemplate.batchUpdate(
                "insert into parameters (name) values (?)",
                missingNames.stream().map(name -> new Object[]{name}).toList()
        );

        return missingNames.isEmpty() ? idsByName : getParameterIdsByName();
    }

    private Map<String, Long> getGameObjectIdsByName() {
        return secondaryJdbcTemplate.query(
                "select id, name from game_objects",
                (rs, rowNum) -> Map.entry(rs.getString("name"), rs.getLong("id"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Long> getParameterIdsByName() {
        return secondaryJdbcTemplate.query(
                "select id, name from parameters",
                (rs, rowNum) -> Map.entry(rs.getString("name"), rs.getLong("id"))
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<ParameterValueKey, ExistingParameterValue> getExistingParameterValuesByKey() {
        return new HashMap<>(secondaryJdbcTemplate.query(
                """
                select pv.id, go.name as game_object_name, p.name as parameter_name, pv.value
                from parameter_values pv
                join game_objects go on go.id = pv.game_object_id
                join parameters p on p.id = pv.parameter_id
                """,
                (rs, rowNum) -> Map.entry(
                        new ParameterValueKey(
                                rs.getString("game_object_name"),
                                rs.getString("parameter_name")
                        ),
                        new ExistingParameterValue(
                                rs.getLong("id"),
                                rs.getObject("value", Double.class)
                        )
                )
        ).stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (current, replacement) -> replacement
        )));
    }
}
