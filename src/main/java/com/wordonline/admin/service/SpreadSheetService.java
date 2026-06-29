package com.wordonline.admin.service;

import com.wordonline.admin.controller.SpreadSheetApiController;
import com.wordonline.admin.dto.sheet.GameObjectComparisonDto;
import com.wordonline.admin.dto.sheet.GameObjectDto;
import com.wordonline.admin.dto.sheet.ParameterComparisonDto;
import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.magic.MagicCard;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.repository.magic.MagicRepository;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpreadSheetService {

    private final GameObjectRepository gameObjectRepository;
    private final MagicRepository magicRepository;
    private final TagRepository tagRepository;
    private final ParameterService parameterService;
    private final Optional<SecondaryParameterSyncService> secondaryParameterSyncService;

    public boolean hasSecondaryDatabase() {
        return secondaryParameterSyncService.isPresent();
    }

    public List<GameObjectDto> getGameObjects(List<String> tagNames) {
        List<GameObject> allGameObjects = gameObjectRepository.findAll(Sort.by("id"));

        if (tagNames == null || tagNames.isEmpty()) {
            return allGameObjects.stream()
                    .map(this::fromGameObject)
                    .collect(Collectors.toList());
        }

        return allGameObjects.stream()
                .filter(gameObject -> {
                    List<String> gameObjectTagNames = gameObject.getGameObjectTags().stream()
                            .map(gameObjectTag -> gameObjectTag.getTag().getName())
                            .toList();
                    return gameObjectTagNames.containsAll(tagNames);
                })
                .map(this::fromGameObject)
                .collect(Collectors.toList());
    }

    public List<GameObjectDto> getGameObjects(List<String> tagNames, boolean secondary) {
        if (!secondary) {
            return getGameObjects(tagNames);
        }

        return secondaryParameterSyncService.orElseThrow().getGameObjects()
                .stream()
                .map(this::fromSecondaryGameObject)
                .collect(Collectors.toList());
    }

    public List<GameObjectComparisonDto> getGameObjectComparisons(List<String> tagNames) {
        List<GameObject> primaryGameObjects = gameObjectRepository.findAll(Sort.by("name"));
        Set<String> filteredPrimaryNames = primaryGameObjects.stream()
                .filter(gameObject -> hasAllTags(gameObject, tagNames))
                .map(GameObject::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Map<String, Double>> primaryValues = primaryGameObjects.stream()
                .filter(gameObject -> filteredPrimaryNames.contains(gameObject.getName()))
                .collect(Collectors.toMap(
                        GameObject::getName,
                        this::getParameterValuesByName,
                        (current, replacement) -> {
                            throw new IllegalStateException("Duplicate primary game object name");
                        },
                        TreeMap::new
                ));

        List<SecondaryParameterSyncService.GameObjectRow> secondaryGameObjects = secondaryParameterSyncService
                .map(SecondaryParameterSyncService::getGameObjects)
                .orElseGet(List::of);
        Map<String, Map<String, Double>> secondaryValues = new TreeMap<>();
        for (SecondaryParameterSyncService.GameObjectRow gameObject : secondaryGameObjects) {
            if (tagNames != null
                    && !tagNames.isEmpty()
                    && !filteredPrimaryNames.contains(gameObject.name())) {
                continue;
            }
            if (secondaryValues.put(gameObject.name(), getSecondaryParameterValuesByName(gameObject)) != null) {
                throw new IllegalStateException("Duplicate secondary game object name: " + gameObject.name());
            }
        }
        Set<String> parameterNames = new TreeSet<>(getComparisonParameterNames());

        Set<String> gameObjectNames = new TreeSet<>(primaryValues.keySet());
        gameObjectNames.addAll(secondaryValues.keySet());

        return gameObjectNames.stream()
                .map(gameObjectName -> {
                    Map<String, Double> primary = primaryValues.getOrDefault(gameObjectName, Map.of());
                    Map<String, Double> secondary = secondaryValues.getOrDefault(gameObjectName, Map.of());
                    List<ParameterComparisonDto> parameters = parameterNames.stream()
                            .map(parameterName -> new ParameterComparisonDto(
                                    parameterName,
                                    primary.get(parameterName),
                                    secondary.get(parameterName)
                            ))
                            .toList();
                    return new GameObjectComparisonDto(
                            gameObjectName,
                            primaryValues.containsKey(gameObjectName),
                            secondaryValues.containsKey(gameObjectName),
                            parameters
                    );
                })
                .toList();
    }

    public GameObjectComparisonDto getGameObjectComparison(String gameObjectName) {
        GameObjectComparisonDto comparison = getGameObjectComparisons(null).stream()
                .filter(gameObject -> gameObject.name().equals(gameObjectName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("GameObject not found: " + gameObjectName));
        return new GameObjectComparisonDto(
                comparison.name(),
                comparison.primaryPresent(),
                comparison.secondaryPresent(),
                comparison.parameters().stream()
                        .filter(parameter ->
                                parameter.primaryValue() != null
                                    || parameter.secondaryValue() != null
                        )
                        .toList()
        );
    }

    public List<String> getComparisonParameterNames() {
        Set<String> parameterNames = new TreeSet<>(parameterService.getParameterDtos(false).stream()
                .map(com.wordonline.admin.dto.ParameterDto::name)
                .toList());
        if (hasSecondaryDatabase()) {
            parameterNames.addAll(parameterService.getParameterDtos(true).stream()
                    .map(com.wordonline.admin.dto.ParameterDto::name)
                    .toList());
        }
        return parameterNames.stream().toList();
    }

    private GameObjectDto fromGameObject(GameObject gameObject) {
        try {
            Double magicIdVal = gameObject.getParameterValue("magic_id")
                    .map(ParameterValue::getValue)
                    .orElse(null);
            if (magicIdVal == null) {
                throw new IllegalArgumentException("Magic Not Found");
            }
            long magicId = magicIdVal.longValue();
            Magic magic = magicRepository.findById(magicId)
                    .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

            int manaCost = magic.getMagicCards().stream()
                    .map(MagicCard::getCard)
                    .map(Card::getGameObject)
                    .map(cardData -> cardData.getParameterValue("mana_cost")
                            .map(ParameterValue::getValue)
                            .orElseThrow(() -> new IllegalArgumentException("Mana Cost not Found")))
                    .mapToInt(Double::intValue)
                    .sum();

            return GameObjectDto.fromGameObject(gameObject, manaCost);
        } catch (IllegalArgumentException e) {
            return GameObjectDto.fromGameObject(gameObject, null);
        }
    }

    @Transactional
    public void batchUpdateParameters(
            List<SpreadSheetApiController.ParameterUpdateDto> updates,
            boolean secondary,
            boolean syncSecondary
    ) {
        if (secondary) {
            SecondaryParameterSyncService service = secondaryParameterSyncService.orElseThrow();
            for (SpreadSheetApiController.ParameterUpdateDto update : updates) {
                SecondaryParameterSyncService.GameObjectRow gameObject = service.getGameObject(update.gameObjectId());
                service.upsertParameterValue(gameObject.name(), update.parameterName(), update.value());
            }
            return;
        }

        for (SpreadSheetApiController.ParameterUpdateDto update : updates) {
            parameterService.updateParameterValue(
                    update.gameObjectId(),
                    update.parameterName(),
                    update.value(),
                    syncSecondary
            );
        }
    }

    @Transactional
    public void batchUpdateParametersByName(List<SpreadSheetApiController.NamedParameterUpdateDto> updates) {
        for (SpreadSheetApiController.NamedParameterUpdateDto update : updates) {
            parameterService.upsertParameterValue(
                    update.gameObjectName(),
                    update.parameterName(),
                    update.value(),
                    "secondary".equalsIgnoreCase(update.db())
            );
        }
    }

    @Transactional
    public String syncToSecondary() {
        SyncResult parameterResult = parameterService.syncParametersToSecondary();
        SyncResult valueResult = parameterService.syncParameterValuesToSecondary();
        return parameterResult.toMessage("Parameters: Deploy -> Dev")
                + "\n\n"
                + valueResult.toMessage("Parameter values: Deploy -> Dev");
    }

    @Transactional
    public String syncToPrimary() {
        SyncResult parameterResult = parameterService.syncParametersToPrimary();
        SyncResult valueResult = parameterService.syncParameterValuesToPrimary();
        return parameterResult.toMessage("Parameters: Dev -> Deploy")
                + "\n\n"
                + valueResult.toMessage("Parameter values: Dev -> Deploy");
    }

    private GameObjectDto fromSecondaryGameObject(SecondaryParameterSyncService.GameObjectRow gameObject) {
        Map<String, Double> parameterValues = gameObject.parameterValues()
                .stream()
                .collect(Collectors.toMap(
                        SecondaryParameterSyncService.ParameterValueRow::parameterName,
                        SecondaryParameterSyncService.ParameterValueRow::value,
                        (current, replacement) -> replacement
                ));

        Double manaCost = parameterValues.get("mana_cost");

        return new GameObjectDto(
                gameObject.id(),
                gameObject.name(),
                List.of(),
                manaCost != null ? manaCost.intValue() : null,
                toInteger(parameterValues.get("hp")),
                toInteger(parameterValues.get("quantity")),
                toFloat(parameterValues.get("attack_range")),
                toInteger(parameterValues.get("damage")),
                toFloat(parameterValues.get("attack_interval")),
                toFloat(parameterValues.get("speed")),
                toFloat(parameterValues.get("mass")),
                toFloat(parameterValues.get("radius")),
                toLong(parameterValues.get("magic_id"))
        );
    }

    private boolean hasAllTags(GameObject gameObject, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return true;
        }

        Set<String> gameObjectTagNames = gameObject.getGameObjectTags().stream()
                .map(gameObjectTag -> gameObjectTag.getTag().getName())
                .collect(Collectors.toSet());
        return gameObjectTagNames.containsAll(tagNames);
    }

    private Map<String, Double> getParameterValuesByName(GameObject gameObject) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (ParameterValue value : gameObject.getParameterValues()) {
            String parameterName = value.getParameter().getName();
            if (values.containsKey(parameterName)) {
                throw new IllegalStateException(
                        "Duplicate primary parameter value: " + gameObject.getName() + "." + parameterName
                );
            }
            values.put(parameterName, value.getValue());
        }
        return values;
    }

    private Map<String, Double> getSecondaryParameterValuesByName(
            SecondaryParameterSyncService.GameObjectRow gameObject
    ) {
        Map<String, Double> values = new TreeMap<>();
        for (SecondaryParameterSyncService.ParameterValueRow value : gameObject.parameterValues()) {
            if (values.containsKey(value.parameterName())) {
                throw new IllegalStateException(
                        "Duplicate secondary parameter value: " + gameObject.name() + "." + value.parameterName()
                );
            }
            values.put(value.parameterName(), value.value());
        }
        return values;
    }

    private Integer toInteger(Double value) {
        return value != null ? value.intValue() : null;
    }

    private Long toLong(Double value) {
        return value != null ? value.longValue() : null;
    }

    private Float toFloat(Double value) {
        return value != null ? value.floatValue() : null;
    }
}
