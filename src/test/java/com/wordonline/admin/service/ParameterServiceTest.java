package com.wordonline.admin.service;

import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.Parameter;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import com.wordonline.admin.repository.parameter.ParameterValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParameterServiceTest {

    @Mock
    private GameObjectRepository gameObjectRepository;
    @Mock
    private ParameterValueRepository parameterValueRepository;
    @Mock
    private ParameterRepository parameterRepository;
    @Mock
    private SecondaryParameterSyncService secondaryParameterSyncService;
    @Mock
    private JdbcTemplate primaryJdbcTemplate;

    private ParameterService parameterService;

    @BeforeEach
    void setUp() {
        parameterService = new ParameterService(
                gameObjectRepository,
                parameterValueRepository,
                parameterRepository,
                Optional.of(secondaryParameterSyncService),
                primaryJdbcTemplate
        );
    }

    @Test
    void getGameObjectComparisons_mergesDatabasesByName() {
        when(gameObjectRepository.findAll()).thenReturn(List.of(new GameObject("deploy-only")));
        when(secondaryParameterSyncService.getGameObjects()).thenReturn(List.of(
                new SecondaryParameterSyncService.GameObjectRow(10L, "dev-only", List.of())
        ));

        var comparisons = parameterService.getGameObjectComparisons();

        assertEquals(List.of("deploy-only", "dev-only"), comparisons.stream().map(item -> item.name()).toList());
        assertTrue(comparisons.getFirst().primaryPresent());
        assertFalse(comparisons.getFirst().secondaryPresent());
        assertFalse(comparisons.get(1).primaryPresent());
        assertTrue(comparisons.get(1).secondaryPresent());
    }

    @Test
    void upsertParameterValue_flushesNewParentsBeforeJdbcInsert() {
        GameObject newGameObject = new GameObject("bubble_spirit");
        ReflectionTestUtils.setField(newGameObject, "id", 3L);
        when(gameObjectRepository.findByName("bubble_spirit")).thenReturn(Optional.empty());
        when(parameterRepository.findByName("hp")).thenReturn(Optional.empty());
        when(gameObjectRepository.saveAndFlush(any(GameObject.class))).thenReturn(newGameObject);
        when(parameterRepository.saveAndFlush(any(Parameter.class))).thenReturn(new Parameter(7L, "hp"));

        parameterService.upsertParameterValue("bubble_spirit", "hp", 120.0, false);

        var order = inOrder(gameObjectRepository, parameterRepository, primaryJdbcTemplate);
        order.verify(gameObjectRepository).saveAndFlush(any(GameObject.class));
        order.verify(parameterRepository).saveAndFlush(any(Parameter.class));
        order.verify(primaryJdbcTemplate).update(anyString(), eq(3L), eq(7L), eq(120.0));
    }

    @Test
    void updateParameter_secondaryWithSyncOther_alsoRenamesPrimary() {
        Parameter primaryParameter = new Parameter(9L, "attack_speed_old");
        when(secondaryParameterSyncService.getParameters()).thenReturn(List.of(
                new SecondaryParameterSyncService.ParameterRow(5L, "attack_speed_old")
        ));
        when(parameterRepository.findByName("attack_speed_old")).thenReturn(Optional.of(primaryParameter));

        parameterService.updateParameter(5L, "attack_speed", true, true);

        verify(secondaryParameterSyncService).updateParameter(5L, "attack_speed");
        assertEquals("attack_speed", primaryParameter.getName());
    }
}
