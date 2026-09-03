package in.project.main.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

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

import in.project.main.entities.Employee;
import in.project.main.entities.Message;
import in.project.main.entities.User;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.MessageService;

@Controller
@RequestMapping("/admin/messages")
public class AdminMessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmployeeRepository employeeRepository;

    private String getAdminEmail(Principal principal) {
        return principal != null ? principal.getName() : "admin@edutake.com";
    }

    @GetMapping
    public String listMessages(
            @RequestParam(name = "folder", defaultValue = "INBOX") String folder,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "messageId", required = false) Long messageId,
            Principal principal,
            Model model) {

        String adminEmail = getAdminEmail(principal);
        String currentFolder = folder.toUpperCase();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Message> messagePage = messageService.getMessagesPage(adminEmail, true, currentFolder, search, pageable);

        Map<String, Object> stats = messageService.getMailboxStats(adminEmail, true);

        // Load specific message if selected or default to first message in page
        Message activeMessage = null;
        List<Message> threadMessages = List.of();
        if (messageId != null) {
            activeMessage = messageService.getMessageById(messageId);
            if (activeMessage != null && activeMessage.getThreadId() != null) {
                threadMessages = messageService.getConversationThread(activeMessage.getThreadId());
            }
        } else if (messagePage.hasContent()) {
            activeMessage = messageService.getMessageById(messagePage.getContent().get(0).getId());
            if (activeMessage != null && activeMessage.getThreadId() != null) {
                threadMessages = messageService.getConversationThread(activeMessage.getThreadId());
            }
        }

        // Load contact directory for quick compose autocomplete
        List<User> students = userRepository.findAll();
        List<Employee> staff = employeeRepository != null ? employeeRepository.findAll() : List.of();

        model.addAttribute("messagePage", messagePage);
        model.addAttribute("messages", messagePage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("activeMessage", activeMessage);
        model.addAttribute("threadMessages", threadMessages);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("students", students);
        model.addAttribute("staff", staff);

        return "admin/communication/messages/list";
    }

    @PostMapping({"/compose", "/send"})
    public String composeMessage(
            @RequestParam String recipientEmail,
            @RequestParam(required = false) String recipientRole,
            @RequestParam String subject,
            @RequestParam String body,
            @RequestParam(defaultValue = "NORMAL") String priority,
            @RequestParam(required = false) String attachmentUrl,
            Principal principal,
            RedirectAttributes ra) {

        String adminEmail = getAdminEmail(principal);
        try {
            messageService.sendMessage(
                    adminEmail,
                    "EduTake Administration",
                    "ADMIN",
                    recipientEmail,
                    subject,
                    body,
                    priority,
                    attachmentUrl
            );
            ra.addFlashAttribute("successMsg", "Message successfully sent to " + recipientEmail + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to send message: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=SENT";
    }

    @PostMapping("/reply")
    public String replyMessage(
            @RequestParam Long parentMessageId,
            @RequestParam String body,
            Principal principal,
            RedirectAttributes ra) {

        String adminEmail = getAdminEmail(principal);
        try {
            messageService.replyToMessage(
                    parentMessageId,
                    adminEmail,
                    "EduTake Administration",
                    "ADMIN",
                    body
            );
            ra.addFlashAttribute("successMsg", "Reply sent successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to send reply: " + e.getMessage());
        }
        return "redirect:/admin/messages?messageId=" + parentMessageId;
    }

    @PostMapping("/{id}/star")
    public String toggleStar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "INBOX") String folder,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.toggleStar(id, getAdminEmail(principal));
        } catch (Exception ignored) {}
        return "redirect:/admin/messages?folder=" + folder + "&messageId=" + id;
    }

    @PostMapping("/{id}/trash")
    public String moveToTrash(
            @PathVariable Long id,
            @RequestParam(defaultValue = "INBOX") String folder,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.moveToTrash(id, getAdminEmail(principal));
            ra.addFlashAttribute("successMsg", "Message moved to Trash.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to move message to trash: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=" + folder;
    }

    @PostMapping("/{id}/restore")
    public String restoreMessage(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.restoreFromTrash(id, getAdminEmail(principal));
            ra.addFlashAttribute("successMsg", "Message restored from Trash.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to restore message: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=INBOX&messageId=" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePermanently(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.deleteMessage(id, getAdminEmail(principal));
            ra.addFlashAttribute("successMsg", "Message permanently deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete message: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=TRASH";
    }
}
