package com.wordonline.admin.service;

import com.wordonline.admin.dto.CardDto;
import com.wordonline.admin.dto.MagicComparisonDto;
import com.wordonline.admin.dto.MagicDto;
import com.wordonline.admin.entity.magic.CardType;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.repository.magic.CardRepository;
import com.wordonline.admin.repository.magic.MagicCardRepository;
import com.wordonline.admin.repository.magic.MagicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagicServiceTest {

    @Mock
    private MagicRepository magicRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private MagicCardRepository magicCardRepository;
    @Mock
    private SecondaryAdminDataService secondaryAdminDataService;
    private MagicService magicService;

    @BeforeEach
    void setUp() {
        magicService = new MagicService(
                magicRepository,
                cardRepository,
                magicCardRepository,
                Optional.of(secondaryAdminDataService)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"spawn", "drop", "explode", "build", "shoot"})
    void createsAMagicForEveryCastTypeTheCheckConstraintAllows(String castType) {
        magicService.createMagic("fireball", castType, "DEFAULT", false);

        ArgumentCaptor<Magic> saved = ArgumentCaptor.forClass(Magic.class);
        verify(magicRepository).saveAndFlush(saved.capture());
        assertEquals("fireball", saved.getValue().getName());
        assertEquals(castType, saved.getValue().getCastType());
        assertEquals("DEFAULT", saved.getValue().getAccessType());
    }

    @Test
    void rejectsACreateWithoutACastType() {
        // This is the bug the page had: no cast_type at all, so the INSERT left out a NOT NULL column.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magicService.createMagic("fireball", null, "DEFAULT", false));

        assertTrue(exception.getMessage().contains("spawn"));
        verify(magicRepository, never()).saveAndFlush(any(Magic.class));
    }

    @Test
    void rejectsACreateWithACastTypeTheCheckConstraintWouldRefuse() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magicService.createMagic("fireball", "summon", "DEFAULT", false));

        assertTrue(exception.getMessage().contains("summon"));
        verify(magicRepository, never()).saveAndFlush(any(Magic.class));
    }

    @Test
    void rejectsACreateWithoutAnAccessType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magicService.createMagic("fireball", "spawn", "  ", false));

        assertTrue(exception.getMessage().contains("access type"));
        verify(magicRepository, never()).saveAndFlush(any(Magic.class));
    }

    @Test
    void rejectsAnAccessTypeLongerThanTheColumn() {
        // access_type is varchar(10); a longer value would only fail as a raw SQL error.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magicService.createMagic("fireball", "spawn", "EVERYONE_FOREVER", false));

        assertTrue(exception.getMessage().contains("10 characters"));
        verify(magicRepository, never()).saveAndFlush(any(Magic.class));
    }

    @Test
    void createsOnTheSecondaryDatabaseWithBothValues() {
        magicService.createMagic("fireball", "build", "DEFAULT", true);

        verify(secondaryAdminDataService).createMagic("fireball", "build", "DEFAULT");
        verify(magicRepository, never()).saveAndFlush(any(Magic.class));
    }

    @Test
    void updatesBothValuesOnAnExistingMagic() {
        Magic magic = new Magic();
        magic.setName("fireball");
        magic.setCastType("spawn");
        magic.setAccessType("DEFAULT");
        when(magicRepository.findByName("fireball")).thenReturn(Optional.of(magic));

        magicService.updateMagic("fireball", "fire_ball", "shoot", "LOCKED", false);

        assertEquals("fire_ball", magic.getName());
        assertEquals("shoot", magic.getCastType());
        assertEquals("LOCKED", magic.getAccessType());
        verify(magicRepository).saveAndFlush(magic);
    }

    @Test
    void updatesBothValuesOnTheSecondaryDatabase() {
        magicService.updateMagic("fireball", "fire_ball", "drop", "DEFAULT", true);

        verify(secondaryAdminDataService).updateMagic("fireball", "fire_ball", "drop", "DEFAULT");
    }

    @Test
    void translatesAConstraintViolationIntoAReadableMessage() {
        // The pre-check and the CHECK can disagree — a value this admin build considers valid may be
        // refused by a database that has not run the migration yet. That must not reach the page as SQL.
        when(magicRepository.saveAndFlush(any(Magic.class)))
                .thenThrow(new DataIntegrityViolationException("chk_magics_cast_type"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> magicService.createMagic("fireball", "spawn", "DEFAULT", false));

        assertTrue(exception.getMessage().contains("fireball"));
        assertTrue(exception.getMessage().contains("spawn, drop, explode, build, shoot"));
    }

    @Test
    void getMagicComparisons_mergesMagicsAndCardsByName() {
        when(magicRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(secondaryAdminDataService.getMagics()).thenReturn(List.of(
                new MagicDto(
                        20L,
                        "fireball",
                        "shoot",
                        "DEFAULT",
                        List.of(new CardDto(200L, "fire-card", CardType.Magic))
                )
        ));

        var comparisons = magicService.getMagicComparisons();

        assertEquals(1, comparisons.size());
        assertEquals("fireball", comparisons.getFirst().name());
        assertFalse(comparisons.getFirst().primaryPresent());
        assertTrue(comparisons.getFirst().secondaryPresent());
        assertEquals("shoot", comparisons.getFirst().secondaryCastType());
        assertEquals("DEFAULT", comparisons.getFirst().secondaryAccessType());
        assertEquals("fire-card", comparisons.getFirst().cards().getFirst().name());
        assertFalse(comparisons.getFirst().cards().getFirst().primaryPresent());
        assertTrue(comparisons.getFirst().cards().getFirst().secondaryPresent());
    }

    @Test
    void getMagicComparisons_marksACastTypeThatDiffersBetweenTheDatabases() {
        Magic primaryMagic = new Magic();
        primaryMagic.setName("fireball");
        primaryMagic.setCastType("shoot");
        primaryMagic.setAccessType("DEFAULT");
        ReflectionTestUtils.setField(primaryMagic, "id", 10L);
        when(magicRepository.findAllByOrderByIdAsc()).thenReturn(List.of(primaryMagic));
        when(secondaryAdminDataService.getMagics()).thenReturn(List.of(
                new MagicDto(20L, "fireball", "explode", "DEFAULT", List.of())
        ));

        MagicComparisonDto comparison = magicService.getMagicComparisons().getFirst();

        assertEquals("shoot", comparison.primaryCastType());
        assertEquals("explode", comparison.secondaryCastType());
        assertTrue(comparison.castTypeDiffers());
        assertFalse(comparison.accessTypeDiffers());
    }
}
