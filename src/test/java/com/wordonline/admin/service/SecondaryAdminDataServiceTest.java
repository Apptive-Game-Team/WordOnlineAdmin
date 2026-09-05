package com.wordonline.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondaryAdminDataServiceTest {

    @Mock JdbcTemplate jdbcTemplate;

    @Test
    void addCardByNameFailsWhenNoRowInserted() {
        assertThrows(IllegalArgumentException.class,
                () -> service().addCardToMagic("MissingMagic", "Fire"));
    }

    @Test
    void addCardByNameSucceedsWhenRowInserted() {
        when(jdbcTemplate.update(anyString(), anyString(), anyString())).thenReturn(1);

        service().addCardToMagic("Fireball", "Fire");
    }

    @Test
    void removeCardByNameFailsWhenNoMatchingRow() {
        assertThrows(IllegalArgumentException.class,
                () -> service().removeCardFromMagic("MissingMagic", "Fire"));
    }

    private SecondaryAdminDataService service() {
        return new SecondaryAdminDataService(jdbcTemplate);
    }
}
