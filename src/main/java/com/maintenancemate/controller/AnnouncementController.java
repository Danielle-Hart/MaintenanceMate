package com.maintenancemate.controller;

import com.maintenancemate.model.Announcement;
import com.maintenancemate.repository.AnnouncementRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    // Everyone who is logged in can view announcements
    @GetMapping("/announcements")
    public String viewAnnouncements(Model model) {
        model.addAttribute("announcements",
                announcementRepository.findAllByOrderByCreatedAtDesc());
        return "announcements";
    }

    // Managers/admins only (enforced in SecurityConfig)
    @GetMapping("/manager/announcements/new")
    public String showForm(Model model) {
        model.addAttribute("announcement", new Announcement());
        return "announcement-form";
    }

    @PostMapping("/manager/announcements")
    public String create(@Valid @ModelAttribute("announcement") Announcement announcement,
                         BindingResult result,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "announcement-form";
        }
        announcement.setPostedBy(auth.getName());
        announcementRepository.save(announcement);
        redirectAttributes.addFlashAttribute("success", "Announcement posted!");
        return "redirect:/announcements";
    }

    @PostMapping("/manager/announcements/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        announcementRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Announcement deleted.");
        return "redirect:/announcements";
    }
}