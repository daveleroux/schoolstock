package org.schoolstock.schoolstock.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Basic password strength policy. The same list of rules is used both to
 * validate a password and to describe the requirements to the user, so the
 * two can never drift apart.
 */
@Component("passwordPolicy")
public class PasswordPolicy {

    static final int MIN_LENGTH = 8;
    /** bcrypt silently ignores (or rejects) anything beyond 72 bytes. */
    static final int MAX_BYTES = 72;

    private record Rule(String description, BiPredicate<String, String> test) {}

    private static final List<Rule> RULES = List.of(
            new Rule("At least " + MIN_LENGTH + " characters",
                    (pw, user) -> pw.length() >= MIN_LENGTH),
            new Rule("At most " + MAX_BYTES + " characters (accented and non-Latin characters may count as more than one)",
                    (pw, user) -> pw.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES),
            new Rule("At least one uppercase letter",
                    (pw, user) -> pw.chars().anyMatch(Character::isUpperCase)),
            new Rule("At least one lowercase letter",
                    (pw, user) -> pw.chars().anyMatch(Character::isLowerCase)),
            new Rule("At least one digit",
                    (pw, user) -> pw.chars().anyMatch(Character::isDigit)),
            new Rule("Must not contain the username",
                    (pw, user) -> user == null || user.isBlank()
                            || !pw.toLowerCase().contains(user.toLowerCase()))
    );

    /** Human-readable descriptions of every requirement, in display order. */
    public List<String> getRequirements() {
        return RULES.stream().map(Rule::description).toList();
    }

    /**
     * @return the descriptions of the requirements the password fails;
     *         empty if the password is acceptable
     */
    public List<String> validate(String password, String username) {
        String pw = password == null ? "" : password;
        return RULES.stream()
                .filter(rule -> !rule.test().test(pw, username))
                .map(Rule::description)
                .toList();
    }
}
