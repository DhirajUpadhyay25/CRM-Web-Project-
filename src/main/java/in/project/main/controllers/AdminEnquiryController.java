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
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Enquiry> enquiries;

        EnquiryStatus enquiryStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                enquiryStatus = EnquiryStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        enquiries = enquiryRepository.searchAndFilter(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                enquiryStatus,
                pageable);

        model.addAttribute("enquiries", enquiries);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("enquiryStatuses", Arrays.asList(EnquiryStatus.values()));
        model.addAttribute("totalEnquiries", enquiryRepository.count());
        model.addAttribute("newEnquiries", enquiryRepository.countByStatus(EnquiryStatus.NEW));
        model.addAttribute("inProgressEnquiries", enquiryRepository.countByStatus(EnquiryStatus.IN_PROGRESS));
        model.addAttribute("respondedEnquiries", enquiryRepository.countByStatus(EnquiryStatus.RESPONDED));

        return "admin/crm/enquiries/list";
    }

    @PostMapping("/{id}/update")
    public String updateEnquiry(
            @PathVariable("id") Long id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "adminNotes", required = false) String adminNotes,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            RedirectAttributes redirectAttributes) {
        try {
            Enquiry enquiry = enquiryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Enquiry not found"));
            if (status != null && !status.trim().isEmpty()) {
                EnquiryStatus newStatus = EnquiryStatus.valueOf(status.trim().toUpperCase());
                enquiry.setStatus(newStatus);
                if (newStatus == EnquiryStatus.RESPONDED || newStatus == EnquiryStatus.CLOSED) {
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
        try {
            enquiryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Enquiry deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete enquiry: " + e.getMessage());
        }
        return "redirect:/admin/enquiries";
    }
}
