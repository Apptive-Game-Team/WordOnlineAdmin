package com.wordonline.admin.repository.parameter;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.parameter.ParameterProfile;

public interface ParameterProfileRepository extends JpaRepository<ParameterProfile, Long> {

    Optional<ParameterProfile> findByIsDefaultTrue();
}
