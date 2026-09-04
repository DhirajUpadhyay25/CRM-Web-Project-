package in.project.main.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_type", columnList = "mimeType"),
    @Index(name = "idx_media_usage", columnList = "usageType"),
    @Index(name = "idx_media_uploaded_by", columnList = "uploadedBy"),
    @Index(name = "idx_media_created", columnList = "createdAt")
})
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String originalName;

    // Legacy fields kept for backward compatibility
    @Column
    private String fileUrl;

    @Column
    private String fileType;

    @Column
    private String size;

    @Column(length = 128)
    private String mimeType;

    @Column(length = 16)
    private String extension;

    @Column
    private Long fileSize;

    @Column(length = 500)
    private String storagePath;

    @Column(length = 500)
    private String publicUrl;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 255)
    private String altText;

    @Column(length = 500)
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String folder;

    @Column
    private Long uploadedBy;

    @Column(length = 255)
    private String uploadedByEmail;

    @Column(length = 64)
    private String usageType;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Convenience Methods ---
    public String getEffectiveUrl() {
        if (publicUrl != null && !publicUrl.isBlank()) return publicUrl;
        return fileUrl;
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return size != null ? size : "Unknown";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    public boolean isImage() {
        if (mimeType != null) return mimeType.startsWith("image/");
        if (extension != null) {
            String ext = extension.toLowerCase();
            return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp") || ext.equals("svg");
        }
        return false;
    }

    public boolean isVideo() {
        if (mimeType != null) return mimeType.startsWith("video/");
        if (extension != null) {
            String ext = extension.toLowerCase();
            return ext.equals("mp4") || ext.equals("webm") || ext.equals("avi") || ext.equals("mov");
        }
        return false;
    }

    public boolean isPdf() {
        if (mimeType != null) return "application/pdf".equals(mimeType);
        return extension != null && extension.equalsIgnoreCase("pdf");
    }

    public boolean isDocument() {
        if (mimeType != null) {
            return mimeType.contains("word") || mimeType.contains("spreadsheet") || mimeType.contains("presentation")
                || mimeType.contains("msword") || mimeType.contains("excel") || mimeType.contains("powerpoint");
        }
        if (extension != null) {
            String ext = extension.toLowerCase();
            return ext.equals("doc") || ext.equals("docx") || ext.equals("xls") || ext.equals("xlsx") || ext.equals("ppt") || ext.equals("pptx");
        }
        return false;
    }

    public String getIconClass() {
        if (isImage()) return "bi-file-earmark-image";
        if (isVideo()) return "bi-file-earmark-play";
        if (isPdf()) return "bi-file-earmark-pdf";
        if (isDocument()) return "bi-file-earmark-word";
        return "bi-file-earmark";
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getUploadedByEmail() { return uploadedByEmail; }
    public void setUploadedByEmail(String uploadedByEmail) { this.uploadedByEmail = uploadedByEmail; }

    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
