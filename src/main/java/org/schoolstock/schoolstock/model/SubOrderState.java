package org.schoolstock.schoolstock.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum SubOrderState {

    NEEDS_PRICES {
        @Override public Set<SubOrderState> validTransitions() { return Set.of(NEEDS_APPROVAL, CANCELLED); }
    },
    NEEDS_APPROVAL {
        @Override public Set<SubOrderState> validTransitions() { return Set.of(PENDING, CANCELLED); }
    },
    PENDING {
        @Override public Set<SubOrderState> validTransitions() { return Set.of(DELIVERED, CANCELLED); }
    },
    CANCELLED {
        @Override public Set<SubOrderState> validTransitions() { return Set.of(); }
    },
    DELIVERED {
        @Override public Set<SubOrderState> validTransitions() { return Set.of(); }
    };

    public abstract Set<SubOrderState> validTransitions();

    public boolean canTransitionTo(SubOrderState target) {
        return validTransitions().contains(target);
    }

    public boolean isTerminal() {
        return validTransitions().isEmpty();
    }

    public String getDisplayName() {
        return Arrays.stream(name().split("_"))
                .map(w -> w.charAt(0) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
