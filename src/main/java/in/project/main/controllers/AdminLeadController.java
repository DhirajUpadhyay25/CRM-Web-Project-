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
        Page<Lead> leads;

        LeadStatus leadStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                leadStatus = LeadStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        leads = leadRepository.searchAndFilter(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                leadStatus,
                null,
                pageable);

        model.addAttribute("leads", leads);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("leadStatuses", Arrays.asList(LeadStatus.values()));
        model.addAttribute("totalLeads", leadRepository.count());
        model.addAttribute("newLeads", leadRepository.countByStatus(LeadStatus.NEW));
        model.addAttribute("convertedLeads", leadRepository.countByStatus(LeadStatus.CONVERTED));
        model.addAttribute("lostLeads", leadRepository.countByStatus(LeadStatus.LOST));

        return "admin/crm/leads/list";
    }

    @PostMapping("/add")
    public String addLead(
            @RequestParam("name") String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "interestedIn", required = false) String interestedIn,
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
            lead.setStatus(LeadStatus.NEW);
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
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            RedirectAttributes redirectAttributes) {
        try {
            Lead lead = leadRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Lead not found"));
            if (status != null && !status.trim().isEmpty()) {
                lead.setStatus(LeadStatus.valueOf(status.trim().toUpperCase()));
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
        try {
            leadRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Lead deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete lead: " + e.getMessage());
        }
        return "redirect:/admin/leads";
    }
}
