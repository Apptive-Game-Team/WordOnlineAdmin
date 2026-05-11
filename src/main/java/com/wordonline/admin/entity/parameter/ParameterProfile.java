package com.wordonline.admin.entity.parameter;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "parameter_profiles")
public class ParameterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true)
    private String name;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_profile_id")
    private ParameterProfile parentProfile;

    @Setter
    private String description;

    @Setter
    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ParameterProfile(String name, ParameterProfile parentProfile, String description, boolean isDefault) {
        this.name = name;
        this.parentProfile = parentProfile;
        this.description = description;
        this.isDefault = isDefault;
        this.createdAt = LocalDateTime.now();
    }
}
