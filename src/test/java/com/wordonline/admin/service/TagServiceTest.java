package com.wordonline.admin.service;

import com.wordonline.admin.dto.tag.TagDto;
import com.wordonline.admin.dto.tag.TagRequestDto;
import com.wordonline.admin.entity.tag.Tag;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.tag.GameObjectTagRepository;
import com.wordonline.admin.repository.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock TagRepository tagRepository;
    @Mock GameObjectRepository gameObjectRepository;
    @Mock GameObjectTagRepository gameObjectTagRepository;

    @Test
    void createsTheTagWhenTheNameIsFree() {
        when(tagRepository.existsByName("CAT_AoE")).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenReturn(new Tag(7L, "CAT_AoE"));

        assertEquals(7L, service().addTag(new TagRequestDto("CAT_AoE")));
    }

    @Test
    void rejectsACreateThatReusesAnExistingName() {
        when(tagRepository.existsByName("CAT_AoE")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().addTag(new TagRequestDto("CAT_AoE")));

        assertTrue(exception.getMessage().contains("CAT_AoE"));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
    }

    @Test
    void rejectsARenameOntoAnotherTagsName() {
        // Guarding only creation is pointless: renaming tag 3 to CAT_AoE makes the same duplicate,
        // and the game server would then sum that matchup's counter weight twice.
        when(tagRepository.existsByNameAndIdNot("CAT_AoE", 3L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().putTag(new TagDto(3L, "CAT_AoE")));

        assertTrue(exception.getMessage().contains("CAT_AoE"));
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
    }

    @Test
    void acceptsARenameThatKeepsTheTagsOwnCurrentName() {
        when(tagRepository.existsByNameAndIdNot("CAT_AoE", 3L)).thenReturn(false);

        service().putTag(new TagDto(3L, "CAT_AoE"));

        verify(tagRepository).saveAndFlush(any(Tag.class));
    }

    @Test
    void translatesTheUniqueConstraintViolationIntoTheSameMessage() {
        // Two admins can pass the pre-check at once; the UNIQUE index on tags(name) then decides,
        // and that failure must not reach the page as raw SQL.
        when(tagRepository.existsByName("CAT_AoE")).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("uq_tags_name"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().addTag(new TagRequestDto("CAT_AoE")));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void translatesTheUniqueConstraintViolationOnRenameToo() {
        when(tagRepository.existsByNameAndIdNot("CAT_AoE", 3L)).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("uq_tags_name"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().putTag(new TagDto(3L, "CAT_AoE")));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    private TagService service() {
        return new TagService(tagRepository, gameObjectRepository, gameObjectTagRepository);
    }
}
