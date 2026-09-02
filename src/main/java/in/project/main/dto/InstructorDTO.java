package in.project.main.dto;

import org.springframework.web.multipart.MultipartFile;

import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating and updating Instructor records.
 */
public class InstructorDTO {

    private Long id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    private String name; // Computed or provided

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{7,20}$", message = "Please provide a valid phone number")
    private String phone;

    private String headline;

    private String specialization;

    private String bio;

    private String skills;

    private String experience;

    private String education;

    private String certifications;

    private String languages;

    private String city;

    private String country;

    private String website;

    private String linkedinUrl;

    private String githubUrl;

    private InstructorStatus status = InstructorStatus.ACTIVE;

    private VerificationStatus verificationStatus = VerificationStatus.VERIFIED;

    private String password; // Optional: password when creating or resetting instructor login

    private MultipartFile profileImage;

    private String existingImageUrl;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getName() { 
        if (name == null || name.trim().isEmpty()) {
            return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        }
        return name; 
    }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public InstructorStatus getStatus() { return status; }
    public void setStatus(InstructorStatus status) { this.status = status; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public MultipartFile getProfileImage() { return profileImage; }
    public void setProfileImage(MultipartFile profileImage) { this.profileImage = profileImage; }

    public String getExistingImageUrl() { return existingImageUrl; }
    public void setExistingImageUrl(String existingImageUrl) { this.existingImageUrl = existingImageUrl; }
}
