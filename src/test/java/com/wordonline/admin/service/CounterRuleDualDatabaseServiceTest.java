package com.wordonline.admin.service;

import com.wordonline.admin.dto.counter.CounterRuleComparisonDto;
import com.wordonline.admin.dto.counter.CounterRuleDto;
import com.wordonline.admin.dto.counter.CounterRuleForm;
import com.wordonline.admin.dto.counter.MagicTagDto;
import com.wordonline.admin.dto.counter.MagicTagForm;
import com.wordonline.admin.dto.counter.MagicTagSyncResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterRuleDualDatabaseServiceTest {

    @Mock CounterRuleAdminService primary;
    @Mock CounterRuleSecondaryAdminService secondary;
    @Mock ObjectProvider<CounterRuleSecondaryAdminService> secondaryProvider;

    @Test
    void comparesRulesByTagNamePairBecauseIdsDifferBetweenDatabases() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        when(primary.findAll()).thenReturn(List.of(new CounterRuleDto(1L, "CAT_AoE", "CAT_Small", 2.0)));
        when(secondary.findAll()).thenReturn(List.of(
                new CounterRuleDto(77L, "CAT_AoE", "CAT_Small", 1.0),
                new CounterRuleDto(78L, "CAT_CC", "CAT_Tank", 1.5)));

        List<CounterRuleComparisonDto> comparisons = service().comparisons();

        assertEquals(2, comparisons.size());
        CounterRuleComparisonDto shared = comparisons.getFirst();
        assertEquals("CAT_AoE", shared.attackerTagName());
        assertEquals(1L, shared.primary().id());
        assertEquals(77L, shared.secondary().id());
        assertTrue(shared.weightsDiffer());
        assertNull(comparisons.get(1).primary());
    }

    @Test
    void bothTargetWritesDeployThenDev() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        CounterRuleForm form = form("CAT_AoE", "CAT_Small", 2.0);

        service().create(form, CounterRuleDualDatabaseService.Target.BOTH);

        verify(primary).create(form);
        verify(secondary).create(form);
    }

    @Test
    void secondaryTargetOnlyWritesDev() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);

        service().delete("CAT_AoE", "CAT_Small", CounterRuleDualDatabaseService.Target.SECONDARY);

        verify(secondary).delete("CAT_AoE", "CAT_Small");
        verify(primary, never()).delete("CAT_AoE", "CAT_Small");
    }

    @Test
    void secondaryTargetFailsWhenDevDatabaseIsMissing() {
        when(secondaryProvider.getIfAvailable()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service().updateWeight(
                form("CAT_AoE", "CAT_Small", 2.0), CounterRuleDualDatabaseService.Target.SECONDARY));
    }

    @Test
    void primaryTargetStillWorksWithoutADevDatabase() {
        CounterRuleForm form = form("CAT_AoE", "CAT_Small", 2.0);

        service().updateWeight(form, CounterRuleDualDatabaseService.Target.PRIMARY);

        verify(primary).updateWeight(form);
    }

    @Test
    void syncToSecondaryUpsertsEveryDeployRule() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        CounterRuleDto created = new CounterRuleDto(1L, "CAT_AoE", "CAT_Small", 2.0);
        CounterRuleDto updated = new CounterRuleDto(2L, "CAT_CC", "CAT_Tank", 1.5);
        when(primary.findAll()).thenReturn(List.of(created, updated));
        when(secondary.upsert(created)).thenReturn(true);
        when(secondary.upsert(updated)).thenReturn(false);

        var result = service().syncToSecondary();

        assertEquals(1, result.created());
        assertEquals(1, result.updated());
        assertEquals(List.of("CAT_AoE -> CAT_Small", "CAT_CC -> CAT_Tank"), result.pairs());
    }

    @Test
    void syncToPrimaryUpsertsEveryDevRule() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        CounterRuleDto rule = new CounterRuleDto(9L, "CAT_Ranged", "CAT_Melee", 1.5);
        when(secondary.findAll()).thenReturn(List.of(rule));
        when(primary.upsert(rule)).thenReturn(true);

        assertEquals(1, service().syncToPrimary().created());
    }

    @Test
    void magicTagSyncRunsOnlyTheTargetedDatabases() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        when(secondary.syncMagicTags()).thenReturn(3);

        MagicTagSyncResult result = service().syncMagicTags(CounterRuleDualDatabaseService.Target.SECONDARY);

        assertNull(result.primaryRows());
        assertEquals(3, result.secondaryRows());
        assertTrue(result.message().contains("Deploy=skipped"));
        verify(primary, never()).syncMagicTags();
    }

    @Test
    void untaggedDevMagicsAreEmptyWithoutADevDatabase() {
        when(secondaryProvider.getIfAvailable()).thenReturn(null);

        assertEquals(List.<MagicTagDto>of(), service().untaggedSecondaryMagics());
    }

    @Test
    void magicTagAttachOnBothWritesDeployThenDev() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        MagicTagForm form = new MagicTagForm("rallying_torch", "CAT_Buff");

        service().attachTag("rallying_torch", "CAT_Buff", CounterRuleDualDatabaseService.Target.BOTH);

        verify(primary).attachTag(form);
        verify(secondary).attachTag(form);
    }

    @Test
    void magicTagDetachOnSecondaryOnlyTouchesDev() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        MagicTagForm form = new MagicTagForm("rallying_torch", "CAT_Buff");

        service().detachTag("rallying_torch", "CAT_Buff", CounterRuleDualDatabaseService.Target.SECONDARY);

        verify(secondary).detachTag(form);
        verify(primary, never()).detachTag(form);
    }

    @Test
    void magicTagEditOnPrimaryStillWorksWithoutADevDatabase() {
        service().attachTag("rallying_torch", "CAT_Buff", CounterRuleDualDatabaseService.Target.PRIMARY);

        verify(primary).attachTag(new MagicTagForm("rallying_torch", "CAT_Buff"));
    }

    @Test
    void magicTagEditOnDevFailsWhenDevDatabaseIsMissing() {
        when(secondaryProvider.getIfAvailable()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service().attachTag(
                "rallying_torch", "CAT_Buff", CounterRuleDualDatabaseService.Target.SECONDARY));
        verify(primary, never()).attachTag(new MagicTagForm("rallying_torch", "CAT_Buff"));
    }

    @Test
    void aFailedDevAttachAfterASucceededDeployAttachSaysSo() {
        when(secondaryProvider.getIfAvailable()).thenReturn(secondary);
        MagicTagForm form = new MagicTagForm("rallying_torch", "CAT_Buff");
        doThrow(new IllegalArgumentException("Dev magic rallying_torch already has tag CAT_Buff"))
                .when(secondary).attachTag(form);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service()
                .attachTag("rallying_torch", "CAT_Buff", CounterRuleDualDatabaseService.Target.BOTH));

        assertTrue(exception.getMessage().contains("Deploy succeeded but Dev magic tag attach failed"));
        verify(primary).attachTag(form);
    }

    private CounterRuleDualDatabaseService service() {
        return new CounterRuleDualDatabaseService(primary, secondaryProvider);
    }

    private CounterRuleForm form(String attacker, String target, double weight) {
        CounterRuleForm form = new CounterRuleForm();
        form.setAttackerTagName(attacker);
        form.setTargetTagName(target);
        form.setWeight(weight);
        return form;
    }
}
