package in.project.main.controllers;

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
import in.project.main.repositories.CouponRepository;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    @Autowired
    private CouponRepository repository;

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
                      RedirectAttributes ra) {
        try {
            Coupon coupon = new Coupon();
            coupon.setCode(code);
            coupon.setDiscountType(discountType);
            coupon.setDiscountValue(discountValue);
            coupon.setExpiryDate(expiryDate);
            coupon.setIsActive(isActive);
            repository.save(coupon);
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
                         RedirectAttributes ra) {
        try {
            Coupon coupon = repository.findById(id).orElseThrow(() -> new RuntimeException("Coupon not found"));
            coupon.setCode(code);
            coupon.setDiscountType(discountType);
            coupon.setDiscountValue(discountValue);
            coupon.setExpiryDate(expiryDate);
            coupon.setIsActive(isActive);
            repository.save(coupon);
            ra.addFlashAttribute("success", "Coupon updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Coupon deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
