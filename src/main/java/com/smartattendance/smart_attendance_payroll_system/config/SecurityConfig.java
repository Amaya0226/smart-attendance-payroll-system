
package com.smartattendance.smart_attendance_payroll_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ComponentScan(basePackages = {
    "com.smartattendance.smart_attendance_payroll_system"
})
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RoleBasedAuthenticationSuccessHandler successHandler)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Public pages and static resources
                .requestMatchers(
                    "/",
                    "/home",
                    "/login",
                    "/forgot-password",
                    "/reset-password",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // Admin-only pages
                .requestMatchers(
                    "/dashboard",
                    "/dashboard/**",
                    "/admin/**",
                    "/employees",
                    "/employees/**",
                    "/attendance",
                    "/attendance/**",
                    "/payroll",
                    "/payroll/**",
                    "/payroll-rules",
                    "/payroll-rules/**",
                    "/holidays",
                    "/holidays/**",
                    "/overtime",
                    "/overtime/**",
                    "/qr-attendance",
                    "/qr-attendance/**",
                    "/salary-revisions",
                    "/salary-revisions/**"
                ).hasRole("ADMIN")

                // Employee-only pages
                .requestMatchers("/employee/**")
                .hasRole("EMPLOYEE")

                // Other authenticated endpoints
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
