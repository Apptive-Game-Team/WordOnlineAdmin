package com.wordonline.admin.service;

import com.wordonline.admin.dto.counter.CounterRuleComparisonDto;
import com.wordonline.admin.dto.counter.CounterRuleDto;
import com.wordonline.admin.dto.counter.CounterRuleForm;
import com.wordonline.admin.dto.counter.CounterRuleSyncResult;
import com.wordonline.admin.dto.counter.MagicTagComparisonDto;
import com.wordonline.admin.dto.counter.MagicTagDto;
import com.wordonline.admin.dto.counter.MagicTagForm;
import com.wordonline.admin.dto.counter.MagicTagSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CounterRuleDualDatabaseService {

    public enum Target { PRIMARY, SECONDARY, BOTH }

    private final CounterRuleAdminService primary;
    private final ObjectProvider<CounterRuleSecondaryAdminService> secondaryProvider;

    public boolean hasSecondary() {
        return secondaryProvider.getIfAvailable() != null;
    }

    // Rule ids are assigned per database, so the deploy and dev rows for one matchup never share
    // an id. The tag name pair is the only identifier both databases agree on.
    public List<CounterRuleComparisonDto> comparisons() {
        Map<String, CounterRuleDto> primaryRules = byPair(primary.findAll());
        CounterRuleSecondaryAdminService secondary = secondaryProvider.getIfAvailable();
        Map<String, CounterRuleDto> secondaryRules = secondary == null ? Map.of() : byPair(secondary.findAll());
        TreeSet<String> pairs = new TreeSet<>(primaryRules.keySet());
        pairs.addAll(secondaryRules.keySet());
        return pairs.stream().map(pair -> {
            CounterRuleDto primaryRule = primaryRules.get(pair);
            CounterRuleDto secondaryRule = secondaryRules.get(pair);
            CounterRuleDto present = primaryRule != null ? primaryRule : secondaryRule;
            return new CounterRuleComparisonDto(present.attackerTagName(), present.targetTagName(),
                    primaryRule, secondaryRule);
        }).toList();
    }

    public List<MagicTagComparisonDto> magicTagComparisons() {
        Map<String, MagicTagDto> primaryMagics = byMagicName(primary.magicTags());
        CounterRuleSecondaryAdminService secondary = secondaryProvider.getIfAvailable();
        Map<String, MagicTagDto> secondaryMagics = secondary == null ? Map.of() : byMagicName(secondary.magicTags());
        TreeSet<String> names = new TreeSet<>(primaryMagics.keySet());
        names.addAll(secondaryMagics.keySet());
        return names.stream()
                .map(name -> new MagicTagComparisonDto(name, primaryMagics.get(name), secondaryMagics.get(name)))
                .toList();
    }

    public List<MagicTagDto> untaggedPrimaryMagics() {
        return primary.untaggedMagics();
    }

    public List<MagicTagDto> untaggedSecondaryMagics() {
        CounterRuleSecondaryAdminService secondary = secondaryProvider.getIfAvailable();
        return secondary == null
                ? List.of()
                : secondary.magicTags().stream().filter(MagicTagDto::untagged).toList();
    }

    public List<String> tagNames() {
        return primary.tagNames();
    }

    public void create(CounterRuleForm form, Target target) {
        primary.validateForm(form);
        requireSecondary(target);
        if (target != Target.SECONDARY) primary.create(form);
        if (target != Target.PRIMARY) applySecondary(target, "rule create", () -> secondary().create(form));
    }

    public void updateWeight(CounterRuleForm form, Target target) {
        primary.validateForm(form);
        requireSecondary(target);
        if (target != Target.SECONDARY) primary.updateWeight(form);
        if (target != Target.PRIMARY) applySecondary(target, "weight update", () -> secondary().updateWeight(form));
    }

    public void delete(String attackerTagName, String targetTagName, Target target) {
        requireSecondary(target);
        if (target != Target.SECONDARY) primary.delete(attackerTagName, targetTagName);
        if (target != Target.PRIMARY) {
            applySecondary(target, "rule delete", () -> secondary().delete(attackerTagName, targetTagName));
        }
    }

    public void attachTag(String magicName, String tagName, Target target) {
        MagicTagForm form = new MagicTagForm(magicName, tagName);
        requireSecondary(target);
        if (target != Target.SECONDARY) primary.attachTag(form);
        if (target != Target.PRIMARY) applySecondary(target, "magic tag attach", () -> secondary().attachTag(form));
    }

    public void detachTag(String magicName, String tagName, Target target) {
        MagicTagForm form = new MagicTagForm(magicName, tagName);
        requireSecondary(target);
        if (target != Target.SECONDARY) primary.detachTag(form);
        if (target != Target.PRIMARY) applySecondary(target, "magic tag detach", () -> secondary().detachTag(form));
    }

    public CounterRuleSyncResult syncToSecondary() {
        CounterRuleSecondaryAdminService secondary = secondary();
        return sync(primary.findAll(), secondary::upsert);
    }

    public CounterRuleSyncResult syncToPrimary() {
        return sync(secondary().findAll(), primary::upsert);
    }

    public MagicTagSyncResult syncMagicTags(Target target) {
        requireSecondary(target);
        Integer primaryRows = target == Target.SECONDARY ? null : primary.syncMagicTags();
        Integer secondaryRows = target == Target.PRIMARY ? null : secondary().syncMagicTags();
        return new MagicTagSyncResult(primaryRows, secondaryRows);
    }

    public Target target(String db) {
        return switch (db == null ? "primary" : db.toLowerCase()) {
            case "secondary" -> Target.SECONDARY;
            case "both" -> Target.BOTH;
            default -> Target.PRIMARY;
        };
    }

    private CounterRuleSyncResult sync(List<CounterRuleDto> source, Function<CounterRuleDto, Boolean> upsert) {
        int created = 0;
        int updated = 0;
        List<String> pairs = new ArrayList<>();
        for (CounterRuleDto rule : source) {
            if (upsert.apply(rule)) created++; else updated++;
            pairs.add(rule.pairKey());
        }
        return new CounterRuleSyncResult(created, updated, pairs);
    }

    private Map<String, CounterRuleDto> byPair(List<CounterRuleDto> rules) {
        return rules.stream().collect(Collectors.toMap(CounterRuleDto::pairKey, Function.identity(),
                (first, second) -> first, LinkedHashMap::new));
    }

    private Map<String, MagicTagDto> byMagicName(List<MagicTagDto> magics) {
        return magics.stream().collect(Collectors.toMap(MagicTagDto::magicName, Function.identity(),
                (first, second) -> first, LinkedHashMap::new));
    }

    private CounterRuleSecondaryAdminService secondary() {
        CounterRuleSecondaryAdminService service = secondaryProvider.getIfAvailable();
        if (service == null) throw new IllegalStateException("Dev database is not configured");
        return service;
    }

    private void requireSecondary(Target target) {
        if (target != Target.PRIMARY) secondary();
    }

    private void applySecondary(Target target, String operation, Runnable action) {
        if (target == Target.BOTH) runSecondaryAfterPrimary(operation, action); else action.run();
    }

    private void runSecondaryAfterPrimary(String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Deploy succeeded but Dev " + operation + " failed", exception);
        }
    }
}
