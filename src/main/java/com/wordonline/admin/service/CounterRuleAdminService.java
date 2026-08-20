package com.wordonline.admin.service;

import com.wordonline.admin.dto.counter.CounterRuleDto;
import com.wordonline.admin.dto.counter.CounterRuleForm;
import com.wordonline.admin.dto.counter.MagicTagDto;
import com.wordonline.admin.repository.counter.CounterRuleRepository;
import com.wordonline.admin.repository.counter.MagicTagRepository;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CounterRuleAdminService {

    private final CounterRuleRepository counterRuleRepository;
    private final MagicTagRepository magicTagRepository;

    @Transactional(readOnly = true)
    public List<CounterRuleDto> findAll() {
        return counterRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<String> tagNames() {
        return counterRuleRepository.findTagNames();
    }

    @Transactional(readOnly = true)
    public List<MagicTagDto> magicTags() {
        return magicTagRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MagicTagDto> untaggedMagics() {
        return magicTags().stream().filter(MagicTagDto::untagged).toList();
    }

    public void create(CounterRuleForm form) {
        validate(form);
        String attacker = form.getAttackerTagName();
        String target = form.getTargetTagName();
        rejectExistingPair(attacker, target);
        try {
            counterRuleRepository.create(attacker, target, form.getWeight());
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            // uq_tag_counter_rules_pair is the last line of defence when two admins create the
            // same pair at once; without this the page would show a raw SQL failure.
            throw new IllegalArgumentException(duplicateMessage(attacker, target), exception);
        }
    }

    public void updateWeight(CounterRuleForm form) {
        validate(form);
        counterRuleRepository.updateWeight(form.getAttackerTagName(), form.getTargetTagName(), form.getWeight());
    }

    public void delete(String attackerTagName, String targetTagName) {
        counterRuleRepository.delete(requireText(attackerTagName, "Attacker tag"),
                requireText(targetTagName, "Target tag"));
    }

    public boolean upsert(CounterRuleDto rule) {
        boolean created = !counterRuleRepository.existsPair(rule.attackerTagName(), rule.targetTagName());
        if (created) {
            counterRuleRepository.create(rule.attackerTagName(), rule.targetTagName(), rule.weight());
        } else {
            counterRuleRepository.updateWeight(rule.attackerTagName(), rule.targetTagName(), rule.weight());
        }
        return created;
    }

    public int syncMagicTags() {
        return magicTagRepository.syncFromGameObjects();
    }

    public void validateForm(CounterRuleForm form) {
        validate(form);
    }

    private void rejectExistingPair(String attackerTagName, String targetTagName) {
        if (counterRuleRepository.existsPair(attackerTagName, targetTagName)) {
            throw new IllegalArgumentException(duplicateMessage(attackerTagName, targetTagName));
        }
    }

    private void validate(CounterRuleForm form) {
        form.setAttackerTagName(requireText(form.getAttackerTagName(), "Attacker tag"));
        form.setTargetTagName(requireText(form.getTargetTagName(), "Target tag"));
        if (!Double.isFinite(form.getWeight())) {
            throw new IllegalArgumentException("Weight must be a finite number");
        }
    }

    private String duplicateMessage(String attackerTagName, String targetTagName) {
        return "Counter rule already exists for " + attackerTagName + " -> " + targetTagName
                + "; edit its weight instead of creating a second rule";
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
