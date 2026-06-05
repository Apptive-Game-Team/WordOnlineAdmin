package com.wordonline.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wordonline.admin.dto.sheet.GameObjectDto;
import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.magic.MagicCard;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.Parameter;
import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.repository.magic.MagicRepository;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SpreadSheetServiceTest {

    @Mock
    private GameObjectRepository gameObjectRepository;

    @Mock
    private MagicRepository magicRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ParameterService parameterService;

    @Mock
    private SecondaryParameterSyncService secondaryParameterSyncService;

    private SpreadSheetService spreadSheetService;

    @BeforeEach
    void setUp() {
        spreadSheetService = new SpreadSheetService(
                gameObjectRepository,
                magicRepository,
                tagRepository,
                parameterService,
                Optional.of(secondaryParameterSyncService)
        );
    }

    @Test
    void testGetGameObjects_withNullMagicId() {
        // Arrange
        GameObject gameObject = new GameObject("TestObject");
        Parameter parameter = new Parameter("magic_id");
        new ParameterValue(null, gameObject, parameter);

        when(gameObjectRepository.findAll(any(Sort.class))).thenReturn(List.of(gameObject));

        // Act
        List<GameObjectDto> results = spreadSheetService.getGameObjects(null);

        // Assert
        assertEquals(1, results.size());
        assertNull(results.get(0).manaCost());
    }

    @Test
    void testGetGameObjects_withMissingMagicId() {
        // Arrange
        GameObject gameObject = new GameObject("TestObject");

        when(gameObjectRepository.findAll(any(Sort.class))).thenReturn(List.of(gameObject));

        // Act
        List<GameObjectDto> results = spreadSheetService.getGameObjects(null);

        // Assert
        assertEquals(1, results.size());
        assertNull(results.get(0).manaCost());
    }

    @Test
    void testGetGameObjects_withNullManaCost() {
        // Arrange
        GameObject gameObject = new GameObject("TestObject");
        Parameter parameter = new Parameter("magic_id");
        new ParameterValue(42.0, gameObject, parameter);

        Magic magic = new Magic();
        magic.setName("TestMagic");

        Card card = mock(Card.class);
        GameObject cardGameObject = new GameObject("CardObject");
        Parameter manaCostParam = new Parameter("mana_cost");
        new ParameterValue(null, cardGameObject, manaCostParam);

        when(card.getGameObject()).thenReturn(cardGameObject);

        MagicCard magicCard = new MagicCard(1L, magic, card);
        magic.addMagicCard(magicCard);

        when(gameObjectRepository.findAll(any(Sort.class))).thenReturn(List.of(gameObject));
        when(magicRepository.findById(42L)).thenReturn(Optional.of(magic));

        // Act
        List<GameObjectDto> results = spreadSheetService.getGameObjects(null);

        // Assert
        assertEquals(1, results.size());
        assertNull(results.get(0).manaCost());
    }
}
