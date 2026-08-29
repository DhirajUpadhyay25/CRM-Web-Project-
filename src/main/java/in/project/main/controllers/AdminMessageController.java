package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import in.project.main.entities.Message;
import in.project.main.repositories.MessageRepository;

@Controller
@RequestMapping("/admin/messages")
public class AdminMessageController {

    @Autowired
    private MessageRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/communication/messages/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Message message = repository.findById(id).orElseThrow(() -> new RuntimeException("Message not found"));
            message.setIsRead(true);
            repository.save(message);
            ra.addFlashAttribute("success", "Message marked as read.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to mark message as read: " + e.getMessage());
        }
        return "redirect:/admin/messages";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Message deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete message: " + e.getMessage());
        }
        return "redirect:/admin/messages";
    }
}
