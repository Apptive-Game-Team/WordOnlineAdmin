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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpreadSheetService {

    private final GameObjectRepository gameObjectRepository;
    private final MagicRepository magicRepository;
    private final TagRepository tagRepository;
    private final ParameterService parameterService;

    public List<GameObjectDto> getGameObjects(List<String> tagNames) {
        List<GameObject> allGameObjects = gameObjectRepository.findAll();

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

    private GameObjectDto fromGameObject(GameObject gameObject) {
        try {
            long magicId = gameObject.getParameterValue("magic_id")
                    .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"))
                    .getValue().longValue();
            Magic magic = magicRepository.findById(magicId)
                    .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

            int manaCost = magic.getMagicCards().stream()
                    .map(MagicCard::getCard)
                    .map(Card::getGameObject)
                    .map(cardData -> cardData.getParameterValue("mana_cost").orElseThrow(() -> new IllegalArgumentException("Mana Cost not Found")))
                    .map(ParameterValue::getValue)
                    .mapToInt(Double::intValue)
                    .sum();

            return GameObjectDto.fromGameObject(gameObject, manaCost);
        } catch (IllegalArgumentException e) {
            return GameObjectDto.fromGameObject(gameObject, null);
        }
    }

    @Transactional
    public void batchUpdateParameters(List<SpreadSheetApiController.ParameterUpdateDto> updates) {
        for (SpreadSheetApiController.ParameterUpdateDto update : updates) {
            parameterService.updateParameterValue(update.gameObjectId(), update.parameterName(), update.value());
        }
    }
}
