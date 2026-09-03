package in.project.main.controllers;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Refund;
import in.project.main.services.RefundService;

@Controller
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    @Autowired
    private RefundService refundService;

    @GetMapping
    public String list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("id").descending());
        Page<Refund> refundsPage = refundService.getRefundsPage(search, status, pageable);
        Map<String, Object> stats = refundService.getRefundStats();

        model.addAttribute("refundsPage", refundsPage);
        model.addAttribute("items", refundsPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("stats", stats);

        return "admin/commerce/refunds/list";
    }

    @PostMapping("/{id}/approve")
    public String approveRefund(
            @PathVariable Long id,
            @RequestParam(name = "adminNote", required = false) String adminNote,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Refund processed = refundService.approveAndProcessRefund(id, actorEmail, adminNote);
            ra.addFlashAttribute("success", "Refund for Order #" + processed.getOrderId() + " approved and processed. Course access revoked and student notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to approve refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    @PostMapping("/{id}/reject")
    public String rejectRefund(
            @PathVariable Long id,
            @RequestParam(name = "rejectionReason", required = false) String rejectionReason,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Refund rejected = refundService.rejectRefund(id, actorEmail, rejectionReason);
            ra.addFlashAttribute("success", "Refund request for Order #" + rejected.getOrderId() + " rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to reject refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    @PostMapping("/direct")
    public String directRefund(
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") String amount,
            @RequestParam("reason") String reason,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Refund processed = refundService.adminInitiateRefund(orderId, amount, reason, actorEmail);
            ra.addFlashAttribute("success", "Direct refund of ₹" + processed.getAmount() + " processed for Order #" + orderId + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to process refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}
