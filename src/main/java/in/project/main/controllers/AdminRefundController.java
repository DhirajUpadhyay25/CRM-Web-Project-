package in.project.main.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Refund;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.RefundRepository;
import in.project.main.services.AuditLogService;

@Controller
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    @Autowired
    private RefundRepository repository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/commerce/refunds/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String orderId,
                      @RequestParam String amount,
                      @RequestParam String reason,
                      @RequestParam String status,
                      @RequestParam String refundDate,
                      Principal principal,
                      RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Refund refund = new Refund();
            refund.setOrderId(orderId);
            refund.setAmount(amount);
            refund.setReason(reason);
            refund.setStatus(status);
            refund.setRefundDate(refundDate);
            Refund saved = repository.save(refund);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.PAYMENT_REFUNDED,
                    "PAYMENT_REFUNDED",
                    "Admin processed refund of ₹" + amount + " for order #" + orderId + " (Reason: " + reason + ")."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("REFUND", String.valueOf(saved.getId()), "Order #" + orderId)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.MEDIUM);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Refund created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            repository.deleteById(id);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.PAYMENT_REFUNDED,
                    "REFUND_DELETED",
                    "Admin deleted refund record ID #" + id + "."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("REFUND", String.valueOf(id), "Refund #" + id)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.LOW);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Refund deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}
