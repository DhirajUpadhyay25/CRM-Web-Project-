package in.project.main.config;

import java.io.File;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageConfig implements WebMvcConfigurer
{
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        String uploadDir = System.getProperty("user.dir") + File.separator + "upload" + File.separator;
        String uploadsStaticDir = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "uploads" + File.separator;

        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadDir, "file:" + uploadsStaticDir, "classpath:/static/uploads/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsStaticDir, "file:" + uploadDir, "classpath:/static/uploads/");
    }
}