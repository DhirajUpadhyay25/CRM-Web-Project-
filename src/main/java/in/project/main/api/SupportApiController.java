package in.project.main.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.dto.ProblemReportDTO;
import in.project.main.dto.SupportQueryDTO;
import in.project.main.dto.SupportTicketDTO;
import in.project.main.entities.Faq;
import in.project.main.entities.SupportTicket;
import in.project.main.entities.User;
import in.project.main.repositories.FaqRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.LmsAiSupportService;
import in.project.main.services.SupportTicketService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/support")
public class SupportApiController {

    private static final Logger logger = LoggerFactory.getLogger(SupportApiController.class);

    @Autowired
    private LmsAiSupportService aiSupportService;

    @Autowired
    private SupportTicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private FaqRepository faqRepository;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email);
        }
        return null;
    }

    /**
     * Resolves context-aware options and tailored WhatsApp deep links for current route/view.
     */
    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getContext(
            @RequestParam(required = false) String route,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String lessonTitle,
            @RequestParam(required = false) String role) {

        User user = getAuthenticatedUser();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String effectiveRole = "GUEST";
        if (user != null) {
            effectiveRole = "STUDENT";
        } else if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            effectiveRole = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("USER");
        } else if (role != null && !role.isBlank()) {
            effectiveRole = role;
        }

        Map<String, Object> context = aiSupportService.resolveContext(route, courseId, courseName, lessonId, lessonTitle, effectiveRole);
        if (user != null) {
            context.put("isAuthenticated", true);
            context.put("userName", user.getName());
            context.put("userEmail", user.getEmail());
            context.put("userRole", effectiveRole);
        } else {
            context.put("isAuthenticated", false);
        }

        return ResponseEntity.ok(context);
    }

    /**
     * Ask the LMS AI Assistant a question with grounded context.
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askAssistant(@Valid @RequestBody SupportQueryDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Please provide a valid question.");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> response = aiSupportService.processQuery(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Live search active FAQs.
     */
    @GetMapping("/faqs")
    public ResponseEntity<List<Map<String, Object>>> searchFaqs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String contextTag) {

        if (faqRepository == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Faq> faqs;
        if (query != null && !query.trim().isEmpty()) {
            faqs = faqRepository.searchActive(query.trim());
        } else if (contextTag != null && !contextTag.trim().isEmpty()) {
            faqs = faqRepository.findByContextTag(contextTag.trim().toUpperCase());
        } else {
            faqs = faqRepository.findByIsActiveTrueOrderBySortOrderAsc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(faqs.size(), 8);
        for (int i = 0; i < limit; i++) {
            Faq f = faqs.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", f.getId());
            item.put("question", f.getQuestion());
            item.put("answer", f.getAnswer());
            item.put("category", f.getFaqCategory() != null ? f.getFaqCategory().getName() : "General");
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Submit a persistent support ticket.
     */
    @PostMapping("/ticket")
    public ResponseEntity<?> submitTicket(@Valid @RequestBody SupportTicketDTO dto, BindingResult bindingResult) {
        User user = getAuthenticatedUser();

        // If not authenticated, email and name are mandatory
        if (user == null) {
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required for guest support requests."));
            }
            if (dto.getName() == null || dto.getName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required for guest support requests."));
            }
        }

        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", firstError));
        }

        try {
            SupportTicket ticket = ticketService.createTicket(dto, user);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("ticketId", ticket.getId());
            resp.put("ticketNumber", ticket.getTicketNumber());
            resp.put("message", "Your support request #" + ticket.getTicketNumber() + " has been submitted. Our team will get back to you shortly.");
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            logger.error("Failed to submit support ticket: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not submit support request. Please try contacting us via WhatsApp."));
        }
    }

    /**
     * Submit a lightweight 1-click problem/bug report.
     */
    @PostMapping("/report-problem")
    public ResponseEntity<?> reportProblem(@Valid @RequestBody ProblemReportDTO dto, BindingResult bindingResult) {
        User user = getAuthenticatedUser();

        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", firstError));
        }

        try {
            SupportTicket ticket = ticketService.reportProblem(dto, user);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("ticketNumber", ticket.getTicketNumber());
            resp.put("message", "Problem reported successfully. Reference #" + ticket.getTicketNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            logger.error("Failed to submit problem report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not submit report. Please try contacting support on WhatsApp."));
        }
    }

    /**
     * Returns list of support tickets for the current authenticated user.
     */
    @GetMapping("/tickets/my")
    public ResponseEntity<?> getMyTickets() {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        List<SupportTicket> tickets = ticketService.getTicketsByUserEmail(user.getEmail());
        List<Map<String, Object>> result = new ArrayList<>();
        for (SupportTicket t : tickets) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("ticketNumber", t.getTicketNumber());
            item.put("subject", t.getSubject());
            item.put("status", t.getStatus().name());
            item.put("statusDisplay", t.getStatus().getDisplayName());
            item.put("statusBadge", t.getStatus().getBadgeClass());
            item.put("priority", t.getPriority().name());
            item.put("category", t.getCategory().getDisplayName());
            item.put("createdAt", t.getCreatedAt().toString());
            item.put("replyCount", t.getReplies() != null ? t.getReplies().size() : 0);
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }
}
