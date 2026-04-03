package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.Role;
import org.schoolstock.schoolstock.service.UserAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserAdminService userAdminService;

    public AdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
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
                             Model model) {
        userAdminService.createUser(username.trim(), password, java.util.Set.of());
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
