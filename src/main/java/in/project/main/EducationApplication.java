package in.project.main;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import in.project.main.services.DataSeederService;

@SpringBootApplication
public class EducationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EducationApplication.class, args);
	}

	/**
	 * Seeds demo data at startup.
	 *
	 * This used to run unconditionally on every boot, which meant any database the
	 * application was pointed at accumulated generated courses, enrollments, orders and
	 * certificates. It is now off unless app.seed.on-startup=true, and unavailable outside
	 * the dev profile.
	 */
	@Bean
	@Profile("dev")
	@ConditionalOnProperty(name = "app.seed.on-startup", havingValue = "true")
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
