package com.wordonline.admin.entity.parameter;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.wordonline.admin.entity.tag.GameObjectTag;

@Entity
@Getter
@Table(name = "game_objects")
@NoArgsConstructor
public class GameObject {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_objects_id_seq")
    @SequenceGenerator(name = "game_objects_id_seq", sequenceName = "game_objects_id_seq", allocationSize = 1)
    private Long id;

    @Column
    @Setter
    private String name;

    @OneToMany(mappedBy = "gameObject")
    private List<ParameterValue> parameterValues = new ArrayList<>();

    @OneToMany(mappedBy = "gameObject")
    private List<GameObjectTag> gameObjectTags = new ArrayList<>();

    public void addParameterValue(ParameterValue parameterValue) {
        parameterValues.add(parameterValue);
    }

    public Optional<ParameterValue> getParameterValue(String parameterName) {
        return parameterValues.stream()
                .filter(parameterValue -> parameterValue.getParameter().getName().equals(parameterName))
                .findAny();
    }

    public GameObject(String name) {
        this.name = name;
    }
}
