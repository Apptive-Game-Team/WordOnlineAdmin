package com.wordonline.admin.controller;

import com.wordonline.admin.dto.GameObjectDto;
import com.wordonline.admin.dto.ParameterValueDto;
import com.wordonline.admin.service.ParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class GameObjectController {

    private final ParameterService parameterService;

    @PostMapping("/game-objects")
    public ResponseEntity<String> saveGameObject(
            @RequestBody GameObjectDto dto
    ) {
        parameterService.createGameObject(dto.name());
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/game-objects/{gameObjectId}")
    public ResponseEntity<String> updateGameObject(
            @PathVariable Long gameObjectId,
            @RequestBody GameObjectDto dto
    ) {
        parameterService.updateGameObject(gameObjectId, dto.name());
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/game-objects/{gameObjectId}")
    public ResponseEntity<String> deleteGameObject(
            @PathVariable Long gameObjectId
    ) {
        parameterService.deleteGameObject(gameObjectId);
        return ResponseEntity.ok("Successfully Deleted");
    }

    @PostMapping("/game-objects/{gameObjectId}/parameter-values")
    public ResponseEntity<String> saveParameterValue(
            @PathVariable Long gameObjectId,
            @RequestBody ParameterValueDto dto
    ) {
        parameterService.createParameterValue(gameObjectId, dto.parameterId(), dto.value());
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/parameter-values/{parameterValueId}")
    public ResponseEntity<String> updateParameterValue(
            @PathVariable Long parameterValueId,
            @RequestBody ParameterValueDto dto
    ) {
        parameterService.updateParameterValue(parameterValueId, dto.value());
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/parameter-values/{parameterValueId}")
    public ResponseEntity<String> deleteParameterValue(
            @PathVariable Long parameterValueId
    ) {
        parameterService.deleteParameterValue(parameterValueId);
        return ResponseEntity.ok("Successfully Deleted");
    }
}