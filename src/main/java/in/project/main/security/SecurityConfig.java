package in.project.main.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Autowired
    private MaintenanceModeFilter maintenanceModeFilter;

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
            .addFilterBefore(maintenanceModeFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                // Student-specific Enrollment Routes (must come before generic /courses/** permitAll)
                .requestMatchers("/courses/free-enroll").hasRole("STUDENT")

                // Purchase APIs. Only a signed-in student can start or settle a payment
                .requestMatchers(
                    "/api/createOrder", "/api/verifyPayment", "/api/failOrder"
                ).hasRole("STUDENT")

                // Public Routes
                .requestMatchers(
                    "/", "/index", "/login", "/register", "/regForm", "/health", "/maintenance",
                    "/courses", "/courses/**", "/services", "/about", "/contact", "/faq",
                    "/api/coupons/validate",
                    "/css/**", "/js/**", "/images/**", "/upload/**", "/uploads/**",
                    "/error", "/error/**"
                ).permitAll()

                // Admin Routes
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Instructor Routes
                .requestMatchers("/instructor/**").hasRole("INSTRUCTOR")

                // Employee Routes
                .requestMatchers(
                    "/employeeProfile", "/sellCourse", "/sellCourseForm",
                    "/inquiryManagement", "/newInquiry", "/submitInquiryForm", "/followUps",
                    "/api/searchInquiries", "/api/myFollowUps"
                ).hasAnyRole("EMPLOYEE", "ADMIN", "SUPER_ADMIN", "STAFF")

                // Student Routes
                .requestMatchers(
                    "/userProfile", "/updateUserProfile", "/myCourses",
                    "/provideFeedback", "/feedbackForm",
                    "/student/feedback/**"
                ).hasAnyRole("STUDENT", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/student/**").hasRole("STUDENT")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/loginForm")
                .usernameParameter("email")
                .successHandler(customAuthenticationSuccessHandler)
                .failureHandler(customAuthenticationFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedHandler(customAccessDeniedHandler)
                .accessDeniedPage("/403")
            );

        return http.build();
    }
}
