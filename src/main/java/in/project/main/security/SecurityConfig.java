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

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private CustomLogoutSuccessHandler customLogoutSuccessHandler;

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
                // Student-specific Enrollment Routes (must come before generic /courses/** permitAll)
                .requestMatchers("/courses/free-enroll").hasRole("STUDENT")

                // Purchase APIs. Only a signed-in student can start or settle a payment, so
                // ownership checks inside OrdersApi always have a real principal to compare
                // against. These must stay ahead of the public matchers below.
                .requestMatchers(
                    "/api/createOrder", "/api/verifyPayment", "/api/failOrder"
                ).hasRole("STUDENT")

                // Public Routes
                .requestMatchers(
                    "/", "/index", "/login", "/register", "/regForm", "/health",
                    "/courses", "/courses/**", "/services", "/about", "/contact", "/faq",
                    "/css/**", "/js/**", "/images/**", "/upload/**", "/uploads/**",
                    "/error", "/error/**"
                ).permitAll()

                // Admin Routes
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Instructor Routes
                .requestMatchers("/instructor/**").hasRole("INSTRUCTOR")

                // Employee Routes
                .requestMatchers(
                    "/employeeProfile", "/sellCourse", "/sellCourseForm",
                    "/inquiryManagement", "/newInquiry", "/submitInquiryForm", "/followUps",
                    "/api/searchInquiries", "/api/myFollowUps"
                ).hasAnyRole("EMPLOYEE", "ADMIN")

                // Student Routes
                .requestMatchers(
                    "/userProfile", "/updateUserProfile", "/myCourses", "/provideFeedback", "/feedbackForm"
                ).hasAnyRole("STUDENT", "ADMIN")
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
        // CSRF stays enabled for every route, including the payment APIs. The checkout page
        // sends the token on its AJAX calls, so no endpoint needs an exemption.

        return http.build();
    }
}
