package com.chronicle.domain.transition;

public record TransitionContext(
        boolean failureDetected,
        boolean workCompleted
) {

    public static TransitionContext empty() {
        return new TransitionContext(false, false);
    }

    public static TransitionContext withFailureDetected() {
        return new TransitionContext(true, false);
    }

    public static TransitionContext withWorkCompleted() {
        return new TransitionContext(false, true);
    }
}
