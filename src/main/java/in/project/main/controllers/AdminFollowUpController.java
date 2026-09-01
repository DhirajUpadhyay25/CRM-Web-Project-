package in.project.main.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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

        FollowUpStatus followUpStatus = parseStatus(status, null);

        Page<AdminFollowUp> followUpPage = (followUpStatus != null)
                ? adminFollowUpRepository.findByStatusWithLead(followUpStatus, pageable)
                : adminFollowUpRepository.findAllWithLead(pageable);

        // These names match what follow-ups/list.html reads. They were previously "followUps",
        // "status" and "*FollowUps", so the page threw on ${followUpPage.content} and all four
        // stat tiles silently rendered 0.
        model.addAttribute("followUpPage", followUpPage);
        model.addAttribute("statusFilter", status);
        model.addAttribute("followUpStatuses", Arrays.asList(FollowUpStatus.values()));
        model.addAttribute("leads", leadRepository.findAll());
        model.addAttribute("totalFollowUps", adminFollowUpRepository.count());
        model.addAttribute("pendingCount", adminFollowUpRepository.countByStatus(FollowUpStatus.PENDING));
        model.addAttribute("completedCount", adminFollowUpRepository.countByStatus(FollowUpStatus.COMPLETED));
        model.addAttribute("missedCount", adminFollowUpRepository.countByStatus(FollowUpStatus.MISSED));
        model.addAttribute("cancelledCount", adminFollowUpRepository.countByStatus(FollowUpStatus.CANCELLED));

        // Previously the full list was put in the model as "todaysFollowUps" and no template ever
        // read it - a query per page load for nothing. A count is what the page can actually use.
        model.addAttribute("dueTodayCount",
                adminFollowUpRepository.findByStatusAndFollowUpDateLessThanEqual(FollowUpStatus.PENDING, LocalDate.now()).size());

        return "admin/crm/follow-ups/list";
    }

    @PostMapping("/add")
    public String addFollowUp(
            @RequestParam("leadId") Long leadId,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            @RequestParam("followUpDate") String followUpDate,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "priority", required = false) String priority,
            RedirectAttributes redirectAttributes) {

        // assignedTo is required by the database but validated here rather than by the
        // @RequestParam. A missing param is rejected before the method body runs, so the
        // try/catch below could never turn it into a readable message - the admin just got a
        // raw 400 page.
        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please say who the follow-up is assigned to.");
            return "redirect:/admin/follow-ups";
        }

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(followUpDate);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please provide a valid follow-up date.");
            return "redirect:/admin/follow-ups";
        }

        Lead lead = leadRepository.findById(leadId).orElse(null);
        if (lead == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "The selected lead no longer exists.");
            return "redirect:/admin/follow-ups";
        }

        try {
            AdminFollowUp followUp = new AdminFollowUp();
            followUp.setLead(lead);
            followUp.setAssignedTo(assignedTo.trim());
            followUp.setFollowUpDate(parsedDate);
            followUp.setNotes(notes);
            followUp.setPriority(normalisePriority(priority));
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
        AdminFollowUp followUp = adminFollowUpRepository.findById(id).orElse(null);
        if (followUp == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Follow-up not found.");
            return "redirect:/admin/follow-ups";
        }
        if (followUp.getStatus() == FollowUpStatus.COMPLETED) {
            redirectAttributes.addFlashAttribute("successMsg", "That follow-up was already completed.");
            return "redirect:/admin/follow-ups";
        }
        try {
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
        if (!adminFollowUpRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Follow-up not found.");
            return "redirect:/admin/follow-ups";
        }
        try {
            adminFollowUpRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Follow-up deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete follow-up: " + e.getMessage());
        }
        return "redirect:/admin/follow-ups";
    }

    /** A blank or unrecognised status means "no filter", never an exception. */
    private FollowUpStatus parseStatus(String status, FollowUpStatus fallback) {
        if (status == null || status.trim().isEmpty()) {
            return fallback;
        }
        try {
            return FollowUpStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Priority is a free-text column, but the UI only offers LOW/MEDIUM/HIGH and the row badge
     * only styles those three. Anything else is stored as MEDIUM so the column stays queryable.
     */
    private String normalisePriority(String priority) {
        if (priority == null) {
            return "MEDIUM";
        }
        String upper = priority.trim().toUpperCase();
        if (upper.equals("LOW") || upper.equals("MEDIUM") || upper.equals("HIGH")) {
            return upper;
        }
        return "MEDIUM";
    }
}
