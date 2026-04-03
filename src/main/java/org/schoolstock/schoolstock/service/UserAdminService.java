package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.model.Role;
import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public User createUser(String username, String rawPassword, Set<Role> roles) {
        var user = new User(username, passwordEncoder.encode(rawPassword), roles);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void addRole(Long userId, Role role) {
        var user = getUser(userId);
        user.getRoles().add(role);
    }

    public void removeRole(Long userId, Role role) {
        var user = getUser(userId);
        user.getRoles().remove(role);

        if (role == Role.ORDERER) {
            // Remove all approvers assigned to this orderer
            user.getApprovers().clear();
        }

        if (role == Role.APPROVER) {
            // Remove this user from every orderer's approver list
            userRepository.findByApproversContaining(user)
                    .forEach(orderer -> orderer.getApprovers().remove(user));
        }
    }

    public void addApprover(Long ordererId, Long approverId) {
        var orderer = getUser(ordererId);
        var approver = getUser(approverId);
        orderer.getApprovers().add(approver);
    }

    public void removeApprover(Long ordererId, Long approverId) {
        var orderer = getUser(ordererId);
        var approver = getUser(approverId);
        orderer.getApprovers().remove(approver);
    }

    @Transactional(readOnly = true)
    public List<User> getApprovers() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .filter(u -> u.getRoles().contains(Role.APPROVER))
                .toList();
    }
}
