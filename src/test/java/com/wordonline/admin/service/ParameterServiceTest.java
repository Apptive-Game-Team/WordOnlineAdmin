package com.wordonline.admin.service;

import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import com.wordonline.admin.repository.parameter.ParameterValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
}
