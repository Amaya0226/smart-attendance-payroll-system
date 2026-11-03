
package com.smartattendance.smart_attendance_payroll_system.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RoleBasedAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        String selectedRole = request.getParameter("role");

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isEmployee = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_EMPLOYEE".equals(authority.getAuthority()));

        if ("ADMIN".equalsIgnoreCase(selectedRole) && isAdmin) {

            response.sendRedirect(
                    request.getContextPath() + "/dashboard");

        } else if ("EMPLOYEE".equalsIgnoreCase(selectedRole)
                && isEmployee) {

            response.sendRedirect(
                    request.getContextPath() + "/employee/dashboard");

        } else {

            response.sendRedirect(
                    request.getContextPath() + "/login?error=true");
        }
    }
}
