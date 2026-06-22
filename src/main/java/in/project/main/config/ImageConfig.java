package in.project.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageConfig implements WebMvcConfigurer
{

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {

        String path = System.getProperty("user.dir") + "/upload/";

        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + path);
    }
}