package com.wordonline.admin.repository.parameter;

import com.wordonline.admin.entity.parameter.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParameterRepository extends JpaRepository<Parameter, Long> {
    Optional<Parameter> findByName(String name);
}
