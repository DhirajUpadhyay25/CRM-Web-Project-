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

import in.project.main.entities.Coupon;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.CouponRepository;
import in.project.main.services.AuditLogService;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    @Autowired
    private CouponRepository repository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/commerce/coupons/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String code,
                      @RequestParam String discountType,
                      @RequestParam String discountValue,
                      @RequestParam String expiryDate,
                      @RequestParam Boolean isActive,
                      Principal principal,
                      RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Coupon coupon = new Coupon();
            coupon.setCode(code);
            coupon.setDiscountType(discountType);
            coupon.setDiscountValue(discountValue);
            coupon.setExpiryDate(expiryDate);
            coupon.setIsActive(isActive);
            Coupon saved = repository.save(coupon);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_CREATED",
                    "Admin created coupon code '" + code + "' (" + discountValue + " " + discountType + ")."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("COUPON", String.valueOf(saved.getId()), code)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Coupon created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String code,
                         @RequestParam String discountType,
                         @RequestParam String discountValue,
                         @RequestParam String expiryDate,
                         @RequestParam Boolean isActive,
                         Principal principal,
                         RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Coupon coupon = repository.findById(id).orElseThrow(() -> new RuntimeException("Coupon not found"));
            coupon.setCode(code);
            coupon.setDiscountType(discountType);
            coupon.setDiscountValue(discountValue);
            coupon.setExpiryDate(expiryDate);
            coupon.setIsActive(isActive);
            repository.save(coupon);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_UPDATED",
                    "Admin updated coupon code '" + code + "'."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("COUPON", String.valueOf(id), code)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Coupon updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            repository.deleteById(id);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_DELETED",
                    "Admin deleted coupon ID #" + id + "."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("COUPON", String.valueOf(id), "Coupon #" + id)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.LOW);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Coupon deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
