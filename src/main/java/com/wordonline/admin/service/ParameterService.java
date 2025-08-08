package com.wordonline.admin.service;

import com.wordonline.admin.entity.GameObject;
import com.wordonline.admin.entity.Parameter;
import com.wordonline.admin.entity.ParameterValue;
import com.wordonline.admin.repository.GameObjectRepository;
import com.wordonline.admin.repository.ParameterRepository;
import com.wordonline.admin.repository.ParameterValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParameterService {
    private final GameObjectRepository gameObjectRepository;
    private final ParameterValueRepository parameterValueRepository;
    private final ParameterRepository parameterRepository;

    public void createParameter(String name) {
        Parameter parameter = new Parameter(name);
        parameterRepository.save(parameter);
    }

    public void updateParameter(Long parameterId, String name) {
        Parameter parameter = parameterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter") );
        parameter.setName(name);
    }

    public void deleteParameter(Long parameterId) {
        parameterRepository.deleteById(parameterId);
    }

    public void createGameObject(String name) {
        GameObject gameObject = new GameObject(name);
        gameObjectRepository.save(gameObject);
    }

    public void updateGameObject(Long gameObjectId, String name) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject"));
        gameObject.setName(name);
    }

    public void deleteGameObject(Long gameObjectId) {
        gameObjectRepository.deleteById(gameObjectId);
    }

    public void createParameterValue(Long gameObjectId, Long parameterId, Double value) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found GameObject"));
        Parameter parameter = parameterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter"));

        ParameterValue parameterValue = new ParameterValue(value, gameObject, parameter);
        parameterValueRepository.save(parameterValue);
    }

    public void updateParameterValue(Long gameObjectId, Long parameterId, Double value) {
        ParameterValue parameterValue = parameterValueRepository.findByGameObjectIdAndParameterId(gameObjectId, parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter Value"));

        parameterValue.setValue(value);
    }

    public void deleteParameterValue(Long gameObjectId, Long parameterId) {
        ParameterValue parameterValue = parameterValueRepository.findByGameObjectIdAndParameterId(gameObjectId, parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Not Found Parameter Value"));

        parameterValueRepository.delete(parameterValue);
    }

    public void getGameObjects() {

    }
}

