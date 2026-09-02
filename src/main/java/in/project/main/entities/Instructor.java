package in.project.main.entities;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "instructor", indexes = {
    @Index(name = "idx_instructor_email", columnList = "email", unique = true),
    @Index(name = "idx_instructor_status", columnList = "status"),
    @Index(name = "idx_instructor_specialization", columnList = "specialization")
})
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phone;

    @Column
    private String imageUrl;

    @Column
    private String headline;

    @Column
    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String skills;

    @Column
    private String experience;

    @Column
    private String education;

    @Column(length = 500)
    private String certifications;

    @Column
    private String languages;

    @Column
    private String city;

    @Column
    private String country;

    @Column
    private String website;

    @Column
    private String linkedinUrl;

    @Column
    private String githubUrl;

    @Convert(converter = in.project.main.entities.converters.InstructorStatusConverter.class)
    @Column(nullable = false)
    private InstructorStatus status = InstructorStatus.ACTIVE;

    @Convert(converter = in.project.main.entities.converters.VerificationStatusConverter.class)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.VERIFIED;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.name == null && (this.firstName != null || this.lastName != null)) {
            this.name = ((this.firstName != null ? this.firstName : "") + " " + 
                         (this.lastName != null ? this.lastName : "")).trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.firstName != null || this.lastName != null) {
            this.name = ((this.firstName != null ? this.firstName : "") + " " + 
                         (this.lastName != null ? this.lastName : "")).trim();
        }
    }

    // Helper method for initials
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) {
            return "IN";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        }
        return parts[0].toUpperCase();
    }

    // Helper to get skills as list of strings
    public List<String> getSkillList() {
        if (skills == null || skills.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

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

    public InstructorStatus getStatus() { 
        return status != null ? status : InstructorStatus.ACTIVE; 
    }
    public void setStatus(InstructorStatus status) { 
        this.status = status != null ? status : InstructorStatus.ACTIVE; 
    }

    public VerificationStatus getVerificationStatus() { 
        return verificationStatus != null ? verificationStatus : VerificationStatus.VERIFIED; 
    }
    public void setVerificationStatus(VerificationStatus verificationStatus) { 
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.VERIFIED; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
