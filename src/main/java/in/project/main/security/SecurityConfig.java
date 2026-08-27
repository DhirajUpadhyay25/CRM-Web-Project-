package in.project.main.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyPlaintextDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
            .authorizeHttpRequests(authz -> authz
                // Public Routes
                .requestMatchers(
                    "/", "/index", "/login", "/register", "/regForm", 
                    "/api/createOrder", "/api/verifyPayment", 
                    "/css/**", "/js/**", "/images/**", "/upload/**", "/uploads/**",
                    "/error"
                ).permitAll()
                
                // Admin Routes
                .requestMatchers(
                    "/adminProfile", "/courseManagement", "/employeeManagement", 
                    "/customerManagement", "/sales", "/adminFeedback", 
                    "/addCourseForm", "/updateCourse", "/deleteCourse"
                ).hasRole("ADMIN")
                
                // Employee Routes
                .requestMatchers(
                    "/employeeProfile", "/sellCourse", "/inquiryManagement", 
                    "/followUps", "/sellCourseForm", "/addInquiryForm"
                ).hasRole("EMPLOYEE")
                
                // Student Routes
                .requestMatchers(
                    "/userProfile", "/myCourses", "/provideFeedback", "/feedbackForm"
                ).hasRole("STUDENT")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/loginForm")
                .usernameParameter("email")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/403")
            )
            // Note: CSRF is enabled by default. Thymeleaf forms will automatically include the token.
            // If the Razorpay AJAX requests fail due to CSRF, we can ignore them, but let's test first.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/createOrder", "/api/verifyPayment")
            );

        return http.build();
    }
}
