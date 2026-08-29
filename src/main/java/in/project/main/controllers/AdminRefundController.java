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
import in.project.main.entities.Refund;
import in.project.main.repositories.RefundRepository;

@Controller
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    @Autowired
    private RefundRepository repository;

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
                      RedirectAttributes ra) {
        try {
            Refund refund = new Refund();
            refund.setOrderId(orderId);
            refund.setAmount(amount);
            refund.setReason(reason);
            refund.setStatus(status);
            refund.setRefundDate(refundDate);
            repository.save(refund);
            ra.addFlashAttribute("success", "Refund created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Refund deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete refund: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}
