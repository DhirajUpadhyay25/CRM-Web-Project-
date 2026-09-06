package in.project.main.controllers;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Media;
import in.project.main.entities.User;
import in.project.main.repositories.UserRepository;
import in.project.main.services.MediaService;

@Controller
@RequestMapping("/admin/media")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('media.view') or hasAuthority('content.view')")
public class AdminMediaController {

    private static final Logger logger = LoggerFactory.getLogger(AdminMediaController.class);

    @Autowired
    private MediaService mediaService;

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // Media Library List & Grid Views
    // ==========================================

    @GetMapping
    public String list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "folder", required = false) String folder,
            @RequestParam(name = "typeCategory", required = false) String typeCategory,
            @RequestParam(name = "usageType", required = false) String usageType,
            @RequestParam(name = "viewMode", defaultValue = "grid") String viewMode,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "24") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            Model model) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Media> mediaPage = mediaService.getMedia(search, folder, typeCategory, usageType, pageable);

        model.addAttribute("mediaPage", mediaPage);
        model.addAttribute("items", mediaPage.getContent());
        model.addAttribute("kpis", mediaService.getMetrics());
        model.addAttribute("folders", mediaService.getAllFolders());

        // Retain view states & filters
        model.addAttribute("search", search);
        model.addAttribute("folder", folder);
        model.addAttribute("typeCategory", typeCategory);
        model.addAttribute("usageType", usageType);
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "admin/content/media/list";
    }

    // ==========================================
    // Upload & Import Endpoints
    // ==========================================

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("folders", mediaService.getAllFolders());
        return "admin/content/media/upload";
    }

    @PostMapping("/upload")
    public String handleUpload(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam(name = "folder", required = false, defaultValue = "general") String folder,
            @RequestParam(name = "usageType", required = false, defaultValue = "GENERAL") String usageType,
            @RequestParam(name = "altText", required = false) String altText,
            @RequestParam(name = "caption", required = false) String caption,
            Principal principal,
            RedirectAttributes ra) {

        try {
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User u = userRepository.findByEmail(principal.getName());
                if (u != null) actorId = u.getId();
            }

            if (files == null || files.isEmpty()) {
                ra.addFlashAttribute("error", "No files selected for upload.");
                return "redirect:/admin/media/upload";
            }

            if (files.size() == 1) {
                Media m = mediaService.uploadFile(files.get(0), folder, usageType, altText, caption, actorId, actorEmail);
                ra.addFlashAttribute("success", "File '" + m.getFileName() + "' uploaded successfully.");
                return "redirect:/admin/media/" + m.getId();
            } else {
                List<Media> uploaded = mediaService.uploadMultipleFiles(files, folder, usageType, actorId, actorEmail);
                ra.addFlashAttribute("success", "Successfully uploaded " + uploaded.size() + " files to folder /" + folder + ".");
                return "redirect:/admin/media";
            }
        } catch (Exception e) {
            logger.error("Upload failed: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to upload file: " + e.getMessage());
            return "redirect:/admin/media/upload";
        }
    }

    @PostMapping("/import-url")
    public String importUrl(
            @RequestParam("url") String url,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "folder", required = false, defaultValue = "external") String folder,
            @RequestParam(name = "usageType", required = false, defaultValue = "EXTERNAL") String usageType,
            Principal principal,
            RedirectAttributes ra) {

        try {
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User u = userRepository.findByEmail(principal.getName());
                if (u != null) actorId = u.getId();
            }

            Media m = mediaService.importExternalUrl(url, title, folder, usageType, actorId, actorEmail);
            ra.addFlashAttribute("success", "External media URL imported successfully.");
            return "redirect:/admin/media/" + m.getId();
        } catch (Exception e) {
            logger.error("Import URL failed: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to import external URL: " + e.getMessage());
            return "redirect:/admin/media";
        }
    }

    // ==========================================
    // Detail & Edit Views
    // ==========================================

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        return mediaService.getMediaById(id)
                .map(media -> {
                    model.addAttribute("media", media);
                    model.addAttribute("folders", mediaService.getAllFolders());
                    return "admin/content/media/detail";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Media not found with ID " + id);
                    return "redirect:/admin/media";
                });
    }

    @PostMapping("/{id}/edit")
    public String updateMetadata(
            @PathVariable("id") Long id,
            @RequestParam(name = "altText", required = false) String altText,
            @RequestParam(name = "caption", required = false) String caption,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "folder", required = false) String folder,
            @RequestParam(name = "usageType", required = false) String usageType,
            Principal principal,
            RedirectAttributes ra) {

        try {
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User u = userRepository.findByEmail(principal.getName());
                if (u != null) actorId = u.getId();
            }

            Media updated = mediaService.updateMetadata(id, altText, caption, description, folder, usageType, actorId, actorEmail);
            ra.addFlashAttribute("success", "Metadata for '" + updated.getFileName() + "' updated successfully.");
        } catch (Exception e) {
            logger.error("Failed to update media #{}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Error updating metadata: " + e.getMessage());
        }
        return "redirect:/admin/media/" + id;
    }

    // ==========================================
    // Delete & Disk Sync
    // ==========================================

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long id,
            @RequestParam(name = "deletePhysical", defaultValue = "true") boolean deletePhysical,
            Principal principal,
            RedirectAttributes ra) {

        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            mediaService.deleteMedia(id, deletePhysical, actorEmail);
            ra.addFlashAttribute("success", "Media asset #" + id + " deleted successfully.");
        } catch (Exception e) {
            logger.error("Failed to delete media #{}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to delete media: " + e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/sync-disk")
    public String syncDisk(Principal principal, RedirectAttributes ra) {
        try {
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User u = userRepository.findByEmail(principal.getName());
                if (u != null) actorId = u.getId();
            }

            int count = mediaService.syncDiskAssets(actorId, actorEmail);
            if (count > 0) {
                ra.addFlashAttribute("success", "Successfully synchronized " + count + " media files from static upload storage!");
            } else {
                ra.addFlashAttribute("success", "All disk assets are already indexed in the Media Library.");
            }
        } catch (Exception e) {
            logger.error("Disk sync failed: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to sync disk assets: " + e.getMessage());
        }
        return "redirect:/admin/media";
    }

    // ==========================================
    // Direct Download Endpoint
    // ==========================================

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadMedia(@PathVariable("id") Long id) {
        return mediaService.getMediaById(id)
                .map(media -> {
                    Resource resource = mediaService.loadAsResource(media);
                    if (resource != null && resource.exists()) {
                        String mime = media.getMimeType() != null ? media.getMimeType() : "application/octet-stream";
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(mime))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getEffectiveAltText() + "\"")
                                .body(resource);
                    }
                    return ResponseEntity.notFound().<Resource>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ==========================================
    // REST API for Modal Asset Pickers
    // ==========================================

    @GetMapping(value = "/api/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> apiList(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "folder", required = false) String folder,
            @RequestParam(name = "typeCategory", required = false) String typeCategory,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("createdAt").descending());
        Page<Media> mediaPage = mediaService.getMedia(search, folder, typeCategory, null, pageable);

        List<Map<String, Object>> items = mediaPage.getContent().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("fileName", m.getFileName());
            map.put("originalName", m.getOriginalName());
            map.put("publicUrl", m.getEffectiveUrl());
            map.put("mimeType", m.getMimeType());
            map.put("fileSizeFormatted", m.getFileSizeFormatted());
            map.put("isImage", m.isImage());
            map.put("width", m.getWidth());
            map.put("height", m.getHeight());
            map.put("altText", m.getEffectiveAltText());
            map.put("folder", m.getFolderSafe());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("totalPages", mediaPage.getTotalPages());
        response.put("totalElements", mediaPage.getTotalElements());
        response.put("currentPage", mediaPage.getNumber());
        return response;
    }
}
