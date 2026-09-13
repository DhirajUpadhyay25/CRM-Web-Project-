package in.project.main.controllers;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.SupportTicket;
import in.project.main.entities.enums.TicketCategory;
import in.project.main.entities.enums.TicketPriority;
import in.project.main.entities.enums.TicketStatus;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.SupportTicketService;

@Controller
@RequestMapping("/admin/support")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSupportController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSupportController.class);

    @Autowired
    private SupportTicketService ticketService;

    @GetMapping({"", "/", "/tickets"})
    public String listTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("oldest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("priority".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "priority");
        } else if ("updated".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, sortObj);
        Page<SupportTicket> ticketPage = ticketService.searchTickets(keyword, status, category, priority, pageable);

        Map<String, Object> stats = ticketService.getStatistics();

        model.addAttribute("tickets", ticketPage.getContent());
        model.addAttribute("page", ticketPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketPage.getTotalPages());
        model.addAttribute("totalElements", ticketPage.getTotalElements());
        model.addAttribute("stats", stats);

        // Filter criteria state
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedSort", sort);

        // Enums for dropdowns
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("categories", TicketCategory.values());
        model.addAttribute("priorities", TicketPriority.values());

        return "admin/support/list";
    }

    @GetMapping("/{id}")
    public String ticketDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<SupportTicket> opt = ticketService.getTicketById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Support ticket #" + id + " was not found.");
            return "redirect:/admin/support";
        }

        SupportTicket ticket = opt.get();
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", ticket.getReplies());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());

        return "admin/support/detail";
    }

    @PostMapping("/{id}/reply")
    public String addReply(
            @PathVariable Long id,
            @RequestParam String message,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        if (message == null || message.trim().isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Reply message cannot be empty.");
            return "redirect:/admin/support/" + id;
        }

        String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
        String adminName = "Support Agent";

        try {
            ticketService.addReply(id, "ADMIN", adminName, adminEmail, message.trim());
            ra.addFlashAttribute("successMessage", "Reply added successfully to ticket conversation.");
        } catch (Exception e) {
            logger.error("Failed to add reply to ticket #{}: {}", id, e.getMessage());
            ra.addFlashAttribute("errorMessage", "Failed to add reply: " + e.getMessage());
        }

        return "redirect:/admin/support/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam TicketStatus newStatus,
            @RequestParam(required = false) String resolution,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";

        try {
            ticketService.updateStatus(id, newStatus, resolution, adminEmail);
            ra.addFlashAttribute("successMessage", "Ticket status updated to " + newStatus.getDisplayName() + ".");
        } catch (Exception e) {
            logger.error("Failed to update status on ticket #{}: {}", id, e.getMessage());
            ra.addFlashAttribute("errorMessage", "Failed to update ticket status: " + e.getMessage());
        }

        return "redirect:/admin/support/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignTicket(
            @PathVariable Long id,
            @RequestParam String adminEmail,
            @RequestParam(required = false) String adminName,
            RedirectAttributes ra) {

        try {
            String name = (adminName != null && !adminName.isBlank()) ? adminName : adminEmail;
            ticketService.assignTicket(id, adminEmail.trim(), name);
            ra.addFlashAttribute("successMessage", "Ticket successfully assigned to " + name + ".");
        } catch (Exception e) {
            logger.error("Failed to assign ticket #{}: {}", id, e.getMessage());
            ra.addFlashAttribute("errorMessage", "Failed to assign ticket: " + e.getMessage());
        }

        return "redirect:/admin/support/" + id;
    }
}
