package com.wordonline.admin.repository.parameter;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.parameter.ParameterProfileValue;

public interface ParameterProfileValueRepository extends JpaRepository<ParameterProfileValue, Long> {

    List<ParameterProfileValue> findAllByProfileId(Long profileId);
}
