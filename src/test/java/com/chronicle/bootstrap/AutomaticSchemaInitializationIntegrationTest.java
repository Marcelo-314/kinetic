package com.chronicle.bootstrap;

import com.chronicle.application.request.CreateProcessRequest;
import com.chronicle.application.usecase.CreateProcessUseCase;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = ChronicleApplication.class,
        properties = {
                "chronicle.runtime.scheduler.enabled=false"
        }
)
class AutomaticSchemaInitializationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreateProcessUseCase createProcessUseCase;

    private Path sourceFolder;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM activity_log");
        jdbcTemplate.execute("DELETE FROM process_result");
        jdbcTemplate.execute("DELETE FROM process_lease");
        jdbcTemplate.execute("DELETE FROM terminal_info");
        jdbcTemplate.execute("DELETE FROM document_execution");
        jdbcTemplate.execute("DELETE FROM execution_control_flags");
        jdbcTemplate.execute("DELETE FROM progress_snapshot");
        jdbcTemplate.execute("DELETE FROM authorization_info");
        jdbcTemplate.execute("DELETE FROM process_plan");
        jdbcTemplate.execute("DELETE FROM process");

        sourceFolder = Files.createTempDirectory("chronicle-schema-init");
        Files.writeString(sourceFolder.resolve("doc-01.txt"), "automatic schema init");
    }

    @Test
    void schemaSqlCreatesAllCoreTablesAutomatically() {
        assertTableExists("PROCESS");
        assertTableExists("PROCESS_PLAN");
        assertTableExists("AUTHORIZATION_INFO");
        assertTableExists("PROGRESS_SNAPSHOT");
        assertTableExists("EXECUTION_CONTROL_FLAGS");
        assertTableExists("TERMINAL_INFO");
        assertTableExists("DOCUMENT_EXECUTION");
        assertTableExists("PROCESS_RESULT");
        assertTableExists("ACTIVITY_LOG");
        assertTableExists("PROCESS_LEASE");
    }

    @Test
    void serviceCanCreateProcessUsingAutomaticallyInitializedSchema() {
        ProcessAggregate created = createProcessUseCase.execute(new CreateProcessRequest(
                "Schema initialization test",
                sourceFolder.toString(),
                SelectionMode.EXPLICIT_SELECTION,
                List.of("doc-01.txt"),
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                true
        ));

        assertNotNull(created);
        assertEquals("PENDING", created.state().code());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM process", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM process_plan", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM authorization_info", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM progress_snapshot", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM execution_control_flags", Integer.class));
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        assertEquals(1, count, "Expected table to exist: " + tableName);
    }
}
