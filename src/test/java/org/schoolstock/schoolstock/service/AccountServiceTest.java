package org.schoolstock.schoolstock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schoolstock.schoolstock.model.Role;
import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private User user;
    private AccountService service;

    @BeforeEach
    void setUp() {
        user = new User("alice", encoder.encode("OldPassw0rd"), Set.of(Role.ORDERER));
        user.setId(1L);
        var repo = mock(UserRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.of(user));
        service = new AccountService(repo, encoder, new PasswordPolicy());
    }

    @Test
    void changesPasswordWhenAllChecksPass() {
        var result = service.changePassword(1L, "OldPassw0rd", "NewPassw0rd", "NewPassw0rd");
        assertTrue(result.success());
        assertTrue(encoder.matches("NewPassw0rd", user.getPassword()));
    }

    @Test
    void rejectsWrongCurrentPassword() {
        var result = service.changePassword(1L, "wrong", "NewPassw0rd", "NewPassw0rd");
        assertFalse(result.success());
        assertTrue(encoder.matches("OldPassw0rd", user.getPassword()));
    }

    @Test
    void rejectsMismatchedConfirmation() {
        var result = service.changePassword(1L, "OldPassw0rd", "NewPassw0rd", "Different1");
        assertFalse(result.success());
        assertTrue(encoder.matches("OldPassw0rd", user.getPassword()));
    }

    @Test
    void rejectsWeakPasswordAndReportsFailedRequirements() {
        var result = service.changePassword(1L, "OldPassw0rd", "weak", "weak");
        assertFalse(result.success());
        assertFalse(result.failedRequirements().isEmpty());
        assertTrue(encoder.matches("OldPassw0rd", user.getPassword()));
    }

    @Test
    void rejectsReuseOfCurrentPassword() {
        var result = service.changePassword(1L, "OldPassw0rd", "OldPassw0rd", "OldPassw0rd");
        assertFalse(result.success());
    }
}
