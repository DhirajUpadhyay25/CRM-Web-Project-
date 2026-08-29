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
import in.project.main.entities.Certificate;
import in.project.main.repositories.CertificateRepository;

@Controller
@RequestMapping("/admin/certificates")
public class AdminCertificateController {

    @Autowired
    private CertificateRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/learning/certificates/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String certificateCode,
                      @RequestParam String enrollmentId,
                      @RequestParam String issueDate,
                      RedirectAttributes ra) {
        try {
            Certificate certificate = new Certificate();
            certificate.setCertificateCode(certificateCode);
            certificate.setEnrollmentId(enrollmentId);
            certificate.setIssueDate(issueDate);
            repository.save(certificate);
            ra.addFlashAttribute("success", "Certificate created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create certificate: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Certificate deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete certificate: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }
}
