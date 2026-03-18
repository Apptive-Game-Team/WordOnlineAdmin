package com.wordonline.admin.entity.adventure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "adventures")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Adventure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 31, nullable = false)
    private String name;

    @Column(name = "access_type", length = 10, nullable = false)
    private String accessType;
}
