package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.state.ProcessState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessPersistenceMapperTest {

    private final ProcessPersistenceMapper mapper = new ProcessPersistenceMapper();

    @Test
    void newEntityLeavesJpaVersionUnsetAndKeepsBusinessFields() {
        ProcessAggregate aggregate = ProcessAggregate.pending("process-1", "Analyze docs", Instant.parse("2026-04-19T18:00:00Z"));

        var entity = mapper.toNewEntity(aggregate);

        assertNull(entity.getVersion());
        assertEquals("Analyze docs", entity.getObjective());
        assertEquals("NONE", entity.getResultKind());
        assertEquals(aggregate.createdAt(), entity.getCreatedAt());
    }

    @Test
    void existingEntityUsesSemanticVersionTranslationAndRoundTripsToDomain() {
        ProcessAggregate aggregate = new ProcessAggregate(
                "process-2",
                ProcessState.running(),
                3L,
                Instant.parse("2026-04-19T18:00:00Z"),
                Instant.parse("2026-04-19T18:05:00Z"),
                "Analyze docs",
                ResultKind.PARTIAL,
                true,
                false
        );

        var entity = mapper.toExistingEntity(aggregate);

        assertEquals(1L, entity.getVersion());

        entity.setVersion(2L);
        assertEquals(aggregate, mapper.toDomain(entity));
    }
}
