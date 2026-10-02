package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.Role;
import jakarta.servlet.http.HttpServletResponse;
import org.schoolstock.schoolstock.service.PasswordPolicy;
import org.schoolstock.schoolstock.service.UserAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserAdminService userAdminService;
    private final PasswordPolicy passwordPolicy;

    public AdminController(UserAdminService userAdminService, PasswordPolicy passwordPolicy) {
        this.userAdminService = userAdminService;
        this.passwordPolicy = passwordPolicy;
    }

    private void populateUserListModel(Model model) {
        model.addAttribute("users", userAdminService.getAllUsers());
        model.addAttribute("allRoles", Role.values());
        model.addAttribute("approvers", userAdminService.getApprovers());
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             Model model,
                             HttpServletResponse response) {
        String trimmedUsername = username.trim();

        var failed = passwordPolicy.validate(password, trimmedUsername);
        if (!failed.isEmpty()) {
            // Re-render just the requirements block (with the failures highlighted)
            // instead of the user list, so the form keeps what the admin typed.
            response.setHeader("HX-Retarget", "#admin-password-info");
            response.setHeader("HX-Reswap", "outerHTML");
            model.addAttribute("passwordError", "User not created: the password does not meet the requirements.");
            model.addAttribute("failedRequirements", failed);
            return "fragments/admin-password-info :: admin-password-info";
        }

        userAdminService.createUser(trimmedUsername, password, java.util.Set.of());
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Model model) {
        userAdminService.deleteUser(id);
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }

    @PostMapping("/users/{id}/roles/add")
    public String addRole(@PathVariable Long id,
                          @RequestParam String role,
                          Model model) {
        userAdminService.addRole(id, Role.valueOf(role));
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }

    @PostMapping("/users/{id}/roles/remove")
    public String removeRole(@PathVariable Long id,
                             @RequestParam String role,
                             Model model) {
        userAdminService.removeRole(id, Role.valueOf(role));
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }

    @PostMapping("/users/{id}/approvers/add")
    public String addApprover(@PathVariable Long id,
                              @RequestParam Long approverId,
                              Model model) {
        userAdminService.addApprover(id, approverId);
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }

    @PostMapping("/users/{id}/approvers/remove")
    public String removeApprover(@PathVariable Long id,
                                 @RequestParam Long approverId,
                                 Model model) {
        userAdminService.removeApprover(id, approverId);
        populateUserListModel(model);
        return "fragments/admin-user-list :: admin-user-list";
    }
}
