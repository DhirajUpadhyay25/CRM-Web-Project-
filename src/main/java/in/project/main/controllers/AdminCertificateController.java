package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
