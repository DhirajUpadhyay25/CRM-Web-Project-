package in.project.main.controllers;

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

import in.project.main.entities.Enquiry;
import in.project.main.entities.enums.EnquiryStatus;
import in.project.main.entities.enums.EnquiryType;
import in.project.main.repositories.EnquiryRepository;

@Controller
@RequestMapping("/admin/enquiries")
public class AdminEnquiryController {

    @Autowired
    private EnquiryRepository enquiryRepository;

    @GetMapping
    public String listEnquiries(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Enquiry> enquiryPage = enquiryRepository.searchAndFilter(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                parseStatus(status, null),
                parseType(type, null),
                pageable);

        // These names match what enquiries/list.html reads. They were previously "enquiries",
        // "status" and "*Enquiries", so the page threw on ${enquiryPage.content}, the four
        // stat tiles always showed 0, and the Type dropdown was inert.
        model.addAttribute("enquiryPage", enquiryPage);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("typeFilter", type);
        model.addAttribute("enquiryStatuses", Arrays.asList(EnquiryStatus.values()));
        model.addAttribute("totalEnquiries", enquiryRepository.count());
        model.addAttribute("newCount", enquiryRepository.countByStatus(EnquiryStatus.NEW));
        model.addAttribute("inProgressCount", enquiryRepository.countByStatus(EnquiryStatus.IN_PROGRESS));
        model.addAttribute("respondedCount", enquiryRepository.countByStatus(EnquiryStatus.RESPONDED));
        model.addAttribute("closedCount", enquiryRepository.countByStatus(EnquiryStatus.CLOSED));

        return "admin/crm/enquiries/list";
    }

    @PostMapping("/{id}/update")
    public String updateEnquiry(
            @PathVariable("id") Long id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "adminNotes", required = false) String adminNotes,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            RedirectAttributes redirectAttributes) {

        Enquiry enquiry = enquiryRepository.findById(id).orElse(null);
        if (enquiry == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Enquiry not found.");
            return "redirect:/admin/enquiries";
        }
        try {
            if (status != null && !status.trim().isEmpty()) {
                EnquiryStatus newStatus = parseStatus(status, enquiry.getStatus());
                enquiry.setStatus(newStatus);
                // Stamp the response time the first time it reaches a responded state, and do
                // not overwrite it if the enquiry is edited again later.
                if ((newStatus == EnquiryStatus.RESPONDED || newStatus == EnquiryStatus.CLOSED)
                        && enquiry.getRespondedAt() == null) {
                    enquiry.setRespondedAt(LocalDateTime.now());
                }
            }
            if (adminNotes != null) {
                enquiry.setAdminNotes(adminNotes);
            }
            if (assignedTo != null) {
                enquiry.setAssignedTo(assignedTo);
            }
            enquiryRepository.save(enquiry);
            redirectAttributes.addFlashAttribute("successMsg", "Enquiry updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update enquiry: " + e.getMessage());
        }
        return "redirect:/admin/enquiries";
    }

    @PostMapping("/{id}/delete")
    public String deleteEnquiry(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (!enquiryRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Enquiry not found.");
            return "redirect:/admin/enquiries";
        }
        try {
            enquiryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Enquiry deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete enquiry: " + e.getMessage());
        }
        return "redirect:/admin/enquiries";
    }

    /** An unrecognised or blank value means "no change" / "no filter", never an exception. */
    private EnquiryStatus parseStatus(String status, EnquiryStatus fallback) {
        if (status == null || status.trim().isEmpty()) {
            return fallback;
        }
        try {
            return EnquiryStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private EnquiryType parseType(String type, EnquiryType fallback) {
        if (type == null || type.trim().isEmpty()) {
            return fallback;
        }
        try {
            return EnquiryType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
