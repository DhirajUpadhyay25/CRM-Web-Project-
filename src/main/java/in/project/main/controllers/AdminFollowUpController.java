package in.project.main.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.AdminFollowUp;
import in.project.main.entities.Lead;
import in.project.main.entities.enums.FollowUpStatus;
import in.project.main.repositories.AdminFollowUpRepository;
import in.project.main.repositories.LeadRepository;

@Controller
@RequestMapping("/admin/follow-ups")
public class AdminFollowUpController {

    @Autowired
    private AdminFollowUpRepository adminFollowUpRepository;

    @Autowired
    private LeadRepository leadRepository;

    @GetMapping
    public String listFollowUps(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("followUpDate").ascending());
        Page<AdminFollowUp> followUps;

        FollowUpStatus followUpStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                followUpStatus = FollowUpStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (followUpStatus != null) {
            followUps = adminFollowUpRepository.findByStatus(followUpStatus, pageable);
        } else {
            followUps = adminFollowUpRepository.findAllByOrderByFollowUpDateAsc(pageable);
        }

        model.addAttribute("followUps", followUps);
        model.addAttribute("status", status);
        model.addAttribute("followUpStatuses", Arrays.asList(FollowUpStatus.values()));
        model.addAttribute("leads", leadRepository.findAll());
        model.addAttribute("totalFollowUps", adminFollowUpRepository.count());
        model.addAttribute("pendingFollowUps", adminFollowUpRepository.countByStatus(FollowUpStatus.PENDING));
        model.addAttribute("completedFollowUps", adminFollowUpRepository.countByStatus(FollowUpStatus.COMPLETED));
        model.addAttribute("missedFollowUps", adminFollowUpRepository.countByStatus(FollowUpStatus.MISSED));
        model.addAttribute("todaysFollowUps",
                adminFollowUpRepository.findByStatusAndFollowUpDateLessThanEqual(FollowUpStatus.PENDING, LocalDate.now()));

        return "admin/crm/follow-ups/list";
    }

    @PostMapping("/add")
    public String addFollowUp(
            @RequestParam("leadId") Long leadId,
            @RequestParam("assignedTo") String assignedTo,
            @RequestParam("followUpDate") String followUpDate,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "priority", required = false) String priority,
            RedirectAttributes redirectAttributes) {
        try {
            Lead lead = leadRepository.findById(leadId)
                    .orElseThrow(() -> new RuntimeException("Lead not found"));
            AdminFollowUp followUp = new AdminFollowUp();
            followUp.setLead(lead);
            followUp.setAssignedTo(assignedTo);
            followUp.setFollowUpDate(LocalDate.parse(followUpDate));
            followUp.setNotes(notes);
            followUp.setPriority(priority);
            followUp.setStatus(FollowUpStatus.PENDING);
            adminFollowUpRepository.save(followUp);
            redirectAttributes.addFlashAttribute("successMsg", "Follow-up created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to create follow-up: " + e.getMessage());
        }
        return "redirect:/admin/follow-ups";
    }

    @PostMapping("/{id}/complete")
    public String completeFollowUp(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            AdminFollowUp followUp = adminFollowUpRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Follow-up not found"));
            followUp.setStatus(FollowUpStatus.COMPLETED);
            followUp.setCompletedAt(LocalDateTime.now());
            adminFollowUpRepository.save(followUp);
            redirectAttributes.addFlashAttribute("successMsg", "Follow-up marked as completed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to complete follow-up: " + e.getMessage());
        }
        return "redirect:/admin/follow-ups";
    }

    @PostMapping("/{id}/delete")
    public String deleteFollowUp(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            adminFollowUpRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Follow-up deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete follow-up: " + e.getMessage());
        }
        return "redirect:/admin/follow-ups";
    }
}
