package com.wordonline.admin.dto.sheet;

import com.wordonline.admin.dto.tag.TagDto;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.entity.tag.GameObjectTag;

import java.util.List;
import java.util.function.Function;

public record GameObjectDto(
        Long id,

        String name,

        List<TagDto> tags,

        Float hpPerMana,
        Float damagePerSecond,
        Float damagePerSecondPerMana,

        Integer manaCost,
        Integer hp,
        Integer quantity,
        Float attackRange,
        Integer damage,
        Float attackInterval,
        Float speed,
        Float mass,
        Float radius,
        Long magicId
) {

    public static GameObjectDto fromGameObject(GameObject gameObject, Integer manaCost) {
        Function<String, Double> getParameterValue = (paramName) ->
                gameObject.getParameterValue(paramName)
                        .map(ParameterValue::getValue)
                        .orElse(null);

        Double hpDouble = getParameterValue.apply("hp");
        Double quantityDouble = getParameterValue.apply("quantity");
        Double attackRangeDouble = getParameterValue.apply("attack_range");
        Double damageDouble = getParameterValue.apply("damage");
        Double attackIntervalDouble = getParameterValue.apply("attack_interval");
        Double speedDouble = getParameterValue.apply("speed");
        Double massDouble = getParameterValue.apply("mass");
        Double radiusDouble = getParameterValue.apply("radius");
        Double magicIdDouble = getParameterValue.apply("magic_id");

        return new GameObjectDto(
                gameObject.getId(),
                gameObject.getName(),

                gameObject.getGameObjectTags().stream().map(GameObjectTag::getTag).map(TagDto::new).toList(),

                manaCost,
                hpDouble != null ? hpDouble.intValue() : null,
                quantityDouble != null ? quantityDouble.intValue() : null,
                attackRangeDouble != null ? attackRangeDouble.floatValue() : null,
                damageDouble != null ? damageDouble.intValue() : null,
                attackIntervalDouble != null ? attackIntervalDouble.floatValue() : null,
                speedDouble != null ? speedDouble.floatValue() : null,
                massDouble != null ? massDouble.floatValue() : null,
                radiusDouble != null ? radiusDouble.floatValue() : null,
                magicIdDouble != null ? magicIdDouble.longValue() : null
        );
    }

    public GameObjectDto(
            Long id,
            String name,

            List<TagDto> tags,

            Integer manaCost,
            Integer hp,
            Integer quantity,
            Float attackRange,
            Integer damage,
            Float attackInterval,
            Float speed,
            Float mass,
            Float radius,
            Long magicId
    ) {
        this(
                id,
                name,
                tags,
                (hp != null && quantity != null && manaCost != null && manaCost != 0) ? (float) (hp * quantity) / manaCost : null,
                (damage != null && attackInterval != null && attackInterval != 0) ? damage / attackInterval : null,
                (damage != null && attackInterval != null && manaCost != null && attackInterval != 0 && manaCost != 0) ? (damage / attackInterval) / manaCost : null,
                manaCost,
                hp,
                quantity,
                attackRange,
                damage,
                attackInterval,
                speed,
                mass,
                radius,
                magicId
        );
    }
}
