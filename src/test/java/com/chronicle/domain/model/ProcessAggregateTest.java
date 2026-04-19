package com.chronicle.domain.model;

import com.chronicle.domain.state.ProcessState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessAggregateTest {

    @Test
    void pendingStartsWithVersionOne() {
        ProcessAggregate process = ProcessAggregate.pending("process-1");

        assertEquals(1L, process.version());
    }

    @Test
    void versionMustBeAtLeastOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessAggregate("process-1", ProcessState.pending(), 0L, false, false)
        );
    }
}
