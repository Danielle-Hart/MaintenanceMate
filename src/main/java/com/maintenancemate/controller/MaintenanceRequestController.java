
package com.maintenancemate.controller;

import com.maintenancemate.model.MaintenanceRequest;
import com.maintenancemate.model.RequestStatus;
import com.maintenancemate.repository.MaintenanceRequestRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestRepository repository;

    public MaintenanceRequestController(
            MaintenanceRequestRepository repository) {
        this.repository = repository;
    }

    private boolean isManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_MANAGER")
                        || role.equals("ROLE_ADMIN"));
    }

    private MaintenanceRequest findAuthorizedRequest(
            Long id, Authentication auth) {

        MaintenanceRequest request = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Request not found"));

        if (!isManager(auth)
                && !auth.getName().equals(request.getSubmittedBy())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }

        return request;
    }

    // Makes the list of statuses available to every view this controller renders
    @ModelAttribute("statuses")
    public RequestStatus[] statuses() {
        return RequestStatus.values();
    }

    // READ: Show requests
    @GetMapping
    public String listRequests(Model model, Authentication auth) {

        if (isManager(auth)) {
            model.addAttribute("requests",
                    repository.findAllByOrderByCreatedAtDesc());
        } else {
            model.addAttribute("requests",
                    repository.findBySubmittedByOrderByCreatedAtDesc(
                            auth.getName()));
        }

        model.addAttribute("isManager", isManager(auth));
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

        request.setId(null);
        request.setSubmittedBy(auth.getName());
        request.setStatus(RequestStatus.OPEN);

        repository.save(request);

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

        MaintenanceRequest request =
                findAuthorizedRequest(id, auth);

        model.addAttribute("request", request);
        model.addAttribute("editing", true);
        model.addAttribute("isManager", isManager(auth));
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

        MaintenanceRequest existing =
                findAuthorizedRequest(id, auth);

        if (result.hasErrors()) {
            form.setId(id);
            model.addAttribute("editing", true);
            model.addAttribute("isManager", isManager(auth));
            return "request-form";
        }

        existing.setTitle(form.getTitle());
        existing.setDescription(form.getDescription());
        existing.setApartmentNumber(form.getApartmentNumber());

        // Only managers/admins may move a request through its lifecycle.
        // A status value posted by a resident is ignored.
        if (isManager(auth) && form.getStatus() != null) {
            existing.setStatus(form.getStatus());
        }

        repository.save(existing);

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

        MaintenanceRequest request =
                findAuthorizedRequest(id, auth);

        repository.delete(request);

        redirectAttributes.addFlashAttribute(
                "success", "Maintenance request deleted!");

        return "redirect:/requests";
    }
}
