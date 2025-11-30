package com.wordonline.admin.repository.parameter;

import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParameterValueRepository extends JpaRepository<ParameterValue, Long> {
    Optional<ParameterValue> findByGameObjectAndParameter(GameObject gameObject, Parameter parameter);
}