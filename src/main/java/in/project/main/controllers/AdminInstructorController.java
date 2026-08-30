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
import in.project.main.entities.Instructor;
import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.repositories.InstructorRepository;
import in.project.main.repositories.EmployeeRepository;

@Controller
@RequestMapping("/admin/instructors")
public class AdminInstructorController {

    @Autowired
    private InstructorRepository repository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/learning/instructors/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String name,
                      @RequestParam String email,
                      @RequestParam String bio,
                      @RequestParam String specialization,
                      @RequestParam String status,
                      RedirectAttributes ra) {
        try {
            Instructor instructor = new Instructor();
            instructor.setName(name);
            instructor.setEmail(email);
            instructor.setBio(bio);
            instructor.setSpecialization(specialization);
            instructor.setStatus(status);
            repository.save(instructor);

            // Sync with Employee table to enable login
            Employee employee = employeeRepository.findByEmail(email);
            if (employee == null) {
                employee = new Employee();
                employee.setEmail(email);
                employee.setPassword("instructor123"); // Default password
            }
            employee.setName(name);
            employee.setRole(Role.INSTRUCTOR);
            employeeRepository.save(employee);

            ra.addFlashAttribute("success", "Instructor created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create instructor: " + e.getMessage());
        }
        return "redirect:/admin/instructors";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam String email,
                         @RequestParam String bio,
                         @RequestParam String specialization,
                         @RequestParam String status,
                         RedirectAttributes ra) {
        try {
            Instructor instructor = repository.findById(id).orElseThrow(() -> new RuntimeException("Instructor not found"));
            String oldEmail = instructor.getEmail();
            instructor.setName(name);
            instructor.setEmail(email);
            instructor.setBio(bio);
            instructor.setSpecialization(specialization);
            instructor.setStatus(status);
            repository.save(instructor);

            // Sync with Employee table
            Employee employee = employeeRepository.findByEmail(email);
            if (employee == null && !email.equals(oldEmail)) {
                employee = employeeRepository.findByEmail(oldEmail);
            }
            if (employee == null) {
                employee = new Employee();
                employee.setEmail(email);
                employee.setPassword("instructor123");
            }
            employee.setEmail(email);
            employee.setName(name);
            employee.setRole(Role.INSTRUCTOR);
            employeeRepository.save(employee);

            ra.addFlashAttribute("success", "Instructor updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update instructor: " + e.getMessage());
        }
        return "redirect:/admin/instructors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Instructor instructor = repository.findById(id).orElseThrow(() -> new RuntimeException("Instructor not found"));
            String email = instructor.getEmail();
            repository.deleteById(id);

            // Remove from Employee table if exists
            Employee employee = employeeRepository.findByEmail(email);
            if (employee != null) {
                employeeRepository.delete(employee);
            }

            ra.addFlashAttribute("success", "Instructor deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete instructor: " + e.getMessage());
        }
        return "redirect:/admin/instructors";
    }
}
