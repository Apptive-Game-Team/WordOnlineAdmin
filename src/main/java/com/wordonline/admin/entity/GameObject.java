package com.wordonline.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_objects")
@NoArgsConstructor
public class GameObject {
    @Id
    private Long id;

    @Column
    @Setter
    private String name;

    @OneToMany(mappedBy = "gameObject")
    private List<ParameterValue> parameterValues = new ArrayList<>();

    public void addParameterValue(ParameterValue parameterValue) {
        parameterValues.add(parameterValue);
    }

    public GameObject(String name) {
        this.name = name;
    }
}
