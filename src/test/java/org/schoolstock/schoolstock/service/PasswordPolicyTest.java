package org.schoolstock.schoolstock.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertTrue(policy.validate("Correct1Horse", "alice").isEmpty());
    }

    @Test
    void rejectsTooShort() {
        assertEquals(1, policy.validate("Abc1def", "alice").size());
    }

    @Test
    void rejectsTooLongInBytes() {
        assertEquals(1, policy.validate("Aa1" + "x".repeat(70), "alice").size());
    }

    @Test
    void rejectsMissingCharacterClasses() {
        assertEquals(1, policy.validate("alllowercase1", "alice").size());
        assertEquals(1, policy.validate("ALLUPPERCASE1", "alice").size());
        assertEquals(1, policy.validate("NoDigitsHere", "alice").size());
    }

    @Test
    void rejectsPasswordContainingUsernameIgnoringCase() {
        assertEquals(1, policy.validate("xxALICExx1", "alice").size());
    }

    @Test
    void treatsNullAsEmpty() {
        assertTrue(!policy.validate(null, "alice").isEmpty());
    }

    @Test
    void everyRequirementIsDescribed() {
        assertEquals(6, policy.getRequirements().size());
    }
}
