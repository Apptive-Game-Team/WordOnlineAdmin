package com.wordonline.admin.repository.parameter;

import com.wordonline.admin.entity.parameter.ParameterValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParameterValueRepository extends JpaRepository<ParameterValue, Long> {
}