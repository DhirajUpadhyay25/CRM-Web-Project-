package in.project.main.controllers;

import in.project.main.dto.*;
import in.project.main.entities.Course;
import in.project.main.entities.enums.CertificateStatus;
import in.project.main.repositories.CourseRepository;
import in.project.main.services.CertificateService;
import in.project.main.services.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/certificates")
public class AdminCertificateController {

    @Autowired private CertificateService certificateService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private RbacService rbacService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.view')")
    public String listCertificates(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String statusStr,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            @RequestParam(name = "tab", defaultValue = "all") String tab,
            Model model) {

        CertificateStatus status = null;
        if ("requests".equalsIgnoreCase(tab)) {
            status = CertificateStatus.REQUESTED;
        } else if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = CertificateStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CertificateDTO> certificatesPage = certificateService.getAdminCertificatesPage(search, status, courseId, pageable);
        CertificateStatsDTO stats = certificateService.getAdminCertificateStats();
        List<Course> courses = courseRepository.findAll();

        model.addAttribute("certificatesPage", certificatesPage);
        model.addAttribute("certificates", certificatesPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("courses", courses);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("statusFilter", statusStr != null ? statusStr : "ALL");
        model.addAttribute("courseIdFilter", courseId);
        model.addAttribute("currentTab", tab);
        model.addAttribute("statuses", CertificateStatus.values());

        return "admin/learning/certificates/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.view')")
    public String certificateDetail(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        try {
            CertificateDTO cert = certificateService.getCertificateById(id);
            model.addAttribute("cert", cert);
            return "admin/learning/certificates/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Certificate not found: " + e.getMessage());
            return "redirect:/admin/certificates";
        }
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.review')")
    public String startReview(
            @PathVariable("id") Long id,
            @ModelAttribute CertificateReviewDTO dto,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
            certificateService.reviewCertificateRequest(id, adminEmail, dto);
            ra.addFlashAttribute("successMsg", "Certificate request marked as Under Review.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to start review: " + e.getMessage());
        }
        return "redirect:/admin/certificates/" + id;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.approve')")
    public String approveAndIssue(
            @PathVariable("id") Long id,
            @ModelAttribute CertificateIssueDTO dto,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
            CertificateDTO cert = certificateService.approveAndIssueCertificate(id, adminEmail, dto);
            ra.addFlashAttribute("successMsg", "Certificate " + cert.getCertificateNumber() + " officially issued to " + cert.getStudentName() + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to issue certificate: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.reject')")
    public String rejectRequest(
            @PathVariable("id") Long id,
            @RequestParam("rejectionReason") String rejectionReason,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
            certificateService.rejectCertificateRequest(id, adminEmail, rejectionReason);
            ra.addFlashAttribute("successMsg", "Certificate request rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to reject request: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.revoke')")
    public String revokeCertificate(
            @PathVariable("id") Long id,
            @RequestParam("revocationReason") String revocationReason,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
            CertificateDTO cert = certificateService.revokeCertificate(id, adminEmail, revocationReason);
            ra.addFlashAttribute("successMsg", "Certificate " + cert.getCertificateNumber() + " has been revoked.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to revoke certificate: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/reissue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.reissue')")
    public String reissueCertificate(
            @PathVariable("id") Long id,
            @ModelAttribute CertificateIssueDTO dto,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
            CertificateDTO reissued = certificateService.reissueCertificate(id, adminEmail, dto);
            ra.addFlashAttribute("successMsg", "Reissued new certificate " + reissued.getCertificateNumber() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to reissue certificate: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.analytics')")
    public String analytics(Model model) {
        CertificateAnalyticsDTO analytics = certificateService.getAdminCertificateAnalytics();
        model.addAttribute("analytics", analytics);
        model.addAttribute("stats", analytics.getStats());
        return "admin/learning/certificates/analytics";
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or @rbac.can('certificates.export')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) CertificateStatus status,
            @RequestParam(name = "courseId", required = false) Long courseId) {
        byte[] csv = certificateService.exportCertificatesCsv(search, status, courseId);
        String filename = "certificates_export_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
