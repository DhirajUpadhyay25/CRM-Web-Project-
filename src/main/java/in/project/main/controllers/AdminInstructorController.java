package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.dto.InstructorDTO;
import in.project.main.dto.InstructorDetailDTO;
import in.project.main.dto.InstructorStatsDTO;
import in.project.main.entities.Instructor;
import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.InstructorService;
import jakarta.validation.Valid;

@Controller
@RequestMapping({"/admin/instructors", "/admin/instructor"})
public class AdminInstructorController {

    @Autowired
    private InstructorService instructorService;

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isEmpty() || "newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("name_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "name");
        } else if ("name_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "name");
        } else if ("specialization_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "specialization");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("statuses", InstructorStatus.values());
        model.addAttribute("verificationStatuses", VerificationStatus.values());
        model.addAttribute("specializations", instructorService.getAllSpecializations());
    }

    // ==========================================
    // 1. INSTRUCTOR LIST & WORKSPACE
    // ==========================================
    @GetMapping({"", "/", "/list"})
    public String listInstructors(
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) InstructorStatus status,
            @RequestParam(name = "verificationStatus", required = false) VerificationStatus verificationStatus,
            @RequestParam(name = "specialization", required = false) String specialization,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "view", defaultValue = "table") String view) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolveSort(sort));
        Page<Instructor> instructorsPage = instructorService.searchAndFilterInstructors(
                keyword, status, verificationStatus, specialization, pageable);
        InstructorStatsDTO stats = instructorService.getInstructorStatistics();

        populateReferenceData(model);
        model.addAttribute("instructorsPage", instructorsPage);
        model.addAttribute("stats", stats);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("verificationStatus", verificationStatus);
        model.addAttribute("specialization", specialization);
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);

        return "admin/learning/instructors/list";
    }

    // ==========================================
    // 2. ADD INSTRUCTOR PAGE
    // ==========================================
    @GetMapping({"/new", "/add", "/create"})
    public String openAddInstructorPage(Model model) {
        InstructorDTO dto = new InstructorDTO();
        dto.setStatus(InstructorStatus.ACTIVE);
        dto.setVerificationStatus(VerificationStatus.VERIFIED);

        populateReferenceData(model);
        model.addAttribute("instructorDTO", dto);
        return "admin/learning/instructors/add";
    }

    // ==========================================
    // 3. CREATE INSTRUCTOR ACTION
    // ==========================================
    @PostMapping({"/create", "/new", "/add", "/save"})
    public String createInstructor(
            @Valid @ModelAttribute("instructorDTO") InstructorDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateReferenceData(model);
            return "admin/learning/instructors/add";
        }

        try {
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                dto.setProfileImage(profileImageFile);
            }

            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            Instructor created = instructorService.createInstructor(dto, adminEmail);

            redirectAttributes.addFlashAttribute("successMsg", 
                    "Instructor '" + created.getName() + "' created successfully!");
            return "redirect:/admin/instructors";
        } catch (IllegalArgumentException e) {
            populateReferenceData(model);
            model.addAttribute("errorMsg", e.getMessage());
            return "admin/learning/instructors/add";
        } catch (Exception e) {
            populateReferenceData(model);
            model.addAttribute("errorMsg", "Failed to create instructor: " + e.getMessage());
            return "admin/learning/instructors/add";
        }
    }

    // ==========================================
    // 4. INSTRUCTOR DETAILS PAGE (5 TABS)
    // ==========================================
    @GetMapping({"/{id}", "/view/{id}", "/detail/{id}"})
    public String openInstructorDetailsPage(
            @PathVariable Long id,
            @RequestParam(name = "tab", defaultValue = "overview") String tab,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            InstructorDetailDTO detail = instructorService.getInstructorDetail(id);
            model.addAttribute("detail", detail);
            model.addAttribute("instructor", detail.getInstructor());
            model.addAttribute("activeTab", tab);
            model.addAttribute("availableCourses", instructorService.getAvailableCoursesForAssignment(id));
            populateReferenceData(model);
            return "admin/learning/instructors/detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/instructors";
        }
    }

    // ==========================================
    // 5. EDIT INSTRUCTOR PAGE
    // ==========================================
    @GetMapping({"/{id}/edit", "/edit/{id}"})
    public String openEditInstructorPage(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Instructor instructor = instructorService.getInstructorById(id);
            if (instructor == null) {
                redirectAttributes.addFlashAttribute("errorMsg", "Instructor not found with ID: " + id);
                return "redirect:/admin/instructors";
            }
            InstructorDTO dto = new InstructorDTO();
            dto.setId(instructor.getId());
            
            String firstName = instructor.getFirstName();
            String lastName = instructor.getLastName();
            if ((firstName == null || firstName.isBlank()) && instructor.getName() != null && !instructor.getName().isBlank()) {
                String[] parts = instructor.getName().trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "Instructor";
            }
            dto.setFirstName(firstName != null ? firstName : "");
            dto.setLastName(lastName != null ? lastName : "");
            dto.setName(instructor.getName());
            dto.setEmail(instructor.getEmail());
            dto.setPhone(instructor.getPhone());
            dto.setHeadline(instructor.getHeadline());
            dto.setSpecialization(instructor.getSpecialization());
            dto.setBio(instructor.getBio());
            dto.setSkills(instructor.getSkills());
            dto.setExperience(instructor.getExperience());
            dto.setEducation(instructor.getEducation());
            dto.setCertifications(instructor.getCertifications());
            dto.setLanguages(instructor.getLanguages());
            dto.setCity(instructor.getCity());
            dto.setCountry(instructor.getCountry());
            dto.setWebsite(instructor.getWebsite());
            dto.setLinkedinUrl(instructor.getLinkedinUrl());
            dto.setGithubUrl(instructor.getGithubUrl());
            dto.setStatus(instructor.getStatus() != null ? instructor.getStatus() : InstructorStatus.ACTIVE);
            dto.setVerificationStatus(instructor.getVerificationStatus() != null ? instructor.getVerificationStatus() : VerificationStatus.VERIFIED);
            dto.setExistingImageUrl(instructor.getImageUrl());

            populateReferenceData(model);
            model.addAttribute("instructorDTO", dto);
            model.addAttribute("instructorId", id);
            return "admin/learning/instructors/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to open edit page: " + e.getMessage());
            return "redirect:/admin/instructors";
        }
    }

    // ==========================================
    // 6. UPDATE INSTRUCTOR ACTION
    // ==========================================
    @PostMapping({"/{id}/update", "/{id}/edit", "/update/{id}", "/edit/{id}"})
    public String updateInstructor(
            @PathVariable Long id,
            @Valid @ModelAttribute("instructorDTO") InstructorDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateReferenceData(model);
            model.addAttribute("instructorId", id);
            return "admin/learning/instructors/edit";
        }

        try {
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                dto.setProfileImage(profileImageFile);
            }

            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            Instructor updated = instructorService.updateInstructor(id, dto, adminEmail);

            redirectAttributes.addFlashAttribute("successMsg", 
                    "Instructor '" + updated.getName() + "' updated successfully!");
            return "redirect:/admin/instructors/" + id;
        } catch (IllegalArgumentException e) {
            populateReferenceData(model);
            model.addAttribute("instructorId", id);
            model.addAttribute("errorMsg", e.getMessage());
            return "admin/learning/instructors/edit";
        } catch (Exception e) {
            populateReferenceData(model);
            model.addAttribute("instructorId", id);
            model.addAttribute("errorMsg", "Failed to update instructor: " + e.getMessage());
            return "admin/learning/instructors/edit";
        }
    }

    // ==========================================
    // 7. STATUS LIFECYCLE ACTION (ACTIVATE/DEACTIVATE/SUSPEND/BAN)
    // ==========================================
    @PostMapping({"/{id}/status", "/status/{id}"})
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam("status") InstructorStatus status,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "redirect", required = false) String redirectUrl,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            Instructor updated = instructorService.updateInstructorStatus(id, status, reason, adminEmail);

            redirectAttributes.addFlashAttribute("successMsg", 
                    "Instructor status changed to " + updated.getStatus().getDisplayName() + " successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update status: " + e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/instructors";
    }

    // ==========================================
    // 8. SAFE DELETE INSTRUCTOR ACTION
    // ==========================================
    @PostMapping({"/{id}/delete", "/delete/{id}"})
    public String deleteInstructor(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            instructorService.deleteInstructor(id, adminEmail);

            redirectAttributes.addFlashAttribute("successMsg", "Instructor deleted successfully.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete instructor: " + e.getMessage());
        }

        return "redirect:/admin/instructors";
    }

    // ==========================================
    // 9. ASSIGN COURSE TO INSTRUCTOR
    // ==========================================
    @PostMapping({"/{id}/courses/assign", "/courses/{id}/assign", "/{id}/assign-course"})
    public String assignCourse(
            @PathVariable Long id,
            @RequestParam("courseId") Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            instructorService.assignCourse(id, courseId, adminEmail);

            redirectAttributes.addFlashAttribute("successMsg", "Course assigned to instructor successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to assign course: " + e.getMessage());
        }

        return "redirect:/admin/instructors/" + id + "?tab=courses";
    }

    // ==========================================
    // 10. UNASSIGN COURSE FROM INSTRUCTOR
    // ==========================================
    @PostMapping({"/{id}/courses/{courseId}/unassign", "/courses/{id}/unassign/{courseId}", "/{id}/unassign-course/{courseId}"})
    public String unassignCourse(
            @PathVariable Long id,
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            instructorService.unassignCourse(id, courseId, adminEmail);

            redirectAttributes.addFlashAttribute("successMsg", "Course removed from instructor.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to unassign course: " + e.getMessage());
        }

        return "redirect:/admin/instructors/" + id + "?tab=courses";
    }
}
