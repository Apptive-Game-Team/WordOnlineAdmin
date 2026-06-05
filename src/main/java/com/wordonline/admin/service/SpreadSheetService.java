package com.wordonline.admin.service;

import com.wordonline.admin.controller.SpreadSheetApiController;
import com.wordonline.admin.dto.sheet.GameObjectDto;
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
import java.util.Map;
import java.util.Optional;
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
    public String syncToSecondary() {
        SyncResult parameterResult = parameterService.syncParametersToSecondary();
        SyncResult valueResult = parameterService.syncParameterValuesToSecondary();
        return parameterResult.toMessage("Parameters: Prod -> Dev")
                + "\n\n"
                + valueResult.toMessage("Parameter values: Prod -> Dev");
    }

    @Transactional
    public String syncToPrimary() {
        SyncResult parameterResult = parameterService.syncParametersToPrimary();
        SyncResult valueResult = parameterService.syncParameterValuesToPrimary();
        return parameterResult.toMessage("Parameters: Dev -> Prod")
                + "\n\n"
                + valueResult.toMessage("Parameter values: Dev -> Prod");
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
