package in.project.main.controllers;

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

import in.project.main.entities.Lead;
import in.project.main.entities.enums.LeadStatus;
import in.project.main.repositories.LeadRepository;

@Controller
@RequestMapping("/admin/leads")
public class AdminLeadController {

    @Autowired
    private LeadRepository leadRepository;

    @GetMapping
    public String listLeads(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        LeadStatus leadStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                leadStatus = LeadStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // An unrecognised status behaves as "no filter" rather than erroring.
            }
        }

        Page<Lead> leadPage = leadRepository.searchAndFilter(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                leadStatus,
                null,
                pageable);

        // The attribute names below are the ones leads/list.html actually reads. They were
        // previously "leads", "status" and "*Leads", so the page threw on ${leadPage.content}
        // and every stat tile silently rendered 0.
        model.addAttribute("leadPage", leadPage);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("leadStatuses", Arrays.asList(LeadStatus.values()));
        model.addAttribute("totalLeads", leadRepository.count());
        model.addAttribute("newCount", leadRepository.countByStatus(LeadStatus.NEW));
        model.addAttribute("contactedCount", leadRepository.countByStatus(LeadStatus.CONTACTED));
        model.addAttribute("qualifiedCount", leadRepository.countByStatus(LeadStatus.QUALIFIED));
        model.addAttribute("convertedCount", leadRepository.countByStatus(LeadStatus.CONVERTED));
        model.addAttribute("lostCount", leadRepository.countByStatus(LeadStatus.LOST));

        return "admin/crm/leads/list";
    }

    @PostMapping("/add")
    public String addLead(
            @RequestParam("name") String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "interestedIn", required = false) String interestedIn,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            Lead lead = new Lead();
            lead.setName(name);
            lead.setEmail(email);
            lead.setPhone(phone);
            lead.setSource(source);
            lead.setInterestedIn(interestedIn);
            lead.setAssignedTo(assignedTo);
            lead.setNotes(notes);
            // The add form has always had a Status select; it used to be discarded and every
            // new lead was forced to NEW.
            lead.setStatus(parseStatus(status, LeadStatus.NEW));
            leadRepository.save(lead);
            redirectAttributes.addFlashAttribute("successMsg", "Lead created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to create lead: " + e.getMessage());
        }
        return "redirect:/admin/leads";
    }

    @PostMapping("/{id}/update")
    public String updateLead(
            @PathVariable("id") Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "interestedIn", required = false) String interestedIn,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            RedirectAttributes redirectAttributes) {

        Lead lead = leadRepository.findById(id).orElse(null);
        if (lead == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lead not found.");
            return "redirect:/admin/leads";
        }
        try {
            if (name != null && !name.trim().isEmpty()) {
                lead.setName(name.trim());
            }
            if (email != null) {
                lead.setEmail(email);
            }
            if (phone != null) {
                lead.setPhone(phone);
            }
            if (source != null) {
                lead.setSource(source);
            }
            if (interestedIn != null) {
                lead.setInterestedIn(interestedIn);
            }
            if (status != null && !status.trim().isEmpty()) {
                lead.setStatus(parseStatus(status, lead.getStatus()));
            }
            if (notes != null) {
                lead.setNotes(notes);
            }
            if (assignedTo != null) {
                lead.setAssignedTo(assignedTo);
            }
            leadRepository.save(lead);
            redirectAttributes.addFlashAttribute("successMsg", "Lead updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update lead: " + e.getMessage());
        }
        return "redirect:/admin/leads";
    }

    @PostMapping("/{id}/delete")
    public String deleteLead(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (!leadRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lead not found.");
            return "redirect:/admin/leads";
        }
        try {
            leadRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Lead deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete lead: " + e.getMessage());
        }
        return "redirect:/admin/leads";
    }

    /**
     * Turns a submitted status string into a LeadStatus, falling back rather than throwing.
     *
     * The previous updateLead called LeadStatus.valueOf directly, so a value outside the enum
     * raised IllegalArgumentException and surfaced to the admin as "Failed to update lead:
     * No enum constant ..." - a stack-trace detail masquerading as a business message.
     */
    private LeadStatus parseStatus(String status, LeadStatus fallback) {
        if (status == null || status.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LeadStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
