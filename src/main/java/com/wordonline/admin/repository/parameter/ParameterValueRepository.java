package com.wordonline.admin.repository.parameter;

import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParameterValueRepository extends JpaRepository<ParameterValue, Long> {
    Optional<ParameterValue> findByGameObjectAndParameter(GameObject gameObject, Parameter parameter);

    @Query("select pv from ParameterValue pv where pv.gameObject.id = :gameObjectId and pv.parameter.id = :parameterId")
    Optional<ParameterValue> findByGameObjectIdAndParameterId(
            @Param("gameObjectId") Long gameObjectId,
            @Param("parameterId") Long parameterId
    );

    @Query("select pv from ParameterValue pv join fetch pv.gameObject join fetch pv.parameter")
    List<ParameterValue> findAllWithGameObjectAndParameter();
}
