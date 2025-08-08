package com.wordonline.admin.repository;

import com.wordonline.admin.entity.ParameterValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParameterValueRepository extends JpaRepository<ParameterValue, Long> {
    Optional<ParameterValue> findByGameObjectIdAndParameterId(Long gameObjectId, Long parameterId);
}
