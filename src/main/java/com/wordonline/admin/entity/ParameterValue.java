package com.wordonline.admin.entity;

import com.wordonline.admin.repository.ParameterValueRepository;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parameter_values")
@NoArgsConstructor
@AllArgsConstructor
public class ParameterValue {
    @Id
    private Long id;

    @Column
    @Setter
    private Double value;

    @ManyToOne
    @JoinColumn(name = "game_object_id")
    private GameObject gameObject;

    @ManyToOne
    @JoinColumn(name = "parameter_id")
    private Parameter parameter;

    public ParameterValue(Double value, GameObject gameObject, Parameter parameter) {
        this(null, value, gameObject, parameter);
        gameObject.addParameterValue(this);
    }
}
