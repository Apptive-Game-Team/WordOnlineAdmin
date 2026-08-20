package com.wordonline.admin.service;

import com.wordonline.admin.dto.counter.CounterRuleDto;
import com.wordonline.admin.dto.counter.CounterRuleForm;
import com.wordonline.admin.dto.counter.MagicTagDto;
import com.wordonline.admin.repository.counter.CounterRuleRepository;
import com.wordonline.admin.repository.counter.MagicTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterRuleAdminServiceTest {

    @Mock CounterRuleRepository counterRuleRepository;
    @Mock MagicTagRepository magicTagRepository;

    @Test
    void createsRuleWhenThePairIsFree() {
        when(counterRuleRepository.existsPair("CAT_AoE", "CAT_Small")).thenReturn(false);

        service().create(form("CAT_AoE", "CAT_Small", 2.0));

        verify(counterRuleRepository).create("CAT_AoE", "CAT_Small", 2.0);
    }

    @Test
    void rejectsADuplicatePairWithAReadableMessageInsteadOfHittingTheConstraint() {
        when(counterRuleRepository.existsPair("CAT_AoE", "CAT_Small")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().create(form("CAT_AoE", "CAT_Small", 2.0)));

        assertTrue(exception.getMessage().contains("CAT_AoE -> CAT_Small"));
        verify(counterRuleRepository, never()).create(anyString(), anyString(), anyDouble());
    }

    @Test
    void translatesTheUniqueConstraintViolationIntoTheSameMessage() {
        // Two admins can pass the pre-check at once; uq_tag_counter_rules_pair then decides, and
        // that failure must not reach the page as raw SQL.
        when(counterRuleRepository.existsPair("CAT_CC", "CAT_Tank")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("uq_tag_counter_rules_pair"))
                .when(counterRuleRepository).create("CAT_CC", "CAT_Tank", 1.5);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().create(form("CAT_CC", "CAT_Tank", 1.5)));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void trimsTagNamesAndRejectsBlankOnes() {
        when(counterRuleRepository.existsPair("CAT_Ranged", "CAT_Melee")).thenReturn(false);

        service().create(form("  CAT_Ranged  ", "CAT_Melee", 1.5));

        verify(counterRuleRepository).create("CAT_Ranged", "CAT_Melee", 1.5);
        assertThrows(IllegalArgumentException.class, () -> service().create(form(" ", "CAT_Melee", 1.5)));
    }

    @Test
    void rejectsANonFiniteWeight() {
        assertThrows(IllegalArgumentException.class,
                () -> service().updateWeight(form("CAT_AoE", "CAT_Small", Double.NaN)));
    }

    @Test
    void updatesAndDeletesByTagNamePair() {
        service().updateWeight(form("CAT_AoE", "CAT_Building", 1.5));
        service().delete("CAT_AoE", "CAT_Building");

        verify(counterRuleRepository).updateWeight("CAT_AoE", "CAT_Building", 1.5);
        verify(counterRuleRepository).delete("CAT_AoE", "CAT_Building");
    }

    @Test
    void untaggedMagicsKeepsOnlyTheMagicsThatNoBotWouldEverScore() {
        when(magicTagRepository.findAll()).thenReturn(List.of(
                new MagicTagDto(1, "bubble_spirit", List.of("TYPE_Unit", "CAT_Small")),
                new MagicTagDto(2, "rallying_torch", List.of()),
                new MagicTagDto(3, "lightning_cloud", List.of("CAT_AoE"))));

        assertEquals(List.of("rallying_torch"),
                service().untaggedMagics().stream().map(MagicTagDto::magicName).toList());
    }

    @Test
    void upsertCreatesTheMissingPairAndUpdatesTheExistingOne() {
        when(counterRuleRepository.existsPair("CAT_CC", "CAT_Large")).thenReturn(false);
        when(counterRuleRepository.existsPair("CAT_AoE", "CAT_Small")).thenReturn(true);

        assertTrue(service().upsert(new CounterRuleDto(1L, "CAT_CC", "CAT_Large", 1.5)));
        assertFalse(service().upsert(new CounterRuleDto(2L, "CAT_AoE", "CAT_Small", 2.0)));

        verify(counterRuleRepository).create("CAT_CC", "CAT_Large", 1.5);
        verify(counterRuleRepository).updateWeight("CAT_AoE", "CAT_Small", 2.0);
    }

    private CounterRuleAdminService service() {
        return new CounterRuleAdminService(counterRuleRepository, magicTagRepository);
    }

    private CounterRuleForm form(String attacker, String target, double weight) {
        CounterRuleForm form = new CounterRuleForm();
        form.setAttackerTagName(attacker);
        form.setTargetTagName(target);
        form.setWeight(weight);
        return form;
    }
}
