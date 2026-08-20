package com.wordonline.admin.service;

import com.wordonline.admin.dto.counter.CounterRuleDto;
import com.wordonline.admin.dto.counter.CounterRuleForm;
import com.wordonline.admin.dto.counter.MagicTagDto;
import com.wordonline.admin.repository.counter.MagicTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@ConditionalOnBean(name = "secondaryJdbcTemplate")
@RequiredArgsConstructor
@Transactional(transactionManager = "secondaryTransactionManager")
public class CounterRuleSecondaryAdminService {

    @Qualifier("secondaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true, transactionManager = "secondaryTransactionManager")
    public List<CounterRuleDto> findAll() {
        return jdbcTemplate.query("""
                SELECT r.id, attacker.name, target.name, r.weight
                FROM tag_counter_rules r
                JOIN tags attacker ON attacker.id = r.attacker_tag_id
                JOIN tags target ON target.id = r.target_tag_id
                ORDER BY attacker.name, target.name
                """, (rs, rowNum) -> new CounterRuleDto(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getDouble(4)));
    }

    @Transactional(readOnly = true, transactionManager = "secondaryTransactionManager")
    public List<String> tagNames() {
        return jdbcTemplate.queryForList("SELECT name FROM tags ORDER BY name", String.class);
    }

    @Transactional(readOnly = true, transactionManager = "secondaryTransactionManager")
    public List<MagicTagDto> magicTags() {
        return jdbcTemplate.query("""
                SELECT m.id, m.name, COALESCE(STRING_AGG(t.name, ',' ORDER BY t.name), '')
                FROM magics m
                LEFT JOIN magic_tags mt ON mt.magic_id = m.id
                LEFT JOIN tags t ON t.id = mt.tag_id
                GROUP BY m.id, m.name
                ORDER BY m.name
                """, (rs, rowNum) -> new MagicTagDto(
                rs.getLong(1), rs.getString(2), MagicTagRepository.splitTagNames(rs.getString(3))));
    }

    public void create(CounterRuleForm form) {
        if (existsPair(form.getAttackerTagName(), form.getTargetTagName())) {
            throw new IllegalArgumentException("Dev counter rule already exists for "
                    + form.getAttackerTagName() + " -> " + form.getTargetTagName());
        }
        int inserted;
        try {
            inserted = jdbcTemplate.update("""
                    INSERT INTO tag_counter_rules(attacker_tag_id, target_tag_id, weight)
                    SELECT attacker.id, target.id, ?
                    FROM tags attacker, tags target
                    WHERE attacker.name = ? AND target.name = ?
                    """, form.getWeight(), form.getAttackerTagName(), form.getTargetTagName());
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Dev counter rule already exists for "
                    + form.getAttackerTagName() + " -> " + form.getTargetTagName(), exception);
        }
        if (inserted != 1) {
            throw new IllegalArgumentException("Dev tag not found: "
                    + form.getAttackerTagName() + " or " + form.getTargetTagName());
        }
    }

    public void updateWeight(CounterRuleForm form) {
        int updated = jdbcTemplate.update("""
                UPDATE tag_counter_rules SET weight = ?
                WHERE attacker_tag_id = (SELECT id FROM tags WHERE name = ?)
                  AND target_tag_id = (SELECT id FROM tags WHERE name = ?)
                """, form.getWeight(), form.getAttackerTagName(), form.getTargetTagName());
        if (updated != 1) {
            throw new IllegalArgumentException("Dev counter rule not found: "
                    + form.getAttackerTagName() + " -> " + form.getTargetTagName());
        }
    }

    public void delete(String attackerTagName, String targetTagName) {
        int deleted = jdbcTemplate.update("""
                DELETE FROM tag_counter_rules
                WHERE attacker_tag_id = (SELECT id FROM tags WHERE name = ?)
                  AND target_tag_id = (SELECT id FROM tags WHERE name = ?)
                """, attackerTagName, targetTagName);
        if (deleted != 1) {
            throw new IllegalArgumentException("Dev counter rule not found: "
                    + attackerTagName + " -> " + targetTagName);
        }
    }

    public boolean existsPair(String attackerTagName, String targetTagName) {
        return !jdbcTemplate.queryForList("""
                SELECT r.id
                FROM tag_counter_rules r
                JOIN tags attacker ON attacker.id = r.attacker_tag_id
                JOIN tags target ON target.id = r.target_tag_id
                WHERE attacker.name = ? AND target.name = ?
                """, Long.class, attackerTagName, targetTagName).isEmpty();
    }

    public boolean upsert(CounterRuleDto rule) {
        CounterRuleForm form = new CounterRuleForm();
        form.setAttackerTagName(rule.attackerTagName());
        form.setTargetTagName(rule.targetTagName());
        form.setWeight(rule.weight());
        boolean created = !existsPair(rule.attackerTagName(), rule.targetTagName());
        if (created) {
            create(form);
        } else {
            updateWeight(form);
        }
        return created;
    }

    public int syncMagicTags() {
        Integer synced = jdbcTemplate.queryForObject("SELECT sync_magic_tags_from_game_objects()", Integer.class);
        return synced == null ? 0 : synced;
    }
}
