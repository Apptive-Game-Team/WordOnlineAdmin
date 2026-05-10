package com.wordonline.admin.entity.parameter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "parameter_profile_values")
public class ParameterProfileValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parameter_profile_id", nullable = false)
    private ParameterProfile profile;

    @ManyToOne
    @JoinColumn(name = "game_object_id", nullable = false)
    private GameObject gameObject;

    @ManyToOne
    @JoinColumn(name = "parameter_id", nullable = false)
    private Parameter parameter;

    private Double value;

    public ParameterProfileValue(ParameterProfile profile, GameObject gameObject, Parameter parameter, Double value) {
        this.profile = profile;
        this.gameObject = gameObject;
        this.parameter = parameter;
        this.value = value;
    }
}
