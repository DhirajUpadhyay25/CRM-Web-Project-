package in.project.main.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Coupon;
import in.project.main.entities.Course;
import in.project.main.repositories.CourseRepository;
import in.project.main.services.CouponService;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public String list(Model model) {
        List<Coupon> coupons = couponService.getAllCoupons();
        Map<String, Object> stats = couponService.getCouponStats();
        List<Course> courses = courseRepository.findAll();

        model.addAttribute("items", coupons);
        model.addAttribute("stats", stats);
        model.addAttribute("courses", courses);
        return "admin/commerce/coupons/list";
    }

    @PostMapping("/add")
    public String add(
            @ModelAttribute Coupon coupon,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Coupon created = couponService.createCoupon(coupon, actorEmail);
            ra.addFlashAttribute("success", "Coupon '" + created.getCode() + "' created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @ModelAttribute Coupon coupon,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Coupon updated = couponService.updateCoupon(id, coupon, actorEmail);
            ra.addFlashAttribute("success", "Coupon '" + updated.getCode() + "' updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/toggle")
    public String toggleStatus(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            boolean active = couponService.toggleCouponStatus(id, actorEmail);
            ra.addFlashAttribute("success", "Coupon status updated to " + (active ? "ACTIVE" : "INACTIVE") + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to toggle status: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            couponService.deleteCoupon(id, actorEmail);
            ra.addFlashAttribute("success", "Coupon deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete coupon: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
