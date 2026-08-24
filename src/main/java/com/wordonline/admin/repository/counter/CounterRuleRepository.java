package com.wordonline.admin.repository.counter;

import com.wordonline.admin.dto.counter.CounterRuleDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CounterRuleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<CounterRuleDto> findAll() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT r.id, attacker.name, target.name, r.weight
                FROM tag_counter_rules r
                JOIN tags attacker ON attacker.id = r.attacker_tag_id
                JOIN tags target ON target.id = r.target_tag_id
                ORDER BY attacker.name, target.name
                """).getResultList();
        return rows.stream().map(row -> new CounterRuleDto(
                ((Number) row[0]).longValue(), (String) row[1], (String) row[2], ((Number) row[3]).doubleValue()
        )).toList();
    }

    public List<String> findTagNames() {
        @SuppressWarnings("unchecked")
        List<String> names = entityManager.createNativeQuery("SELECT name FROM tags ORDER BY name")
                .getResultList();
        return List.copyOf(names);
    }

    public boolean existsPair(String attackerTagName, String targetTagName) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM tag_counter_rules r
                JOIN tags attacker ON attacker.id = r.attacker_tag_id
                JOIN tags target ON target.id = r.target_tag_id
                WHERE attacker.name = :attacker AND target.name = :target
                """).setParameter("attacker", attackerTagName)
                .setParameter("target", targetTagName)
                .getSingleResult();
        return count.intValue() > 0;
    }

    public void create(String attackerTagName, String targetTagName, double weight) {
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO tag_counter_rules(attacker_tag_id, target_tag_id, weight)
                SELECT attacker.id, target.id, :weight
                FROM tags attacker, tags target
                WHERE attacker.name = :attacker AND target.name = :target
                """).setParameter("attacker", attackerTagName)
                .setParameter("target", targetTagName)
                .setParameter("weight", weight)
                .executeUpdate();
        if (inserted != 1) {
            throw new IllegalArgumentException("Tag not found: " + attackerTagName + " or " + targetTagName);
        }
    }

    public void updateWeight(String attackerTagName, String targetTagName, double weight) {
        int updated = entityManager.createNativeQuery("""
                UPDATE tag_counter_rules SET weight = :weight
                WHERE attacker_tag_id = (SELECT id FROM tags WHERE name = :attacker)
                  AND target_tag_id = (SELECT id FROM tags WHERE name = :target)
                """).setParameter("attacker", attackerTagName)
                .setParameter("target", targetTagName)
                .setParameter("weight", weight)
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalArgumentException("Counter rule not found: " + attackerTagName + " -> " + targetTagName);
        }
    }

    public void delete(String attackerTagName, String targetTagName) {
        int deleted = entityManager.createNativeQuery("""
                DELETE FROM tag_counter_rules
                WHERE attacker_tag_id = (SELECT id FROM tags WHERE name = :attacker)
                  AND target_tag_id = (SELECT id FROM tags WHERE name = :target)
                """).setParameter("attacker", attackerTagName)
                .setParameter("target", targetTagName)
                .executeUpdate();
        if (deleted != 1) {
            throw new IllegalArgumentException("Counter rule not found: " + attackerTagName + " -> " + targetTagName);
        }
    }
}
