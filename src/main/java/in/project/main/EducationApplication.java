package in.project.main;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import in.project.main.services.DataSeederService;

@SpringBootApplication
public class EducationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EducationApplication.class, args);
	}

	@Bean
	CommandLineRunner seedData(DataSeederService dataSeederService) {
		return args -> {
			try {
				dataSeederService.seedAll();
				System.out.println("=== Data seeding completed ===");
			} catch (Exception e) {
				System.err.println("Data seeding failed: " + e.getMessage());
			}
		};
	}

}
