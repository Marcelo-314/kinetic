package com.chronicle.domain.state;

public sealed interface ProcessState permits Pending, Running, Paused, Completed, Failed, Stopped {

    static Pending pending() {
        return new Pending();
    }

    static Running running() {
        return new Running();
    }

    static Paused paused() {
        return new Paused();
    }

    static Completed completed() {
        return new Completed();
    }

    static Failed failed() {
        return new Failed();
    }

    static Stopped stopped() {
        return new Stopped();
    }

    default boolean isTerminal() {
        return this instanceof Completed || this instanceof Failed || this instanceof Stopped;
    }

    default String code() {
        return switch (this) {
            case Pending ignored -> "PENDING";
            case Running ignored -> "RUNNING";
            case Paused ignored -> "PAUSED";
            case Completed ignored -> "COMPLETED";
            case Failed ignored -> "FAILED";
            case Stopped ignored -> "STOPPED";
        };
    }
}
