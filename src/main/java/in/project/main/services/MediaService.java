package in.project.main.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.project.main.entities.Media;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.MediaRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    public static class MediaMetrics {
        private final long totalAssets;
        private final String totalStorageFormatted;
        private final long imageCount;
        private final long documentCount;
        private final long videoCount;
        private final long folderCount;

        public MediaMetrics(long totalAssets, String totalStorageFormatted, long imageCount, long documentCount, long videoCount, long folderCount) {
            this.totalAssets = totalAssets;
            this.totalStorageFormatted = totalStorageFormatted;
            this.imageCount = imageCount;
            this.documentCount = documentCount;
            this.videoCount = videoCount;
            this.folderCount = folderCount;
        }

        public long getTotalAssets() { return totalAssets; }
        public String getTotalStorageFormatted() { return totalStorageFormatted; }
        public long getImageCount() { return imageCount; }
        public long getDocumentCount() { return documentCount; }
        public long getVideoCount() { return videoCount; }
        public long getFolderCount() { return folderCount; }
    }

    // ==========================================
    // Querying & Filter Specifications
    // ==========================================

    public Page<Media> getMedia(String search, String folder, String typeCategory, String usageType, Pageable pageable) {
        Specification<Media> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate matchFileName = cb.like(cb.lower(root.get("fileName")), pattern);
                Predicate matchOriginalName = cb.like(cb.lower(root.get("originalName")), pattern);
                Predicate matchAltText = cb.like(cb.lower(root.get("altText")), pattern);
                Predicate matchCaption = cb.like(cb.lower(root.get("caption")), pattern);
                Predicate matchDescription = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(matchFileName, matchOriginalName, matchAltText, matchCaption, matchDescription));
            }

            if (folder != null && !folder.trim().isEmpty() && !"ALL".equalsIgnoreCase(folder)) {
                predicates.add(cb.equal(cb.lower(root.get("folder")), folder.trim().toLowerCase()));
            }

            if (usageType != null && !usageType.trim().isEmpty() && !"ALL".equalsIgnoreCase(usageType)) {
                predicates.add(cb.equal(cb.lower(root.get("usageType")), usageType.trim().toLowerCase()));
            }

            if (typeCategory != null && !typeCategory.trim().isEmpty() && !"ALL".equalsIgnoreCase(typeCategory)) {
                String cat = typeCategory.trim().toUpperCase();
                switch (cat) {
                    case "IMAGE":
                    case "IMAGES":
                        Predicate mimeImage = cb.like(cb.lower(root.get("mimeType")), "image/%");
                        Predicate extImage = root.get("extension").in(Arrays.asList("jpg", "jpeg", "png", "webp", "gif", "svg"));
                        predicates.add(cb.or(mimeImage, extImage));
                        break;
                    case "VIDEO":
                    case "VIDEOS":
                        Predicate mimeVideo = cb.like(cb.lower(root.get("mimeType")), "video/%");
                        Predicate extVideo = root.get("extension").in(Arrays.asList("mp4", "webm", "mov", "avi"));
                        predicates.add(cb.or(mimeVideo, extVideo));
                        break;
                    case "DOCUMENT":
                    case "DOCUMENTS":
                    case "PDF":
                        Predicate mimePdf = cb.equal(root.get("mimeType"), "application/pdf");
                        Predicate mimeDoc = cb.like(cb.lower(root.get("mimeType")), "%document%");
                        Predicate extDoc = root.get("extension").in(Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip"));
                        predicates.add(cb.or(mimePdf, mimeDoc, extDoc));
                        break;
                    case "AUDIO":
                        Predicate mimeAudio = cb.like(cb.lower(root.get("mimeType")), "audio/%");
                        Predicate extAudio = root.get("extension").in(Arrays.asList("mp3", "wav", "ogg", "m4a"));
                        predicates.add(cb.or(mimeAudio, extAudio));
                        break;
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return mediaRepository.findAll(spec, pageable);
    }

    public MediaMetrics getMetrics() {
        long total = mediaRepository.count();
        long images = mediaRepository.countByMimeTypeStartingWith("image/");
        long docs = mediaRepository.countDocuments();
        long videos = mediaRepository.countVideos();
        List<String> folders = getAllFolders();
        long folderCount = folders.size();

        // Calculate total storage
        long totalBytes = 0;
        List<Media> all = mediaRepository.findAll();
        for (Media m : all) {
            if (m.getFileSize() != null && m.getFileSize() > 0) {
                totalBytes += m.getFileSize();
            }
        }

        String formattedStorage;
        if (totalBytes < 1024) formattedStorage = totalBytes + " B";
        else if (totalBytes < 1024 * 1024) formattedStorage = String.format("%.1f KB", totalBytes / 1024.0);
        else if (totalBytes < 1024 * 1024 * 1024) formattedStorage = String.format("%.1f MB", totalBytes / (1024.0 * 1024.0));
        else formattedStorage = String.format("%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0));

        return new MediaMetrics(total, formattedStorage, images, docs, videos, folderCount);
    }

    public List<String> getAllFolders() {
        List<String> folders = mediaRepository.findAllFolders();
        List<String> result = new ArrayList<>();
        result.add("general");
        result.add("courses");
        result.add("blogs");
        result.add("testimonials");
        result.add("banners");
        result.add("avatars");
        result.add("documents");
        if (folders != null) {
            for (String f : folders) {
                if (f != null && !f.isBlank() && !result.contains(f.toLowerCase())) {
                    result.add(f.toLowerCase());
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    public Optional<Media> getMediaById(Long id) {
        return mediaRepository.findById(id);
    }

    // ==========================================
    // File Upload & Storage Management
    // ==========================================

    @Transactional
    public Media uploadFile(MultipartFile file, String folder, String usageType, String altText, String caption, Long actorId, String actorEmail) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided for upload.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "upload_" + System.currentTimeMillis();
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex + 1).toLowerCase();
        }

        String baseName = (dotIndex > 0) ? originalName.substring(0, dotIndex) : originalName;
        String cleanBase = baseName.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (cleanBase.length() > 40) cleanBase = cleanBase.substring(0, 40);

        String uniqueFileName = cleanBase + "_" + System.currentTimeMillis() + (extension.isEmpty() ? "" : "." + extension);

        // Resolve MIME Type
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank() || mimeType.equals("application/octet-stream")) {
            mimeType = URLConnection.guessContentTypeFromName(uniqueFileName);
            if (mimeType == null) mimeType = "application/octet-stream";
        }

        // Save physical files to disk
        String projectRoot = System.getProperty("user.dir");
        Path staticUploadsPath = Paths.get(projectRoot, "src", "main", "resources", "static", "uploads");
        Path targetUploadsPath = Paths.get(projectRoot, "target", "classes", "static", "uploads");
        Path uploadDirPath = Paths.get(projectRoot, "upload");

        Files.createDirectories(staticUploadsPath);
        Files.createDirectories(uploadDirPath);
        if (Files.exists(Paths.get(projectRoot, "target", "classes"))) {
            Files.createDirectories(targetUploadsPath);
        }

        Path destStatic = staticUploadsPath.resolve(uniqueFileName);
        Path destUpload = uploadDirPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), destStatic, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(destStatic, destUpload, StandardCopyOption.REPLACE_EXISTING);

        if (Files.exists(targetUploadsPath)) {
            Path destTarget = targetUploadsPath.resolve(uniqueFileName);
            Files.copy(destStatic, destTarget, StandardCopyOption.REPLACE_EXISTING);
        }

        // Extract Dimensions if image
        Integer width = null;
        Integer height = null;
        if (mimeType.startsWith("image/") && !mimeType.contains("svg")) {
            try {
                BufferedImage bimg = ImageIO.read(destStatic.toFile());
                if (bimg != null) {
                    width = bimg.getWidth();
                    height = bimg.getHeight();
                }
            } catch (Exception e) {
                logger.warn("Could not read image dimensions for {}: {}", uniqueFileName, e.getMessage());
            }
        }

        Media media = new Media();
        media.setFileName(uniqueFileName);
        media.setOriginalName(originalName);
        media.setFileUrl("/uploads/" + uniqueFileName);
        media.setPublicUrl("/uploads/" + uniqueFileName);
        media.setThumbnailUrl("/uploads/" + uniqueFileName);
        media.setStoragePath(destStatic.toString());
        media.setMimeType(mimeType);
        media.setExtension(extension);
        media.setFileSize(file.getSize());
        media.setSize(media.getFileSizeFormatted());
        media.setFileType(mimeType);
        media.setWidth(width);
        media.setHeight(height);
        media.setFolder((folder != null && !folder.isBlank()) ? folder.toLowerCase().trim() : "general");
        media.setUsageType((usageType != null && !usageType.isBlank()) ? usageType.toUpperCase().trim() : "GENERAL");
        media.setAltText((altText != null && !altText.isBlank()) ? altText.trim() : originalName);
        media.setCaption(caption);
        media.setUploadedBy(actorId);
        media.setUploadedByEmail(actorEmail != null ? actorEmail : "admin@edutake.com");

        Media saved = mediaRepository.save(media);

        logAudit(actorEmail, AuditEventType.MEDIA_UPLOADED, "MEDIA_UPLOAD", "Uploaded media asset '" + saved.getFileName() + "' (" + saved.getFileSizeFormatted() + ") to folder /" + saved.getFolder(), saved.getId(), saved.getFileName());

        return saved;
    }

    @Transactional
    public List<Media> uploadMultipleFiles(List<MultipartFile> files, String folder, String usageType, Long actorId, String actorEmail) {
        List<Media> results = new ArrayList<>();
        if (files == null || files.isEmpty()) return results;

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    Media m = uploadFile(file, folder, usageType, null, null, actorId, actorEmail);
                    results.add(m);
                } catch (Exception e) {
                    logger.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                }
            }
        }
        return results;
    }

    @Transactional
    public Media importExternalUrl(String url, String title, String folder, String usageType, Long actorId, String actorEmail) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("External asset URL cannot be empty.");
        }

        String cleanUrl = url.trim();
        String fileName = title != null && !title.isBlank() ? title.trim() : "External Asset";
        String extension = "";
        int dot = cleanUrl.lastIndexOf('.');
        if (dot > 0 && dot < cleanUrl.length() - 1) {
            int queryIdx = cleanUrl.indexOf('?', dot);
            extension = (queryIdx > 0 ? cleanUrl.substring(dot + 1, queryIdx) : cleanUrl.substring(dot + 1)).toLowerCase();
        }

        String mimeType = URLConnection.guessContentTypeFromName("file." + extension);
        if (mimeType == null) {
            if (extension.matches("jpg|jpeg|png|webp|gif|svg")) mimeType = "image/" + extension;
            else if (extension.matches("mp4|webm|mov")) mimeType = "video/" + extension;
            else if (extension.equals("pdf")) mimeType = "application/pdf";
            else mimeType = "application/octet-stream";
        }

        Media media = new Media();
        media.setFileName(fileName);
        media.setOriginalName(fileName);
        media.setFileUrl(cleanUrl);
        media.setPublicUrl(cleanUrl);
        media.setThumbnailUrl(cleanUrl);
        media.setMimeType(mimeType);
        media.setFileType(mimeType);
        media.setExtension(extension);
        media.setFileSize(0L);
        media.setSize("External");
        media.setFolder((folder != null && !folder.isBlank()) ? folder.toLowerCase().trim() : "external");
        media.setUsageType((usageType != null && !usageType.isBlank()) ? usageType.toUpperCase().trim() : "EXTERNAL");
        media.setAltText(fileName);
        media.setUploadedBy(actorId);
        media.setUploadedByEmail(actorEmail != null ? actorEmail : "admin@edutake.com");

        Media saved = mediaRepository.save(media);

        logAudit(actorEmail, AuditEventType.MEDIA_UPLOADED, "MEDIA_IMPORT", "Registered external media URL: " + cleanUrl, saved.getId(), saved.getFileName());

        return saved;
    }

    @Transactional
    public Media updateMetadata(Long id, String altText, String caption, String description, String folder, String usageType, Long actorId, String actorEmail) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Media not found with ID: " + id));

        media.setAltText(altText);
        media.setCaption(caption);
        media.setDescription(description);
        if (folder != null && !folder.isBlank()) media.setFolder(folder.toLowerCase().trim());
        if (usageType != null && !usageType.isBlank()) media.setUsageType(usageType.toUpperCase().trim());

        Media updated = mediaRepository.save(media);

        logAudit(actorEmail, AuditEventType.MEDIA_UPDATED, "MEDIA_UPDATE", "Updated metadata for media asset #" + updated.getId(), updated.getId(), updated.getFileName());

        return updated;
    }

    @Transactional
    public void deleteMedia(Long id, boolean deletePhysicalFile, String actorEmail) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Media not found with ID: " + id));

        String fileName = media.getFileName();

        if (deletePhysicalFile && media.getPublicUrl() != null && media.getPublicUrl().startsWith("/uploads/")) {
            try {
                String projectRoot = System.getProperty("user.dir");
                Path p1 = Paths.get(projectRoot, "src", "main", "resources", "static", "uploads", fileName);
                Path p2 = Paths.get(projectRoot, "upload", fileName);
                Path p3 = Paths.get(projectRoot, "target", "classes", "static", "uploads", fileName);

                Files.deleteIfExists(p1);
                Files.deleteIfExists(p2);
                Files.deleteIfExists(p3);
            } catch (Exception e) {
                logger.warn("Could not delete physical files for {}: {}", fileName, e.getMessage());
            }
        }

        mediaRepository.delete(media);

        logAudit(actorEmail, AuditEventType.MEDIA_DELETED, "MEDIA_DELETE", "Deleted media asset '" + fileName + "' (Physical delete: " + deletePhysicalFile + ")", id, fileName);
    }

    // ==========================================
    // Disk Sync Scanner & Auto-Seeder
    // ==========================================

    @Transactional
    public int syncDiskAssets(Long actorId, String actorEmail) {
        String projectRoot = System.getProperty("user.dir");
        Path staticUploadsPath = Paths.get(projectRoot, "src", "main", "resources", "static", "uploads");

        if (!Files.exists(staticUploadsPath)) {
            return 0;
        }

        File folder = staticUploadsPath.toFile();
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) return 0;

        int indexedCount = 0;
        for (File f : files) {
            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.startsWith(".") || fileName.isBlank()) continue;

                String publicUrl = "/uploads/" + fileName;
                if (!mediaRepository.existsByFileName(fileName) && !mediaRepository.existsByPublicUrl(publicUrl)) {
                    try {
                        String ext = "";
                        int dot = fileName.lastIndexOf('.');
                        if (dot > 0 && dot < fileName.length() - 1) {
                            ext = fileName.substring(dot + 1).toLowerCase();
                        }

                        String mimeType = URLConnection.guessContentTypeFromName(fileName);
                        if (mimeType == null) {
                            if (ext.matches("jpg|jpeg|png|webp|gif|svg")) mimeType = "image/" + ext;
                            else if (ext.equals("pdf")) mimeType = "application/pdf";
                            else mimeType = "application/octet-stream";
                        }

                        Integer width = null;
                        Integer height = null;
                        if (mimeType.startsWith("image/") && !mimeType.contains("svg")) {
                            try {
                                BufferedImage bimg = ImageIO.read(f);
                                if (bimg != null) {
                                    width = bimg.getWidth();
                                    height = bimg.getHeight();
                                }
                            } catch (Exception ignored) {}
                        }

                        Media m = new Media();
                        m.setFileName(fileName);
                        m.setOriginalName(fileName);
                        m.setFileUrl(publicUrl);
                        m.setPublicUrl(publicUrl);
                        m.setThumbnailUrl(publicUrl);
                        m.setStoragePath(f.getAbsolutePath());
                        m.setMimeType(mimeType);
                        m.setFileType(mimeType);
                        m.setExtension(ext);
                        m.setFileSize(f.length());
                        m.setSize(m.getFileSizeFormatted());
                        m.setWidth(width);
                        m.setHeight(height);
                        m.setFolder("uploads");
                        m.setUsageType("AUTO_INDEXED");
                        m.setAltText(fileName);
                        m.setUploadedBy(actorId);
                        m.setUploadedByEmail(actorEmail != null ? actorEmail : "system@edutake.com");

                        mediaRepository.save(m);
                        indexedCount++;
                    } catch (Exception e) {
                        logger.error("Error indexing disk file {}: {}", fileName, e.getMessage());
                    }
                }
            }
        }

        if (indexedCount > 0) {
            logAudit(actorEmail, AuditEventType.MEDIA_SYNCED, "DISK_SYNC", "Synchronized and indexed " + indexedCount + " existing media assets from static uploads directory.", 0L, "DiskSync");
        }

        return indexedCount;
    }

    public Resource loadAsResource(Media media) {
        if (media == null) return null;
        String projectRoot = System.getProperty("user.dir");

        if (media.getFileName() != null) {
            Path p1 = Paths.get(projectRoot, "src", "main", "resources", "static", "uploads", media.getFileName());
            if (Files.exists(p1)) return new FileSystemResource(p1);

            Path p2 = Paths.get(projectRoot, "upload", media.getFileName());
            if (Files.exists(p2)) return new FileSystemResource(p2);

            Path p3 = Paths.get(projectRoot, "target", "classes", "static", "uploads", media.getFileName());
            if (Files.exists(p3)) return new FileSystemResource(p3);
        }

        if (media.getStoragePath() != null) {
            Path p = Paths.get(media.getStoragePath());
            if (Files.exists(p)) return new FileSystemResource(p);
        }

        return null;
    }

    private void logAudit(String actorEmail, AuditEventType eventType, String action, String description, Long entityId, String entityName) {
        if (auditLogService == null) return;
        try {
            PlatformAuditEvent event = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "SYSTEM",
                    eventType != null ? eventType : AuditEventType.MEDIA_UPDATED,
                    action,
                    description
            )
            .withCategory(AuditCategory.SYSTEM)
            .withEntity("MEDIA", entityId != null ? String.valueOf(entityId) : null, entityName)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(event);
        } catch (Exception e) {
            logger.debug("Failed to record audit log for media: {}", e.getMessage());
        }
    }
}
