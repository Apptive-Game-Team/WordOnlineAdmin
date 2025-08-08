package com.wordonline.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parameters")
@AllArgsConstructor
@NoArgsConstructor
public class Parameter {
    @Id
    private Long id;

    @Column
    @Setter
    private String name;

    public Parameter(String name) {
        this(null, name);
    }
}
