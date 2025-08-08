package com.wordonline.admin.controller;

import com.wordonline.admin.service.ParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/game-objects")
@RequiredArgsConstructor
public class GameObjectController {

    private final ParameterService parameterService;

    @PostMapping
    public ResponseEntity<String> saveGameObject(
            @RequestBody String name
    ) {
        parameterService.createGameObject(name);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/{gameObjectId}")
    public ResponseEntity<String> updateGameObject(
            @PathVariable Long gameObjectId,
            @RequestBody String name
    ) {
        parameterService.updateGameObject(gameObjectId, name);
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/{gameObjectId}")
    public ResponseEntity<String> deleteGameObject(
            @PathVariable Long gameObjectId
    ) {
        parameterService.deleteGameObject(gameObjectId);
        return ResponseEntity.ok("Successfully Deleted");
    }

    @PostMapping("/{gameObjectId}/parameter-values/{parameterId}")
    public ResponseEntity<String> saveParameterValue(
            @PathVariable Long gameObjectId,
            @PathVariable Long parameterId,
            @RequestBody Double value
    ) {
        parameterService.createParameterValue(gameObjectId, parameterId, value);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully Created");
    }

    @PatchMapping("/{gameObjectId}/parameter-values/{parameterId}")
    public ResponseEntity<String> updateParameterValue(
            @PathVariable Long gameObjectId,
            @PathVariable Long parameterId,
            @RequestBody Double value
    ) {
        parameterService.updateParameterValue(gameObjectId, parameterId, value);
        return ResponseEntity.ok("Successfully Updated");
    }

    @DeleteMapping("/{gameObjectId}/parameter-values/{parameterId}")
    public ResponseEntity<String> deleteParameterValue(
            @PathVariable Long gameObjectId,
            @PathVariable Long parameterId
    ) {
        parameterService.deleteParameterValue(gameObjectId, parameterId);
        return ResponseEntity.ok("Successfully Deleted");
    }
}
