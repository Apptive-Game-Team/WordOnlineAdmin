package com.wordonline.admin.entity.parameter;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name = "parameters")
@AllArgsConstructor
@NoArgsConstructor
public class Parameter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parameters_id_seq")
    @SequenceGenerator(name = "parameters_id_seq", sequenceName = "parameters_id_seq", allocationSize = 1)
    private Long id;

    @Column
    @Setter
    private String name;

    public Parameter(String name) {
        this(null, name);
    }
}
