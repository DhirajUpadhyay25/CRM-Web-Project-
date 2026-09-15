package in.project.main.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ImageConfig.class);

    @Value("${app.upload.dir:upload/}")
    private String configuredUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(baseDir, configuredUploadDir);
        Path coursesUploadPath = uploadPath.resolve("courses");
        Path instructorsUploadPath = uploadPath.resolve("instructors");
        Path usersUploadPath = uploadPath.resolve("users");

        try {
            Files.createDirectories(uploadPath);
            Files.createDirectories(coursesUploadPath);
            Files.createDirectories(instructorsUploadPath);
            Files.createDirectories(usersUploadPath);
        } catch (Exception e) {
            log.warn("Could not pre-create upload directories: {}", e.getMessage());
        }

        String uploadLocation = "file:" + uploadPath.toAbsolutePath().toString().replace("\\", "/") + "/";
        String uploadsStaticDir = "file:" + Paths.get(baseDir, "src", "main", "resources", "static", "uploads").toAbsolutePath().toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/upload/**")
                .addResourceLocations(uploadLocation, uploadsStaticDir, "classpath:/static/uploads/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation, uploadsStaticDir, "classpath:/static/uploads/");
    }
}