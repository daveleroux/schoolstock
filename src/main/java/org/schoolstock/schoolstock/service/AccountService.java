package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Self-service operations a user can perform on their own account. */
@Service
@Transactional
public class AccountService {

    public record PasswordChangeResult(boolean success, String error, List<String> failedRequirements) {
        static PasswordChangeResult ok() {
            return new PasswordChangeResult(true, null, List.of());
        }

        static PasswordChangeResult failed(String error) {
            return new PasswordChangeResult(false, error, List.of());
        }

        static PasswordChangeResult weak(List<String> failed) {
            return new PasswordChangeResult(false, "The new password does not meet the requirements.", failed);
        }
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public AccountService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    public PasswordChangeResult changePassword(Long userId,
                                               String currentPassword,
                                               String newPassword,
                                               String confirmPassword) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return PasswordChangeResult.failed("Your current password is incorrect.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return PasswordChangeResult.failed("The new password and its confirmation do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            return PasswordChangeResult.failed("The new password must be different from your current password.");
        }

        var failed = passwordPolicy.validate(newPassword, user.getUsername());
        if (!failed.isEmpty()) {
            return PasswordChangeResult.weak(failed);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return PasswordChangeResult.ok();
    }
}
