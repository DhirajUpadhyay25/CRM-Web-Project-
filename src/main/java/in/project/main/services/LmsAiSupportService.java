package in.project.main.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.dto.SupportQueryDTO;
import in.project.main.entities.Faq;
import in.project.main.repositories.FaqRepository;

@Service
public class LmsAiSupportService {

    private static final Logger logger = LoggerFactory.getLogger(LmsAiSupportService.class);

    private static final String WHATSAPP_PHONE = "918130279213";
    private static final String WHATSAPP_BASE_URL = "https://wa.me/" + WHATSAPP_PHONE;

    @Autowired(required = false)
    private FaqRepository faqRepository;

    // Patterns that strictly require human escalation
    private static final Pattern ESCALATION_PATTERNS = Pattern.compile(
            "\\b(refund|money back|dispute|chargeback|fraud|stolen|hacked|ban|suspended|lawsuit|legal|override grade|failed exam dispute|reset password for another)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Resolves context information, contextual quick actions, and deep-link pre-filled WhatsApp messages
     * based on the user's current URL, role, course, and lesson.
     */
    public Map<String, Object> resolveContext(String route, Long courseId, String courseName, Long lessonId, String lessonTitle, String role) {
        Map<String, Object> result = new HashMap<>();

        String cleanRoute = (route != null) ? route.trim().toLowerCase() : "/";
        String contextType = "GENERAL";
        String contextTitle = "EduTake Learning Platform";
        String contextDescription = "We're here to help you keep learning smoothly.";
        List<Map<String, String>> quickActions = new ArrayList<>();
        String defaultWhatsAppText;

        // 1. Determine Context by route / parameters
        if (cleanRoute.contains("/player")) {
            contextType = "LESSON_PLAYER";
            contextTitle = "Course Player: " + (lessonTitle != null ? lessonTitle : "Active Lesson");
            contextDescription = (courseName != null ? "Course: " + courseName : "Learning Session");
            
            quickActions.add(createAction("video_issue", "Video isn't playing", "bi-play-circle", "video"));
            quickActions.add(createAction("progress_issue", "Progress not updating", "bi-arrow-repeat", "progress"));
            quickActions.add(createAction("lesson_question", "Question about this lesson", "bi-chat-text", "ai"));
            quickActions.add(createAction("report_bug", "Report lesson problem", "bi-bug", "report"));

            defaultWhatsAppText = "Hello EduTake Support, I need help with lesson \"" +
                    (lessonTitle != null ? lessonTitle : "video") + "\" in course \"" +
                    (courseName != null ? courseName : "my enrolled course") + "\" (URL: " + route + ").";

        } else if (cleanRoute.contains("/quiz")) {
            contextType = "QUIZ_ASSESSMENT";
            contextTitle = "Assessment: " + (courseName != null ? courseName : "Quiz");
            contextDescription = "Quiz attempts require 80% score to pass for certification.";

            quickActions.add(createAction("quiz_load", "Quiz didn't load properly", "bi-exclamation-circle", "report"));
            quickActions.add(createAction("quiz_answers", "Answer wasn't saved", "bi-save", "report"));
            quickActions.add(createAction("quiz_rules", "Passing score & retake policy", "bi-info-circle", "ai"));
            quickActions.add(createAction("talk_support", "Contact support immediately", "bi-whatsapp", "whatsapp"));

            defaultWhatsAppText = "Hello EduTake Support, I encountered an issue during a quiz attempt in course \"" +
                    (courseName != null ? courseName : "LMS Quiz") + "\".";

        } else if (cleanRoute.contains("/assignment")) {
            contextType = "ASSIGNMENT";
            contextTitle = "Course Assignment";
            contextDescription = "Upload ZIP or PDF files up to 10MB.";

            quickActions.add(createAction("upload_issue", "Upload isn't working", "bi-upload", "report"));
            quickActions.add(createAction("submission_status", "Check submission status", "bi-check2-circle", "ai"));
            quickActions.add(createAction("assignment_rules", "Allowed file formats & limits", "bi-file-earmark-code", "ai"));
            quickActions.add(createAction("talk_support", "WhatsApp Support", "bi-whatsapp", "whatsapp"));

            defaultWhatsAppText = "Hello EduTake Support, I am having trouble uploading an assignment in course \"" +
                    (courseName != null ? courseName : "my course") + "\".";

        } else if (cleanRoute.contains("/certificate")) {
            contextType = "CERTIFICATE";
            contextTitle = "Certificate Center";
            contextDescription = "View, download, and verify your verified course certificates.";

            quickActions.add(createAction("cert_not_ready", "Certificate isn't available", "bi-award", "ai"));
            quickActions.add(createAction("cert_name_error", "Name or details incorrect", "bi-pencil-square", "ticket"));
            quickActions.add(createAction("cert_verify", "How to verify certificate", "bi-shield-check", "ai"));
            quickActions.add(createAction("talk_support", "WhatsApp Assistance", "bi-whatsapp", "whatsapp"));

            defaultWhatsAppText = "Hello EduTake Support, I have an inquiry about my course completion certificate.";

        } else if (cleanRoute.contains("/order") || cleanRoute.contains("/payment") || cleanRoute.contains("/cart")) {
            contextType = "PAYMENT";
            contextTitle = "Orders & Payment";
            contextDescription = "Payment gateway, invoice, receipt, and enrollment access.";

            quickActions.add(createAction("payment_failed", "Payment deducted but course locked", "bi-credit-card-2-front", "whatsapp"));
            quickActions.add(createAction("invoice_download", "Where is my invoice/receipt?", "bi-receipt", "ai"));
            quickActions.add(createAction("refund_query", "Refund policy & process", "bi-arrow-counterclockwise", "ai"));
            quickActions.add(createAction("submit_ticket", "Submit billing ticket", "bi-ticket-detailed", "ticket"));

            defaultWhatsAppText = "Hello EduTake Support, I have a query regarding a payment/order on EduTake.";

        } else if (cleanRoute.contains("/admin")) {
            contextType = "ADMIN_PORTAL";
            contextTitle = "LMS Administration";
            contextDescription = "EduTake management, student records, and system health.";

            quickActions.add(createAction("admin_tickets", "View open support tickets", "bi-ticket-perforated", "navigate:/admin/support"));
            quickActions.add(createAction("system_health", "Check system logs & health", "bi-activity", "navigate:/admin/monitoring"));
            quickActions.add(createAction("faqs_manage", "Manage Help FAQs", "bi-patch-question", "navigate:/admin/faqs"));

            defaultWhatsAppText = "Hello EduTake Admin Support, administrator assistance requested.";

        } else if (cleanRoute.contains("/instructor")) {
            contextType = "INSTRUCTOR_PORTAL";
            contextTitle = "Instructor Portal";
            contextDescription = "Course curriculum, student submissions, and feedback.";

            quickActions.add(createAction("review_submissions", "Assignment submissions", "bi-journal-check", "ai"));
            quickActions.add(createAction("curriculum_help", "How to add video lessons", "bi-collection-play", "ai"));
            quickActions.add(createAction("instructor_support", "Staff Support WhatsApp", "bi-whatsapp", "whatsapp"));

            defaultWhatsAppText = "Hello EduTake Support, instructor assistance requested for my course batch.";

        } else if (cleanRoute.contains("/course")) {
            contextType = "COURSE_CATALOG";
            contextTitle = (courseName != null ? courseName : "Course Details & Enrollment");
            contextDescription = "Explore syllabus, instructor background, and enrollment.";

            quickActions.add(createAction("course_enroll", "How do I enroll in this course?", "bi-mortarboard", "ai"));
            quickActions.add(createAction("course_prereq", "Recommended prerequisites", "bi-card-checklist", "ai"));
            quickActions.add(createAction("chat_advisor", "Chat with Academic Advisor", "bi-whatsapp", "whatsapp"));
            quickActions.add(createAction("report_issue", "Report page issue", "bi-bug", "report"));

            defaultWhatsAppText = "Hello EduTake Team, I am interested in course \"" +
                    (courseName != null ? courseName : "courses") + "\" and have questions before enrolling.";

        } else {
            // General / Home / Profile
            contextType = "GENERAL";
            contextTitle = "EduTake Learning Support";
            contextDescription = "How can we help you continue your learning journey?";

            quickActions.add(createAction("search_help", "Search Help & FAQs", "bi-search", "search"));
            quickActions.add(createAction("ask_ai", "Ask LMS Assistant", "bi-robot", "ai"));
            quickActions.add(createAction("whatsapp_direct", "Chat on WhatsApp", "bi-whatsapp", "whatsapp"));
            quickActions.add(createAction("submit_ticket", "Submit Support Ticket", "bi-ticket-detailed", "ticket"));
            quickActions.add(createAction("report_bug", "Report a Problem", "bi-bug", "report"));

            defaultWhatsAppText = "Hello EduTake Support, I need assistance with my learning account.";
        }

        result.put("contextType", contextType);
        result.put("contextTitle", contextTitle);
        result.put("contextDescription", contextDescription);
        result.put("quickActions", quickActions);
        result.put("whatsAppUrl", buildWhatsAppUrl(defaultWhatsAppText));
        result.put("whatsAppText", defaultWhatsAppText);

        return result;
    }

    /**
     * Answers learner queries using grounded LMS knowledge, live FAQ search, safety guardrails,
     * and human escalation when needed.
     */
    public Map<String, Object> processQuery(SupportQueryDTO dto) {
        Map<String, Object> response = new HashMap<>();

        String query = (dto.getMessage() != null) ? dto.getMessage().trim() : "";
        if (query.isBlank()) {
            response.put("answer", "Please enter a question or select one of the suggested options.");
            response.put("requiresHumanEscalation", false);
            return response;
        }

        // 1. SAFETY & HUMAN ESCALATION CHECK
        if (ESCALATION_PATTERNS.matcher(query).find()) {
            logger.info("Query matched human escalation pattern: {}", query);
            String escalationWhatsAppText = "Hello EduTake Support, I need human assistance regarding: " + query;
            response.put("answer",
                    "This appears to be an account-specific or administrative request (e.g. payment dispute, refund, account access, or grade review) that requires verification by our support team.\n\n" +
                    "For your security, automated assistants cannot modify transactions or credentials. You can reach our team immediately via WhatsApp or submit a formal ticket below.");
            response.put("requiresHumanEscalation", true);
            response.put("escalationType", "ADMIN_VERIFICATION_REQUIRED");
            response.put("whatsAppUrl", buildWhatsAppUrl(escalationWhatsAppText));
            response.put("suggestedAction", "whatsapp");
            return response;
        }

        // 2. LIVE FAQ LOOKUP
        if (faqRepository != null) {
            try {
                List<Faq> matchingFaqs = faqRepository.searchActive(query);
                if (!matchingFaqs.isEmpty()) {
                    Faq topFaq = matchingFaqs.get(0);
                    response.put("answer", cleanHtml(topFaq.getAnswer()));
                    response.put("source", "LMS Knowledge Base: " + topFaq.getQuestion());
                    response.put("faqId", topFaq.getId());
                    response.put("requiresHumanEscalation", false);
                    return response;
                }
            } catch (Exception e) {
                logger.warn("Live FAQ search failed: {}", e.getMessage());
            }
        }

        // 3. GROUNDED LMS KNOWLEDGE BASE ENGINE
        String lowerQuery = query.toLowerCase();
        String answer = null;
        List<String> relatedActions = new ArrayList<>();

        if (containsAny(lowerQuery, "video", "play", "buffering", "stuck", "black screen", "player")) {
            answer = "Here is how to resolve video playback issues in the Course Player:\n\n" +
                    "1. **Refresh your player tab**: Most intermittent network interruptions resolve upon reloading.\n" +
                    "2. **Check Browser Hardware Acceleration**: Ensure hardware acceleration is enabled in Chrome/Firefox settings.\n" +
                    "3. **Clear browser cache**: Outdated cached chunks can cause media stalls.\n" +
                    "4. **Switch network connection**: High-definition streams require at least a stable 5 Mbps connection.\n\n" +
                    "If the video remains unplayable, click **Report a Problem** so our technical team can inspect the video server stream.";
            relatedActions.add("Report a Problem");
            relatedActions.add("Chat on WhatsApp");

        } else if (containsAny(lowerQuery, "progress", "percent", "not updating", "mark complete", "checkbox")) {
            answer = "Your course progress updates automatically when you finish a lesson:\n\n" +
                    "• **Video Lessons**: Watch the video to at least 90% or click the **'Mark as Complete'** button at the end of the lesson.\n" +
                    "• **Sequential Unlock**: If a lesson appears locked with a padlock icon, complete all preceding lessons first.\n" +
                    "• **Quizzes & Assignments**: Quizzes and assignments require passing scores to count toward completion.";
            relatedActions.add("View My Courses");

        } else if (containsAny(lowerQuery, "certificate", "cert", "download", "diploma", "verify")) {
            answer = "To receive your verified EduTake Course Certificate:\n\n" +
                    "1. **Complete 100% of the lessons** in the course.\n" +
                    "2. **Pass all required quizzes** with an 80% or higher score.\n" +
                    "3. **Submit any required capstone assignments**.\n" +
                    "4. Navigate to **Certificates** in your student navigation bar and click **'Generate Certificate'**.\n\n" +
                    "Each certificate comes with a unique verification code that can be verified publicly at `/verify-certificate`.";
            relatedActions.add("Go to Certificates");

        } else if (containsAny(lowerQuery, "quiz", "assessment", "score", "retake", "attempts")) {
            answer = "EduTake Quiz Guidelines:\n\n" +
                    "• The passing threshold is **80%**.\n" +
                    "• Once submitted, you will immediately see your detailed scorecard with correct answers.\n" +
                    "• If you do not pass on your first attempt, you can retake the quiz from the Course Player or Quiz Result page.\n" +
                    "• Quiz results are instantly saved and linked to your student progress record.";

        } else if (containsAny(lowerQuery, "assignment", "submit", "upload", "format", "deadline")) {
            answer = "Submitting Course Assignments:\n\n" +
                    "• Navigate to the specific assignment in your course syllabus.\n" +
                    "• Accepted file types include **ZIP, PDF, DOCX, and TXT** (up to 10MB).\n" +
                    "• Once uploaded, your instructor will review and provide feedback. You will receive a notification as soon as it is graded.";

        } else if (containsAny(lowerQuery, "password", "reset", "login", "account", "profile")) {
            answer = "You can update your name, email, and password directly from your **Profile Settings** page (`/student/profile` or `/userProfile`). If you have forgotten your password, use the **'Forgot Password'** option on the login screen.";
            relatedActions.add("Go to Profile");

        } else if (containsAny(lowerQuery, "invoice", "receipt", "billing", "tax")) {
            answer = "You can view and download invoices for all completed course purchases under **Orders & Invoices** (`/student/orders`). Each invoice includes the transaction ID, date, course item, and payment breakdown.";
            relatedActions.add("View Orders");

        } else if (containsAny(lowerQuery, "instructor", "mentor", "ask teacher", "doubt")) {
            answer = "To get assistance with course concepts:\n\n" +
                    "• Check the course discussion tab or instructor feedback area.\n" +
                    "• Attend the weekly live Q&A sessions scheduled on your calendar.\n" +
                    "• Submit a question through this help drawer, and our academic mentors will respond.";
            relatedActions.add("Chat on WhatsApp");

        } else {
            // General platform guidance with graceful escalation
            answer = "EduTake provides hands-on technical learning with expert-led courses, coding modules, quizzes, and verified career certificates.\n\n" +
                    "I am here to guide you with course navigation, player troubleshooting, progress, and certificates.\n\n" +
                    "If your query is specific to your account or requires personal assistance, our support team is available on WhatsApp or via a support ticket.";
            relatedActions.add("Chat on WhatsApp");
            relatedActions.add("Submit Support Ticket");
        }

        response.put("answer", answer);
        response.put("relatedActions", relatedActions);
        response.put("requiresHumanEscalation", false);
        response.put("whatsAppUrl", buildWhatsAppUrl("Hello EduTake Support, I need assistance regarding: " + query));

        return response;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
    }

    private Map<String, String> createAction(String id, String label, String icon, String actionType) {
        Map<String, String> map = new HashMap<>();
        map.put("id", id);
        map.put("label", label);
        map.put("icon", icon);
        map.put("actionType", actionType);
        return map;
    }

    public String buildWhatsAppUrl(String prefilledMessage) {
        try {
            String encoded = URLEncoder.encode(prefilledMessage, StandardCharsets.UTF_8);
            return WHATSAPP_BASE_URL + "?text=" + encoded;
        } catch (Exception e) {
            return WHATSAPP_BASE_URL;
        }
    }
}
