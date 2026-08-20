package com.wordonline.admin.repository.counter;

import com.wordonline.admin.dto.counter.MagicTagDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class MagicTagRepository {

    // Every magic is returned, tagged or not, so the untagged ones are a filter over one result
    // set rather than a second query that could drift away from the first.
    private static final String FIND_ALL = """
            SELECT m.id, m.name, COALESCE(STRING_AGG(t.name, ',' ORDER BY t.name), '')
            FROM magics m
            LEFT JOIN magic_tags mt ON mt.magic_id = m.id
            LEFT JOIN tags t ON t.id = mt.tag_id
            GROUP BY m.id, m.name
            ORDER BY m.name
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public List<MagicTagDto> findAll() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(FIND_ALL).getResultList();
        return rows.stream().map(row -> new MagicTagDto(
                ((Number) row[0]).longValue(), (String) row[1], splitTagNames((String) row[2])
        )).toList();
    }

    public int syncFromGameObjects() {
        return ((Number) entityManager.createNativeQuery("SELECT sync_magic_tags_from_game_objects()")
                .getSingleResult()).intValue();
    }

    public static List<String> splitTagNames(String aggregated) {
        if (aggregated == null || aggregated.isBlank()) {
            return List.of();
        }
        return Arrays.stream(aggregated.split(",")).map(String::trim).filter(name -> !name.isEmpty()).toList();
    }
}
