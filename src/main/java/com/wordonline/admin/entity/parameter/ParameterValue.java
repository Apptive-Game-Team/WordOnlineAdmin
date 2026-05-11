package com.wordonline.admin.entity.parameter;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name = "parameter_values")
@NoArgsConstructor
@AllArgsConstructor
public class ParameterValue {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parameter_values_id_seq")
    @SequenceGenerator(name = "parameter_values_id_seq", sequenceName = "parameter_values_id_seq", allocationSize = 1)
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

    @ManyToOne
    @JoinColumn(name = "parameter_profile_id")
    private ParameterProfile parameterProfile;

    public ParameterValue(Double value, GameObject gameObject, Parameter parameter) {
        this(null, value, gameObject, parameter, null);
        gameObject.addParameterValue(this);
    }

    public ParameterValue(Double value, GameObject gameObject, Parameter parameter, ParameterProfile parameterProfile) {
        this(null, value, gameObject, parameter, parameterProfile);
    }
}
