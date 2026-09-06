package in.project.main.controllers;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.EmailTemplate;
import in.project.main.entities.Employee;
import in.project.main.entities.Message;
import in.project.main.entities.User;
import in.project.main.repositories.CourseRepository;
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

    @Autowired(required = false)
    private CourseRepository courseRepository;

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
        String currentFolder = folder != null ? folder.trim().toUpperCase() : "INBOX";

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

        // Load contact directory, courses, and templates for composer
        List<User> students = userRepository.findAll();
        List<Employee> staff = employeeRepository != null ? employeeRepository.findAll() : List.of();
        List<Course> courses = courseRepository != null ? courseRepository.findAll() : List.of();
        List<EmailTemplate> emailTemplates = messageService.getAllEmailTemplates();

        model.addAttribute("messagePage", messagePage);
        model.addAttribute("messages", messagePage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("activeMessage", activeMessage);
        model.addAttribute("threadMessages", threadMessages);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("students", students);
        model.addAttribute("staff", staff);
        model.addAttribute("courses", courses);
        model.addAttribute("emailTemplates", emailTemplates);

        return "admin/communication/messages/list";
    }

    @PostMapping({"/compose", "/send"})
    public String composeMessage(
            @RequestParam(name = "composeMode", defaultValue = "INDIVIDUAL") String composeMode,
            @RequestParam(name = "targetAudience", defaultValue = "INDIVIDUAL") String targetAudience,
            @RequestParam(name = "recipientEmail", required = false) String recipientEmail,
            @RequestParam(name = "recipientRole", required = false) String recipientRole,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "subject") String subject,
            @RequestParam(name = "body") String body,
            @RequestParam(name = "priority", defaultValue = "NORMAL") String priority,
            @RequestParam(name = "attachmentUrl", required = false) String attachmentUrl,
            Principal principal,
            RedirectAttributes ra) {

        String adminEmail = getAdminEmail(principal);
        try {
            if ("BROADCAST".equalsIgnoreCase(composeMode) || !"INDIVIDUAL".equalsIgnoreCase(targetAudience)) {
                int count = messageService.sendBroadcastMessage(
                        adminEmail,
                        "EduTake Administration",
                        "ADMIN",
                        targetAudience,
                        courseId,
                        subject,
                        body,
                        priority,
                        attachmentUrl
                );
                ra.addFlashAttribute("successMsg", "Bulk email broadcast dispatched to " + count + " recipients.");
                ra.addFlashAttribute("success", "Bulk email broadcast dispatched to " + count + " recipients.");
            } else {
                if (recipientEmail == null || recipientEmail.isBlank()) {
                    ra.addFlashAttribute("errorMsg", "Recipient email address is required.");
                    ra.addFlashAttribute("error", "Recipient email address is required.");
                    return "redirect:/admin/messages?folder=INBOX";
                }
                messageService.sendMessage(
                        adminEmail,
                        "EduTake Administration",
                        "ADMIN",
                        recipientEmail.trim(),
                        subject,
                        body,
                        priority,
                        attachmentUrl
                );
                ra.addFlashAttribute("successMsg", "Message successfully sent to " + recipientEmail + ".");
                ra.addFlashAttribute("success", "Message successfully sent to " + recipientEmail + ".");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to send message: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to send message: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=SENT";
    }

    @PostMapping("/reply")
    public String replyMessage(
            @RequestParam(name = "parentMessageId") Long parentMessageId,
            @RequestParam(name = "body") String body,
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
            ra.addFlashAttribute("success", "Reply sent successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to send reply: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to send reply: " + e.getMessage());
        }
        return "redirect:/admin/messages?messageId=" + parentMessageId;
    }

    @PostMapping("/{id}/star")
    public String toggleStar(
            @PathVariable("id") Long id,
            @RequestParam(name = "folder", defaultValue = "INBOX") String folder,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.toggleStar(id, getAdminEmail(principal));
        } catch (Exception ignored) {}
        return "redirect:/admin/messages?folder=" + folder + "&messageId=" + id;
    }

    @PostMapping("/{id}/trash")
    public String moveToTrash(
            @PathVariable("id") Long id,
            @RequestParam(name = "folder", defaultValue = "INBOX") String folder,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.moveToTrash(id, getAdminEmail(principal));
            ra.addFlashAttribute("successMsg", "Message moved to Trash.");
            ra.addFlashAttribute("success", "Message moved to Trash.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to move message to trash: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to move message to trash: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=" + folder;
    }

    @PostMapping("/{id}/restore")
    public String restoreMessage(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.restoreFromTrash(id, getAdminEmail(principal));
            ra.addFlashAttribute("successMsg", "Message restored from Trash.");
            ra.addFlashAttribute("success", "Message restored from Trash.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to restore message: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to restore message: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=INBOX&messageId=" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePermanently(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes ra) {
        try {
            messageService.deleteMessage(id, getAdminEmail(principal));
            ra.addFlashAttribute("successMsg", "Message permanently deleted.");
            ra.addFlashAttribute("success", "Message permanently deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete message: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to delete message: " + e.getMessage());
        }
        return "redirect:/admin/messages?folder=TRASH";
    }

    @PostMapping("/templates/save")
    public String saveTemplate(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "templateKey", required = false) String templateKey,
            @RequestParam(name = "category", defaultValue = "GENERAL") String category,
            @RequestParam(name = "subjectTemplate") String subjectTemplate,
            @RequestParam(name = "bodyTemplate") String bodyTemplate,
            @RequestParam(name = "description", required = false) String description,
            RedirectAttributes ra) {
        try {
            String key = (templateKey != null && !templateKey.isBlank()) 
                    ? templateKey.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_") 
                    : "tpl_" + System.currentTimeMillis();

            EmailTemplate tpl = messageService.getEmailTemplateByKey(key);
            if (tpl == null) {
                tpl = new EmailTemplate();
                tpl.setTemplateKey(key);
                tpl.setIsSystemTemplate(false);
            }
            tpl.setName(name.trim());
            tpl.setCategory(category != null ? category.trim().toUpperCase() : "GENERAL");
            tpl.setSubjectTemplate(subjectTemplate.trim());
            tpl.setBodyTemplate(bodyTemplate.trim());
            tpl.setDescription(description != null ? description.trim() : "");

            messageService.saveEmailTemplate(tpl);
            ra.addFlashAttribute("successMsg", "Email template '" + tpl.getName() + "' saved successfully.");
            ra.addFlashAttribute("success", "Email template '" + tpl.getName() + "' saved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to save template: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to save template: " + e.getMessage());
        }
        return "redirect:/admin/messages";
    }

    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(
            @PathVariable("id") Long id,
            RedirectAttributes ra) {
        try {
            messageService.deleteEmailTemplate(id);
            ra.addFlashAttribute("successMsg", "Email template removed.");
            ra.addFlashAttribute("success", "Email template removed.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete template: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to delete template: " + e.getMessage());
        }
        return "redirect:/admin/messages";
    }

    @PostMapping("/seed-defaults")
    public String seedDefaults(RedirectAttributes ra) {
        try {
            messageService.seedDefaultTemplates();
            messageService.seedSampleConversations();
            ra.addFlashAttribute("successMsg", "Seeded default email templates and sample inbox messages.");
            ra.addFlashAttribute("success", "Seeded default email templates and sample inbox messages.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to seed defaults: " + e.getMessage());
            ra.addFlashAttribute("error", "Failed to seed defaults: " + e.getMessage());
        }
        return "redirect:/admin/messages";
    }

    // ==========================================
    // REST API Endpoints
    // ==========================================

    @GetMapping(value = "/api/templates", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<EmailTemplate>> getTemplatesApi() {
        return ResponseEntity.ok(messageService.getAllEmailTemplates());
    }

    @GetMapping(value = "/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatsApi(Principal principal) {
        return ResponseEntity.ok(messageService.getMailboxStats(getAdminEmail(principal), true));
    }
}
