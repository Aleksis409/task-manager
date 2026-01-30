package com.javarush.taskmanager.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibase_ShouldCreateAllTables() {
        String[] expectedTables = {"users", "tasks", "refresh_tokens"};

        for (String table : expectedTables) {
            Boolean tableExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)",
                    Boolean.class,
                    table
            );
            assertTrue(tableExists, "Table " + table + " should exist");
        }
    }

    @Test
    void liquibase_ShouldCreateIndexes() {
        Boolean indexExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_users_username')",
                Boolean.class
        );
        assertTrue(indexExists, "Index idx_users_username should exist");
    }

    @Test
    void database_ShouldBeAccessible() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertTrue(result == 1, "Database should be accessible");
    }
}
