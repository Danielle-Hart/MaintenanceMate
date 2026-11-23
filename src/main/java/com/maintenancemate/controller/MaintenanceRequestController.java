
package com.maintenancemate.controller;

import com.maintenancemate.model.MaintenanceRequest;
import com.maintenancemate.model.RequestStatus;
import com.maintenancemate.service.MaintenanceRequestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    public MaintenanceRequestController(MaintenanceRequestService service) {
        this.service = service;
    }

    private boolean isManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_MANAGER")
                        || role.equals("ROLE_ADMIN"));
    }

    // Makes the list of statuses available to every view this controller renders
    @ModelAttribute("statuses")
    public RequestStatus[] statuses() {
        return RequestStatus.values();
    }

    // READ: Show requests
    @GetMapping
    public String listRequests(Model model, Authentication auth) {
        boolean manager = isManager(auth);
        model.addAttribute("requests",
                service.findVisibleRequests(auth.getName(), manager));
        model.addAttribute("isManager", manager);
        return "requests";
    }

    // CREATE: Display form
    @GetMapping("/new")
    public String newRequest(Model model) {
        model.addAttribute("request", new MaintenanceRequest());
        model.addAttribute("editing", false);
        return "request-form";
    }

    // CREATE: Save request
    @PostMapping
    public String createRequest(
            @Valid @ModelAttribute("request") MaintenanceRequest request,
            BindingResult result,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            return "request-form";
        }

        service.create(request, auth.getName());

        redirectAttributes.addFlashAttribute(
                "success", "Maintenance request submitted!");
        return "redirect:/requests";
    }

    // UPDATE: Display edit form
    @GetMapping("/{id}/edit")
    public String editRequest(
            @PathVariable Long id,
            Authentication auth,
            Model model) {

        boolean manager = isManager(auth);
        model.addAttribute("request",
                service.findAuthorized(id, auth.getName(), manager));
        model.addAttribute("editing", true);
        model.addAttribute("isManager", manager);
        return "request-form";
    }

    // UPDATE: Save changes
    @PostMapping("/{id}/edit")
    public String updateRequest(
            @PathVariable Long id,
            @Valid @ModelAttribute("request") MaintenanceRequest form,
            BindingResult result,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean manager = isManager(auth);

        if (result.hasErrors()) {
            // Check ownership before echoing the form back, so a resident
            // can't probe other people's request ids with invalid input.
            service.findAuthorized(id, auth.getName(), manager);
            form.setId(id);
            model.addAttribute("editing", true);
            model.addAttribute("isManager", manager);
            return "request-form";
        }

        service.update(id, form, auth.getName(), manager);

        redirectAttributes.addFlashAttribute(
                "success", "Maintenance request updated!");
        return "redirect:/requests";
    }

    // DELETE: Remove request
    @PostMapping("/{id}/delete")
    public String deleteRequest(
            @PathVariable Long id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        service.delete(id, auth.getName(), isManager(auth));

        redirectAttributes.addFlashAttribute(
                "success", "Maintenance request deleted!");
        return "redirect:/requests";
    }
}
